#!/bin/bash

INTF=$1
ip addr list $INTF |grep "inet " |cut -d' ' -f6|cut -d/ -f1
