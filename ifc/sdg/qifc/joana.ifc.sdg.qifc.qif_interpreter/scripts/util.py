import json
import os

import math


def generate_possible_inputs(param_num, param_width):
    min_ = int(-math.pow(2, param_width - 1))
    max_ = int(math.pow(2, param_width - 1))

    base = [[i] for i in range(min_, max_)]
    next_ = []

    for i in range(1, param_num):
        for j in range(min_, max_):
            for l in base:
                copy = l.copy()
                copy.append(j)
                next_.append(copy)
        base = next_
        next_ = []
    return base


def get_name_from_testcase_path(path):
    return path.strip().split("/")[-1].split(".")[0]


def result_file_name(name):
    return "testResources/results/" + name + ".json"


def load_results(testcase_name):
    path = result_file_name(testcase_name)
    if not os.path.exists(path):
        print("Skipping testcase: " + testcase_name + " -- result file not found")
        return None
    with open(path) as f:
        data = json.load(f)
    return data