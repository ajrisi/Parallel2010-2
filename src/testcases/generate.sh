#!/bin/bash

file_sizes="512 1k 2k 4k 8k 16k 32k 64k 128k"

for i in ${file_sizes}; do
	echo $i
	dd if=/dev/urandom of=test${i}_b bs=${i} count=1
	dd if=/dev/urandom of=test${i}_a bs=${i} count=1
done
