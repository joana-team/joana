#include "graph.pb.h"
#include "lib/concurrentqueue.h"
#include "lib/fmt/include/fmt/core.h"
#include "lib/fmt/include/fmt/ostream.h"
#include "lib/fmt/include/fmt/ranges.h"
#include "worklist.hpp"
#include <boost/graph/connected_components.hpp>
#include <boost/graph/directed_graph.hpp>
#include <boost/graph/dominator_tree.hpp>
#include <boost/graph/stoer_wagner_min_cut.hpp>
#include <boost/graph/strong_components.hpp>
#include <cmath>
#include <fstream>
#include <google/protobuf/io/zero_copy_stream_impl.h>
#include <google/protobuf/util/delimited_message_util.h>
#include <iostream>
#include <numeric>
#include <queue>
#include <random>
#include <thread>
#include <variant>

using namespace parex::graph;
using namespace std::chrono_literals;


std::chrono::milliseconds cur_ms() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
    );
}


class None {

};

// Source: https://en.cppreference.com/w/cpp/utility/variant/visit
template<class... Ts>
struct overloaded : Ts ... {
    using Ts::operator()...;
};
template<class... Ts> overloaded(Ts...) -> overloaded<Ts...>;

/**
 * Index in this variant has to refer to the index in the NodeHeader enum
 */
typedef std::variant<None, CallNode, FuncNode, ActualInNode, ActualOutNode, FormalInNode, FormalOutNode, NormalNode> NodeVariant;

std::ostream &operator<<(std::ostream &os, const NodeVariant &c) // OK
{
    std::visit(overloaded{
            [&os](None &arg) {
                os << "None";
            },
            [&os](auto &arg) {
                os << typeid(arg).name() << " " << ((google::protobuf::Message *) (&arg))->DebugString();
            }
    }, c);
    return os;
}

struct Graph {

    Graph(int32_t entry, int32_t number_of_nodes) : entry(entry) {
        nodes.reserve(number_of_nodes);
    }

    int32_t entry;
    std::vector<NodeVariant> nodes;
    /**
     * Guaranteed to be FuncNodes
     */
    std::vector<int32_t> funcs;


    static int32_t readIntlikeByte(google::protobuf::io::CodedInputStream &stream) {
        stream.Skip(3);
        char val;
        stream.ReadRaw(&val, 1);
        return (int32_t) val;
    }

    template<int T>
    static void parseNode(google::protobuf::io::CodedInputStream &stream, std::vector<NodeVariant> &nodes) {
        nodes.emplace_back(std::in_place_index<T>);
        NodeVariant &node = nodes.back();
        google::protobuf::util::ParseDelimitedFromCodedStream(&std::get<T>(node), &stream, NULL);
    }

    static void
    parseNode(google::protobuf::io::CodedInputStream &stream, std::vector<NodeVariant> &nodes, int32_t type) {
        switch (static_cast<NodeHeader>(type)) {
            case NodeHeader::NONE:
                nodes.emplace_back(NodeVariant(std::in_place_index<NodeHeader::NONE>));
                break;
            case NodeHeader::CALL_NODE:
                parseNode<NodeHeader::CALL_NODE>(stream, nodes);
                break;
            case NodeHeader::FUNC_NODE:
                parseNode<NodeHeader::FUNC_NODE>(stream, nodes);
                break;
            case NodeHeader::ACTUAL_IN_NODE:
                parseNode<NodeHeader::ACTUAL_IN_NODE>(stream, nodes);
                break;
            case NodeHeader::FORMAL_IN_NODE:
                parseNode<NodeHeader::FORMAL_IN_NODE>(stream, nodes);
                break;
            case NodeHeader::ACTUAL_OUT_NODE:
                parseNode<NodeHeader::ACTUAL_OUT_NODE>(stream, nodes);
                break;
            case NodeHeader::FORMAL_OUT_NODE:
                parseNode<NodeHeader::FORMAL_OUT_NODE>(stream, nodes);
                break;
            case NodeHeader::NORMAL_NODE:
                parseNode<NodeHeader::NORMAL_NODE>(stream, nodes);
                break;
            default:
                std::cerr << "unknown node id " << type << std::endl;
                std::exit(1);
        }
    }

    static std::unique_ptr<Graph> parse(std::istream &input) {
        google::protobuf::io::IstreamInputStream raw_input(&input);
        parex::graph::GraphHeader graphHeader;
        google::protobuf::io::CodedInputStream stream(&raw_input);
        google::protobuf::util::ParseDelimitedFromCodedStream(&graphHeader, &stream, NULL);
        int32_t entry = graphHeader.entry();
        stream.Skip(graphHeader.node_header_bytes() + graphHeader.number_of_functions() * 4);
        auto g = std::make_unique<Graph>(entry, graphHeader.number_of_nodes());
        for (int i = 0; i < graphHeader.number_of_nodes(); i++) {
            parseNode(stream, g->nodes, readIntlikeByte(stream));
            auto &last = g->nodes.back();
            if (std::holds_alternative<FuncNode>(last)) {
                g->funcs.push_back(i);
            }
            //g->assert_funcs_are_funcs();
        }
        return g;
    }

    NodeVariant &at(int32_t index) {
        return nodes.at(index);
    }

    template<typename T>
    T &at(int32_t index) {
        return std::get<T>(nodes.at(index));
    }

    friend std::ostream &operator<<(std::ostream &os, const Graph &graph) {
        os << "entry: " << graph.entry << " nodes: [ ";
        for (const auto &item : graph.nodes) {
            os << item << " ";
        }
        os << " ] funcs: [ ";
        for (const auto &func : graph.funcs) {
            os << func << " ";
        }
        os << " ]";
        return os;
    }

    void assert_funcs_are_funcs() {
        for (const auto &func : funcs) {
            if (!std::holds_alternative<FuncNode>(at(func))) {
                throw std::runtime_error(std::string("") + std::to_string(func) + " not a func");
            }
        }
    }
};

class SummaryEdges {

public:

    SummaryEdges(const Graph &g) {
        std::for_each(g.funcs.begin(), g.funcs.end(), [&](auto func) { sumsPerFunc.try_emplace(func); });
    }

    /**
     * Safe for multi threading per func
     */
    std::unordered_set<int32_t> &get(int32_t func, int32_t actIn) {
        auto[actInEntry, success2] = sumsPerFunc.find(func)->second.try_emplace(actIn);
        return actInEntry->second;
    }

    /**
     * Safe for multi threading per func
     */
    void add(int32_t func, int32_t actIn, int32_t actOut) {
        get(func, actIn).insert(actOut);
    }

    friend std::ostream &operator<<(std::ostream &os, const SummaryEdges &edges) {
        for (const auto &[k, v] : edges.sumsPerFunc) {
            os << k << " = ";
            for (const auto &[k, v] : v) {
                os << k << ":";
                for (const auto &e : v) {
                    os << e << ",";
                }
            }
        }
        return os;
    }

    /**
    * Safe for multi threading per func
    */
    bool contains(int32_t func, int32_t actIn, int32_t actOut) {
        auto actInToOuts = sumsPerFunc.find(func);
        if (actInToOuts == sumsPerFunc.end()) {
            return false;
        }
        auto outs = actInToOuts->second.find(actIn);
        if (outs == actInToOuts->second.end()) {
            return false;
        }
        return outs->second.find(actOut) != outs->second.end();
    }

    void protobuf_output(google::protobuf::io::CodedOutputStream &stream, int32_t funcId) {
        ActInToOuts &actInToOuts = sumsPerFunc.at(funcId);

        // output the header
        SummaryFunctionHeader header;
        header.set_id(funcId);
        header.set_number_of_actins(actInToOuts.size());
        google::protobuf::util::SerializeDelimitedToCodedStream(header, &stream);

        // output all summary edges per actual in node
        for (const auto &[k, v] : actInToOuts) {
            SummaryEdgesPerActin edges;
            edges.set_actin(k);
            std::for_each(v.begin(), v.end(), [&edges](auto act_out) { edges.add_actouts(act_out); });
            google::protobuf::util::SerializeDelimitedToCodedStream(edges, &stream);
        }
    }

    void protobuf_output(std::ostream &output) {
        google::protobuf::io::OstreamOutputStream raw_output(&output);
        google::protobuf::io::CodedOutputStream stream(&raw_output);

        // output number of functions
        int32_t size = __builtin_bswap32(sumsPerFunc.size());
        output.write(reinterpret_cast<char *>(&size), 4);

        // output the summary edges per function
        for (const auto &[k, v] : sumsPerFunc) {
            if (v.empty()) {
                continue;
            }
            protobuf_output(stream, k);
        }
    }

    const size_t count(){
        return std::accumulate(sumsPerFunc.begin(), sumsPerFunc.end(), 0, [](const size_t acc, const std::pair<int32_t, ActInToOuts> p){
            return acc + std::accumulate(p.second.begin(), p.second.end(), 0, [](const size_t acc, const std::pair<int32_t, std::unordered_set<int32_t>> p){
                return acc + p.second.size();
            });
        });
    }

private:
    typedef std::unordered_map<int32_t, std::unordered_set<int32_t>> ActInToOuts;
    // summary edges per func
    std::unordered_map<int32_t, ActInToOuts> sumsPerFunc;

};

class Analysis {

protected:

    Graph &g;

public:

    Analysis(Graph &g) : g(g) {}

    virtual std::unique_ptr<SummaryEdges> process() = 0;

};

namespace basic_analysis {

    class SeenVec {
        std::vector<bool> seen; // by default false

    public:
        SeenVec(size_t size) : seen(size, false) {}

        bool is_seen(int32_t node) {
            return seen.at(node);
        }

        void see(int32_t node) {
            seen.at(node) = true;
        }
    };

    /**
     * Idea: Have several
     */
    class NodeSetsPerFormalIn {
        const size_t seen_vec_size;
        std::vector<std::unique_ptr<SeenVec>> seen_per_formal_in_id;

    public:
        NodeSetsPerFormalIn(size_t seen_vec_size) : seen_vec_size(seen_vec_size) {}

        SeenVec &at(size_t formal_in_id) {
            while (seen_per_formal_in_id.size() <= formal_in_id) {
                seen_per_formal_in_id.emplace_back(std::make_unique<SeenVec>(seen_vec_size));
            }
            return *seen_per_formal_in_id.at(formal_in_id);
        }
    };

    class NodeQueue {
        SeenVec &seen;
        std::deque<int32_t> queue;

      std::mutex blub;

    public:
        NodeQueue(SeenVec &seen) : seen(seen) {}

        void push(int32_t node) {
            if (!seen.is_seen(node)) {
                queue.push_back(node);
                seen.see(node);
            }
        }

        template<typename T>
        void push(T nodes) {
            for (const auto &item : nodes) {
                push(item);
            }
        }

        NodeQueue(SeenVec &seen, int32_t initialElement) : NodeQueue(seen) {
            push(initialElement);
        }

        template<typename T>
        NodeQueue(SeenVec &seen, const T &initialElements): NodeQueue(seen) {
            push(initialElements);
        }

        int32_t poll() {
         //  std::cerr << "p" << queue.size() << std::endl;
          auto size = queue.size();
          auto isNotE = isNotEmpty();
          auto last = queue.front();
            queue.pop_front();
            return last;
        }

      int32_t poll2() {
          if (queue.empty()){
            return -1;
          }
        //std::cerr << (size_t)this << " called from " << std::this_thread::get_id() << std::endl;
        //  std::cerr << "p" << queue.size() << std::endl;
        auto size = queue.size();
        auto isNotE = isNotEmpty();
        auto last = queue.front();
        queue.pop_front();
        return last;
      }

        bool isNotEmpty() {
            return !queue.empty();
        }

        bool alreadySeen(int32_t node) {
            return seen.is_seen(node);
        }
    };

    typedef std::map<int32_t, NodeQueue> State; // Map<FormalInNode, NodeQueue<Node>>

    /**
 * An item in the inter-procedural worklist that states that the analysis of another funcNode found for a given funcNode
 * the following new summary edges that should be added
 *
 * actualInToOut == null: the first evaluation of a function
 */
    struct QueueItem { // (val funcNode: FuncNode, val actualInToOut: Map<ActualInNode, Collection<OutNode>>? = null)

        int32_t func_node;
        std::optional<std::unordered_map<int32_t, std::vector<int32_t>>> actualInToOuts;

        QueueItem(int32_t funcNode, std::unordered_map<int32_t, std::vector<int32_t>> actualInToOuts) : func_node(
                funcNode),
                                                                                                        actualInToOuts(
                                                                                                                actualInToOuts) {}

        QueueItem(int32_t funcNode) : func_node(funcNode) {}

    };

    auto int_pair_hash = [](const std::pair<int32_t, int32_t> &p) { return p.first * 31 + p.second; };

    class IntPairHash {
    public:
        std::size_t operator()(const std::pair<int32_t, int32_t> &v) const {
            return std::hash<int32_t>()(v.first) ^ std::hash<int32_t>()(v.second);;
        }
    };


    class BasicAnalysis : public Analysis {

    protected:
        NodeSetsPerFormalIn seen_nodes;

        std::map<int32_t, State> funcStates;

        std::unordered_set<std::pair<int32_t, int32_t>, IntPairHash> formalInToOuts;

        /**
     * Initialize each the state for each function node
     */
        void initFuncStates() {
            funcStates.clear();
            for (const auto &funcId : g.funcs) {
                auto[entry, succeeded] = funcStates.try_emplace(funcId);
                size_t index = 0;
                for (const auto &fi : g.at<FuncNode>(funcId).formal_ins()) {
                    /**
                             * Each formal in node gets its own queue state that is initialized with the current neighbors of the formal in
                             * node. We can assume that the formal in node itself will never be part of this queue
                             */
                    entry->second.try_emplace(fi, seen_nodes.at(index), g.at<FormalInNode>(fi).neighbors());
                    index++;
                }
            }
            std::cerr << "initialized func states" << std::endl;
        }

        void addInitialEntries(std::queue<std::unique_ptr<QueueItem>> &queue) {
            std::for_each(g.funcs.begin(), g.funcs.end(), [&](auto func_id) {
                queue.push(std::make_unique<QueueItem>(func_id));
            });
        }

        /**
     * Starts where it stopped, returns a possibly empty list of found summary edges
         *
         * @return Map<FormalInNode, Set<FormalOutNode>>
     */
        std::unique_ptr<std::unordered_map<int32_t, std::unordered_set<int32_t>>>
        process(int32_t funcId, SummaryEdges &sums) {
            auto newFormalInToOuts = std::make_unique<std::unordered_map<int32_t, std::unordered_set<int32_t>>>(); // = mutableMapOf<FormalInNode, MutableSet<FormalOutNode>>()
            for (auto &[fi, state] : funcStates.at(funcId)) {
                /**
                 * Start for each formal in node where the last iteration ended
                 */
                while (true) {
                    int32_t cur = state.poll2();
                    if (cur == -1){
                      break;
                    }
                    std::visit(overloaded{
                            [&state=state, &sums, funcId, cur](ActualInNode &node) {
                                /**
                                 * Use summary edges if they are there
                                 */
                                state.push(sums.get(funcId, cur));
                                state.push(node.neighbors());
                            },
                            [&,&fi=fi](FormalOutNode &node) {
                                if (formalInToOuts.find(std::pair(fi, cur)) == formalInToOuts.end()) {
                                    /**
                                     * Add the newly found connection between the current formal in and formal out nodes
                                     */
                                    auto[entry, succeeded] = newFormalInToOuts->try_emplace(fi);
                                    entry->second.insert(cur);
                                }
                            },
                            [](None none) {},
                            [&state=state](auto &node) {
                                state.push(node.neighbors());
                            }
                    }, g.nodes.at(cur));
                }
            }
            return newFormalInToOuts;
        }

    public:
        BasicAnalysis(Graph &g) : Analysis(g), seen_nodes(g.nodes.size()) {}


        virtual std::unique_ptr<SummaryEdges> process() {

            auto sums = std::make_unique<SummaryEdges>(g);

            initFuncStates();

            std::queue<std::unique_ptr<QueueItem>> worklist;
            addInitialEntries(worklist);

            while (!worklist.empty()) {
                auto head = std::move(worklist.front());
                worklist.pop();
                process_item(std::move(head), [&worklist](std::unique_ptr<QueueItem> item) { worklist.emplace(std::move(item)); },
                             *sums);
            }

            return sums;
        }

        void process_item(std::unique_ptr<QueueItem> item, std::function<void(std::unique_ptr<QueueItem>)> &&worklist,
                          SummaryEdges &sums) {
            /**
             * Try to add summary edges and return directly if nothing changed
             */
            if (item->actualInToOuts) {
                bool changed = false;
                for (auto &[ai, os] : item->actualInToOuts.value()) {
                    for (auto &o : os) {
                        if (!sums.contains(item->func_node, ai, o)) {
                            /**
                             * We found a new summary edge
                             */
                            sums.add(item->func_node, ai, o);
                            /**
                             * add to queue if ai ∈ alreadySeen and o ∉ alreadySeen
                             * i.e. if we found an previously not seen actual out that is connected to an already seen actual in node
                             * (if we have not yet seen the actual in node, then the actual out node will be added to the queue later
                             * anyway)
                             */
                            for (auto &[_, state] : funcStates[item->func_node]) {
                                if (state.alreadySeen(ai) && !state.alreadySeen(o)) {
                                    state.push(o);
                                }
                            }
                            changed = true;
                        }
                    }
                }
                if (!changed) {
                    return;
                }
            }



            /**
             * Process the function and find new connections between formal in and formal out nodes
             */
            auto changes = process(item->func_node, sums);
            /**
             * Add to queue
             */
            std::unordered_map<int32_t, std::unordered_map<int32_t, std::vector<int32_t>>> meta; // std::unordered_map<FuncNode, MutableMap<ActualInNode, Collection<OutNode>>>
            for (const auto &[fi, fos] : *changes) {
                for (const auto &[callNode, ai] : g.at<FormalInNode>(fi).actual_ins()) {
                    /**
                     * val actualOuts:  List<OutNode> = fos.mapNotNull { fo.actualOuts[callNode] }
                     * if (actualOuts.isNotEmpty()){
                        meta.computeIfAbsent(callNode.owner, {HashMap()})[ai] = actOuts
                    }
                }
                     */
                    auto[entry, success] = meta.try_emplace(g.at<CallNode>(callNode).owner());
                    auto &actualOuts = entry->second;
                    for (const auto &fo : fos) {
                        auto &actualOutPerCall = g.at<FormalOutNode>(fo).actual_outs();
                        auto actualOut = actualOutPerCall.find(callNode);
                        if (actualOut != actualOutPerCall.end()) {
                            auto[vecEntry, success] = actualOuts.try_emplace(ai);
                            vecEntry->second.push_back(actualOut->second);
                        }
                    }
                }
            }
            std::for_each(meta.begin(), meta.end(),
                          [&worklist](auto &entry) {
                              worklist(std::make_unique<QueueItem>(entry.first, entry.second));
                          });
         // std::cerr << "end " << item->func_node << " " << std::this_thread::get_id() << std::endl;
        }

    };

    namespace parallel {

        class BasicParallelAnalysis;

        /**
        * Processes only queue items for specific function nodes
        */
        class Computer {

            const size_t id;

            std::vector<int32_t> assignedFuncNodes;
            BasicParallelAnalysis *ana;
            moodycamel::ConcurrentQueue<std::unique_ptr<QueueItem>> queue;
            std::atomic_int32_t queue_count = 0;
            std::queue<std::unique_ptr<QueueItem>> local_queue;
            std::unique_ptr<std::thread> thread;

            void copy_to_local_queue();

        public:
            Computer(BasicParallelAnalysis *ana, size_t id) : id(id), ana(ana) {}

            void assign_func(int32_t func);

            void run(SummaryEdges &sums);

            /**
             * Offer from another thread
             */
            void offer(std::unique_ptr<QueueItem> item);

            void join() {
                if (thread) {
                    thread->join();
                }
            }
        };


        typedef std::function<std::vector<std::vector<int32_t>>(Graph &,
                                                                int32_t /* number of threads */)> FuncToComputerGroupingPolicy;

        const FuncToComputerGroupingPolicy SEQUENTIAL_POLICY = [](Graph &g, size_t number_of_threads) {
            size_t funcsPerThread = std::ceil(g.funcs.size() * 1.0 / number_of_threads);
            std::vector<std::vector<int32_t>> comp_groups;
            for (size_t i = 0; i < std::min(number_of_threads, g.funcs.size()); i++) {
                auto &comp_group = comp_groups.emplace_back();
                for (size_t f = i * funcsPerThread; f < std::min((i + 1) * funcsPerThread, g.funcs.size()); f++) {
                    comp_group.push_back(g.funcs.at(f));
                }
            }
            return comp_groups;
        };

        /**
         * Trivially parallelized sequential analysis, that assigns each thread a number of functions it looks out for
         *
         * What it does not: it does no work balancing between the threads, nor does it calculate strongly connected components
         *
         * Uses a local (non synchronized) and a non-local (lock-free) queue per computer
         */
        class BasicParallelAnalysis : public BasicAnalysis {

            friend class Computer;

            const size_t number_of_threads;

            std::unordered_map<int32_t, Computer &> func_id_to_computer;

            std::vector<std::unique_ptr<Computer>> computers;
            std::atomic_int32_t queue_item_counter = 0;
            FuncToComputerGroupingPolicy policy;

            void init() {
                func_id_to_computer.clear();
                computers.clear();
                auto time = cur_ms();
                auto comp_groups = policy(g, number_of_threads);
                std::cout << "executing policy " << (cur_ms() - time).count() << std::endl;
                size_t id = 0;
                for (auto &comp_group : comp_groups) {
                    auto &comp = computers.emplace_back(std::make_unique<Computer>(this, id));
                    for (auto &func : comp_group) {
                        comp->assign_func(func);
                        func_id_to_computer.emplace(func, *comp);
                    }
                    id++;
                }
            }


            void run(SummaryEdges &sums) {
                std::for_each(computers.begin(), computers.end(), [&sums](auto &computer) { computer->run(sums); });
                std::for_each(computers.begin(), computers.end(), [](auto &computer) { computer->join(); });
            }

        public:

            BasicParallelAnalysis(Graph &g, FuncToComputerGroupingPolicy policy = SEQUENTIAL_POLICY,
                                  size_t number_of_threads = std::thread::hardware_concurrency()) :
                    BasicAnalysis(g), number_of_threads(number_of_threads),
                    policy(policy) {}

            virtual std::unique_ptr<SummaryEdges> process() {
                init();
                initFuncStates();
                auto sums = std::make_unique<SummaryEdges>(g);
                run(*sums);
                return sums;
            }
        };

        /**
            * Processes only queue items for specific function nodes
            */
        void Computer::assign_func(int32_t func) {
            assignedFuncNodes.push_back(func);
            local_queue.emplace(std::make_unique<QueueItem>(func));
            ana->queue_item_counter++;
        }

        void Computer::run(SummaryEdges &sums) {
            std::cerr << "Init " << id << std::endl;
            thread = std::make_unique<std::thread>([&] {
                while (ana->queue_item_counter.load() > 0) {
                    //std::cerr << "Hi " << ana->queue_item_counter.load() << std::endl;
                    auto proc = [&](std::unique_ptr<QueueItem> it) {
                        ana->process_item(std::move(it), [&](std::unique_ptr<QueueItem> item) {
                            offer(std::move(item));
                        }, sums);
                    };

                    if (local_queue.size() > 0) {
                        proc(std::move(local_queue.front()));
                        local_queue.pop();
                        ana->queue_item_counter--;
                    } else {
                        std::unique_ptr<QueueItem> it;
                        if (queue.try_dequeue(it)) {
                            proc(std::move(it));
                            ana->queue_item_counter--;
                        } else {
                            std::cout << id << ":" << ana->queue_item_counter.load() << " ";
                            for (const auto &computer : ana->computers) {
                                std::cout << computer->id << "=" << (computer->local_queue.size() + computer->queue.size_approx()) << ";";
                            }
                            std::cout << " " << std::flush;
                        }
                    }
                }
            });
        }

        void Computer::offer(std::unique_ptr<QueueItem> item) {
            auto &comp = ana->func_id_to_computer.at(item->func_node);
            if (&comp == this) {
                comp.local_queue.emplace(std::move(item));
            } else {
                comp.queue.enqueue(std::move(item));
            }
            comp.queue_count++;
            ana->queue_item_counter++;
        }

        typedef std::vector<std::vector<int32_t>> CompGroups;

        class FuncToComputerGrouping {
            Graph &g;
            std::unordered_map<int32_t, int16_t> funcsAssignedToThreads;
            int32_t maxComputerId = 0;
            size_t unassignedFuncs;

        public:
            FuncToComputerGrouping(Graph &g) : g(g), unassignedFuncs(g.funcs.size()) {}

            bool is_assigned(int32_t func) {
                return funcsAssignedToThreads.find(func) != funcsAssignedToThreads.end();
            }

            void assign(int32_t func, size_t computer) {
                if (!is_assigned(func)) {
                    unassignedFuncs--;
                }
                funcsAssignedToThreads.emplace(func, computer);
                maxComputerId = std::max(computer, static_cast<size_t>(maxComputerId));
            }

            CompGroups toCompGroups() {
                assert(unassignedFuncs == 0);
                std::vector<std::vector<int32_t>> comp_groups(maxComputerId + 1);
                for (const auto &[k, v] : funcsAssignedToThreads) {
                    comp_groups.at(v).push_back(k);
                }
                return comp_groups;
            }

            std::vector<size_t> computeGroupSizes() {
                std::vector<size_t> comp_groups(maxComputerId + 1);
                for (const auto &[k, v] : funcsAssignedToThreads) {
                    comp_groups.at(v)++;
                }
                return comp_groups;
            }

            size_t computeGroupSizesStd() {
                auto groupSizes = computeGroupSizes();
                auto mean = std::accumulate(groupSizes.begin(), groupSizes.end(), 0);
                return std::sqrt(std::accumulate(groupSizes.begin(), groupSizes.end(), 0, [&mean](size_t a, size_t b) {
                    return a + (b - mean) * (b - mean);
                })) / groupSizes.size();
            }

            size_t getUnassignedFuncs() {
                return unassignedFuncs;
            }

            std::optional<size_t> geUnassignedFunc() {
                if (unassignedFuncs > 0) {
                    for (const auto &func : g.funcs) {
                        if (!is_assigned(func)) {
                            return func;
                        }
                    }
                }
                return {};
            }
        };

        /**
         * Select a seed func for each computer and grow it from there
         */
        const FuncToComputerGroupingPolicy GROWING_POLICY = [](Graph &g, size_t number_of_threads) {

            auto find_grouping = [&]() {

                auto comp = std::make_unique<FuncToComputerGrouping>(g);

                std::vector<std::vector<std::pair<int32_t, bool>>> possibleBordersPerComputer(number_of_threads);

                // init randomly
                std::default_random_engine generator;
                std::uniform_int_distribution<int32_t> distribution(0, g.funcs.size() - 1);
                for (size_t i = 0; i < number_of_threads; ++i) {
                    int32_t possible;
                    do {
                        auto index = distribution(generator);
                        possible = g.funcs.at(index);
                    } while (comp->is_assigned(possible));
                    possibleBordersPerComputer.at(i).emplace_back(possible, true);
                    comp->assign(possible, i);
                }

                size_t curComputer = 0;

                while (comp->getUnassignedFuncs() > 0) {
                    // get the possible border elements for the current computer
                    auto possibleBorders = possibleBordersPerComputer.at(curComputer);
                    bool found = false;

                    for (auto &[func, usable] : possibleBorders) {
                        if (usable) {
                            bool localFound = false;
                            auto &funcNode = g.at<FuncNode>(func);
                            // callers
                            for (const auto &callerCallId : funcNode.callers()) {
                                auto &caller = g.at<CallNode>(callerCallId);
                                if (!comp->is_assigned(caller.owner())) {
                                    comp->assign(caller.owner(), curComputer);
                                    localFound = true;
                                    usable = false;
                                }
                            }
                            if (localFound) {
                                found = true;
                                break;
                            }
                        }
                    }

                    if (!found) {
                        auto func = comp->geUnassignedFunc();
                        comp->assign(func.value(), curComputer);
                        possibleBordersPerComputer.at(curComputer).emplace_back(func.value(), true);
                    }

                    curComputer = (curComputer + 1) % number_of_threads;
                }

                return comp;
            };

            std::unique_ptr<FuncToComputerGrouping> lastCompGroup = find_grouping();
            size_t lastStd = lastCompGroup->computeGroupSizesStd();

            for (size_t i = 0; i < 3; i++) {
                std::unique_ptr<FuncToComputerGrouping> curCompGroup = find_grouping();
                size_t curStd = curCompGroup->computeGroupSizesStd();
                if (curStd < lastStd) {
                    lastCompGroup.swap(curCompGroup);
                    lastStd = curStd;
                }
            }
            return lastCompGroup->toCompGroups();
        };

        namespace cg {
            struct node_properties {
                int32_t func;
                size_t index;
            };
            typedef boost::adjacency_list<boost::vecS, boost::vecS, boost::bidirectionalS,
                    node_properties,   boost::property<boost::vertex_color_t, boost::default_color_type>> graph_t;
            typedef typename boost::graph_traits<graph_t>::vertex_descriptor vertex_t;
            typedef typename boost::graph_traits<graph_t>::edge_descriptor edge_t;

        }
    }
}
// see https://stackoverflow.com/a/36390244
namespace boost {
    template <>
    struct property_map<basic_analysis::parallel::cg::graph_t, vertex_index_t> {
        typedef typename property_map<basic_analysis::parallel::cg::graph_t, size_t basic_analysis::parallel::cg::node_properties::*>::type type;
        typedef typename property_map<basic_analysis::parallel::cg::graph_t, size_t basic_analysis::parallel::cg::node_properties::*>::const_type const_type;
    };

    static auto get(vertex_index_t, basic_analysis::parallel::cg::graph_t& g)       { return get(&basic_analysis::parallel::cg::node_properties::index, g); }
    static auto get(vertex_index_t, basic_analysis::parallel::cg::graph_t const& g) { return get(&basic_analysis::parallel::cg::node_properties::index, g); }
}

namespace basic_analysis {
    namespace parallel {
        namespace cg {
            class CallGraph {

                Graph &g;
                graph_t graph;
                std::unordered_set<int32_t> funcs;
                std::unordered_map<int32_t, vertex_t&> vertexPerFunc;
                std::vector<vertex_t> vertices;
                std::function<bool(int32_t,int32_t)> ignore_edge_pred;

            public:

                CallGraph(Graph &g, std::optional<std::unordered_set<int32_t>> only_use = {}, std::function<bool(int32_t,int32_t)> ignore_edge = [](auto a, auto b){ return false; }):
                    g(g), ignore_edge_pred(ignore_edge) {
                    funcs = only_use ? only_use.value() : std::unordered_set<int32_t>(g.funcs.begin(), g.funcs.end());
                    vertices.reserve(funcs.size());
                    size_t i = 0;
                    for (const auto &func_id : funcs) {
                        vertices.push_back(boost::add_vertex({func_id, i}, graph));
                        vertexPerFunc.emplace(func_id, vertices.back());
                        i++;
                    }
                    for (const auto &func_id : funcs) {
                        auto &func = g.at<FuncNode>(func_id);
                        auto &caller = vertexPerFunc.at(func_id);
                        for (const auto &callee_call_id : func.callees()) {
                            auto &callee_call = g.at<CallNode>(callee_call_id);
                            for (const auto &target_id : callee_call.targets()) {
                                if (funcs.find(target_id) != funcs.end()) {
                                    if (!ignore_edge(func_id, target_id)) {
                                        auto &callee = vertexPerFunc.at(target_id);
                                        boost::add_edge(caller, callee, graph);
                                    }
                                }
                            }
                        }
                    }
                }

                void print_graph() {
                    std::cout << "Graph:" << std::endl;
                    auto edges = boost::edges(graph);
                    for (auto it = edges.first; it != edges.second; ++it) {
                        std::cout << boost::source(*it, graph) << " -> "
                                  << boost::target(*it, graph) << std::endl;
                    }
                    std::cout << graph.vertex_set().size() << "dfg\n";
                }


                auto sccs() -> decltype(auto) {
                    std::map<graph_t::vertex_descriptor, unsigned long> mapping;
                    size_t component_count = boost::strong_components(graph, boost::associative_property_map(mapping));

                    auto grouped_funcs = std::make_unique<std::vector<std::vector<int32_t>>>(component_count);
                    for (const auto &[descr, comp] : mapping) {
                        grouped_funcs->at(comp).push_back(graph[descr].func);
                    }
                    return grouped_funcs;
                }

                std::vector<std::unique_ptr<CallGraph>> scc_cgs(){
                    auto scc_vecs = sccs();
                    auto ret = std::vector<std::unique_ptr<CallGraph>>();
                    ret.reserve(scc_vecs->size());
                    for (const auto &scc_vec : *scc_vecs) {
                        ret.emplace_back(std::make_unique<CallGraph>(g, std::unordered_set(scc_vec.begin(), scc_vec.end())));
                    }
                    return ret;
                }

                bool is_scc() {
                    std::map<graph_t::vertex_descriptor, unsigned long> mapping;
                    size_t component_count = boost::strong_components(graph, boost::associative_property_map(mapping));
                    return component_count == 1;
                }

                CallGraph ignore_edge(std::pair<int32_t, int32_t> ignored_edge){
                    return CallGraph(g, funcs, [&ignored_edge](int32_t caller, int32_t callee){
                        return ignored_edge.first == caller && ignored_edge.second == callee;
                    });
                }

                CallGraph ignore_edges(std::vector<std::pair<int32_t, int32_t>> ignored_edges){
                    auto hash = [](const std::pair<int32_t, int32_t>& p){ return p.first * 31 + p.second; };
                    std::unordered_set<std::pair<int32_t, int32_t>, decltype(hash)> ignored_edge_set(ignored_edges.size(), hash);
                    ignored_edge_set.insert(ignored_edges.begin(), ignored_edges.end());
                    return CallGraph(g, funcs, [&ignored_edge_set](int32_t caller, int32_t callee){
                        return ignored_edge_set.find(std::make_pair(caller, callee)) != ignored_edge_set.end();
                    });
                }

                bool is_scc(std::pair<int32_t, int32_t> ignored_edge) {
                    return ignore_edge(ignored_edge).is_scc();
                }

                void print_scc_hist(){
                    auto grouped_funcs = sccs();
                    std::map<size_t, size_t> sizeToCount;
                    for (const auto &vec : *grouped_funcs) {
                        auto [entry, success] = sizeToCount.try_emplace(vec.size(), 0);
                        sizeToCount.at(vec.size()) = entry->second + 1;
                    }
                    for (const auto &[k, v] : sizeToCount) {
                        std::cout << k << ", " << v << std::endl;
                    }
                }

                /**
                 * Find the functions the call no other functions, ignoring the passed functions
                 */
                std::unordered_set<int32_t> find_funcs_without_callees(std::unordered_set<int32_t> &assume_dead){
                    std::unordered_set<int32_t> ret;
                    std::copy_if(funcs.begin(), funcs.end(), std::inserter(ret, ret.end()), [&](int32_t func){
                        if (assume_dead.find(func) != assume_dead.end()){
                            return false;
                        }
                        for (const auto &callee_call_id : g.at<FuncNode>(func).callees()) {
                            for (const auto &target : g.at<CallNode>(callee_call_id).targets()) {
                                if (funcs.find(target) != funcs.end() && assume_dead.find(target) == assume_dead.end()){
                                    return false;
                                }
                            }
                        }
                        return true;
                    });
                    return ret;
                }

                /**
                 * Layer 0 contains only functions that don't call other functions
                 * Layer 1 … that calls layer 0 functions
                 * …
                 * Layer n contains the rest
                 */
                std::vector<std::vector<int32_t>> find_layers(){
                    std::vector<std::vector<int32_t>> layers;
                    std::unordered_set<int32_t> collected;
                    while (collected.size() < funcs.size()){
                        auto found = find_funcs_without_callees(collected);
                        layers.emplace_back(found.begin(), found.end());
                        collected.insert(found.begin(), found.end());
                        if (found.size() == 0){
                            break;
                        }
                    }
                    auto &last = layers.emplace_back();
                    std::copy_if(funcs.begin(), funcs.end(), std::back_inserter(last), [&collected](int32_t func){
                        return collected.find(func) == collected.end();
                    });
                    return layers;
                }

                void print_layers(){
                    std::cout << "Functions that call no functions besides the ones of the previous layers:" << std::endl;
                    std::unordered_set<int32_t> collected;
                    size_t layer_id = 0;
                    while (collected.size() < funcs.size()){
                        auto found = find_funcs_without_callees(collected);
                        collected.insert(found.begin(), found.end());
                        std::cout << "  " << layer_id << ": " << found.size() << std::endl;
                        layer_id++;
                        if (found.size() == 0){
                            std::cout << "No new functions found, collected " << collected.size() << " of " << funcs.size() << std::endl;
                            break;
                        }
                    }
                }

                std::vector<int32_t> findEntryFuncs(){
                    std::vector<int32_t> ret;
                    std::copy_if(funcs.begin(), funcs.end(), std::back_inserter(ret), [this](int32_t func){
                        for (const auto &caller_call_id : g.at<FuncNode>(func).callers()) {
                            auto caller = g.at<CallNode>(caller_call_id).owner();
                            if (funcs.find(caller) != funcs.end()){
                                return false;
                            }
                        }
                        return true;
                    });
                    return ret;
                }

                std::unordered_map<int32_t, std::set<int32_t>> computeDominators(int32_t entryFunc){

                    entryFunc = findEntryFuncs().at(0);

                    // see https://stackoverflow.com/a/36390244
                   std::vector<vertex_t> dom_pred(boost::num_vertices(graph), boost::graph_traits<graph_t>::null_vertex());

                    auto index_map = boost::get(&node_properties::index, graph); // equivalent to vertex_index_t now
                    auto dom_tree_pred_map (boost::make_iterator_property_map(std::begin(dom_pred), index_map));
                    // Run main algorithm
                    boost::lengauer_tarjan_dominator_tree(graph, vertexPerFunc.at(entryFunc), dom_tree_pred_map);

                    std::cout << "Result: ";
                   for (auto v : dom_pred) {
                        if (v == boost::graph_traits<graph_t>::null_vertex())
                            std::cout << "(root) ";
                        else
                            std::cout << boost::get(boost::vertex_index, graph)[v] << " ";
                    }

                    return {};
                }

                std::unordered_map<int32_t, size_t> computeFuncToBFSDepth(int32_t entryFunc){

                }

                /**
                 * Inserts sink and sources that are connected to all nodes
                 * @return
                 */
                std::pair<std::unique_ptr<CallGraph>, std::unique_ptr<CallGraph>> minimum_cut(std::function<int(int32_t,int32_t)> weighter =
                        [](int32_t caller, int32_t callee){ return 1; }){

                    struct edge_t
                    {
                        unsigned long first;
                        unsigned long second;
                    };

                    typedef boost::adjacency_list<boost::vecS, boost::vecS, boost::undirectedS,
                            node_properties, boost::property<boost::edge_weight_t, int> > undirected_graph;
                    typedef boost::property_map<undirected_graph, boost::edge_weight_t>::type weight_map_type;
                    typedef boost::property_traits<weight_map_type>::value_type weight_type;

                    undirected_graph graph;

                    std::unordered_map<int32_t, vertex_t&> vertexPerFunc;
                    std::vector<vertex_t> vertices;
                    vertices.reserve(funcs.size());
                    size_t i = 0;
                    for (const auto &func_id : funcs) {
                        vertices.push_back(boost::add_vertex({func_id, i}, graph));
                        vertexPerFunc.emplace(func_id, vertices.back());
                        i++;
                    }
                    for (const auto &func_id : funcs) {
                        auto &func = g.at<FuncNode>(func_id);
                        auto &caller = vertexPerFunc.at(func_id);
                        for (const auto &callee_call_id : func.callees()) {
                            auto &callee_call = g.at<CallNode>(callee_call_id);
                            for (const auto &target_id : callee_call.targets()) {
                                if (funcs.find(target_id) != funcs.end()) {
                                    if (!ignore_edge_pred(func_id, target_id)) {
                                        auto &callee = vertexPerFunc.at(target_id);
                                        if (graph[caller].func == 29390 || graph[caller].func == 30372){
                                            std::cout << "Hallo\n";
                                        }
                                        boost::add_edge(caller, callee, weighter(graph[caller].func, graph[callee].func), graph);
                                    } else {
                                        std::cout << "a\n";
                                    }
                                } else {
                                    std::cout << "b\n";
                                }
                            }
                        }
                    }

                    // define a property map, `parities`, that will store a boolean value for each vertex.
                    // Vertices that have the same parity after `stoer_wagner_min_cut` runs are on the same side of the min-cut.
                    auto parities2 = boost::make_one_bit_color_map(boost::num_vertices(graph), boost::get(boost::vertex_index, graph));

                    int w = boost::stoer_wagner_min_cut(graph, get(boost::edge_weight, graph), boost::parity_map(parities2));

                    std::cout << "The min-cut weight of G is " << w << ".\n" << std::endl;

                    std::cout << "One set of vertices consists of:" << std::endl;

                    auto copy_par = [&](bool in, std::unordered_set<int32_t> &dest){
                        for (i = 0; i < boost::num_vertices(graph); ++i) {
                            if (boost::get(parities2, i) == in)
                                dest.emplace(graph[i].func);
                        }
                    };
                    std::unordered_set<int32_t> first_funcs;
                    copy_par(true, first_funcs);
                    std::unordered_set<int32_t> second_funcs;
                    copy_par(false, second_funcs);

                    std::cout << first_funcs.size() << " vs " << second_funcs.size() << std::endl;

                    /*for (i = 0; i < boost::num_vertices(graph); ++i) {
                        if (boost::get(parities2, i))
                            std::cout << graph[i].func << std::endl;
                    }
                    std::cout << std::endl;

                    std::cout << "The other set of vertices consists of:" << std::endl;
                    for (i = 0; i < boost::num_vertices(graph); ++i) {
                        if (!boost::get(parities2, i))
                            std::cout << graph[i].func << std::endl;
                    }
                    std::cout << std::endl;*/
                    return std::make_pair<std::unique_ptr<CallGraph>, std::unique_ptr<CallGraph>>(std::make_unique<CallGraph>(g, first_funcs), std::make_unique<CallGraph>(g, second_funcs));
                }

                std::pair<std::unique_ptr<CallGraph>, std::unique_ptr<CallGraph>> min_cut_bisect(size_t lower_bound = 5){
                    std::unordered_set<int32_t> smaller_set;
                    std::pair<std::unique_ptr<CallGraph>, std::unique_ptr<CallGraph>> pair;
                    std::unique_ptr<CallGraph> cur;
                    while (smaller_set.size() < lower_bound){
                        pair = minimum_cut([&](int32_t caller, int32_t callee){
                            if (smaller_set.find(caller) != smaller_set.end() || smaller_set.find(callee) != smaller_set.end()){
                                std::cout << caller << " → " << callee << std::endl;
                                return static_cast<int>(funcs.size());
                            }
                            return 1;
                        });
                        if (pair.first->size() >= lower_bound && pair.second->size() >= lower_bound){
                            return pair;
                        }
                        if (pair.first->size() < lower_bound){
                            smaller_set.insert(pair.first->funcs.begin(), pair.first->funcs.end());
                        } else if (pair.second->size() < lower_bound){
                            smaller_set.insert(pair.second->funcs.begin(), pair.second->funcs.end());
                        }
                    }
                }

                std::vector<std::unique_ptr<CallGraph>> iterative_min_cut(size_t lower_bound = 5, size_t upper_bound = 8){
                    std::vector<std::unique_ptr<CallGraph>> ret;
                    std::queue<std::unique_ptr<CallGraph>> queue;
                    if (size() < upper_bound){
                        ret.emplace_back(std::move(std::make_unique<CallGraph>(g, funcs)));
                        return ret;
                    }
                    auto mc = min_cut_bisect(lower_bound);
                    queue.emplace(std::move(mc.first));
                    queue.emplace(std::move(mc.second));
                    while (!queue.empty()){
                        auto cur = std::move(queue.front());
                        queue.pop();
                        if (cur->funcs.size() > upper_bound) {
                            mc = cur->min_cut_bisect(lower_bound);
                            queue.emplace(std::move(mc.first));
                            queue.emplace(std::move(mc.second));
                        } else {
                            ret.push_back(std::move(cur));
                        }
                    }
                    return ret;
                }

                size_t size(){
                    return funcs.size();
                }
            };

        }

        const FuncToComputerGroupingPolicy PRIMITIVE_SCC = [](Graph &g, size_t number_of_threads){

            cg::CallGraph cg(g);

            auto sccs = cg.sccs();
            std::sort(sccs->begin(), sccs->end(), [](auto &x, auto &y){ return x.size() > y.size(); });

            std::vector<std::vector<int32_t>> comp_groups(number_of_threads);

            for (const auto &scc : *sccs) {
                auto &smallest = *std::min_element(comp_groups.begin(), comp_groups.end(), [](auto &x, auto &y){ return x.size() < y.size(); });
                smallest.insert(smallest.begin(), scc.begin(), scc.end());
            }
            return comp_groups;
        };
    }
}

namespace layering {

    typedef std::unique_ptr<basic_analysis::QueueItem> qi_ptr;

    /**
     * Package of functions with queues
     */
    class Package {

        size_t id;
        std::unordered_set<int32_t> funcs;
        moodycamel::ConcurrentQueue<qi_ptr> global_queue;
        std::queue<qi_ptr> local_queue;
        std::atomic_size_t queue_size_ = 0;

    public:
        std::atomic_int8_t state = {0};

        Package(size_t id, std::unordered_set<int32_t> funcs, std::vector<qi_ptr> initial_elements): id(id), funcs(funcs) {
            for (auto &initial_element : initial_elements) {
                local_queue.emplace(std::move(initial_element));
            }
            queue_size_ = initial_elements.size();
        }

        Package(size_t id, std::unordered_set<int32_t> funcs): Package(id, funcs, std::vector<qi_ptr>()) {
            for (const auto &func : funcs) {
                local_queue.emplace(std::make_unique<basic_analysis::QueueItem>(func));
            }
            queue_size_ = funcs.size();
        }

        void insert_local(qi_ptr item){
            local_queue.emplace(std::move(item));
            queue_size_++;
        }

        void insert_global(qi_ptr item){
            global_queue.enqueue(std::move(item));
            queue_size_++;
        }

        std::vector<qi_ptr> pop_all(){
            std::vector<qi_ptr> ret;
            ret.reserve(local_queue.size() + global_queue.size_approx());
            while (true){
                qi_ptr it;
                if (!global_queue.try_dequeue(it)){
                    break;
                }
                ret.push_back(std::move(it));
            }
            while (!local_queue.empty()){
                ret.push_back(std::move(local_queue.front()));
                local_queue.pop();
            }
            return ret;
        }

        bool responsible_for(int32_t func){
            return funcs.find(func) != funcs.end();
        }

        /**
         * Is an upper bound, might be lower
         */
        bool queue_size(){
            return queue_size_;
        }

        qi_ptr pop(){
            if (local_queue.size() > 0) {
                qi_ptr item = std::move(local_queue.front());
                local_queue.pop();
                queue_size_--;
                return item;
            } else {
                qi_ptr it;
                if (global_queue.try_dequeue(it)) {
                    queue_size_--;
                    return it;
                }
            }
            return {};
        }

        friend std::ostream &operator<<(std::ostream &os, const Package &pkg) {
            os << fmt::format("Package(id={}, funcs=[{}], in_queue={}, #item={})", pkg.id, pkg.funcs, pkg.state.load(), pkg.queue_size_.load());
            return os;
        }
    };

    typedef std::unique_ptr<Package> pkg_ptr;

    typedef std::function<std::vector<std::unordered_set<int32_t>>(Graph&, std::vector<int32_t>, size_t)> PackageGrouper; // funcs → number of packages → grouping

    typedef std::function<size_t(Graph&, std::vector<int32_t>, size_t)> PackageNumberComputer;

    class PackageThreadPool;

    typedef std::function<void(PackageThreadPool&, Package*)> ComputerWorker;

    class Computer {
        const size_t id;
        PackageThreadPool &pool;
        std::unique_ptr<std::thread> thread;
        std::function<bool()> abort;
        ComputerWorker worker;

    public:
        Computer(size_t id, PackageThreadPool &pool, ComputerWorker worker, std::function<bool()> abort);

        void join(){
            if (thread){
                thread->join();
            }
        }

        friend std::ostream &operator<<(std::ostream &os, const Computer &comp) {
            os << fmt::format("Computer(id={})", comp.id);
            return os;
        }
    };

    template <typename T>
    using WorklistQueue = worklist::LockedAllocatingWorkListQueue<T>;

    /**
     * Process the packages with a simple thread pool
     *
     * Does not own the packages.
     *
     * The worker function has to return false if a package should not be reinserted into the queue
     */
    class PackageThreadPool {

        friend Computer;
        WorklistQueue<Package*> package_queue;
        std::vector<Computer> computers;
        std::atomic_size_t unfinished_pkgs;

    public:

        PackageThreadPool(std::vector<Package*> packages, ComputerWorker worker, std::function<bool()> abort, size_t number_of_threads = std::thread::hardware_concurrency()):
            unfinished_pkgs(packages.size()), package_queue(packages.size()) {
          for (auto &item : packages) {
            package_queue.push(item);
          }
            computers.reserve(number_of_threads);
            for (size_t i = 0; i < number_of_threads; i++){
                computers.emplace_back(i, *this, worker, abort);
            }
        }

        void join(){
            for (auto &computer : computers) {
                computer.join();
            }
        }

        void try_enqueue(Package *pkg){
          package_queue.push(pkg);
        }

        bool try_dequeue(Package* &pkg){
          return package_queue.try_pop(pkg);
        }

        void finish(Package *&pkg){
          unfinished_pkgs--;
          package_queue.finish(pkg);
        }

        bool is_finished(){
          return unfinished_pkgs.load() == 0;
        }

        friend std::ostream &operator<<(std::ostream &os, const PackageThreadPool &pool) {
            os << fmt::format("Pool(computers=<{}>, queue_size={})", pool.computers, pool.package_queue.size());
            return os;
        }
    };

    Computer::Computer(size_t id, PackageThreadPool &pool, ComputerWorker worker, std::function<bool()> abort)
            : id(id), pool(pool), abort(abort), worker(worker) {
            thread = std::make_unique<std::thread>([&]{
                while (!this->abort()){
                    Package *pkg;
                    if (pool.try_dequeue(pkg)) {
                      fmt::print("w{}\n ", this->id);
                        this->worker(pool, pkg);
                      fmt::print("f{}\n ", this->id);
                      pool.finish(pkg);
                    } else {
                     // fmt::print("n{} ", id);
                    }
                }
            });
    }

    // what is done?
    // Thread Pool + abstract computer + package definition
    //

    const PackageGrouper DEFAULT_GROUPER = [](Graph &g, std::vector<int32_t> funcs, size_t number){
        std::unordered_set<int32_t> fs(funcs.begin(), funcs.end());
        std::vector<std::unordered_set<int32_t>> groups(number);
        size_t cur = 0;
        for (const auto &func : fs) {
            groups.at(cur).insert(func);
            cur = (cur + 1) % number;
        }
        return groups;
    };

    PackageNumberComputer DEFAULT_NUMBER_OF_PKGS_COMPUTER = [](Graph &g, auto funcs, size_t number_of_threads){
      return std::max(funcs.size() / 50, std::min(number_of_threads, funcs.size()));
    };

    class LayeredPackages : public basic_analysis::BasicAnalysis {

        std::vector<pkg_ptr> packages;
        std::vector<std::vector<Package*>> layers;
        std::unordered_map<int32_t, Package*> package_per_func;
        size_t current_layer = 0;
        PackageGrouper grouper;
        PackageNumberComputer number_of_packages_computer;
        size_t number_of_threads;
        basic_analysis::parallel::cg::CallGraph cg;

        void init_packages_and_layers(){
            size_t id = 0;
            for (const auto &funcs_in_layer : cg.find_layers()) {
                auto number = number_of_packages_computer(g, funcs_in_layer, number_of_threads);
                auto &package_layer = layers.emplace_back();
                for (const auto &package_items : grouper(g, funcs_in_layer, number)) {
                    auto pkg = std::make_unique<Package>(id, package_items);
                    package_layer.push_back(&*pkg);
                    for (const auto &func : package_items) {
                        package_per_func.emplace(func, &*pkg);
                    }
                    packages.push_back(std::move(pkg));
                    id++;
                }
            }
            std::cerr << "initialized packages and layers" << std::endl;
        }

    public:

        LayeredPackages(Graph &g, PackageGrouper grouper = DEFAULT_GROUPER,
                PackageNumberComputer number_of_packages_computer = DEFAULT_NUMBER_OF_PKGS_COMPUTER,
                size_t number_of_threads = std::thread::hardware_concurrency()):
                BasicAnalysis(g), grouper(grouper), number_of_packages_computer(number_of_packages_computer),
                number_of_threads(number_of_threads), cg(g) {
        }

        LayeredPackages(Graph &g, size_t number_of_threads): LayeredPackages(g, DEFAULT_GROUPER, DEFAULT_NUMBER_OF_PKGS_COMPUTER, number_of_threads) {}

        LayeredPackages(Graph &g, size_t number_of_threads, size_t package_number): LayeredPackages(g, DEFAULT_GROUPER, [&](Graph &g, auto funcs, size_t number_of_threads){ return package_number; }, number_of_threads) {}

        void init(){
            init_packages_and_layers();
        }

        /**
         * Offer the queueitem to the global queue
         * @param source_pkg
         * @param item
         */
        void offer(PackageThreadPool &pool, Package *source_pkg, qi_ptr item) {
            if (source_pkg->responsible_for(item->func_node)){
                source_pkg->insert_local(std::move(item));
            } else {
                auto pkg = package_per_func.find(item->func_node)->second;
                pkg->insert_global(std::move(item));
                pool.try_enqueue(pkg);
            }
        }

        /**
         * Returns true if the package is not finished yet
         * @param pkg
         * @return
         */
        void process_package(PackageThreadPool &pool, Package *pkg, SummaryEdges &sums){
            while (pkg->queue_size() > 0) {
                auto item = pkg->pop();
                if (item){
                    fmt::print(stderr, "{} works on {}: {}\n", std::this_thread::get_id(), *pkg, item->func_node);
                    process_item(std::move(item), [&](qi_ptr item) {
                        offer(pool, pkg, std::move(item));
                    }, sums);
                    //fmt::print(stderr, "finished\n");
                } else {
                  break;
                }
            }
          fmt::print(stderr, "{} finished\n", std::this_thread::get_id());
        }

        void run(SummaryEdges &sums, std::vector<Package*> &pkgs){
            using namespace std::placeholders;
            PackageThreadPool pool(pkgs, [&](PackageThreadPool &pool, Package *pkg){
                   // std::cerr << "Process package " << *pkg << "  " << std::this_thread::get_id() << std::endl;
                    process_package(pool, pkg, sums);
              //std::cerr << "end package " << *pkg << "  " << std::this_thread::get_id() << std::endl;
                }, [&](){
                return pool.is_finished();
            }, number_of_threads);
            pool.join();
        }

        /**
         * Basic version:
         *  merge all layers, TODO: prioritize lower layers…
         * @param sums
         */
        void run(SummaryEdges &sums){
            std::vector<Package*> pkgs;
            for (size_t i = 0; i < layers.size(); ++i) {
                pkgs.insert(pkgs.end(), layers.at(i).begin(), layers.at(i).end());
            }
            std::cerr << "start running" << std::endl;
            run(sums, pkgs);
        }

        virtual std::unique_ptr<SummaryEdges> process() {
            init();
            initFuncStates();
            auto sums = std::make_unique<SummaryEdges>(g);
            run(*sums);
            return sums;
        }
    };
}

int32_t parse_env(const char* variable, int32_t default_val){
    const char* tmp = std::getenv(variable);
    if (tmp == nullptr){
        return default_val;
    }
    return std::stoi(tmp);
}

int main(int count, char *args[]) {
    std::unique_ptr<Graph> g;
    std::chrono::milliseconds cur = cur_ms();
    if (count >= 3) {
        std::ifstream input(args[2], std::ios::in | std::ios::binary);
        g = Graph::parse(input);
    } else {
        g = Graph::parse(std::cin);
    }

    basic_analysis::parallel::cg::CallGraph cg(*g);
   // cg.print_scc_hist();
    //cg.print_layers();
    //std::exit(0);
    //cg.minimum_cut();
    std::cerr << "CPP: Time for parsing " << (cur_ms() - cur).count() << std::endl;
    //g->assert_funcs_are_funcs();
    cur = cur_ms();
    std::unique_ptr<Analysis> ana = std::make_unique<basic_analysis::BasicAnalysis>(*g);
    if (count >= 2) {
        switch (*args[1]) {
            case 's':
                break;
            case 'p': {
                    auto policy = basic_analysis::parallel::SEQUENTIAL_POLICY;
                    if (strlen(args[1]) > 1) {
                        switch (args[1][1]) {
                            case 's':
                                break;
                            case 'g':
                                policy = basic_analysis::parallel::GROWING_POLICY;
                                break;
                            case 'c':
                                policy = basic_analysis::parallel::PRIMITIVE_SCC;
                        }
                    }
                    ana = std::make_unique<basic_analysis::parallel::BasicParallelAnalysis>(*g, policy,
                                                                                            parse_env("CPP_THREADS", 2));
                    }
                break;
            case 'l':
                ana = std::make_unique<layering::LayeredPackages>(*g, parse_env("CPP_THREADS", 1));
                break;
        }
    }
    auto sums = ana->process();
    std::cerr << "CPP: Time for summary computation " << (cur_ms() - cur).count() << std::endl;
    std::cerr << sums->count() << std::endl;
    cur = cur_ms();
    if (parse_env("CPP_V", 0) == 1) {
      std::cerr << *sums << std::endl;
    }
    sums->protobuf_output(std::cout);
    std::cerr << "CPP: Time for output " << (cur_ms() - cur).count() << std::endl;
    std::cout << std::endl;
    google::protobuf::ShutdownProtobufLibrary();
    return 0;
}