#!/bin/bash

# get path of run script from current working directory
pushd . > /dev/null
SCRIPT_PATH="${BASH_SOURCE[0]}";

while([ -h "${SCRIPT_PATH}" ]); do
    cd "`dirname "${SCRIPT_PATH}"`"
    SCRIPT_PATH="$(readlink "`basename "${SCRIPT_PATH}"`")";
done

cd "`dirname "${SCRIPT_PATH}"`" > /dev/null
SCRIPT_PATH="`pwd`";

popd  > /dev/null

java -jar ${SCRIPT_PATH}/../../../../dist/joana.ifc.sdg.qifc.qif_interpreter.jar "$@" --workingDir `pwd`
