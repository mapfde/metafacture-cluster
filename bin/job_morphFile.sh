#!/bin/bash

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` INPUT_PATH FORMAT MORPH_DEF"
  exit 65
fi

hadoop fs -rmr out/$3_$1 > /dev/null
hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.stat.FileMorph -D cg.morphdef=statistics/$3 -D cg.format=$2 -D cg.input.path=$1 -D cg.output.path=out/$3_$1
rm -r $3/$1
hadoop fs -copyToLocal out/$3_$1 ./$3/$1 > /dev/null