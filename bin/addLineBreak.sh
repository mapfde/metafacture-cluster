#!/bin/bash


for file in $*
do
    echo processing $file
    gunzip $file -c | tr "\035" "\n" |  sed "s/$/\x1d/g" | gzip -c > lb_$file
done