#!/bin/bash

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` MATCH_TABLE"
  exit 65
fi

hadoop fs -rm -r out/$1/overlap/
hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.match.MatchOverlap -D cg.input.table=$1 -D cg.output.path=out/$1/overlap

 