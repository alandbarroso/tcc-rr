#!/bin/bash

FILES=`ls *.dot`

for f in $FILES
do
	dot -Tpng:cairo -o${f%\.dot}.png $f
done

