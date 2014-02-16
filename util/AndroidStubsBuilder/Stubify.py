#!/usr/bin/python
# -*- coding: UTF-8 -*-

#  Copyright (c) 2013,
#      Tobias Blaschke <code@tobiasblaschke.de>
#  All rights reserved.
#
#  Redistribution and use in source and binary forms, with or without
#  modification, are permitted provided that the following conditions are met:
#
#  1. Redistributions of source code must retain the above copyright notice,
#     this list of conditions and the following disclaimer.
#
#  2. Redistributions in binary form must reproduce the above copyright notice,
#     this list of conditions and the following disclaimer in the documentation
#     and/or other materials provided with the distribution.
#
#  3. The names of the contributors may not be used to endorse or promote
#     products derived from this software without specific prior written
#     permission.
#
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
#  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
#  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
#  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
#  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
#  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
#  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
#  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
#  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
#  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
#  POSSIBILITY OF SUCH DAMAGE.

import ConfigParser
import re

class Stubifier:
    """Does String-Replacements in Java-Files read from the Android-SDK based 
       on the Settings of a "stubsBuilder.ini"."""

    config = None
    as_object = []
    int_consts = {}
    add_import = []
    hash_functions = []
    string_functions = []
    equals_functions = []
    replace_fields = []
    zap_functions = []

    def __init__(self, configFile = "stubsBuilder.ini", section = "stubify"):
        self.config = ConfigParser.SafeConfigParser()
        self.config.read(configFile)

        as_object = self.config.get(section, "as_object")
        self.as_object = as_object.split()

        consts = self.config.get(section, "int_consts")
        consts = consts.split()
        cur = 1
        for c in consts:
            self.int_consts[c] = cur
            cur = cur << 1

        self.str_consts = self.config.get(section, "str_consts").split()
        h_fkt = self.config.get(section, "hash_functions")
        self.hash_functions = h_fkt.split()
        s_fkt = self.config.get(section, "string_functions")
        self.string_functions = s_fkt.split()
        e_fkt = self.config.get(section, "equals_functions")
        self.equals_functions = e_fkt.split()
        add_i = self.config.get(section, "add_import")
        self.add_import = add_i.split()
        r_fd = self.config.get(section, "replace_fields")
        self.replace_fields = r_fd.split()
        zap = self.config.get(section, "zap_function")
        self.zap_functions = zap.split()
        self.int_zap_functions = self.config.get(section, "int_zap_function").split()
        self.false_zap_functions = self.config.get(section, "false_zap_function").split()
        self.singleton = self.config.get(section, "singleton").split()
        self.delete_lines = self.config.get(section, "delete_lines").split()


    def stubify(self, inFile, outFile):
        if inFile == outFile:
            raise Exception("In- and output file may not be the same.")
        if isinstance(inFile, str):
            inFile = open(inFile, 'r')
        assert isinstance(inFile, file)
        if isinstance(outFile, str):
            outFile = open(outFile, 'w')
        assert isinstance(outFile, file)

        outFile.write(self.stubify_string(inFile.read()))

        inFile.close()
        outFile.close()

    def stubify_string(self, inS):
        ret = inS
        ret = self._add_imports(ret)
        ret = self._singleton(ret)
        ret = self._zap_functions(ret)
        ret = self._replace_new(ret, None)
        ret = self._replace_consts(ret, None)
        ret = self._strip_calls(ret)
        ret = self._replace_fields(ret)
        ret = self._delete_lines(ret)
        return ret

    def _delete_lines(self, line):
        for d in self.delete_lines:
            r = re.compile(r'[\n\r]+\s*' + d + r'.*[\n\r]*')
            line = r.sub("\n/* zap line */\n", line)
        return line

    def _singleton(self, line):
        for s in self.singleton:
            to = s.replace('.', '_').replace('(','').replace(')','')
            to = "android.StubFields.singleton_" + to
            while s in line:
                line = line.replace(s, to)
        return line

    def _zap_functions(self, line):
        all = []
        all.extend(self.zap_functions)
        all.extend(self.int_zap_functions)
        all.extend(self.false_zap_functions)
        for fkt in all:
            r = re.compile(r'[\w]+[\.\w\d]*\.' + fkt + '\s*\(')
            match = r.search(line)
            while match:
                #print "ZAP MATCH: " + fkt
                start = match.start()
                end = match.end()
                opening = 1
                while opening > 0:
                    if line[end] == '(':
                        opening = opening + 1
                    elif line[end] == ')':
                        opening = opening - 1
                    end = end + 1
                retLine = line[:start]
                # Test if line would be empty after zapping
                empty = False
                eStart = start - 1
                while line[eStart] in (' ', '\t'):
                    eStart -= 1
                eEnd = end
                while line[eEnd] in (' ', '\t'):
                    eEnd += 1
                if (line[eStart] in ('\n', '\r', ';')) and line[eEnd] == ';':
                    retLine += "/* ZAP! */"
                else:
                    if fkt in self.zap_functions:
                        retLine = retLine + " null "
                    elif fkt in self.int_zap_functions:
                        retLine = retLine + " 42 "
                    elif fkt in self.false_zap_functions:
                        retLine = retLine + " false "
                    else:
                        raise Exception("Unknown ZAP-Type for " + fkt)
                retLine += line[end:]
        
                line = retLine
                match = r.search(line)
        return line


    def _add_imports(self, line):
        r = re.compile(r'\s*import\s*', 0)
        match = r.search(line)
        if match:
            retLine = line[:match.start()]
            for imp in self.add_import:
                retLine = retLine + "\nimport " + imp + "; // from stubsBuilder"
            retLine = retLine + line[match.start():]
            return retLine
        else:
            # TODO
            return line

    def _replace_consts(self, line, inFile):
        for k, v in self.int_consts.items():
            line = line.replace(k, str(v))
        for c in self.str_consts:
            line = line.replace(c, "\"" + c + "\"")
        return line

    def _replace_fields(self, line):
        for fd in self.replace_fields:
            while fd in line:
                basename = fd.split('.')[1]
                line = line.replace(fd, 'StubFields.' + basename)
        return line

    def _replace_new(self, line, inFile):
        if "new" in line:
            for test in self.as_object:
                r = re.compile(r'new[\s\n\r]+' + test + r'[\s\n\r]*\(', re.MULTILINE)
                match = r.search(line)
                if match:
                    opening = 1
                    end = match.end()
                    while opening > 0:
                        if line[end] == '(':
                            opening = opening + 1
                        elif line[end] == ')':
                            opening = opening - 1
                        end = end + 1
                    retLine = line[:match.start()]
                    retLine += "((%s) new Object())" % test
                    retLine += line[end:]
                    return self._replace_new(retLine, inFile)
            return line
        else:
            return line

    def _strip_calls(self, line):
        all = []
        all.extend(self.hash_functions)
        all.extend(self.string_functions)
        all.extend(self.equals_functions)

        for fkt in all:
            r = re.compile(r'[\.\s]' + fkt + '\s*\(')
            match = r.search(line)
            while match:
                start = match.start() + 1
                end = match.end()
                opening = 1
                while opening > 0:
                    if line[end] == '(':
                        opening = opening + 1
                    elif line[end] == ')':
                        opening = opening - 1
                    end = end + 1
                retLine = line[:start]
                if fkt in self.hash_functions:
                    retLine = retLine + "hashCode()"
                elif fkt in self.string_functions:
                    retLine = retLine + "toString()"
                elif fkt in self.equals_functions:
                    retLine = retLine + "equals(42)"
                else:
                    raise "Unknown function type"
                retLine += line[end:]
        
                line = retLine
                match = r.search(line)
        return line

if __name__ == "__main__":
    print "This is test-code! Run stubsBuilder for the real thing!"

    s = Stubifier()
    print repr(s.as_object)
    print "\n\n"
    print repr(s.int_consts)

    inS = """
a = new Foo   (6, foo(9)); b = 12;
c = new
    Foo
    (
    12,
    3
    );
    """
    expectedS = """
a = ((Foo) new Object()); b = 12;
c = ((Foo) new Object());
    """
    print "--------------------------------------"
    s.as_object = ['Foo']
    print inS
    outS = s.stubify_string(inS)
    print "--------------------------------------"
    print outS
    assert (outS == expectedS)

    s.int_consts={'g.FOO': 17}
    inS = """
        int c = 7 + g.FOO + 3;
    """
    expectedS = """
        int c = 7 + 17 + 3;
    """

    print "--------------------------------------"
    print inS
    outS = s.stubify_string(inS)
    print "--------------------------------------"
    print outS
    assert (outS == expectedS)

    s.hash_functions = ['replaceMeH']
    inS = """
        int foo = bar.replaceMeH(17, 12) + 3;
        baz.replaceMeH("foo");
    """
    expectedS = """
        int foo = bar.hashCode() + 3;
        baz.hashCode();
    """
    print "--------------------------------------"
    print inS
    outS = s.stubify_string(inS)
    print "--------------------------------------"
    print outS
    assert (outS == expectedS)

    s.add_import = ['foo.bar']
    s.replace_fields = ['bla.blubb']
    inS = """
        package bar;
        import foo;
        bla.blubb = 17;
    """
    expectedS = """
        package bar;
import foo.bar; // from stubsBuilder
        import foo;
        StubFields.blubb = 17;
    """
    print "--------------------------------------"
    print inS
    outS = s.stubify_string(inS)
    print "--------------------------------------"
    print outS
    assert (outS == expectedS)




