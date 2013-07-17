#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` TABLE_NAME"
  exit 65
fi


echo "create '$1', {NAME=>'prop', COMPRESSION=>'snappy', VERSIONS=>1} " | hbase shell