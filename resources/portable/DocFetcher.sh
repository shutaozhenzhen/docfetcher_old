#!/bin/sh

prg=$0
if [ ! -e "$prg" ]; then
  case $prg in
    (*/*) exit 1;;
    (*) prg=$(command -v -- "$prg") || exit;;
  esac
fi
dir=$(
  cd -P -- "$(dirname -- "$prg")" && pwd -P
) || exit
prg=$dir/$(basename -- "$prg") || exit

cd $dir

CLASSPATH=
for FILE in `ls ./lib/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

for FILE in `ls ./lib/linux/*.jar`
do
   CLASSPATH=${CLASSPATH}:${FILE}
done

java -cp ".:${CLASSPATH}" -Djava.library.path="lib" net.sourceforge.docfetcher.DocFetcher "$@"