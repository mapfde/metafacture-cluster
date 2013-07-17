#!/bin/bash

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` TABLE_NAME MORPH_DEF OUT_TABLE"
  exit 65
fi


hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.stat.HTableMorph -D cg.morphdef=statistics/$2 -D cg.input.table=$1 -D cg.output.path=$3 -D cg.to_table=true -D mapred.reduce.tasks=4