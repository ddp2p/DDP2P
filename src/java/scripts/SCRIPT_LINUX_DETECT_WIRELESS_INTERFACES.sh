#!/bin/bash

ip link show|grep "^[0-9]\+"|awk '{ print $2 }'|tr -d ":"|sort
