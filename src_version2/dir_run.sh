#!/bin/bash
java -cp 'jars/sqlite4java.jar:jars/sqlite-jdbc-3.7.2.jar:jars/DD.jar:./' util.tools.AddrBook ./directory-app.db $1 $2
