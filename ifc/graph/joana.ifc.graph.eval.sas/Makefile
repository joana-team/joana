.PHONY: all

all: mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv mywodTIME-3000-x1-20n-maxdepth100-taurec.csv randomnticd.oneentryperline.csv

mywod.csv :
	mvn clean install -DskipTests && java -cp 'target/benchmarks.jar' org.openjdk.jmh.Main countNTIODSize -rf csv -rff $@

mywodSIZE-3000-x1-20n-maxdepth100-taurec.csv : mywod.csv
	cat $< | grep ":size" | awk '{gsub(/\"/,"")};1' > $@

mywodTIME-3000-x1-20n-maxdepth100-taurec.csv : mywod.csv
	cat $< | grep -v ":size" | grep -v "Unit" | awk '{gsub(/\"/,"")};1' > $@


randomnticd.csv : 
	mvn clean install -DskipTests && java -cp 'target/benchmarks.jar' org.openjdk.jmh.Main testRandom -rf csv -rff $@

randomnticd.oneentryperline.csv : randomnticd.csv
	cat $< | dos2unix | tail -n +2 |                                awk '{gsub(/\"/,"")};1' > $@



