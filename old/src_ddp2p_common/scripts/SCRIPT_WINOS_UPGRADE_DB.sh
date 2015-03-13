@echo off
SetLocal

set OLD_DB=%1
set NEW_DB=%2

set NUM=%random%
set TMP_DIR=old
set DDL=%TMP_DIR%\DDL.%NUM%
set INSERTS=%TMP_DIR%\inserts.%NUM%
set STORE_DB=%TMP_DIR%\deliberation-app.%NUM%

mkdir %TMP_DIR%

REM solution if the database is open
REM scripts\sqlite3.exe %OLD_DB% .dump > %INSERTS%
REM scripts\sqlite3.exe %NEW_DB% < %INSERTS%

REM solution if the database is closed
move %NEW_DB% %STORE_DB%
move %OLD_DB% %NEW_DB%

EndLocal

