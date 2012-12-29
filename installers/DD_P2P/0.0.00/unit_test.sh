#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
DIR_ALL="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do 
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
DIR_ALL="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

echo $DIR_ALL

java -cp "${DIR_ALL}/jars/DD.jar:${DIR_ALL}/jars/sqlite4java.jar:./" -Djava.net.preferIPv4Stack=true $1 $2 $3 $4 $5

