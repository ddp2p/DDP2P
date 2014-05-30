#!/bin/bash
NB=$1
# mv deliberation_app.db deliberation_app.db-good$NB
./unit_test.sh util.DBAlter deliberation-app.db-good$NB deliberation-app.db DDL_$NB

