import os

exec_cmd = "sudo chrt -f 99 perf stat -ddd {cmd} ;"
approxflow_cmd = "./run.sh {benchmark} --static"
pipe = "2> perf.txt > res.txt"


def run_qifci(benchmark_path):
    cmd = "{ " + exec_cmd.format(cmd=approxflow_cmd.format(benchmark=benchmark_path)) + " } " + pipe
    os.system(cmd)

    with open("perf.txt", 'r') as perf:
        for line in perf.readlines():
            if "seconds time elapsed" in line:
                time = float((line.strip().split()[0].replace(',', '.')))

    with open("res.txt", 'r') as res:
        for line in res.readlines():
            if "Channel capacity:" in line:
                leak = float(line.strip().split()[2])

    return time, leak


def avg(lst):
    return sum(lst) / len(lst)


def all_af_benchmarks():
    directory = "benchmarks/approxFlow"
    for f in os.listdir(directory):
        benchmark(f, dir)


def benchmark(f, directory):
    print(f)
    times = []
    for i in range(0, 10):
        time, leak = run_qifci(directory + f)
        times.append(time)
        print("Time: " + str(time) + " Leak: " + str(leak))

    print("Average: " + str(avg(times)))


benchmark("Battlebits.java", "benchmarks/")