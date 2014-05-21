#!/bin/bash

#JavaSE-1.6
#source=\"1.6\"
#source=1.6
#target=\"1.6\"
#target=1.6
#compliance=\"1.6\"
#compliance=1.6
#targetPlatform=\"1.6\"
#targetPlatform=1.6

PATTERNS=(\\\(JavaSE-\\\)\\\(1.6\\\) \\\(source\=\"\\\)\\\(1.6\\\) \\\(source\=\\\)\\\(1.6\\\) \\\(target\=\"\\\)\\\(1.6\\\) \\\(target\=\\\)\\\(1.6\\\) \\\(compliance\=\"\\\)\\\(1.6\\\) \\\(compliance\=\\\)\\\(1.6\\\) \\\(targetPlatform\=\"\\\)\\\(1.6\\\) \\\(targetPlatform\=\\\)\\\(1.6\\\))


for (( i = 0 ; i < ${#PATTERNS[@]} ; i=$i+1 ));
do
  echo ${PATTERNS[${i}]}
  find api/ deprecated/ doc/ example/ ifc/ ui/ util/ wala/ -name "*.xml" -exec sed -i -e "s/${PATTERNS[${i}]}/\11.7/g" {} \;
  find api/ deprecated/ doc/ example/ ifc/ ui/ util/ wala/ -name "*.classpath" -exec sed -i -e "s/${PATTERNS[${i}]}/\11.7/g" {} \;
  find api/ deprecated/ doc/ example/ ifc/ ui/ util/ wala/ -name "MANIFEST.MF" -exec sed -i -e "s/${PATTERNS[${i}]}/\11.7/g" {} \;
  find api/ deprecated/ doc/ example/ ifc/ ui/ util/ wala/ -name "org.eclipse.jdt.core.prefs" -exec sed -i -e "s/${PATTERNS[${i}]}/\11.7/g" {} \;
done


#grep -R "1.6" api/ deprecated/ doc/ example/ ifc/ ui/ util/ wala/ | grep -v source=\"1.6\" | grep -v JavaSE-1.6 | grep -v source=1.6 | grep -v target=\"1.6\"  | grep -v target=1.6  | grep -v compliance=\"1.6\"  | grep -v  compliance=1.6  | grep -v targetPlatform=\"1.6\" | grep -v targetPlatform=1.6 | less
