#!/usr/bin/env bash


components="core-activity-stream core-administration core-user-management core-api core-audit core-authentication core-authorization core-analytics core-content core-data-access-layer core-extension-points core-gamification core-connectivity core-i18n core-cache core-utility core-lifecycle core-mobile core-moderation core-notification core-places core-monitoring core-search core-integrations core-storage-integration core-task-engine core-web-and-ui core-legacy"

input="input.csv"
total=$(grep -c ^ $input)
(( total-=1 ))

echo "total number of packages: $total"

x=(count*100)/total

echo "component,package numbers,percent"
for comp in $components; do
    countPerComp=$(grep -c "$comp" $input)
    percent=$( printf "%.2f" $( echo -e "scale=2\n(($countPerComp * 100)/$total)" | bc ) )
    # echo "  packages for $comp -> $countPerComp :: representing: $percent"
    echo "$comp,$countPerComp,$percent"
done
