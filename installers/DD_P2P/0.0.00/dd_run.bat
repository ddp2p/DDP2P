@ECHO OFF
SETLOCAL
SET DIR_ALL=%~dp0

IF NOT EXIST "%DIR_ALL%\deliberation-app.db" call "%DIR_ALL%\dd_install.bat"

java -cp "%DIR_ALL%/jars/DD.jar;%DIR_ALL%/jars/MultiSplit.jar;%DIR_ALL%/jars/sqlite4java.jar;." -D"java.net.preferIPv4Stack=true" config.DD "%DIR_ALL%\deliberation-app.db"
EndLocal
