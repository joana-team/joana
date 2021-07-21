import os

exec_cmd = "sudo chrt -f 99 perf stat -ddd {cmd} ;"
static_cmd = "./run.sh {benchmark} --static"
dyn_cmd = "./run.sh {benchmark}"
pipe = "2> perf.txt > res.txt"


def run_qifci(benchmark_path, do_static):
    if do_static:
        raw_cmd = static_cmd
    else:
        raw_cmd = dyn_cmd
    cmd = "{ " + exec_cmd.format(cmd=raw_cmd.format(benchmark=benchmark_path)) + " } " + pipe
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


def benchmark(f, directory, do_static):
    print(f)
    times = []
    for i in range(0, 10):
        time, leak = run_qifci(directory + f, do_static)
        times.append(time)
        print("Time: " + str(time) + " Leak: " + str(leak))

    print("Average: " + str(avg(times)))


benchmark("x_Mask --args 0", "benchmarks/", False)

# benchmark("x_CallMask.java --args 0", "benchmarks/")
# benchmark("x_RecursiveLaundry.java --args 0", "benchmarks/")
# benchmark("x_Parity.java --args 0", "benchmarks/")
# benchmark("x_Voting1.java", "benchmarks/")
# benchmark("x_ShiftAndLaunder.java", "benchmarks/")
# benchmark("x_Laundry.java -- args 0", "benchmarks/")
# benchmark("x_ImplicitFlow.java", "benchmarks/")
# benchmark("x_SanityCheck.java --args 0", "benchmarks/")
# benchmark("x_Sum.java --args 0 1 2", "benchmarks/")
# benchmark("x_Mask.java --args 0", "benchmarks/")
# benchmark("TableLookup.java --args 0", "benchmarks/")
# benchmark("x_DeadRecursiveId.java", "benchmarks/")

# print("Hybrid + PP")
# benchmark("x_SaneLaundering.java --pp --hybrid", "benchmarks/")
# benchmark("x_MaskedLaundry.java --pp --hybrid", "benchmarks/")

# print("PP")
# benchmark("x_SaneLaundering.java", "benchmarks/")
# benchmark("x_MaskedLaundry.java", "benchmarks/")