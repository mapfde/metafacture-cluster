#!/bin/bash
if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` TABLE_NAME"
  exit 65
fi
echo "scan '$1', {LIMIT=>3}" | hbase shell