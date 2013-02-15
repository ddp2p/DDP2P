#!/bin/bash
#mv deliberation-app.db deliberation-app.db.0
cat createEmptyDelib.sqlite.comments | sed 's/\#.*//g' >createEmptyDelib.sqlite
echo "comments parsed"
sqlite3 deliberation-app.db < createEmptyDelib.sqlite
echo "basic db created"
sqlite3 deliberation-app.db < createInitDelib.sqlite
echo "initial data inserted"
sqlite3 deliberation-app.db < createTrigDelib.sqlite
echo "triggers added"
if [ -e directory.db ]
then
 unlink directory.db
fi
sqlite3 directory.db < createEmptyDir.sqlite
echo "directory database made"
make
#./qin.sh

#scripts/addpaths.sh
pwd
#./unit_test.sh widgets.wireless.WirelessSetup `pwd`/

