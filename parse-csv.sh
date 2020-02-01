#!/usr/bin/env bash


declare -A arr

#arr["key1"]=val1

input="/Users/albertlr/workspace/work/jive/diagrams/input.csv"
while IFS= read -r line
do
    #echo "line: $line"
    packageToComponentMap=(${line//;/ })
    # echo "key- value: (${packageToComponentMap[0]} :: ${packageToComponentMap[1]})"
    arr["${packageToComponentMap[0]}"]=${packageToComponentMap[1]}
done < "$input"


for key in "${!arr[@]}"; do
    echo "($key :: ${arr[$key]})"
done