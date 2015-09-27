# should return a list of wireless interface names, separated by ":"
/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport prefs | fgrep ":" | sed "s/^.* //g" | sed "s/://g" | tr "\r\n" "::" | sed "s/:+/:/g" | sed "s/:$//g"
