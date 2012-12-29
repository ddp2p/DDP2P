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
cd ${DIR_ALL}

VERSION=`cat LATEST | tr -d ' \r\n'`
mkdir -p "${VERSION}"
cd "${VERSION}"
echo "BASE PATH:" `pwd`

if [ ! -e "${DIR_ALL}/${VERSION}/dd_install.sh" ]
then
    "${DIR_ALL}/${VERSION}/update.sh"
fi

./dd_install.sh

