#!/bin/bash

if [ $# -ne 3 ]
then
  echo "Usage: `basename $0` RECORD_TABLE MATCH_ALG MATCH_TABLE"
  exit 65
fi


echo starting match job
hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.match.Matcher -D cg.morphdef=matching/algs/$2 -D cg.input.table=$1 -D cg.output.table=$3

if [ ! -f $1 ];
then
    mkdir $1
fi

echo analyzing overlap
hadoop fs -rmr $1_overlap_tmp
hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.cluster.job.match.MatchOverlap -D cg.input.table=$3 -D cg.output.path=$1_overlap_tmp
rm $1/overlap_$3.txt
hadoop fs -copyToLocal $1_overlap_tmp/part-r-00000 $1/overlap_$3.txt


echo analyzing match groups
hadoop fs -rmr $1_matchgroups_tmp
hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.cluster.job.stat.HTableCounter -D cg.input.table=$3 -D cg.output.path=$1_matchgroups_tmp -D cg.morphdef=statistics/matchGroups.xml
rm $1/matchgroups_$3.txt
hadoop fs -copyToLocal $1_matchgroups_tmp/part-r-00000 $1/matchgroups_$3.txt


