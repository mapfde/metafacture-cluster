#!/bin/bash

rm all.gz

for file in $(ls *.gz)
do
    echo adding $file
    cat $file  >> all.gz
done