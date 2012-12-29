@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION
SET DIR_ALL=%~dp0
CD %DIR_ALL%

SET LATEST="%DIR_ALL%\LATEST"

SET VERSION=
FOR /F "usebackq delims=" %%i in (%LATEST%) do set VERSION=!VERSION!%%i

SET VERSION=%VERSION: =%

MKDIR "%VERSION%"
cd "%VERSION%"
echo "BASE PATH:" %CD%

IF NOT EXIST "%DIR_ALL%\%VERSION%\dd_install.bat" call "%DIR_ALL%\%VERSION%\update.bat"

.\dd_install.bat
EndLocal
