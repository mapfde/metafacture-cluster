#!/bin/bash

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` INPUT_PATH FORMAT MORPH_DEF"
  exit 65
fi

hadoop fs -rmr out/$3

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.stat.FileCounter -D cg.format=$2 -D cg.input.path=$1 -D cg.output.path=out/$3 -D cg.morphdef=statistics/$3

