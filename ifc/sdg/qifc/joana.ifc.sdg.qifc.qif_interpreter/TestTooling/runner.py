import os
import subprocess
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


def test(path):
    num_param = util.load_results(util.get_name_from_testcase_path(test_case))["args"]
    all_inputs = util.generate_possible_inputs(num_param, 3)

    print("Starting " + path)

    fail = 0
    success = 0

    for args in all_inputs:
        check = check_testcase(path, args)

        if not check:
            args = [str(i) for i in args]
            print("Test failed for input: " + " ".join(args))
            fail += 1
        else:
            success += 1

    print("Testcase " + path + ": " + str(success) + " successful, " + str(fail) + " failed")
    return fail > 0


if __name__ == "__main__":
    print("Make sure all debug prints are turned off !!!")
    test_case = sys.argv[1]

    successful = []

    if test_case == "all":
        for filename in os.listdir("testResources/"):
            test_case = "testResources/" + filename
            if os.path.isdir(test_case):
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

            if test_case in unnecessary:
                continue

            res = test(test_case)

            if res:
                successful.append(filename)
            os.system("rm -r out_*")

        with open("testResources/results/successful.txt", 'a') as f:
            for case in successful:
                f.write(case)
                f.write("\n")

    else:
        res = test(test_case)
        if res:
            with open("testResources/results/successful.txt", 'a') as f:
                f.write(test_case + "\n")
