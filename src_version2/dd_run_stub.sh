#!/bin/bash
java -cp 'libs/MultiSplit.jar:sqlite4java-201/sqlite4java.jar:./' -Djava.net.preferIPv4Stack=true config.DD -jar $0 $*
exit

