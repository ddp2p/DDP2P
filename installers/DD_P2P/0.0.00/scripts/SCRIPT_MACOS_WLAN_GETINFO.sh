#!/bin/bash
INTF=$1
echo running >&2
/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport ${INTF} -I | fgrep ' SSID' | cut -d ":" -f 2 | sed 's/ *\(.*\) */\1/g'

