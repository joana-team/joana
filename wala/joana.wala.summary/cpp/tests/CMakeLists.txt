# Download and unpack googletest at configure time
configure_file(CMakeLists.txt.in googletest-download/CMakeLists.txt)
execute_process(COMMAND ${CMAKE_COMMAND} -G "${CMAKE_GENERATOR}" .
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/googletest-download )
if(result)
    message(FATAL_ERROR "CMake step for googletest failed: ${result}")
endif()
execute_process(COMMAND ${CMAKE_COMMAND} --build .
        RESULT_VARIABLE result
        WORKING_DIRECTORY ${CMAKE_CURRENT_BINARY_DIR}/googletest-download )
if(result)
    message(FATAL_ERROR "Build step for googletest failed: ${result}")
endif()

# Prevent overriding the parent project's compiler/linker
# settings on Windows
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

# Add googletest directly to our build. This defines
# the gtest and gtest_main targets.
add_subdirectory(${CMAKE_CURRENT_BINARY_DIR}/googletest-src
        ${CMAKE_CURRENT_BINARY_DIR}/googletest-build
        EXCLUDE_FROM_ALL)

# The gtest/gtest_main targets carry header search path
# dependencies automatically when using CMake 2.8.11 or
# later. Otherwise we have to add them here ourselves.
if (CMAKE_VERSION VERSION_LESS 2.8.11)
    include_directories("${gtest_SOURCE_DIR}/include")
endif()

include_directories(..)

set(CMAKE_CXX_FLAGS  "-pthread -Wall -pedantic -O3 -fopenmp")
set(CMAKE_CXX_FLAGS_RELEASE  "-O3")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -D_GLIBCXX_DEBUG -g -O0 -Wall -pedantic -fopenmp")

add_executable(worklist test_worklist.cpp ../src/worklist.cpp)
target_link_libraries(worklist fmt::fmt-header-only gtest gtest_main stdc++fs)

add_executable(summary test_summary.cpp ../src/analysis.cpp ../src/util.cpp ../src/graph.cpp ../src/packages.cpp ../graph.pb.cc ../src/worklist.hpp ../src/analysis.hpp ../src/summary.hpp ../src/worklist.cpp utilities.cpp)
target_link_libraries(summary protobuf fmt::fmt-header-only gtest gtest_main stdc++fs)

add_executable(graph2 test_graph2.cpp ../src/util.cpp ../src/graph2.cpp ../src/packages.cpp ../graph.pb.cc ../src/worklist.hpp ../src/analysis.hpp ../src/summary.hpp ../src/graph2.hpp ../src/graph_convert.cpp ../src/worklist.cpp utilities.cpp utilities.hpp)
target_link_libraries(graph2 protobuf fmt::fmt-header-only gtest gtest_main stdc++fs)

add_executable(graph test_graph.cpp ../src/util.cpp ../src/graph2.cpp ../src/packages.cpp ../graph.pb.cc ../src/worklist.hpp ../src/analysis.hpp ../src/summary.hpp ../src/graph2.hpp ../src/graph_convert.cpp ../src/worklist.cpp utilities.cpp utilities.hpp)
target_link_libraries(graph protobuf fmt::fmt-header-only gtest gtest_main stdc++fs)