#!/bin/sh
cd "$(dirname "$0")"
protoc resources/graph.proto --java_out src
protoc resources/graph.proto --cpp_out cpp
mv cpp/resources/* cpp
rm -r cpp/resources
