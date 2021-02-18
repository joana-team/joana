#!/bin/bash

printf "Removing old output files + directories\n"
find . -type d -name 'out_*' -exec rm -r {} \; > /dev/null 2>&1
find . -type f -path "./testResources/*" -name "*.class" -exec rm -f {} \;
find . -type f -path "./examples/*" -name "*.class" -exec rm -f {} \;
rm .attach_pid* > /dev/null 2>&1
printf "Done\n\n"

ant clean