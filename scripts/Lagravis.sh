#!/bin/bash

DIR=`pwd`

cd "$( dirname "${BASH_SOURCE[0]}" )"
cd ..

mvn -q -e exec:java -Duser.dir=$DIR -Dexec.mainClass="ch.tkuhn.lagravis.Lagravis" -Dexec.args="$*"
