cmake_minimum_required(VERSION 3.10)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS  "-pthread -Wl,--no-as-needed -Wall -pedantic")
project(cpp)

add_subdirectory(lib/fmt)
find_package(Protobuf REQUIRED)
#protobuf_generate_cpp(PROTO_SRCS PROTO_HDRS ../resources/graph.proto)

add_executable(${PROJECT_NAME} main.cpp graph.pb.cc worklist.hpp)
target_link_libraries(${PROJECT_NAME} PUBLIC protobuf fmt::fmt-header-only)

add_subdirectory(tests)