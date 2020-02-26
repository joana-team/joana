#! /bin/sh
# first argument is the target, if omitted, all targets are built
set -e
cd "$(dirname "$0")"
./build_protobuf.sh
mkdir -p build
cmake -DCMAKE_BUILD_TYPE=Release -S . -B build
cd build
if [ $# -ne 1 ]; then
  make -j
else
  make -j $1
fi
cd -
