/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package edu.kit.joana.wala.jodroid.entrypointsFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.ibm.wala.ipa.callgraph.Entrypoint;

import edu.kit.joana.wala.jodroid.entrypointsFile.Exceptions.ParserException;
/**
 *  Just wraps together everything for writing.
 *
 *  All the work is actually done by other classes.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-27
 */
public class Writer {
    private final Marshall marshall;
    private final Serializer serializer;

    public Writer() {
        this.marshall = new Marshall();
        this.serializer = new Serializer(marshall);
    }

    public void add(Serializable obj) {
        // TODO: collect all to write, arrange it a clever order, then serialize?
        this.serializer.serialize(obj);
    }

    public void add(Collection<? extends Entrypoint> obj) {
        this.serializer.serialize(obj);
    }

    public void add(Map<?, ?> obj) {
        this.serializer.serialize(obj);
    }


    public void write(File file) throws IOException, FileNotFoundException  { 
        OutputStreamWriter out = null;
        //try {
            if (! file.exists() ) {
                System.out.println(" FILE " + file);
                file.createNewFile();
            }
            out = new FileWriter(file, /* append= */ false);
            write(out);
            out.close();
        //} 
    }

    public void write(OutputStreamWriter stream) {
        try {
            final XMLOutputFactory fac = XMLOutputFactory.newInstance();
            final XMLStreamWriter writer = fac.createXMLStreamWriter(stream);
            this.marshall.write(writer);
            writer.flush();
            writer.close();
        } catch (XMLStreamException e) {
            throw new ParserException("EX", e); // XXX
        }
    }

}
