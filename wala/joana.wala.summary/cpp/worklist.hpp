/**
 * A worklist queue. A bounded queue that can only contain a known set
 * of elements (usually pointers) and that ensures that each element is only
 * worked on by one thread at a time (via a `finish` method).
 *
 * The implementations are FIFO queues with multiple consumers and producers.
 *
 * This file contains multiple, interchangeable implementations.
 */

#ifndef CPP_WORKLIST_HPP
#define CPP_WORKLIST_HPP

#include <atomic>
#include <mutex>
#include <queue>
#include <unordered_set>

namespace worklist {
/**
 * Basic implementation with a full lock and allocations.
 *
 * Simple but slow.
 *
 * @tparam T element type
 */
template <typename T> class LockedAllocatingWorkListQueue {

  std::queue<T> queue;
  std::unordered_set<T> in_queue;
  std::unordered_set<T> in_work;
  std::unordered_set<T> should_insert;
  mutable std::mutex mutex;

public:
  LockedAllocatingWorkListQueue(size_t size) {
    in_queue.reserve(size);
    in_work.reserve(size);
    should_insert.reserve(size);
  }

  void push(T &t) {
    std::lock_guard l(mutex);
    if (in_queue.find(t) != in_queue.end()) {
      return;
    }
    if (in_work.find(t) != in_work.end()) {
      should_insert.insert(t);
    } else {
      queue.push(t);
      in_queue.insert(t);
    }
  }

  bool try_pop(T &t) {
    std::lock_guard l(mutex);
    if (queue.size() == 0) {
      return false;
    }
    t = queue.front();
    queue.pop();
    in_queue.erase(t);
    in_work.insert(t);
    return true;
  }

  /**
   * Call this after finishing the work on a element, it can reappear in the queue again
   * @param t
   */
  void finish(T &t) {
    std::lock_guard l(mutex);
    in_work.erase(t);
    if (should_insert.find(t) != should_insert.end()) {
      in_queue.insert(t);
      queue.push(t);
    }
  }

  size_t size() const {
    std::lock_guard<std::mutex> l(mutex);
    return queue.size();
  }
};

/**
 * Circular buffer of fixed without error checking
 * @tparam T
 */
template <typename T> class CircularBuffer {
  std::vector<T> buffer;
  /**
   * if tail == head: empty
   */
  size_t head_index;
  /**
   * first free element
   */
  size_t tail_index;

  size_t buffer_size;

public:
  CircularBuffer(size_t size): buffer(size + 1), head_index(0), tail_index(0), buffer_size(size + 1) {}

  T &front() { return buffer.at(head_index); }

  void pop() { head_index = (head_index + 1) % buffer_size; }

  void push(const T &t) {
    buffer.at(tail_index) = t;
    tail_index = (tail_index + 1) % buffer_size;
  }

  size_t size() const {
    if (head_index > tail_index){
      return head_index - tail_index;
    }
    return tail_index - head_index;
  }
};

/**
 * Basic implementation without a single lock and without dynamic allocations
 * (using a circular buffer)
 *
 * Requires a get_state function that returns a pointer to an integer state
 * variable per element (0 by default if not inserted)
 *
 * Faster but with more requirements
 *
 * @tparam T element type
 */
template <typename T, std::atomic_int8_t *get_state(T &), typename B = CircularBuffer<T>>
class LockedBoundedWorkListQueueWithStates {

  B buffer;
  mutable std::mutex mutex;

  enum State { NOT_IN_QUEUE = 0, IN_QUEUE = 1, IN_WORK = 2, SHOULD_INSERT = 3 };

public:
  LockedBoundedWorkListQueueWithStates(size_t size) : buffer(size) {}

  void push(T &t) {
    std::lock_guard l(mutex);
    switch (*get_state(t)) {
    case SHOULD_INSERT:
    case IN_QUEUE:
      return;
    case IN_WORK:
      *get_state(t) = SHOULD_INSERT;
      break;
    default:
      buffer.push(t);
      *get_state(t) = IN_QUEUE;
    }
  }

  bool try_pop(T &t) {
    std::lock_guard l(mutex);
    if (buffer.size() == 0) {
      return false;
    }
    t = buffer.front();
    buffer.pop();
    *get_state(t) = IN_WORK;
    return true;
  }

  /**
   * Call this after finishing the work on a element, it can reappear in the queue again
   * @param t
   */
  void finish(T &t) {
    std::lock_guard l(mutex);
    if (*get_state(t) == SHOULD_INSERT) {
      buffer.push(t);
      *get_state(t) = IN_QUEUE;
    } else {
      *get_state(t) = NOT_IN_QUEUE;
    }
  }

  size_t size() const {
    std::lock_guard l(mutex);
    return buffer.size();
  }
};


template <typename T, typename S>
T increment_modulo(std::atomic<T> &val, S modulo){
  T old_value = val.load();
  T new_value;
  do {
    new_value = (old_value + 1) % modulo;
  } while (!val.compare_exchange_strong(old_value, new_value));
  return old_value;
}

/**
 * Basic implementation with a full lock and without dynamic allocations
 * (using a circular buffer)
 *
 * Requires a get_state function that returns a pointer to an integer state
 * variable per element (0 by default if not inserted)
 *
 * Faster, lock free but with more requirements
 *
 * Problem: try to alter the state and the queue at the same time
 * Idea:
 *  try_pop: might fail even if there are elements to pop, schedule away and try
 *           shortly later
 *      - if size() == 0: return  # conservative
 *      - elements[head].cs(IN_QUEUE, IN_WORK)
 *          - increment head_index % size (cs loop, get and then replace, spin lock article)
 *          - improve by aligning sizes to powers of two and use modulo
 *            only on access (bit map), modulo via overflow
 *          - Problem: next element could already be out of the queue
 *          - for i in range(0, size):
 *              - break if elements[(head_index + i) % size].load() == IN_QUEUE || head_index == tail_index
 *      - get next element where cs(IN_QUEUE, IN_WORK) is successful
 *          - for i in range(1, size)
 *              - break if elements[(head_index + i) % size].cs(IN_QUEUE, IN_WORK)
 *          - else return # no element found
 *      - now owning the element, work on it
 *  finish:
 *      - cs(IN_WORK, NOT_IN_QUEUE)
 *      - else if cs(SHOULD_INSERT, IN_QUEUE) # here: state cannot be IN_WORK (we checked before)
 *          - # try to insert it
 *          - increment tail index (see above, capture last tail index value)
 *              - the element still lying at last tail index, isn't in the
 *                IN_QUEUE state
 *          - store element at last tail index
 *  push:  (loop it? no)
 *      - cs(NOT_IN_QUEUE, IN_QUEUE)
 *          - increment tail index (see above, capture last tail index value)
 *              - the element still lying at last tail index, isn't in the
 *                IN_QUEUE state
 *          - store element at last tail index
 *      - else if cs(IN_WORK, SHOULD_INSERT)
 *      - don't do anything for SHOULD_INSERT or IN_QUEUE state
 *  probably with a few loops
 * @tparam T element type
 */
template <typename T, std::atomic_int8_t *get_state(T &)>
class LockLessBoundedWorkListQueueWithStates {

  std::vector<std::atomic<T>> buffer;
  /**
   * if tail == head: empty
   */
  std::atomic_size_t  head_index;
  /**
   * first free element
   */
  std::atomic_size_t tail_index;

  std::atomic_size_t in_queue_count;
  std::atomic_size_t in_work_count;

  enum State { NOT_IN_QUEUE = 0, IN_QUEUE = 1, IN_WORK = 2, SHOULD_INSERT = 3 };

  std::atomic_int8_t* get_state_at(size_t i){
    auto val = buffer[i].load();
    return get_state(val);
  }

public:
  LockLessBoundedWorkListQueueWithStates(size_t size) :
    buffer(size + 1), tail_index(0), head_index(0), in_queue_count(0), in_work_count(0) {}

  void push(T &t) {
    auto state = get_state(t);
    int8_t expected = NOT_IN_QUEUE;
    if (state->compare_exchange_strong(expected, IN_QUEUE)){
      // really insert element
      auto old_tail = increment_modulo(tail_index, buffer.size());
      buffer[old_tail].store(t);
      in_queue_count++;
    } else {
      expected = IN_WORK;
      state->compare_exchange_strong(expected, SHOULD_INSERT);
    }
  }

  /**
   * Might fail even if there are elements in the queue (class comment)
   */
  bool try_pop(T &t) {
    if (in_queue_count.load() == 0){
      return false;
    }
    int8_t expected = IN_QUEUE;
    if (get_state_at(head_index.load())->compare_exchange_strong(expected, IN_WORK)){
      t = buffer[head_index.load()].load();
      do {
        increment_modulo(head_index, buffer.size());
        // any problems here?
      } while (head_index != tail_index && get_state_at(head_index.load())->load() != IN_QUEUE);
    } else {
      size_t cur = head_index.load();
      expected = IN_QUEUE;
      while (!get_state_at(cur)->compare_exchange_strong(expected, IN_WORK)){
        cur++;
        if (cur == tail_index){
          return false;
        }
        expected = IN_QUEUE;
      }
      t = buffer[cur].load();
    }
    in_queue_count--;
    in_work_count++;
    return true;
  }

  /**
   * Call this after finishing the work on a element, it can reappear in the queue again
   * @param t
   */
  void finish(T &t) {
    auto state = get_state(t);
    int8_t expected = IN_WORK;
    if (!state->compare_exchange_strong(expected, NOT_IN_QUEUE)){
      if (expected != SHOULD_INSERT){
        throw new std::exception();
      }
      state->store(IN_QUEUE);
      // try to insert it
      auto old_tail = increment_modulo(tail_index, buffer.size());
      buffer[old_tail].store(t);
      in_queue_count++;
      in_work_count--;
    }
  }

  size_t size() const {
    return in_queue_count.load();
  }
};

}

#endif // CPP_WORKLIST_HPP
