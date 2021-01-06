#!/bin/bash

printf "Removing old output files + directories\n"
find . -type d -name 'out_*' -exec rm -r {} \; > /dev/null 2>&1
printf "Done\n\n"

ant clean