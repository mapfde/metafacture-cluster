#!/bin/bash

if [ $# -ne 0 ]
then
  echo "Usage: `basename $0`"
  exit 65
fi

echo "list" | hbase shell