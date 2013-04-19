#!/bin/bash

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` RECORD_TABLE MATCH_ALG MATCH_TABLE"
  exit 65
fi

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.match.Matcher -D cg.morphdef=matching/algs/$2 -D cg.input.table=$1 -D cg.output.table=$3