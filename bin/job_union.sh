#!/bin/bash

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` INPUT_TABLE MORPH_DEF OUTPUT_TABLE"
  exit 65
fi

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.merge.Union -D cg.morphdef=matching/$2 -D cg.input.table=$1 -D cg.output.table=$3