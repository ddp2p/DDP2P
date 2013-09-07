#!/bin/bash
java -cp 'jars/DD.jar:jars/javax.mail.jar:jars/sqlite-jdbc-3.7.2.jar:jars/MultiSplit.jar:jars/sqlite4java.jar:jars/icepdf-core.jar:jars/icepdf-viewer.jar:jars/MetaphaseEditor-1.0.0.jar:./' -Djava.net.preferIPv4Stack=true config.DD ./deliberation-app.db
