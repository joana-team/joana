#!/bin/sh
cd "$(dirname "$0")"
protoc resources/graph.proto --java_out src
cd resources
protoc graph.proto --cpp_out ../cpp
