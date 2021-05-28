import sys


class MCInvocation:
    solver_time = 0
    num_vars = 0
    num_clauses = 0


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
    computedBySATAna = []
    mcInvocs = []
    computedCC = 0

    def __init__(self, name):
        self.testcase = name


def parseLogfile(output_dir, classname):
    eval = Evaluation(classname)
    lines = get_relevant_lines(output_dir, classname)

    for line in lines:
        parts = line.strip().split(" ")
        if "Starting:" in parts:
            eval.stages[parts[-1]] = Stage(parts[-1], parts[0])
        if "Finished:" in parts:
            eval.stages[parts[-1]].end_time = parts[0]
            eval.stages[parts[-1]].duration = eval.stages[parts[-1]].end_time - eval.stages[parts[-1]].start_time
            eval.stages[parts[-1]].success = True
        if "Failed:" in parts:
            eval.stages[parts[-1]].end_time = parts[0]
            eval.stages[parts[-1]].duration = eval.stages[parts[-1]].end_time - eval.stages[parts[-1]].start_time
            eval.stages[parts[-1]].success = False
    return eval


def get_relevant_lines(output_dir, classname):
    with open(output_dir + "/" + classname + ".log", "r") as f:
        lines = [line.strip() for line in f.readlines() if "[EVAL]" in line]
    return lines


if __name__ == "__main__":
    eval = parseLogfile(sys.argv[1], sys.argv[2])
    for key in eval.stages.keys():
        print(str(eval.stages[key]))