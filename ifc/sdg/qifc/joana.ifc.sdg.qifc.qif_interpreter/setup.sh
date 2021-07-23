#!/bin/bash

# installs cryptominisat + approxmc
# the following packages need to be installed:
# build-essential cmake zlib1g-dev libboost-program-options-dev libm4ri-dev

git clone https://github.com/msoos/cryptominisat
cd cryptominisat && mkdir build
cd cryptominisat/build && cmake .. && make && make install

git clone https://github.com/meelgroup/approxmc/
cd approxmc && mkdir build
cd approxmc/build && cmake .. && make && make install