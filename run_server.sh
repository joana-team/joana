#! /bin/sh

# Runs the CLIServer for the component based analysis
# e.g. use `curl --request POST --data-binary @ZIP_FILE localhost:8004` to
# run an analysis directly from the console

set -e
cd "$(dirname "$0")"

if ! (java -version 2>&1 >/dev/null | grep "1.8" > /dev/null) ; then
    echo "Java version is not 1.8, trying to use java8 if installed"
    lib=`update-java-alternatives -l | grep "[/].*1\.8.*" --only-matching`
    if [ -z $lib ] ; then
      $lib="/usr/lib/jvm/java-1.8.0-openjdk-amd64"
    fi
    echo "Execute \`update-alternatives --config java\` and \`update-alternatives --config javac\`"
    echo "or prepend \$PATH with $lib"
    export PATH="$lib/bin:$PATH"
  else
    if !(javac -version 2>&1 >/dev/null | grep "1.8" > /dev/null); then
      echo "You're java and javac versions differ"
      exit 1
    fi
  fi

java -cp dist/joana.ui.ifc.wala.console.jar edu.kit.joana.ui.ifc.wala.console.console.component_based.CLIServer
