import os

for i in range(0, 10):
    os.system('sudo chrt -f 99 perf stat -ddd ./run.sh benchmarks/Parity.java --args 0')
    os.system('rm benchmarks/Parity.class')
    print("------------------------------------------------------------------------------")