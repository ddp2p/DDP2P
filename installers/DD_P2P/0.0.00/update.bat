@echo off
SetLocal

SET VERSION=0.9.3
set DIR_ALL=%~dp0
cd "%DIR_ALL%"

java -jar "ddp2p_%VERSION%.jar"

echo "Current Path: " %CD%

call .\dd_install.bat

set PREVIOUS="%DIR_ALL%..\PREVIOUS"

rem set "RPATH=%DIR_ALL:~0,-1%"
rem FOR /F (%RPATH%) set PREVIOUS=%dpi

rem Next line has to be called even if %PREVIOUS% is not yet created
rem IF EXIST %PREVIOUS% call .\unit_test.bat config.UnitSetImport "%PREVIOUS%"

rem Will set importing indicators only if called without parameters
SET /A ARGS_COUNT=0
FOR %%A in (%*) DO SET /A ARGS_COUNT+=1

IF %ARGS_COUNT% EQU 0 call .\unit_test.bat config.UnitSetImport "%PREVIOUS%"

EndLocal

