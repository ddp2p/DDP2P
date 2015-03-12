#!/bin/bash
dir=$1
for a in $dir/*.java;
do 
cp tmp/$a $a;
done

