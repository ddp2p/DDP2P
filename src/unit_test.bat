@ECHO OFF
SETLOCAL
SET DIR_ALL=%~dp0
echo "par 1=" %1
echo "par 2=" %2
echo "par 3=" %3
echo "par 4=" %4
echo "par 5=" %5

java -cp "%DIR_ALL%/jars/DD.jar;%DIR_ALL%/jars/sqlite4java.jar;%DIR_ALL%/jars/MultiSplit.jar" -D"java.net.preferIPv4Stack=true" %1 %2 %3 %4 %5
