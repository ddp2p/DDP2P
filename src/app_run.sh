#!/bin/bash
java -cp 'jars/DD.jar:jars/MultiSplit.jar:jars/sqlite4java.jar:./' -Djava.net.preferIPv4Stack=true config.DD ./deliberation-app.db
