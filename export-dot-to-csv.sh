#!/usr/bin/env bash

# set -x

input="jive-core-3000.5.0-SNAPSHOT.jar.components.result.dot"

echo ":: prepare result file to be uniquely linked"
result="${input}.csv"
echo "component;first degree dependency;jar file (container)"> $result
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
        # they are equal, skip the line
        ;
    else

        jarReference=$(echo "$line" | cut -d "(" -f2 | cut -d ")" -f1)
        line=$(echo "$line" | rev | cut -d";" -f2-  | rev)
    
        for i in $(echo ${compToComp[0]} | sed "s/,/ /g"); do
            # drop quotes
            temp="${i%\"}"
            temp="${temp#\"}"
            i=$temp

            directRef="${compToComp[1]}"
            # remove the jarReference
            directRef=$(echo "$directRef" | rev | cut -d"(" -f2-  | rev)

            # call your procedure/other scripts here below
            for j in $(echo "$directRef" | sed "s/,/ /g"); do
                # drop quotes
                temp="${j%\"}"
                temp="${temp#\"}"
                j=$temp

                # echo "   \"$i\" -> \"$j ($jarReference)\";"
                echo "$i;$j;$jarReference" >> $result
            done
        done

    fi

done < "$outputDotFile"


printf '\n:: finished!\n'
