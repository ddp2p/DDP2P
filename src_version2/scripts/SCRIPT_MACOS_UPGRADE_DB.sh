#!/bin/bash
OLD_DB=$1
NEW_DB=$2
# deliberation-app.db

NUM=$RANDOM
TMP_DIR=old
DDL=${TMP_DIR}/DDL.${NUM}
INSERTS=${TMP_DIR}/inserts.${NUM}

mkdir -p ${TMP_DIR}
# cp deliberation-app.db old/deliberation-app.db.${NUM}

scripts/sqlite_db_2_DDL.sh ${OLD_DB} > ${DDL}
scripts/sqlite_DDL_db_2_inserts_do.sh ${OLD} ${DDL} > ${INSERTS}
# ./install.sh
sqlite3 ${NEW_DB} < ${INSERTS} 2>&1 | fgrep -v ' is not unique'

