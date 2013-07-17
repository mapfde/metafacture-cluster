#!/bin/bash

for file in $* 
do 
    unzip $file 
    tar -xf ${file/.gz/}
done


