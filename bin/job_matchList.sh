#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` MATCH_TABLE"
  exit 65
fi

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.match.MatchList -D cg.input.table=$1 -D cg.output.path=$1_list_tmp
rm -r match/list/$1
hadoop fs -copyToLocal $1_list_tmp match/list/$1
hadoop fs -rmr $1_list_tmp 