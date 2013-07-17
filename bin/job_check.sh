#!/bin/bash

if [ $# -ne 2 ]
then
  echo "Usage: `basename $0` MATCH_TABLES MORPH_DEF"
  exit 65
fi

hadoop fs -rmr out/tmp

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.match.BundleCheck -D cg.table.match=$1 -D cg.morphdef=$2 -D cg.output.path=out/tmp -D cg.key_name=bundle

hadoop fs -copyToLocal out/tmp .
