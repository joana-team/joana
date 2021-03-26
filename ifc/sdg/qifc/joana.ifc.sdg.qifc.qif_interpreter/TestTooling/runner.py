import os
import subprocess
import json
import util
import sys


def run_test(args, path):
    cwd = os.getcwd()
    script_path = cwd + "/run.sh"

    cmd = [script_path, path, "-args"] + [str(i) for i in args]
    result = subprocess.run(cmd, stdout=subprocess.PIPE)
    return result.stdout.decode("utf-8").strip()


def check_result(output, model_count, testcase_name):
    json_data = util.load_results(testcase_name)

    if output not in json_data["result"]:
        return False

    expected = int(json_data["result"][output])
    # print("Expected: " + str(expected))
    # print("Got: " + model_count)
    return int(model_count) == expected


def parse_result(result_string):
    (output, model_count) = result_string.split("# of inputs w/ the same output:")
    output = output.strip()
    model_count = model_count.strip()
    return output, model_count


def check_testcase(test_case, args):
    name = util.get_name_from_testcase_path(test_case)
    result_string = run_test(args, test_case)
    output, model_count = parse_result(result_string)

    return check_result(output, model_count, name)


if __name__ == "__main__":
    print("Make sure all debug prints are turned off !!!")
    test_case = sys.argv[1]
    required_args_num = int(util.load_results(util.get_name_from_testcase_path(test_case))["args"])

    all_inputs = util.generate_possible_inputs(required_args_num, 3)
    for args in all_inputs:
        check = check_testcase(test_case, args)
        print(str(args) + " " + str(check))

