#!/bin/bash

JAVA=java
HEAPSPACE=4096M

. /data1/joana/use_64bit_java.env


if [ -f $1 ]; then
        CONFIGFILE=$1
else
        CONFIGFILE=jSDG.config
fi

LOGFILE=`echo $CONFIGFILE | sed -e 's/\//\./g'`.log

JSDG=jSDG.jar

echo "starting \"$JAVA -Xmx$HEAPSPACE -jar $JSDG -cfg $CONFIGFILE 2>&1 | tee $LOGFILE\""

$JAVA -Xmx$HEAPSPACE -jar $JSDG -cfg $CONFIGFILE 2>&1 | tee $LOGFILE
