#!/bin/bash

if [ -f $1 ]; then
	echo "Searching called Methods in '$1' - writing to '$1.called'"
	grep -A2 ENTR $1 | grep 'V "' | grep -v '\*Start\*' | sort | uniq > $1.called
	COUNT=`wc -l $1.called | cut -d ' ' -f 1` 
	echo "$COUNT methods found."
else
	echo "File '$1' not found."
fi
