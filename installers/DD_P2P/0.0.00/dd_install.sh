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

mkdir -p old
VAL=$RANDOM
if [ -e "./deliberation-app.db" ]
then
	mv ./deliberation-app.db ./old/deliberation-app.db.${VAL}
fi

if [ -e "./directory-app.db" ]
then
	mv ./directory-app.db ./old/directory-app.db.${VAL}
fi

sqlite3 ./deliberation-app.db < ./createEmptyDelib.sqlite
sqlite3 ./deliberation-app.db < ./createInitDelib.sqlite
#sqlite3 ./deliberation-app.db < ./createTrigDelib.sqlite
sqlite3 ./directory-app.db < ./createEmptyDir.sqlite

#REM compile.bat
#REM echo "Compiled"
#REM find the path where 'this' script is found (install.bat should be in the root of the installation)
#REM should use %CD% if the relevant path is the one from which the command is run
pwd
./unit_test.sh  widgets.wireless.WirelessSetup "" `pwd`/

cd scripts
make
cd ..
#sudo chown root scripts/script_linux_suid_*c
#sudo chmod a+s scripts/script_linux_suid_*c

