#!/bin/bash


if [ $# -ne 2 ]
then
  echo "Usage: `basename $0` INPUT_PATH TABLE"
  exit 65
fi


hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.ingest.WikipediaIngest -D cg.output.table=$2 -D cg.input.path=$1  -D cg.store_rawdata=false