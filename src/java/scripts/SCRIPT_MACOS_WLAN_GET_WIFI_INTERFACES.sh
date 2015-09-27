# should return a list of wireless interface names, separated by ":"
ifconfig | egrep "^[a-zA-Z]" | fgrep -v "LOOPBACK" | fgrep "BROADCAST" | sed "s/:.*$//g" | tr "\r\n" "::" | sed "s/:+/:/g" | sed "s/:$//g"
