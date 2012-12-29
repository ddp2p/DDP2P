@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION
SET DIR_ALL=%~dp0
echo "Batch Folder:" %DIR_ALL%

set LATEST="%DIR_ALL%\LATEST"

set VERSION=
for /F "usebackq delims=" %%i in (%LATEST%) do set VERSION=!VERSION!%%i

echo "Instalation Version=" "%VERSION%" 
set VERSION=%VERSION: =%

echo "Path=" "%DIR_ALL%\%VERSION%\dd_run.bat"

IF NOT EXIST "%DIR_ALL%\%VERSION%\dd_run.bat" cd "%DIR_ALL%\%VERSION%\"
IF NOT EXIST "%DIR_ALL%\%VERSION%\dd_run.bat" call "%DIR_ALL%\%VERSION%\update.bat" NO PREVIOUS DATABASE TO IMPORT

"%DIR_ALL%\%VERSION%\dd_run.bat"
EndLocal
