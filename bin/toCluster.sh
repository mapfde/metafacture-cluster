#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` CLUSTER_HOST"
  exit 65
fi


mvn clean assembly:assembly
scp target/*-job.jar $1:jobs/
