#!/bin/bash
java -cp 'jars/DD.jar:jars/sqlite4java.jar:./' -Djava.net.preferIPv4Stack=true $1 $2 $3 $4 $5
