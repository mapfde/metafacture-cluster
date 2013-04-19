#!/bin/bash

if [ $# -ne 2 ]
then
  echo "Usage: `basename $0` TABLE_NAME MORPH_DEF"
  exit 65
fi

hadoop fs -rmr out/$1/$2

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.stat.HTableCounter -D cg.input.table=$1 -D cg.output.path=out/$1/$2/ -D cg.morphdef=statistics/$2
