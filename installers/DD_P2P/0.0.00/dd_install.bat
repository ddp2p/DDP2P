@ECHO OFF
SETLOCAL
SET DIR_ALL=%~dp0
cd "%DIR_ALL%"

SET VAL=%random%
echo "Will save old database, if existant, using ending:" %VAL%
MKDIR old
IF EXIST deliberation-app.db MOVE deliberation-app.db old\deliberation-app.db.%VAL%
IF EXIST directory-app.db MOVE directory-app.db old\directory-app.db.%VAL%

echo "Will create new databases"
.\scripts\sqlite3.exe deliberation-app.db < createEmptyDelib.sqlite
.\scripts\sqlite3.exe deliberation-app.db < createInitDelib.sqlite
REM .\scripts\sqlite3.exe deliberation-app.db < createTrigDelib.sqlite
.\scripts\sqlite3.exe directory-app.db < createEmptyDir.sqlite

echo "Will configure install dir as:" %DIR_ALL%
set DIR_ALL=%DIR_ALL:~0,-1%
call .\\unit_test.bat  widgets.wireless.WirelessSetup "" "%DIR_ALL%"

EndLocal
