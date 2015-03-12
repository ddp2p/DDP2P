SET INTERF=%3
SET INTERFACE=%INTERF:"=%
RUNAS /savecred /USER:%1 "netsh interface ip set address """%INTERFACE%""" static 10.0.0.%2 255.0.0.0"

