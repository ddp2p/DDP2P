#!/bin/bash
DB=$1
sqlite3 ${DB} 'select sql from sqlite_master' | tr "\n\r" "  " | sed 's/CREATE/\#CREATE/g' | tr "#" "\n" | tr '"' ' ' | sed 's/CREATE TABLE[[:space:]]*//g' | sed 's/^\([^ ]*\)[[:space:]]/\1 , \1 , /g' | tr "()" "  " | egrep -v '^CREATE' | sed 's/[[:space:]][[:space:]]*/ /g' | sed 's/\([a-zA-Z0-9_]\) [^\,]*/\1/g' | tr "," " " | sed 's/FOREIGN//g'

