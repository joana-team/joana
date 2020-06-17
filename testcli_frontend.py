#! /usr/bin/python3
"""
./testcli_frontend.py JAVA_FILES --out /tmp/bla --joana_jar "location of the joana jar" --options "options directly passed to testcli"

Compiles the passed Java files into the passed folder and runs the TestCLI with the passed options to create .pdg, .pg, .ssv and
.dot files
"""

import click

def compile_java(file: Path, out: Path):
    """
    Compiles the passed Java file. Assumes that out is a folder without class files
    """
    os.system(f"javac -target 1.8 {file} -d {out}")
    pass