#! /bin/zsh
set -eu
MY_MAIN_FILE=$(readlink -f $MAIN_FILE)
pushd /data1/bechberger/joana/wala/joana.wala.summary.test
JAR="../../dist/joana.wala.summary.test.jar"
DEST=$(mktemp -d)
TMP=creduce/$MAIN_FILE
mkdir -p $TMP
cp $MY_MAIN_FILE $TMP/$(date +%s%N)
javac $MY_MAIN_FILE -d $DEST
java -cp $JAR:$DEST edu.kit.joana.wala.summary.test.Test $DEST $MAIN_CLASS
RET=$?
popd
exit $RET