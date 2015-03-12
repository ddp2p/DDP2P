SET NB=%1
mv deliberation_app.db deliberation_app.db-good%NB%
./unit_test.bat util.DBAlter deliberation_app.db-good%NB% deliberation_app.db DDL_%NB%

