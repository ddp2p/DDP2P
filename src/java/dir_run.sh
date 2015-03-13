#!/bin/bash
java -cp 'jars/sqlite4java.jar:jars/sqlite-jdbc-3.7.2.jar:jars/DD.jar:./' util.tools.AddrBook -d ./directory-app.db $1 $2 $3 $4 $5 $6 $7 $8 $9
