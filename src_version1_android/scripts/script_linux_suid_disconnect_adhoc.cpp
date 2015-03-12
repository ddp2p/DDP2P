#include <stdlib.h>
#include <stdio.h>
#define MAX 100
int main(int argc, char*argv[]){
 char cmd[MAX];
 int err;
 snprintf(cmd, 100,"service network-manager start");
 err=system(cmd);
 printf("result=%d cmd=%s\n",err,cmd);
}
