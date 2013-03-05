#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` TABLE_NAME"
  exit 65
fi

echo "disable '$1'; drop '$1'" | hbase shell