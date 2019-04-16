


.PHONY: all

all: mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv mywodTIME-3000-x1-20n-maxdepth100-taurec.csv

mywod.csv :
	mvn clean install && java -cp '../../dist/joana.wala.core.jar:target/benchmarks.jar' org.openjdk.jmh.Main countMyWodSize -rf csv -rff mywod.csv

mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv : mywod.csv
	cat mywod.csv | grep ":size" | awk '{gsub(/\"/,"")};1' > mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv

mywodTIME-3000-x1-20n-maxdepth100-taurec.csv : mywod.csv
	cat mywod.csv | grep -v ":size" | grep -v "Unit" | awk '{gsub(/\"/,"")};1' > mywodTIME-3000-x1-20n-maxdepth100-taurec.csv



javanitcd.csv :
	mvn clean install && java -cp '../../dist/joana.api.jar:target/benchmarks.jar' org.openjdk.jmh.Main testProcedureCFGNTICD -rf csv -rff javanitcd.csv

javanitcd.oneentryperline.csv : javanitcd.csv Makefile
	cat javanitcd.csv | dos2unix | tail -n +2 | sed -e '$!N;$!N;s/\n/,/g' | awk '{gsub(/\"/,"")};1' > javanitcd.oneentryperline.csv


javanitcd-icfg.csv :
	mvn clean install && java -cp '../../dist/joana.api.jar:target/benchmarks.jar' org.openjdk.jmh.Main testWholeProgramCFGNTICD -rf csv -rff javanitcd-icfg.csv

javanitcd-icfg.oneentryperline.csv : javanitcd-icfg.csv Makefile
	cat javanitcd-icfg.csv | dos2unix | tail -n +2 | sed -e '$!N;$!N;s/\n/,/g' | awk '{gsub(/\"/,"")};1' > javanitcd-icfg.oneentryperline.csv


randomnticd.csv : 
	mvn clean install && java -cp '../../dist/joana.api.jar:target/benchmarks.jar' org.openjdk.jmh.Main testRandom -rf csv -rff randomnticd.csv

randomnticd.oneentryperline.csv : randomnticd.csv Makefile
	cat randomnticd.csv | dos2unix | tail -n +2 |                                awk '{gsub(/\"/,"")};1' > randomnticd.oneentryperline.csv



