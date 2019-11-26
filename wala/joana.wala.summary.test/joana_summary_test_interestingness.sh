#! /bin/sh
JAR="../../dist/joana.wala.summary.test.jar"
DEST=$(mktemp -d)
javac $1 -d $DEST
x=$1
java -cp $JAR:$DEST edu.kit.joana.wala.summary.test.Test $MAIN_CLASS
