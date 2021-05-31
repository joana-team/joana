#!/bin/bash

printf "Removing old output files + directories\n"
find . -type d -name 'out_*' -exec rm -r {} \; >/dev/null 2>&1
find . -type d -name 'test_*' -exec rm -r {} \; >/dev/null 2>&1
rm *.dot
rm *.cnf
rm *.map
find . -type f -path "./testResources/*" -name "*.class" -exec rm -f {} \;
find . -type f -path "./examples/*" -name "*.class" -exec rm -f {} \;
rm .attach_pid* >/dev/null 2>&1
printf "Done\n\n"

ant clean