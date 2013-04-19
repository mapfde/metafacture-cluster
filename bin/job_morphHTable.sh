#!/bin/bash

if [ $# -ne 4 ]
then
  echo "Usage: `basename $0` INPUT_TABLE MORPH_DEF (table|path) OUTPUT"
  exit 65
fi

if [ "$3" = "path" ]; then
    echo output to hdfs
    output=out/$4    
    hadoop fs -rmr $output
    R=1
else
    echo output to table
    output=$4
    R=4
fi


hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.stat.HTableMorph -D mapreduce.job.reduces=$R -D mapreduce.map.speculative=false -D cg.morphdef=$2 -D cg.input.table=$1 -D cg.output.$3=$output

