#!/bin/bash

if [ $# -ne 4 ]
then
  echo "Usage: `basename $0` INPUT_PATH FORMAT MORPH_DEF TABLE_NAME"
  exit 65
fi

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.cluster.job.ingest.BibIngest -D cg.output.table=$4 -D cg.input.path=$1 -D cg.format=$2  -D cg.ingest.prefix="" -D cg.store_rawdata=false -D cg.morphdef=mapping/$3