#!/bin/bash
javac -cp ../../DD.jar:javamail-1.4.4/mail.jar:./ chatApp/*.java dd_p2p/plugin/*.java 
jar cf chatApp.jar chatApp/*.class dd_p2p/plugin/*.class chatApp_icons/*

