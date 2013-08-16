@ECHO OFF
SETLOCAL
scripts\sqlite3.exe deliberation-app.db < createEmptyDelib.sqlite
scripts\sqlite3.exe deliberation-app.db < createInitDelib.sqlite
scripts\sqlite3.exe deliberation-app.db < createTrigDelib.sqlite
scripts\sqlite3 directory-app.db < createEmptyDir.sqlite

REM compile.bat
REM echo "Compiled"
REM find the path where 'this' script is found (install.bat should be in the root of the installation)
SET MYDDPATH=%~dp0
REM should use %CD% or %__CD__% if the relevant path is the one from which the command is run
echo "MYDDPATH=" %MYDDPATH%
set MYDDPATH=%MYDDPATH:~0,-1%
.\\unit_test.bat  widgets.wireless.WirelessSetup "" "%MYDDPATH%"

