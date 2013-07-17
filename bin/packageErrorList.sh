#!/bin/bash

V="BVB DNB HEB BSZ GBV HBZ"

for v in $V
do
    echo packaging $v
    grep "^$v" $1 | grep InvalidISBN > ${v}_errors.txt

done