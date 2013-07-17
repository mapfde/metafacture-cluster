#!/bin/bash

rm beacon*
awk '{print $1 "|" $3 > $2".txt"}' part-r-00000
for file in $(ls *.txt)
do
    echo "#FORMAT: PND-BEACON" > beacon.$file
    echo "#VERSION: 0.1">> beacon.$file
    echo "#TARGET: http://d-nb.info/gnd/{ID}">> beacon.$file
    echo "#CONTACT: Frau Werner <c.werner@dnb.de>">> beacon.$file
    echo "#INSTITUTION: Deutsche National Bibliothek">> beacon.$file
    echo "#DESCRIPTION:">> beacon.$file
    echo "#TIMESTAMP: " $(date) >> beacon.$file
    cat $file >> beacon.$file
    rm $file
done