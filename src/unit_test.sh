#!/bin/bash
java -cp 'jars/DD.jar:jars/javax.mail.jar:jars/sqlite-jdbc-3.7.2.jar:jars/sqlite4java.jar:./' -Djava.net.preferIPv4Stack=true $1 $2 $3 $4 $5 $6 $7 $8 $9
