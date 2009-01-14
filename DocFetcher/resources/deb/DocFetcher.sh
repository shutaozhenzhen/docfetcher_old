#!/bin/sh

cd /usr/share/docfetcher

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

export LD_LIBRARY_PATH="./lib"

java -cp ".:${CLASSPATH}" -Djava.library.path="lib" net.sourceforge.docfetcher.DocFetcher "$@"
