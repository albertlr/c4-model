#!/usr/bin/env bash

source prepare-env.sh

java -jar target/c4-model-1.0-SNAPSHOT-mapping-cli-jar-with-dependencies.jar $@
#  --dot-file "$1" --csv-file "$2" \
#  --filter-