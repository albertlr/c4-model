# C4 Model Command Line Interface (CLI)

## Why
todo
## Purpose

todo

## How to build

```
mvn clean install
```

and then you can run the jar directly from the `target` folder as seen in the bash scripts

## Blast Radius computation

Whern building the project will create in `target` folder the `c4-model-1.0-SNAPSHOT-blast-radius-cli-jar-with-dependencies.jar` jar file which will invoke the `ro.albertlr.c4.BlastRadius` main class.

usage:
```shell script
usage: ./compute-blast-radius.sh <dot-file> <csv-file>
 -c,--csv-file <csv-file>    CSV file (actually not comma, need to be
                             semi-colon separated)
 -d,--dot-file <dot-file>    Dot file
 -x,--xls-file <xslx-file>   XLSX file
```


example
```shell script
 ./compute-blast-radius.sh jive-core-3000.5.0.jar-fas-clean.dot.comp.dot.out jive-core-v2-FAs-97-components.csv 
```

