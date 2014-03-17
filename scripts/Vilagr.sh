#!/bin/bash

DIR=`pwd`

cd "$( dirname "${BASH_SOURCE[0]}" )"
cd ..

export MAVEN_OPTS="-Xmx8g"
mvn -q -e exec:java -Duser.dir=$DIR -Dexec.mainClass="ch.tkuhn.vilagr.Vilagr" -Dexec.args="$*"
