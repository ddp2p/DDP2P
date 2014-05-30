#!/bin/bash
path=`pwd`
sqlite3 deliberation-app.db <<EOF
insert into application (field, value) VALUES ("SCRIPT_WIRELESS_LINUX_PATH", $path);
EOF

