import os

for i in range(0, 1):
    os.system('sudo chrt -f 99 perf stat -ddd ./run.sh benchmarks/Laundry.java --args 0')
    os.system('rm benchmarks/Laundry.class')
    print("------------------------------------------------------------------------------")