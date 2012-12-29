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


VERSION=`cat ${DIR_ALL}/LATEST | tr -d ' \r\n'`
echo "Path=" "${DIR_ALL}/${VERSION}/dd_run.sh"

if [ ! -e "${DIR_ALL}/${VERSION}/dd_run.sh" ]
then
    cd "${DIR_ALL}/${VERSION}"
    "${DIR_ALL}/${VERSION}/update.sh" NO PREVIOUS DATABASE TO IMPORT
    cd ..
fi

${DIR_ALL}/${VERSION}/dd_run.sh


