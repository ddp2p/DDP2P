#!/bin/bash
DB=$1
DDL=$2
awk '{print "SED " $0}' ${DDL}
sqlite3 ${DB} .dump | tr "\n\r" "@@" | sed 's/CREATE/\#CREATE/g' | sed 's/DELETE/\#DELETE/g' | sed 's/@INSERT/\#INSERT/g' | sed 's/@UPDATE/@\#UPDATE/g' | tr "#@@" "\n  " | egrep  -i '^INSERT' | tr '"' " "

