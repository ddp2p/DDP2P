#!/bin/bash
java -cp './jars/sqlite4java.jar:./jars/sqlite-jdbc-3.7.2.jar:./:./jars/DD.jar/:./jars/javax.mail.jar' hds/DirectoryServer ./directory-app.db

