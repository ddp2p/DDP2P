#!/bin/bash
dir=$1
for a in $dir/*.java;
do mkdir -p tmp/$dir;
cat $a|sed 's/_DEBUG/DEBUG/g' > tmp/$a;
done

