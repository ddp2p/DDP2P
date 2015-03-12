#!/bin/bash
javac -cp ../../DD.jar:./ plugin/Main.java
jar cf hello1.jar plugin/*.class
