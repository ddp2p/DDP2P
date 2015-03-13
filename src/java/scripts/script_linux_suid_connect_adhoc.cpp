//g++ ad-hoc_linux.cpp -o ad-hoc_linux
//sudo chown root:root ad-hoc_linux
//sudo chmod a+s ad-hoc_linux
//./ad-hoc_linux wlan1 DirectDemocracy 0.0.10
#define MAX 100
#include <stdlib.h>
#include <stdio.h>
int main(int argc, char*argv[]){
 char cmd[MAX];
 char* intf = argv[1];
 char* essid = argv[2];
 char* ip3 = argv[3];
 int err;
 snprintf(cmd, 100,"service network-manager stop");
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "ip link set %s down",intf);
 err= system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "iwconfig %s mode ad-hoc",intf);
 err= system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "iwconfig %s channel 11",intf);
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "iwconfig %s essid \"%s\"",intf,essid);
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "iwconfig %s key off",intf);
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "ip link set %s up",intf);
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "ip addr add 10.%s/8 dev %s",ip3,intf);
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 snprintf(cmd, 100, "/sbin/ifconfig %s 10.%s broadcast 10.255.255.255",intf,ip3);
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);

 //snprintf(cmd, 100, "ip link set %s down",intf)
 //system(cmd);
}
