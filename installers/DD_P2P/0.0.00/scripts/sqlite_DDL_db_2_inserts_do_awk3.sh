#!/bin/bash
DB=$1
DDL=$2
./scripts/sqlite_DDL_db_2_inserts.sh ${DB} ${DDL} | awk '/^SED/ {k=$2; n=$3; $3=$2=$1=""; OFS=",";  r[k] = n  " ("  $0  ")"; sub(/\(,,,/,"(",r[k]);} /^INSERT/ { OFS=" "; $3=r[$3]; print;}'
