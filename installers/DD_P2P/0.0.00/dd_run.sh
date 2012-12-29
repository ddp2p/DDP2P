#!/bin/bash
#pwd
#DIR_NO_SYMLINK="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
#echo $DIR $DIR_NO_SYMLINK
##echo "$(dirname $0)"

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


java -cp "${DIR_ALL}/jars/DD.jar:${DIR_ALL}/jars/MultiSplit.jar:${DIR_ALL}/jars/sqlite4java.jar:." -Djava.net.preferIPv4Stack=true config.DD ${DIR_ALL}/deliberation-app.db

