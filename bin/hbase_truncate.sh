#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` TABLE_NAME"
  exit 65
fi

echo "truncate '$1'" | hbase shell