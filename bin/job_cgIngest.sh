#!/bin/bash
if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` INPUT_PATH FORMAT TABLE_NAME"
  exit 65
fi
hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.ingest.BibIngest -D cg.output.table=$3 -D cg.input.path=data/$1 -D cg.format=$2  -D cg.ingest.prefix=$1- -D cg.morphdef=mapping/ingest.$2.xml -D cg.store_rawdata=false