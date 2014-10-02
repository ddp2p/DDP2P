import sqlite3,sys
if (len(sys.argv)<>3):
    print "usage: script1.py dbname outputFileName"
else:
    f=open(sys.argv[2],'w')#output file
    conn = sqlite3.connect(sys.argv[1])
    c = conn.cursor()
    dict={}#key=table name, value=tuple of field names
    sqlGetTableName="select name from sqlite_master where type = 'table';"
    c.execute(sqlGetTableName)
    #Example output of executing sqlGetTableName
    ##    motion
    ##    justifications
    ##    organizations
    ##    signatures
    ##    witnesses
    ##    registration
    ##    peers_addresses
    ##    directory
    ##    identities
    ##    oids
    ##    identity_values
    ##    motion_choice
    ##    certificates
    ##    constituents
    ##    neighborhoods
    ##    fields_values
    ##    fields_extra
    ##    translations
    ##    news
    ##    application
    ##    peers_my_data
    ##    peers
    ##    keys
    ##    peers_orgs
    l=[]#A list consists of table names
    for row in c:
        l.append(row[0])#each table name
    for item in l:
        tableName=str(item)
        sqlGetFieldByTableName="PRAGMA table_info("+tableName+")"
        c.execute(sqlGetFieldByTableName)
    #Example output of executing sqlGetFieldByTableName
    ##    0|peer_ID|INTEGER|1||1
    ##    1|global_peer_ID|TEXT|0||0
    ##    2|name|TEXT|0||0
    ##    3|broadcastable|NUMERIC|0||0
    ##    4|last_sync_date|TEXT|0||0
    ##    5|date|TIMESTAMP|0||0
    ##    6|slogan|TEXT|0||0
    ##    7|used|INTEGER|0|0|0
    ##    8|hash_alg|TEXT|0||0
    ##    9|signature|BLOB|0||0
    ##    10|picture|BLOB|0||0
    ##    11|no_update|INTEGER|0|0|0
    ##    12|exp_avg|REAL|0||0
    ##    13|experience|INTEGER|0||0
    ##    14|date_creation|TEXT|0||0
    ##    15|filtered|TEXT|0||0
        s="("
        for row in c:
            s=s+str(row[1])+","#field name
        s=s[:-1]#Removing trailing comma
        s=s+")"
        dict[tableName]=s#Add key(table name) and value(tuple of field names) pair to dictionary
    if(len(dict)==0):
        print "Dictionary Empty."
    else:
        for key in dict:
            f.write(str(key)+" "+str(key)+" " +str(dict[key])+"\n")
    f.close()

