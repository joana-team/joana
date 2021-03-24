import sys
import math
import subprocess
import os
import json


def generate_possible_inputs(param_num, param_width):
    min = int(-math.pow(2, param_width - 1))
    max = int(math.pow(2, param_width - 1))

    base = [[i] for i in range(min, max)]
    next = []

    for i in range(1, param_num):
        for j in range(min, max):
            for l in base:
                copy = l.copy()
                copy.append(j)
                next.append(copy)
        base = next
        print(base)
        next = []

    return base


def execute(args, path):
    cwd = os.getcwd()
    script_path = cwd + "/run.sh"

    cmd = [script_path, path, "--run", "-args"] + [str(i) for i in args]
    result = subprocess.run(cmd, stdout=subprocess.PIPE)
    return str(result.stdout)


def collect_results(path, possible_args):
    results = {}
    for args in possible_args:
        res = execute(args, path)
        if res not in results.keys():
            results[res] = 0
        results[res] += 1
    return results


def write_results_file(results, name):
    path = "testResources/results/" + name + ".json"
    with open(path, 'w+') as f:
        json.dump(results, f)


if __name__ == "__main__":
    param_num = int(sys.argv[1])
    param_width = int(sys.argv[2])
    path = sys.argv[3]
    name = path.strip().split("/")[-1].split(".")[0]


    possible_inputs = generate_possible_inputs(param_num, param_width)
    results = collect_results(path, possible_inputs)
    write_results_file(results, name)
