import sqlite3,sys
if (len(sys.argv)<>4):
    print "usage: script2.py database_name inputFileName outputFileName"
else:
    conn=sqlite3.connect(sys.argv[1])
    fin=open(sys.argv[2],'r')
    fout=open(sys.argv[3],'w')
    dict={}
    for row in fin.readlines():
        s=row.split(' ')
        value="\""+str(s[1])+"\" "+str(s[2])
        dict[s[0]]=str(value).rstrip()
    #Get dump data from sqlite3 database,same as command: sqlite3 ex.db .dump
    for line in conn.iterdump():
        j=line.split(' ')
        b=""#String to construct new insert sentence
        if(j[0].lower()=='insert'):#Lines start with 'insert' keyword
            b=""+str(j[0])+" "+ str(j[1])+" "+dict[str(j[2]).replace('\"','')]+" "
            for item in j[3:]:
                b=b+" "+item
            fout.write(b+"\n")
    fout.close()

