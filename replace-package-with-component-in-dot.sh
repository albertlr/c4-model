#!/usr/bin/env bash

# set -x

# 1. Create ProgressBar function
# 1.1 Input is currentState($1) and totalState($2)
function ProgressBar {
# Process data
    let _progress=(${1}*100/${2}*100)/100
    let _done=(${_progress}*4)/10
    let _left=40-$_done
# Build progressbar string lengths
    _fill=$(printf "%${_done}s")
    _empty=$(printf "%${_left}s")

# 1.2 Build progressbar strings and print the ProgressBar line
# 1.2.1 Output example:                           
# 1.2.1.1 Progress : [########################################] 100%
printf "\rProgress : [${_fill// /#}${_empty// /-}] ${_progress}%%"

}

# Variables
_start=1


declare -A packageToComponentMap

# csvFile="/Users/albertlr/workspace/work/jive/diagrams/input.csv"
# dotfile="/Users/albertlr/workspace/work/jive/diagrams/jive-core-3000.5.0-SNAPSHOT.jar.components.dot"

csvFile="$(pwd)/$1"
dotfile="$(pwd)/$2"

echo "csv: $csvFile"
echo "dot: $dotFile"

# csvFile="/Users/albertlr/workspace/work/jive/diagrams/test-input.csv"
# dotfile="/Users/albertlr/workspace/work/jive/diagrams/test.dot"

echo ":: read package to component mapping"

delimiter=";"
while IFS= read -r line
do
    # echo "line: $line"
    keyValuePair=()
    string=$line$delimiter
    while [[ $string ]]; do
        keyValuePair+=( "${string%%"$delimiter"*}" )
        string=${string#*"$delimiter"}
    done
    #echo "key- value: (${keyValuePair[0]} :: ${keyValuePair[1]})"
    packageToComponentMap["${keyValuePair[0]}"]=${keyValuePair[1]}
done < "$csvFile"

# This accounts as the "totalState" variable for the ProgressBar function
_end=${#packageToComponentMap[@]}


echo ":: replace packages with components"

iteration=1

for key in "${!packageToComponentMap[@]}"; do
    #echo "($key :: ${packageToComponentMap[$key]})"
    search="$key"
    replace="${packageToComponentMap[$key]}"
    # replace the package "fully.qualified.package.name[" ]
    sed -i "" "s|\(\"${search}\)\([\" ]\)|\"${replace}\2|g" $dotfile

    ProgressBar ${iteration} ${_end}
    iteration=$((iteration+1))
done

echo
echo ":: remove empty space around \"->\""
# remove empty space around "->"
sed -i "" -E "s|\"[ ]+-> \"|\" -> \"|g" $dotfile


outputDotFile="${dotfile}.unique.dot"
echo ":: remove duplicates and output them to $outputDotFile"
# reference: https://unix.stackexchange.com/a/194790

cat -n $dotfile | sort -k2 -k1n  | uniq -f1 | sort -nk1,1 | cut -f2- > $outputDotFile

packages="java javax jdk org net oauth gnu colt joptsimple freemarker info software io cz ru"
for package in $packages
do
    echo "remove lines containing mapping for $package.* packages"
    sed -i "" "/-> \"$package\./d" $outputDotFile
done

echo "remove lines containing mapping for 'com.google.*' packages"
sed -i "" '/-> "com\.[^j]/d' $outputDotFile
echo "remove lines containing mapping for 'ognl.*' packages"
sed -i "" '/-> "ognl./d' $outputDotFile
echo "remove lines containing mapping for 'com.google.*' packages"
sed -i "" '/-> "com\.jayway\.]/d' $outputDotFile

echo ":: prepare result file to be uniquely linked"
result="${outputDotFile}.result"
echo > $result
delimiter="->"
while IFS= read -r line
do
    # echo "line: $line"
    compToComp=()
    string=$line$delimiter
    while [[ $string ]]; do
        compToComp+=( "${string%%"$delimiter"*}" )
        string=${string#*"$delimiter"}
    done
    # echo "key- value: (${compToComp[0]} :: ${compToComp[1]})"

    if [ "${#compToComp[@]}" -eq "1" ]; then
        # they are equal
        echo "$line" >> $result
    else

        jarReference=$(echo "$line" | cut -d "(" -f2 | cut -d ")" -f1)
    
        for i in $(echo ${compToComp[0]} | sed "s/,/ /g"); do
            # drop quotes
            temp="${i%\"}"
            temp="${temp#\"}"
            i=$temp

            directRef="${compToComp[1]}"
            directRef=$(echo "$directRef" | rev | cut -d";" -f2-  | rev)
            # remove the jarReference
            directRef=$(echo "$directRef" | rev | cut -d"(" -f2-  | rev)

            # call your procedure/other scripts here below
            for j in $(echo "$directRef" | sed "s/,/ /g"); do
                # drop quotes
                temp="${j%\"}"
                temp="${temp#\"}"
                j=$temp

                # echo "   \"$i\" -> \"$j ($jarReference)\";"
                echo "   \"$i\" -> \"$j ($jarReference)\";" >> $result
            done
        done

    fi

done < "$outputDotFile"


outputResult="${result}.dot"
echo ":: remove duplicates and output them to $outputResult"
cat -n $result | sort -k2 -k1n  | uniq -f1 | sort -nk1,1 | cut -f2- > $outputResult


printf '\n:: finished!\n'
