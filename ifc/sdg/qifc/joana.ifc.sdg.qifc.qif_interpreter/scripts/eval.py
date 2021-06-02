import json
import os
from json import JSONEncoder

import sys

START = "[start]"
FINISH = "[finish]"
FAIL = "[fail]"
MC_INVOKE = "[mc]"
SYM_EXE = "[se]"
EVALUATION_DIR = "eval/"


class MCInvocation:
    solver_time = 0
    num_vars = 0
    num_clauses = 0
    
    def __init__(self, time, num_vars, num_clauses):
        self.solver_time = time
        self.num_vars = num_vars
        self.num_clauses = num_clauses


class SATAnalysis:
    visitedInstructions = 0
    exec_time = 0

    def __init__(self, instr, exec):
        self.exec_time = exec
        self.visitedInstructions = instr


class Stage:
    name = None
    start_time = 0
    end_time = 0
    duration = 0
    success = None

    def __init__(self, name, start):
        self.name = name
        self.start_time = start

    def __str__(self):
        return self.name + " " + str(self.start_time) + " - " + self.end_time + " (" + self.duration + ") " + str(
            self.success)


class Evaluation:
    testcase = None
    stages = {}
    sat_analysis = []
    mcInvocs = []
    computedCC = 0

    def __init__(self, name):
        super().__init__()
        self.testcase = name


class EvaluationSerializer(JSONEncoder):

    def default(self, o):
        if isinstance(o, Evaluation):
            sat_analysis_str = json.dumps([ana.__dict__ for ana in o.sat_analysis])
            mc_str = json.dumps([mc.__dict__ for mc in o.mcInvocs])
            return {"testcase": o.testcase, "stages": json.dumps(o.stages, cls=EvaluationSerializer),
                    "sat_analysis": sat_analysis_str, "mc": mc_str, "computedCC": o.computedCC}

        elif isinstance(o, Stage):
            return {"name": o.name, "duration": o.duration, "success": o.success}

        else:
            return JSONEncoder.default(self, o)


def parseLogfile(output_dir, classname):
    eval = Evaluation(classname)
    lines = get_relevant_lines(output_dir, classname)

    for line in lines:
        parts = line.strip().split(" ")
        if START in parts:
            eval.stages[parts[-1]] = Stage(parts[-1], int(parts[0]))
        if FINISH in parts:
            eval.stages[parts[-1]].end_time = int(parts[0])
            eval.stages[parts[-1]].duration = eval.stages[parts[-1]].end_time - eval.stages[parts[-1]].start_time
            eval.stages[parts[-1]].success = True
        if FAIL in parts:
            eval.stages[parts[-1]].end_time = int(parts[0])
            eval.stages[parts[-1]].duration = eval.stages[parts[-1]].end_time - eval.stages[parts[-1]].start_time
            eval.stages[parts[-1]].success = False
        if MC_INVOKE in parts:
            vars_, clauses = parse_dimacs_file(parts[-1])
            eval.mcInvocs.append(MCInvocation(int(parts[-2]), vars_, clauses))
        if SYM_EXE in parts:
            eval.sat_analysis.append(SATAnalysis(int(parts[-2]), int(parts[-1])))
    return eval


def get_relevant_lines(output_dir, classname):
    with open(output_dir + "/" + classname + ".log", "r") as f:
        lines = [line.strip() for line in f.readlines() if "[EVAL]" in line]
    return lines


def parse_dimacs_file(file_path):
    with open(file_path, 'r') as f:
        first_line = f.readline().strip().split()
    return int(first_line[2]), int(first_line[3])


def evaluate_testcase(output_dir, classname, name_suffix):
    evaluation = parseLogfile(output_dir, classname)
    json_str = EvaluationSerializer().encode(evaluation)

    if not os.path.exists(EVALUATION_DIR):
        os.mkdir(EVALUATION_DIR)

    eval_file = EVALUATION_DIR + classname + name_suffix + ".json"
    with open(eval_file, 'w+') as f:
        f.write(
            json_str.replace("\\", "").replace("\"{", "{").replace("\"[", "[").replace("}\"", "}").replace("]\"", "]"))


if __name__ == "__main__":
    evaluate_testcase(sys.argv[1], sys.argv[2], sys.argv[3])