#!/bin/bash
cat createEmptyDelib.sqlite.comments | sed 's/\#.*//g' >createEmptyDelib.sqlite

