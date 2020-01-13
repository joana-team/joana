
#include "../worklist.hpp"
#include <thread>

#include "gtest/gtest.h"

namespace worklist {
namespace {

using namespace std::chrono_literals;

struct Entry {
  size_t id;
  std::atomic_int thread_count = 0;
  std::atomic_int8_t state = 0;
  std::atomic_int worked_on_times = 0;

  Entry(size_t id): id(id) {}
};

// The fixture for testing class Foo.
template <typename W>
class WorklistTest : public ::testing::Test {

public:

  virtual W create_worklist(size_t entries) = 0;

  void test_insert_elements_seq(size_t entry_count){
    W w = create_worklist(entry_count);
    std::vector<std::unique_ptr<Entry>> entries;
    for (size_t i = 0; i < entry_count; i++){
      entries.emplace_back(std::make_unique<Entry>(i));
    }
    for (auto &entry : entries) {
      Entry *entry_ptr = &*entry;
      w.push(entry_ptr);
    }
    for (size_t i = 0; i < entry_count; i++){
      Entry* entry_ptr;
      EXPECT_TRUE(w.try_pop(entry_ptr));
    }
  }

  void test_insert_elements(size_t entry_count){
    W w = create_worklist(entry_count);
    std::vector<std::unique_ptr<std::thread>> workers;
    std::vector<std::unique_ptr<Entry>> entries;
    for (size_t i = 0; i < entry_count; i++){
      entries.emplace_back(std::make_unique<Entry>(i));
    }
    workers.emplace_back(std::make_unique<std::thread>([&] {
      for (auto &entry : entries) {
        Entry *entry_ptr = &*entry;
        w.push(entry_ptr);
      }
    }));
    workers.emplace_back(std::make_unique<std::thread>([&] {
      for (size_t i = 0; i < entry_count; i++){
        Entry* entry_ptr;
        while (!w.try_pop(entry_ptr)){
        }
      }
    }));
    for (const auto &worker : workers) {
      worker->join();
    }
  }

  void test_random_mpmc(size_t threads, size_t entry_count, std::chrono::milliseconds duration){
    W w = create_worklist(entry_count);
    std::vector<std::unique_ptr<std::thread>> workers;
    auto start = std::chrono::steady_clock::now();
    std::vector<std::unique_ptr<Entry>> entries;
    for (size_t i = 0; i < entry_count; i++){
      entries.emplace_back(std::make_unique<Entry>(i));
    }
    for (size_t i = 0; i < threads; i++){
      workers.emplace_back(std::make_unique<std::thread>([&]{
        while (std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - start) < duration){
          Entry* entry_ptr;
          if (w.try_pop(entry_ptr)) {
            for (auto &entry : entries) {
              if (rand() % 3 == 0) {
                Entry *entry_ptr = &*entry;
                w.push(entry_ptr);
              }
            }
            entry_ptr->thread_count.fetch_add(1);
            std::this_thread::sleep_for(10ms);
            w.finish(entry_ptr);
            entry_ptr->thread_count.fetch_add(-1);
            for (auto &entry : entries) {
              if (rand() % 2 == 0) {
                Entry *entry_ptr = &*entry;
                w.push(entry_ptr);
              }
            }
          }
          for (auto &entry : entries) {
            if (rand() % 2 == 0) {
              Entry *entry_ptr = &*entry;
              w.push(entry_ptr);
            }
          }
        }
      }));
    }
    for (const auto &worker : workers) {
      worker->join();
    }
    for (const auto &entry : entries) {
      EXPECT_EQ(entry->thread_count, 0);
    }
  }

  void test_random_mpmc2(size_t threads, size_t entry_count, std::chrono::milliseconds duration){
    W w = create_worklist(entry_count);
    std::vector<std::unique_ptr<std::thread>> workers;
    auto start = std::chrono::steady_clock::now();
    Entry entry{42};
    for (size_t i = 0; i < threads; i++){
      workers.emplace_back(std::make_unique<std::thread>([&]{
        while (std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - start) < duration){
          Entry* entry_ptr;
          if (w.try_pop(entry_ptr)) {
            //std::this_thread::sleep_for(10ms);
            w.finish(entry_ptr);
          }
          entry_ptr = &entry;
          w.push(entry_ptr);
        }
      }));
    }
    for (const auto &worker : workers) {
      worker->join();
    }
  }

  void test(){
    test_insert_elements_seq(10);
    test_insert_elements(10);
    test_random_mpmc(10, 10, 1s);
  }
};

class LockedAllocatingWorkListQueueTest : public WorklistTest<LockedAllocatingWorkListQueue<Entry*>> {
  LockedAllocatingWorkListQueue<Entry*> create_worklist(size_t entries) override {
    return LockedAllocatingWorkListQueue<Entry*>(entries);
  }
};

std::atomic_int8_t *get_int_state(Entry* &entry){
  return &entry->state;
}

class LockedBoundedWorkListQueueWithStatesTest : public WorklistTest<LockedBoundedWorkListQueueWithStates<Entry*, get_int_state>> {
  LockedBoundedWorkListQueueWithStates<Entry*, get_int_state> create_worklist(size_t entries) override {
    return LockedBoundedWorkListQueueWithStates<Entry*, get_int_state>(entries);
  }
};

class LockLessBoundedWorkListQueueWithStatesTest : public WorklistTest<LockLessBoundedWorkListQueueWithStates<Entry*, get_int_state>> {

  LockLessBoundedWorkListQueueWithStates<Entry*, get_int_state> create_worklist(size_t entries) override {
    return LockLessBoundedWorkListQueueWithStates<Entry*, get_int_state>(entries);
  }
};

TEST_F(LockedAllocatingWorkListQueueTest, SequentialTestBasicQueueTest) {
  test();
}
TEST_F(LockedAllocatingWorkListQueueTest, SequentialTestBasicQueue) {
  test_random_mpmc(100, 1000, 1s);
}
TEST_F(LockedBoundedWorkListQueueWithStatesTest, SequentialTestBasicQueue2) {
  test_random_mpmc(2, 10, 1s);
}
TEST_F(LockedBoundedWorkListQueueWithStatesTest, SequentialInsertTestBasicQueueTest2) {
    test_insert_elements_seq(10);
}
TEST_F(LockedBoundedWorkListQueueWithStatesTest, InsertTestBasicQueueTest2) {
    test_insert_elements(10);
}
TEST_F(LockedBoundedWorkListQueueWithStatesTest, TestBasicQueueTest2) {
  test();
}

TEST_F(LockLessBoundedWorkListQueueWithStatesTest, SequentialInsertTestBasicQueueTest3) {
  test_insert_elements_seq(1);
}
TEST_F(LockLessBoundedWorkListQueueWithStatesTest, InsertTestBasicQueueTest3) {
  test_insert_elements(10);
}
TEST_F(LockLessBoundedWorkListQueueWithStatesTest, TestBasicQueueTest3) {
  test_random_mpmc(2, 2, 1s);
}
TEST_F(LockLessBoundedWorkListQueueWithStatesTest, TestBasicQueueTestMPMC23) {
  test_random_mpmc2(2, 1, 1s);
}

}
}

int main(int argc, char **argv) {
  ::testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}