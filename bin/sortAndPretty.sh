#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` FILE"
  exit 65
fi
echo -e 'count\tname' > $1.csv
awk 'BEGIN { FS = "\t" };{ print $2"\t"$1 }' $1 | sort -nr >> $1.csv