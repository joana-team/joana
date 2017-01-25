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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import edu.kit.joana.wala.jodroid.entrypointsFile.Exceptions.ParserException;
import edu.kit.joana.wala.jodroid.entrypointsFile.Items.EPFileHandler;
/**
 *  Just wraps together everything for reading.
 *
 *  All the work is actually done by other classes.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-27
 */
public class Reader {
    /**
     *  The types of objects we can read.
     */
    enum Target {
        ENTRYPOINTS(List.class, AndroidEntryPoint.class),
        INSTANTIATION(IInstantiationBehavior.class, null);

        private final Class type;
        private final Class subType;
        private Target(final Class type, final Class subType) {
            this.type = type;
            this.subType = subType;
        }

        public Class getType() {
            return this.type;
        }
    }
    private Map<Target, Object> targets = new EnumMap<Target, Object>(Target.class);

    private final InputStream input;
    
    public Reader(final File xmlFile, IClassHierarchy cha) throws java.io.FileNotFoundException {
        if (xmlFile == null) {
            throw new IllegalArgumentException("xmlFile may not be null");
        }
        this.input = new FileInputStream(xmlFile);
        new Items(new WalaObjectFactory(cha), new Deserializer(targets));  // does static stuff
    }

    public Reader(final InputStream xmlFile, IClassHierarchy cha) {
        if (xmlFile == null) {
            throw new IllegalArgumentException("xmlFile may not be null");
        }
        this.input = xmlFile;
        new Items(new WalaObjectFactory(cha), new Deserializer(targets));
    }

    protected void addTarget(final Object target, final Target of) {
        if (target == null) {
            throw new IllegalArgumentException("The target-object may not be null");
        }
        if (of == null) {
            throw new IllegalArgumentException("The parameter \"of\" may not be null");
        }
        final Class expectedType = of.getType();
        if (! expectedType.isAssignableFrom(target.getClass())) {
            throw new IllegalArgumentException("The expected type " + expectedType + " is not assignable " +
                    "from the given object " + target.getClass());
        }
        if (targets.containsKey(of)) {
            throw new IllegalStateException("The target for " + of + " is already set.");
        }
        targets.put(of, target);
    }

    public void addTarget(final List<? super AndroidEntryPoint> entrypoints) {
        addTarget(entrypoints, Target.ENTRYPOINTS);
    }

    public void addTarget(final IInstantiationBehavior beh) {
        addTarget(beh, Target.INSTANTIATION);
    }

    public Object getTarget(Target which) {
        return null;
    }

    public void read() throws ParserException {
        assert(this.input != null) : "Stream of the XML-File is null";

        try {
            final EPFileHandler handler = new EPFileHandler();
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.newSAXParser().parse(new InputSource(this.input), handler);
        } catch (ParserConfigurationException e) {
            throw new ParserException("When preparing parser:", e);
        } catch (SAXException e) { 
            throw new ParserException("When preparing parser:", e);
        } catch (IOException e) {
            throw new ParserException("When preparing parser:", e);
        }
    }

}
