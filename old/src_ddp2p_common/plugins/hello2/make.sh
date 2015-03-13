#!/bin/bash
javac -cp ../../DD.jar:./ plugin/Main.java
jar cf hello2.jar plugin/*.class
