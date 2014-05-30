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

# OSTYPEs are "linux-gnu", "cygwin", "win32", "freebsd", "darwin"*
if [[ "$OSTYPE" == "linux-gnu" ]]; then
  ICON=
else
  ICON=-Xdock:icon=${DIR_ALL}/p2pdd_resources/p2pdd40.gif
fi
java $ICON -cp "${DIR_ALL}/jars/DD.jar:${DIR_ALL}/jars/javax.mail.jar:${DIR_ALL}/jars/sqlite-jdbc-3.7.2.jar:${DIR_ALL}/jars/MultiSplit.jar:${DIR_ALL}/jars/sqlite4java.jar:${DIR_ALL}/jars/icepdf-core.jar:${DIR_ALL}/jars/icepdf-viewer.jar:${DIR_ALL}/jars/MetaphaseEditor-1.0.0.jar:${DIR_ALL}/:./" -Djava.net.preferIPv4Stack=true widgets.app.MainFrame -d ${DIR_ALL}/deliberation-app.db $1 $2 $3 $4 $5
