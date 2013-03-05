#!/bin/bash

for INSTITUTION in "BSZ" "BVB" "DNB" "GBV" "HBZ" "OBV"
do
    echo Ingesting $INSTITUTION
    job_cgIngest.sh $INSTITUTION marc21 cg
done

echo Ingesting HEB
job_cgIngest.sh HEB mab2 cg


job_countInHTable.sh cg allProperties.xml