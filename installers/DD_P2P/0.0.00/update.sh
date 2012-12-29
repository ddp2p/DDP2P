#!/bin/bash
# Should be called with parameter to set it for importing

VERSION=0.9.3

SOURCE="${BASH_SOURCE[0]}"
DIR_ALL="$( dirname "$SOURCE" )"
while [ -h "$SOURCE" ]
do 
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
  DIR="$( cd -P "$( dirname "$SOURCE"  )" && pwd )"
done
DIR_ALL="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
cd "${DIR_ALL}"

jar xvf "${DIR_ALL}/ddp2p_${VERSION}.jar"

chmod a+x *.sh scripts/*sh

./dd_install.sh


PREVIOUS="$(dirname ${DIR_ALL})/PREVIOUS"
### PREVIOUS MAY NOT YET HAVE BEEN CREATED, BUT HAS TO BE SET
#if [ -e "${PREVIOUS}" ]
if [ $# -eq 0 ]
then
	./unit_test.sh config.UnitSetImport "${PREVIOUS}"
fi

