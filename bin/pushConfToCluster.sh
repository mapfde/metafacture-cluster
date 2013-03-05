#!/bin/bash

for host in $(cat /hadoop/conf/slaves)
do
    echo $host
    scp -r /hadoop/conf $host:/hadoop/
done