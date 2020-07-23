#! /usr/bin/python3.8
"""
./testcli_frontend.py JAVA_FILES --out /tmp/bla --joana_jar "location of the joana jar" --options "options directly passed to testcli"

Compiles the passed Java files into the passed folder and runs the TestCLI with the passed options to create .pdg, .pg, .ssv and
.dot files

Assumes that the Java file is in the default package
"""
import json
import math
import os
import re
import shutil
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path
from random import random, choice, sample, randint
from shlex import shlex
from typing import List, Optional, Callable

import click


def compile_java(files: List[Path], out: Path, joana_jar: Path) -> Path:
    """
    Compiles the passed Java file. Assumes that out is a folder without class files

    Returns the path to the created JAR
    """
    subprocess.check_call(f"javac -cp {joana_jar} -target 1.8 {' '.join(map(str, files))} -d {out}", shell=True)
    main_class = java_class(files)
    subprocess.check_call(f"jar -cfe {main_class}.jar {main_class} *", shell=True, cwd=out)
    return out / (main_class + ".jar")


def run_test_cli_command(jar: Path, joana_jar: Path, options: str, draw_graphs: bool = False) -> str:
    return f"java -cp {joana_jar} edu.kit.joana.wala.summary.test.TestCLI {jar} {options}" +\
           (" -g " if draw_graphs else "")

def run_test_cli(jar: Path, joana_jar: Path, options: str, timeout: Optional[float] = None, draw_graphs: bool = False):
    subprocess.check_call(run_test_cli_command(jar, joana_jar, options, draw_graphs), shell=True, timeout=timeout)


def run_test_cli_w_output(jar: Path, joana_jar: Path, options: str, timeout: Optional[float] = None,
                          draw_graphs: bool = False) -> str:
    print("##########" + run_test_cli_command(jar, joana_jar, options, draw_graphs))
    return subprocess.check_output(run_test_cli_command(jar, joana_jar, options, draw_graphs),
                                   shell=True, timeout=timeout).decode()


def locate_joana_jar() -> Path:
    return Path(__file__).absolute().parent.parent / "dist" / "joana.wala.summary.test.jar"


def java_class(files: List[Path]) -> str:
    return files[0].name.split(".java")[0]


def basename(files: List[Path], options: str) -> str:
    app = ""
    opts = list(shlex(options, punctuation_chars=True))
    if "--config" in opts:
        index = opts.index("--config")
        if len(opts) > index + 1:
            app = "_" + opts[index + 1].lower()
    return java_class(files) + app


def run(files: List[Path], out: Path, joana_jar: Path, options: str, timeout: float = None, draw_graphs: bool = False):
    if out.exists():
        shutil.rmtree(out, ignore_errors=True)
    os.makedirs(out)
    jar = compile_java(files, out, joana_jar)
    run_test_cli(jar, joana_jar, options, timeout, draw_graphs)


def run_cpp(cpp_line: str, base: Path, pg_file: Path, ssv_file: Path, discard_regex: str = None):
    cmd = cpp_line.replace("$BASE", str(base.absolute())).replace("\\/", "/") \
                          .replace("$PG_FILE", str(pg_file.absolute())) \
                          .replace("$SSV_FILE", str(ssv_file.absolute()))
    print("#####" + cmd)
    proc = subprocess.Popen(cmd, shell=True,
                     stderr=subprocess.PIPE, stdout=subprocess.PIPE)
    out, err = proc.communicate()

    def prints(ins, outs):
        for line in ins.decode().splitlines():
            if not discard_regex or not re.match(discard_regex, line):
                print(line, file=outs)
    prints(out, sys.stdout)
    prints(err, sys.stderr)
    if proc.returncode > 0:
        raise subprocess.CalledProcessError(proc.returncode, cmd, out.decode(), err.decode())


@dataclass
class FuzzerAction:

    entry: Optional[int] = None
    removed_nodes: List[int] = field(default_factory=list)

    @staticmethod
    def find_set_entry(*actions: 'FuzzerAction') -> Optional[int]:
        return ([a.entry for a in reversed(actions) if a.entry] + [None])[0]

    @staticmethod
    def to_string(*actions: 'FuzzerAction') -> str:
        return ' '.join(f"-d {r}" for n in actions for r in n.removed_nodes) + \
               (f" --set_entry {FuzzerAction.find_set_entry(*actions)} " if FuzzerAction.find_set_entry(*actions) else "")


@dataclass(frozen=True)
class FuzzerStats:

    functions: List[int]
    nodes: List[int]
    edge_count: int
    entry: int
    after_reorder_nodes: List[int]
    after_reorder_functions: List[int]
    after_reorder_entry: int

    def __str__(self):
        return f"State(edge_count={self.edge_count}, nodes={len(self.nodes)}, functions={len(self.functions)})"


@dataclass(frozen=True)
class FuzzerState:

    pg_file: Path
    ssv_file: Path
    stats: FuzzerStats
    actions: List[FuzzerAction] = field(default_factory=list)

    def __str__(self):
        return f"State({self.stats})"

    @property
    def summary_count(self) -> int:
        with self.ssv_file.open("r") as f:
            lines = f.readlines()
            l = len(lines)
            if l > 0 and lines[-1].strip() == "":
                l -= 1
            return l


class Fuzzer:
    """
    Algorithm:
    1. create initial JAR (setup)
    2. create initial FuzzerState
    3. remove random node or set entry to random node and
    4. if cpp still fails go back to 3.
    5. finish
    """

    def __init__(self, files: List[Path], cpp: str, out: Path = None, joana_jar: Path = locate_joana_jar(),
                 options: str = "", timeout: Optional[float] = None, draw_graphs: bool = False,
                 cpp_out_discard_regex: str = None):
        self.files = files
        self.cpp = cpp
        self.out = out or Path(tempfile.mkdtemp())
        self.joana_jar = joana_jar
        self.options = options
        self.current_state = None  # type: Optional[FuzzerState]
        self.jar = None  # type: Optional[Path]
        self.best_folder = self.out / "best"
        self.timeout = timeout
        self.draw_graphs = draw_graphs
        self.cpp_out_discard_regex = cpp_out_discard_regex

    def setup(self):
        if self.out.exists():
            shutil.rmtree(self.out, ignore_errors=True)
        os.makedirs(self.out)
        os.makedirs(self.best_folder)
        self.jar = compile_java(self.files, self.out, self.joana_jar)
        self.current_state = self.initial_joana_run()
        self.copy_to_best()

    def copy_to_best(self):
        for f in self.out.glob("*"):
            if f.is_file():
                try:
                    shutil.copy(f, self.best_folder)
                except OSError:
                    pass
                
    def copy_from_best(self):
        for f in self.best_folder.glob("*.dot"):
            os.remove(f)
        for f in self.out.glob("*"):
            if f.is_file():
                try:
                    shutil.copy(self.best_folder / f.name, f)
                except OSError:
                    pass

    def initial_joana_run(self) -> FuzzerState:
        """ Runs JOANA without any actions """
        cmd = self.options + " -e --list_nodes --remove_normal --remove_unreachable"
        base = self.out / basename(self.files, self.options)
        options_file = Path(str(base) + "_best.options")
        with options_file.open("w") as f:
            f.write(cmd)
        print(cmd)
        res = run_test_cli_w_output(self.jar, self.joana_jar, f"@{options_file}", timeout=self.timeout,
                                    draw_graphs=self.draw_graphs)
        return FuzzerState(Path(str(base) + ".pg"), Path(str(base) + ".ssv"), self.parse_output_to_stats(res), [])

    def run_joana(self, actions: List[FuzzerAction]) -> FuzzerState:
        for f in self.out.glob("*.dot"):
            os.remove(f)
        def opt_cmd(all: bool):
            return self.options + " -e " + FuzzerAction.to_string(*(actions
                                                                   if self.draw_graphs or all
                                                                   else actions[len(self.current_state.actions):])) + \
                  " --list_nodes --remove_unreachable"
        all_cmd = opt_cmd(True)
        cmd = opt_cmd(False)
        base = self.out / basename(self.files, self.options)
        options_file = Path(str(base) + "_best.options")
        with options_file.open("w") as f:
            f.write(cmd)
        all_options_file = Path(str(base) + "_best_all.options")
        with all_options_file.open("w") as f:
            f.write(all_cmd)
        #print("# #" + cmd)
        res = ""
        if self.draw_graphs:
            res = run_test_cli_w_output(self.jar, self.joana_jar, f"@{options_file} --remove_normal", timeout=self.timeout,
                                        draw_graphs=self.draw_graphs)
        else:
            # choose more efficient version
            joana_cmd = f"java -cp {self.joana_jar} edu.kit.joana.wala.summary.test.TestCLI {self.jar} @{options_file} --use_pg_for_cpp {self.best_pg_file}"
            print(joana_cmd)
            res = subprocess.check_output(joana_cmd,
                                    shell=True, timeout=self.timeout).decode()
        return FuzzerState(Path(str(base) + ".pg"), Path(str(base) + ".ssv"), self.parse_output_to_stats(res), actions)

    @property
    def best_pg_file(self) -> Path:
        return self.out / "best" / (basename(self.files, self.options) + ".pg")

    def parse_output_to_stats(self, res: str) -> FuzzerStats:
        d = {}
        for line in res.splitlines():
            if line.startswith("{"):
                obj = json.loads(line)
                if not d:
                    d["functions"] = obj["functions"]
                    d["nodes"] = obj["nodes"]
                    d["edge_count"] = obj["edge_count"][0]
                    d["entry"] = obj["entry"][0]
                else:
                    d["after_reorder_functions"] = obj["functions"]
                    d["after_reorder_nodes"] = obj["nodes"]
                    d["after_reorder_entry"] = obj["entry"][0]
            else:
                print("java> " + line)
        if "after_reorder_functions" not in d:
            d["after_reorder_functions"] = d["functions"]
            d["after_reorder_nodes"] = d["nodes"]
            d["after_reorder_entry"] = d["entry"][0]
        return FuzzerStats(**d)

    def increment_state(self, current_state: FuzzerState, action: FuzzerAction) -> FuzzerState:
        return self.run_joana(current_state.actions + [action])

    def loop(self, required_set_entry_unchanged: int = 20, max_number_exponent: float = 0.6, set_entry_prob: float = 0.2):
        self.setup()
        if required_set_entry_unchanged > -1:
            # try to set the entry first
            self._loop(lambda stats: min(len(stats.functions) * 3, required_set_entry_unchanged),
                       lambda state, counter: FuzzerAction(entry=choice(self.get_functions(state.stats))),
                       -1,
                       lambda a, b: b.edge_count - a.edge_count > 0.001 * b.edge_count)
        self._loop(lambda stats: len(stats.nodes), lambda state, counter: self.choose_action2(state, counter,
                                                                                              max_number_exponent,
                                                                                              set_entry_prob), -1)
        def fun(state, counter):
            action = self.choose_action2(state, counter, 1, set_entry_prob)
            print(f"Choose action {action}")
            return action
        self._loop(lambda stats: len(stats.nodes) * 30, fun, -1)

    def get_functions(self, stats: FuzzerStats) -> List[int]:
        if self.draw_graphs:
            return stats.functions
        else:
            return stats.after_reorder_functions

    def get_nodes(self, stats: FuzzerStats) -> List[int]:
        if self.draw_graphs:
            return stats.nodes
        else:
            return stats.after_reorder_nodes

    def get_entry(self, stats: FuzzerStats) -> int:
        if self.draw_graphs:
            return stats.entry
        else:
            return stats.after_reorder_entry

    def _loop(self, abort_after: Callable[[FuzzerStats], int], next_action: Callable[[FuzzerState, int], FuzzerAction],
              abort_after_n_successes: int = -1,
              is_better: Callable[[FuzzerStats, FuzzerStats], bool] = lambda a, b:  len(a.nodes) < len(b.nodes)):
        counter = 0
        successes = 0
        while True:
            print(self.current_state)
            #print(FuzzerAction.to_string(*self.current_state.actions))
            try:
                new_state = self.increment_state(self.current_state, next_action(self.current_state, counter))
            except subprocess.TimeoutExpired:
                counter += 1
                continue
            if self.check(new_state):
                if is_better(new_state.stats, self.current_state.stats):
                    self.current_state = new_state
                    self.copy_to_best()
                    print("BEST: " + str(len(new_state.actions)))
                    counter = 0
                    successes += 1
                else:
                    print(f"not better (new {new_state.stats}; old {self.current_state.stats}): {FuzzerAction.to_string(*new_state.actions)}")
                if abort_after_n_successes > -1 and successes >= abort_after_n_successes:
                    break
            else:
                print("failed: " + str(len(new_state.actions)))
                counter += 1
            if counter > abort_after(self.current_state.stats):
                self.copy_from_best()
                break

    def check(self, current_state: FuzzerState) -> bool:
        if current_state.summary_count == 0:
            return False
        try:
            run_cpp(self.cpp, current_state.pg_file.with_suffix(""), current_state.pg_file, current_state.ssv_file,
                    self.cpp_out_discard_regex)
        except subprocess.CalledProcessError as err:
            return err.returncode == 1
        return False

    def choose_action(self, current_state: FuzzerState, count: int) -> FuzzerAction:
        if random() < min(0.1, 1 / (count + 3)) or len(current_state.stats.nodes) == 0:
            return FuzzerAction(entry=choice(self.get_functions(current_state.stats)))
        return FuzzerAction(removed_nodes=[choice(self.get_nodes(current_state.stats))])

    def choose_action2(self, current_state: FuzzerState, count: int,
                       max_number_exponent: float = 0.6, set_entry_prob: float = 0.2) -> FuzzerAction:
        if random() < set_entry_prob or len(current_state.stats.nodes) == 0:
            return FuzzerAction(entry=choice(self.get_functions(current_state.stats)))
        funcs = set(self.get_functions(current_state.stats))
        ns = [n for n in self.get_nodes(current_state.stats) if n != self.get_entry(current_state.stats)]
        return FuzzerAction(removed_nodes=sample(ns,
                                                 randint(1,
                                                         min(
                                                             len(ns),
                                                             max(1, math.ceil(math.pow(len(ns), max_number_exponent)
                                                                              / (count + 1)))
                                                            )
                                                         )
                                                 )
                            )


@click.group()
def cli():
    pass


@cli.command()
@click.argument("files", type=Path, nargs=-1, required=True)
@click.option("--out", type=Path, default=lambda: Path(tempfile.mkdtemp()))
@click.option("--joana_jar", type=Path, default=lambda: locate_joana_jar())
@click.option("--options", type=str, default="")
@click.option("--cpp", type=str, default="")
@click.option("--timeout", type=float, default=None)
@click.option("--graph/--no_graph", default=False)
def base(files: List[Path], out: Path, joana_jar: Path, options: str, cpp: str, timeout: float, graph: bool):
    """
    All Java files have to be in the default package, the first Java file has the same name "
    as the main class
    """
    assert joana_jar.exists()
    run(files, out, joana_jar, options, timeout, graph)
    if cpp:
        base = out / basename(files, options)
        run_cpp(cpp, base, Path(str(base) + ".pg"), Path(str(base) + ".ssv"))


@cli.command()
@click.argument("files", type=Path, nargs=-1, required=True)
@click.option("--out", type=Path, default=lambda: Path(tempfile.mkdtemp()))
@click.option("--joana_jar", type=Path, default=lambda: locate_joana_jar())
@click.option("--options", type=str, default="")
@click.option("--cpp", type=str)
@click.option("--timeout", type=float, default=100)
@click.option("--se_unchanged", type=int, default=100, help="required_set_entry_unchanged")
@click.option("--exponent", type=float, default=0.5,
              help="Delete randint(1, len(nodes) ** exponent) nodes in every step")
@click.option("--set_entry_prob", type=float, default=0.2, help="Probability of using the set_entry action")
@click.option("--graph/--no_graph", default=False)
@click.option("--out_discard", default=None, type=str, help="Discard all cpp out and err lines that matches this regexp")
def fuzz(files: List[Path], out: Path, joana_jar: Path, options: str, cpp: str, timeout: float,
         se_unchanged: int, exponent: float, set_entry_prob: float, graph: bool, out_discard: str):
    Fuzzer(files, cpp, out, joana_jar, options, timeout, graph, out_discard).loop(se_unchanged, exponent, set_entry_prob)


if __name__ == '__main__':
    cli()
