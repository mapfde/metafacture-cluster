#!/bin/bash

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` INPUT_PATH MORPH_DEF TABLE_NAME"
  exit 65
fi

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.ingest.BibIngest -D cg.output.table=$3 -D cg.input.path=$1 -D cg.format=pica  -D cg.ingest.prefix="" -D cg.store_rawdata=false -D cg.morphdef=mapping/$2