#!/bin/bash

hadoop fs -rmr out/beacon

hadoop jar $HOME/jobs/$CULTUREGRAPH_JOB_JAR org.culturegraph.mf.cluster.job.beacon.FileBeacon -D cg.input.path=cbs -D cg.output.path=out/beacon -D cg.morphdef=beacon/beacon.xml

rm -r beacon
hadoop fs -copyToLocal out/beacon ./beacon/