#!/bin/bash

INTF=$1

iwconfig $INTF | grep 'ESSID' | sed 's/.*ESSID://g'|tr "\"" " " | sed 's/ //g'
