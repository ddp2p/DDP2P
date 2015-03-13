#!/bin/bash
javac -cp ../../DD.jar:./ dd_p2p/plugin/Main.java
jar cf hello1.jar dd_p2p/plugin/*.class
