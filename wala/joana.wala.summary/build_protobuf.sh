#!/bin/sh
cd "$(dirname "$0")"
protoc resources/graph.proto --java_out src
