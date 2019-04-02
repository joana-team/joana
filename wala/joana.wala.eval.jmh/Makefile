


.PHONY: all

all: mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv mywodTIME-3000-x1-20n-maxdepth100-taurec.csv

mywod.csv :
	mvn clean install && java -cp '../../dist/joana.wala.core.jar:target/benchmarks.jar' org.openjdk.jmh.Main -rf csv -rff mywod.csv

mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv : mywod.csv
	cat mywod.csv | grep ":size" | awk '{gsub(/\"/,"")};1' > mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv

mywodTIME-3000-x1-20n-maxdepth100-taurec.csv : mywod.csv
	cat mywod.csv | grep -v ":size" | grep -v "Unit" | awk '{gsub(/\"/,"")};1' > mywodTIME-3000-x1-20n-maxdepth100-taurec.csv



