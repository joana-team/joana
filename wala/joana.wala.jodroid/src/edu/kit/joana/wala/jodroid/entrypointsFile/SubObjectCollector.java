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
import java.io.ObjectStreamClass;
import java.util.HashSet;

/**
 *  Is used to find child-objects needing special handling.
 *
 *  Some Serializables can not be read by only reflecting the member-fields. These Serializbles 
 *  offer a special Function ("writeObject(ObjectOutputStream in)") to offer their Data.
 *
 *  The SubObjectCollector is used by the Serializer to collect this special data.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-26
 */
class SubObjectCollector extends java.io.ObjectOutputStream {
    private final HashSet<Object> findings = new HashSet<Object>();

    public SubObjectCollector() throws java.io.IOException {
        System.out.println("new MyStream");
    }
   
    public HashSet<Object> getFindings() {
        return this.findings;
    }

    @Override
    protected void  annotateClass(Class<?> cl) { }

    @Override
    protected void annotateProxyClass(Class<?> cl) { }

    @Override
    public void    close() {}

    @Override
    public void    defaultWriteObject() { }

    @Override
    protected void  drain() { }

    @Override
    protected boolean enableReplaceObject(boolean enable) { return false; }

    @Override
    public void    flush() { }

    @Override
    protected Object    replaceObject(Object obj) {
        System.out.println("replaceObject" + obj);
        findings.add(obj);
        return null;
    }

    @Override
    public void    reset() { }

    @Override
    public void    useProtocolVersion(int version) {}

    @Override
    public void    write(byte[] buf) { }

    @Override
    public void    write(byte[] buf, int off, int len) { }

    @Override
    public void    write(int val) { }

    @Override
    public void    writeBoolean(boolean val) { }
    public void    writeByte(int val) { }
    public void    writeBytes(String str) { }
    public void    writeChar(int val) { }
    public void    writeChars(String str) { }

    @Override
    protected void  writeClassDescriptor(ObjectStreamClass desc) {
        System.out.println("writeClassDescriptor " + desc);
    }

    public void    writeDouble(double val) { }
    public void    writeFields() { }
    public void    writeFloat(float val) { }
    public void    writeInt(int val) { }
    public void    writeLong(long val) { }
    /*public void    writeObject(Object obj) {
        System.out.println("writeObject " + obj);
    }*/
    protected void  writeObjectOverride(Object obj) {
        findings.add(obj);
    }
    public void    writeShort(int val) { }
    protected void  writeStreamHeader() { }
    public void    writeUnshared(Object obj) {
        findings.add(obj);
    }
    public void    writeUTF(String str) { }

}
