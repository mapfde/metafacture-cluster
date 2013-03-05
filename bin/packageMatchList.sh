#!/bin/bash

V="BVB DNB HEB BSZ GBV HBZ"

sed 's/match-eki-intern\$//g' $1 > cleaned.tmp
for v in $V
do
    echo packaging $v
    grep "^$v" cleaned.tmp | sed "s/$v[:-]//g" | gzip -c > $v.gz

done

rm cleaned.tmp