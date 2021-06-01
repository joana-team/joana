import os
import subprocess

import math
import sys

import util

ignored = ["testResources/PrintInCall.java", "testResources/LoopinLoop2.java", "testResources/Iterations.java"]


def run_test(args, path):
    cwd = os.getcwd()
    script_path = cwd + "/run.sh"

    cmd = [script_path, path, "--args"] + [str(i) for i in args]
    result = subprocess.run(cmd, stdout=subprocess.PIPE)
    return result.stdout.decode("utf-8").strip()


def check_result(output, model_count, testcase_name):
    if model_count == -1:
        print("Crashed!")
        return None
    json_data = util.load_results(testcase_name)
    if json_data is None:
        return None

    if output not in json_data["result"]:
        return False

    expected = int(json_data["result"][output])
    model_count = model_count.split("---")[0].strip()
    # print("Expected: " + str(expected))
    # print("Got: " + model_count)
    return int(model_count) == expected


def parse_result(result_string, num_args):
    if result_string.strip() == "No information leakage":
        return "", math.pow(2, num_args * 3)

    try:
        mcs = [mc.strip() for mc in result_string.split("---")]
        all_results = []
        for mc in mcs:
            (output, model_count) = result_string.split("# of inputs w/ the same output:")
            output = output.strip()
            model_count = model_count.strip()
            all_results.append((output, model_count))
    except ValueError:
        (output, model_count) = ("-1", "-1")

    return all_results


def check_testcase(test_case, args):
    name = util.get_name_from_testcase_path(test_case)
    result_string = run_test(args, test_case)
    all_results = parse_result(result_string, len(args))

    all_correct = True
    count = 0
    for (output, model_count) in all_results:
        output = output + str(count)
        all_correct = all_correct and check_result(output, model_count, name)

    return all_correct


def test(path):
    if util.load_results(util.get_name_from_testcase_path(test_case)) is None:
        print("Skipping testcase: " + path + " -- result file not found")
        return True
    num_param = util.load_results(util.get_name_from_testcase_path(test_case))["args"]
    all_inputs = util.generate_possible_inputs(num_param, 3)

    print("Starting " + path)

    fail = 0
    success = 0

    for args in all_inputs:
        check = check_testcase(path, args)
        if check is None:
            return False

        if not check:
            args = [str(i) for i in args]
            print("Test failed for input: " + " ".join(args))
            fail += 1
        else:
            success += 1

    print("Testcase " + path + ": " + str(success) + " successful, " + str(fail) + " failed")
    return fail == 0


if __name__ == "__main__":
    print("Make sure all debug prints are turned off !!!")
    test_case = sys.argv[1]

    successful = []

    if test_case == "all":
        for filename in os.listdir("testResources/"):
            test_case = "testResources/" + filename
            if os.path.isdir(test_case) or test_case in ignored:
                continue

            res = test(test_case)

            if res:
                successful.append(filename)
            os.system("rm -r out_*")

        with open("testResources/results/successful.txt", 'w') as f:
            for case in successful:
                f.write(case)
                f.write("\n")

    if test_case == "failed":
        unnecessary = []

        with open("testResources/results/successful.txt", 'r') as f:
            content = f.readlines()
            unnecessary = [x.strip() for x in content]

        for filename in os.listdir("testResources/"):
            test_case = "testResources/" + filename

            if os.path.isdir(test_case):
                continue

            if test_case in unnecessary or test_case in ignored:
                continue

            res = test(test_case)

            if res:
                successful.append(filename)
            os.system("rm -r out_*")

        with open("testResources/results/successful.txt", 'a') as f:
            for case in successful:
                f.write("\n")
                f.write(case)

    else:
        res = test(test_case)
        if res:
            with open("testResources/results/successful.txt", 'a') as f:
                f.write("\n" + test_case)