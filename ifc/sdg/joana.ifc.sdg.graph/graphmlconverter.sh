#!/bin/sh
BASEDIR=$(dirname $(readlink -f "$0"))
java -verbose -Xms256m -Xmx27224m -Xss32m -cp "$BASEDIR/bin:$BASEDIR/../../../contrib/lib/*:$BASEDIR/../../../util/joana.util/bin/" edu.kit.joana.ifc.sdg.io.graphml.PDGFile2GraphMLFile "$@"
