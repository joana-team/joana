import json
import os
import subprocess
import sys

import util


def execute(args, path):
    cwd = os.getcwd()
    script_path = cwd + "/run.sh"

    cmd = [script_path, path, "--run", "--args"] + [str(i) for i in args]
    result = subprocess.run(cmd, stdout=subprocess.PIPE)
    return result.stdout.decode("utf-8").strip()


def collect_results(path, possible_args):
    results = {}
    for args in possible_args:
        res = execute(args, path)
        print("in " + ''.join(str(args)) + " -- out: " + res)
        if res == "2\n1\n1":
            print(args)
        if res not in results.keys():
            results[res] = 0
        results[res] += 1
    return results


def write_results_file(results, name, arg_num):
    path = "testResources/results/" + name + ".json"
    json_data = {"args": arg_num, "result": results}
    with open(path, 'w+') as f:
        json.dump(json_data, f)


if __name__ == "__main__":
    param_num = int(sys.argv[1])
    # param_width = int(sys.argv[2])
    param_width = 3
    path = sys.argv[2]
    name = util.get_name_from_testcase_path(path)

    possible_inputs = util.generate_possible_inputs(param_num, param_width)
    results = collect_results(path, possible_inputs)
    write_results_file(results, name, param_num)
