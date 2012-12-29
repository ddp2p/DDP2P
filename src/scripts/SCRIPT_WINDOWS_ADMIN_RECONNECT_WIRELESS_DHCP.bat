SET INTERF=%2
SET INTERFACE=%INTERF:"=%
RUNAS /savecred /USER:%1 "netsh int ip set address """%INTERFACE%""" dhcp"


