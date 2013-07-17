#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` INPUT_PATH"
  exit 65
fi

hadoop fs -rmr out/$1

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.text.TokenCounter -D cg.input.path=$1 -D cg.output.path=out/$1

rm -r text/$1
hadoop fs -copyToLocal out/$1 ./text/