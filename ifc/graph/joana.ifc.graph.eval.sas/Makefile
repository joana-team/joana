.PHONY: all

all: ntiodSIZE-3000-x1-15n-maxdepth100-taurec.csv ntiodTIME-3000-x1-15n-maxdepth100-taurec.csv randomnticd.oneentryperline.csv

ntiod.csv :
	mvn clean install -DskipTests && java -cp 'target/benchmarks.jar' org.openjdk.jmh.Main countNTIODSize -rf csv -rff $@

ntiodSIZE-3000-x1-15n-maxdepth100-taurec.csv : ntiod.csv
	cat $< | grep ":size" | awk '{gsub(/\"/,"")};1' > $@

ntiodTIME-3000-x1-15n-maxdepth100-taurec.csv : ntiod.csv
	cat $< | grep -v ":size" | grep -v "Unit" | awk '{gsub(/\"/,"")};1' > $@


randomnticd.csv : 
	mvn clean install -DskipTests && java -cp 'target/benchmarks.jar' org.openjdk.jmh.Main testRandom -rf csv -rff $@

randomnticd.oneentryperline.csv : randomnticd.csv
	cat $< | dos2unix | tail -n +2 |                                awk '{gsub(/\"/,"")};1' > $@


JAVAFILES = $(shell find src -name '*.java')
MISCFILES = README Makefile pom.xml .project .classpath $(shell find .settings)
 
sas-artifact-joana.tar.gz : $(MISCFILES) $(JAVAFILES)
	rm -f $@ ; tar hczf $@ $^ $(MISCFILES) $(JAVAFILES)
