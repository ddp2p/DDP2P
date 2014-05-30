#!/bin/bash
NUM=$1
mv deliberation-app.db deliberation-app.db.${NUM}
scripts/sqlite_db_2_DDL.sh deliberation-app.db.${NUM} > ./DDL.${NUM}
scripts/sqlite_DDL_db_2_inserts_do_awk3.sh deliberation-app.db.${NUM} DDL.${NUM} > ./inserts.${NUM}
./install.sh
sqlite3 deliberation-app.db < ./inserts.${NUM} 2>&1 | fgrep -v ' is not unique'
