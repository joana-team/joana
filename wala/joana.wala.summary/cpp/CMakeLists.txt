cmake_minimum_required(VERSION 3.10)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS "-pthread -Wall -pedantic -g -fopenmp")
set(CMAKE_CXX_FLAGS_RELEASE "-pthread -Wall -pedantic -g -O3 -fopenmp")
project(cpp)

add_subdirectory(lib/fmt)
find_package(Protobuf REQUIRED)

file(GLOB SOURCES src/*.cpp lib/* *.cc)

set(ProtobufIncludePath ${CMAKE_CURRENT_BINARY_DIR})
include_directories(${ProtobufIncludePath})
include_directories(.)

add_executable(${PROJECT_NAME} ${SOURCES} src/summary.cpp src/summary.hpp src/graph2.cpp src/graph2.hpp src/graph_convert.hpp src/analysis2.cpp src/analysis2.hpp src/graph_convert.cpp src/analysis.cpp src/worklist.cpp)
target_link_libraries(${PROJECT_NAME} PUBLIC protobuf fmt::fmt-header-only stdc++fs)

add_subdirectory(tests)
