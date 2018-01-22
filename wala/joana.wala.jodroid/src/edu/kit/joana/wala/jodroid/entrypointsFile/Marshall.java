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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.wala.jodroid.entrypointsFile.Exceptions.ParserException;
import edu.kit.joana.wala.jodroid.entrypointsFile.Tags.Attr;
import edu.kit.joana.wala.jodroid.entrypointsFile.Tags.Tag;
/**
 *  Arranges data coming in from the Serializers into a document.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-26
 */
class Marshall {
    private static final Logger logger = LoggerFactory.getLogger(Marshall.class);

    static class Document {
        private final Node root;
        private final Map<Tag, Node> top;

        public Document() {
            this.top = new EnumMap<Tag, Node>(Tag.class);
            this.root = new Node(Tag.ROOT, null);
            this.top.put(Tag.ROOT, this.root);
        }

        private class Node extends DetachedNode {
            private final Node parent;
            private final List<Node> children;

            public Node(Tag self, Node parent) {
                super(self);
                this.parent = parent;
                this.children = new ArrayList<Node>();
                top.put(self, this);
            }

            public void addChild(Node child) {
                this.children.add(child);
                top.put(child.self, child);
                logger.debug("Inserted: " + child.self + " under " + this.self);
            }

            public void addChild(DetachedNode det) {
                if (! self.getAllowedSubTags().contains(det.self)) {
                    throw new IllegalArgumentException(det.self.toString() + " is not allowed as child of " + self);
                }
                final Node elevated = new Node(det.self, parent);
                elevated.attrs = det.attrs;
                addChild(elevated);
            }

            public List<Node> getChildren() {
                return this.children; // XXX is still mutable
            }
        }        

        public void attach(DetachedNode det) {
            Tag exParent = det.self.parent(); 
            final Stack<Tag> toCreate = new Stack<Tag>();
            while (! top.containsKey(exParent)) {
                toCreate.push(exParent);
                exParent = exParent.parent(); 
            }
            while (! toCreate.empty() ) {
                final Node parent = top.get(exParent);
                final Node nd = new Node(toCreate.peek(), parent);
                parent.addChild(nd);
                exParent = toCreate.pop();
            }
            final Node parent = top.get(exParent);
            parent.addChild(det);
        }

        /**
         *  Creates a new XML-Document.
         */
        public void write(final XMLStreamWriter writer) throws ParserException { // XXX: ParserException?
            try {
                writer.writeStartDocument();    // XXX: More specific?
                writer.writeCharacters("\n");

                final List<Node> children = this.root.getChildren();
                assert (children.size() > 0) : "Root-Node has no children.";
                for (final Node child: children) {
                    write(writer, child, 0);
                }

                writer.writeCharacters("\n");
                writer.writeEndDocument();
                writer.flush();
            } catch (XMLStreamException e) {
                throw new ParserException("Error writing the document root", e);
            }
        }

        /**
         *  Writes a subtree.
         */
        public void write(final XMLStreamWriter writer, final Node current, final int indent) throws ParserException { // XXX: ParserException?
            try {
                writer.writeCharacters("\n");
                for (int i = 0; i < indent; ++i) {
                    writer.writeCharacters("    ");
                }
                writer.writeStartElement(current.self.getName());       // Start tag
                { // Attributes
                    final Map<Attr, String> attrs = current.getAttrs();
                    for (final Attr attr: attrs.keySet()) { 
                        writer.writeAttribute(attr.getName(), attrs.get(attr));
                    }
                }

                final List<Node> children = current.getChildren();
                { // Recurse
                    for (final Node child: children) {
                        write(writer, child, indent + 1);
                    }
                }

                if (children.isEmpty()) {
                    writer.writeEndElement();
                } else {
                    writer.writeCharacters("\n");
                    for (int i = 0; i < indent; ++i) {
                        writer.writeCharacters("    ");
                    }
                    writer.writeEndElement();                               // End tag
                }
            } catch (XMLStreamException e) {
                throw new ParserException("Error adding elements to the node: " + current, e);
            }
        }
    }

    static class DetachedNode {
        public final Tag self;
        protected Map<Attr, String> attrs;

        public DetachedNode(Tag self) {
            this.self = self;
            this.attrs = new EnumMap<Attr, String>(Attr.class);
        }

        public void putAttr(Attr key, String value) {
            this.attrs.put(key, value);
        }

        public Map<Attr, String> getAttrs() {
            return Collections.unmodifiableMap(this.attrs);
        }
    }

    private final Document doc;

    public Marshall() {
        this.doc = new Document();
    }

    public void write(final XMLStreamWriter writer) throws ParserException {
        this.doc.write(writer);
    }

    void defaultBehaviour(final IInstantiationBehavior.InstanceBehavior behaviour) {
        final DetachedNode entry = new DetachedNode(Tag.INSTANTIATION);
        entry.putAttr(Attr.DEFAULT, behaviour.toString());
        this.doc.attach(entry);
        // TODO: Assert there is only one INSTANTIATION-Tag
    }

    void behaviour(final TypeName type, final TypeName asParameterTo, final MethodReference inCall, 
            final String withName, final IInstantiationBehavior.InstanceBehavior behaviour, final 
            IInstantiationBehavior.Exactness exactness) {

        final DetachedNode entry = new DetachedNode(Tag.BEHAVIOUR);
        entry.putAttr(Attr.TYPE, type.toString());
        entry.putAttr(Attr.OF, behaviour.toString());

        if (exactness != null) {
            entry.putAttr(Attr.EXACTNESS, exactness.toString());
        }
        if (withName != null) {
            entry.putAttr(Attr.NAME, withName);
        }
        if (inCall != null) {
            entry.putAttr(Attr.CALL, inCall.toString());
        }
        if (asParameterTo != null) {
            entry.putAttr(Attr.TO, asParameterTo.toString());
        }

        this.doc.attach(entry);
    }

    void behaviour(final Atom type, final TypeName asParameterTo, final MethodReference inCall, 
            final String withName, final IInstantiationBehavior.InstanceBehavior behaviour, final 
            IInstantiationBehavior.Exactness exactness) {

        final DetachedNode entry = new DetachedNode(Tag.BEHAVIOUR);
        entry.putAttr(Attr.PACKAGE, type.toString());
        entry.putAttr(Attr.OF, behaviour.toString());

        if (exactness != null) {
            entry.putAttr(Attr.EXACTNESS, exactness.toString());
        }
        if (withName != null) {
            entry.putAttr(Attr.NAME, withName);
        }
        if (inCall != null) {
            entry.putAttr(Attr.CALL, inCall.toString());
        }
        if (asParameterTo != null) {
            entry.putAttr(Attr.TO, asParameterTo.toString());
        }

        this.doc.attach(entry);
    }

    private static IExecutionOrder currentSection = null;
    void entrypoint(final IExecutionOrder section, final IMethod method, final AndroidComponent component,
            final Map<Integer, List<TypeName>> with) {
        if ((currentSection == null) || (currentSection.getOrderValue() < section.getOrderValue())) {
            // Section switch
            currentSection = section;
            final DetachedNode sectionNd = new DetachedNode(Tag.SECTION);
            sectionNd.putAttr(Attr.NAME, section.toString());
            this.doc.attach(sectionNd);
        } else if (section.getOrderValue() > currentSection.getOrderValue()) {
            throw new IllegalArgumentException("Entrypoints have to be in ascending order!");
        }

        final DetachedNode point = new DetachedNode(Tag.ENTRYPOINT);
        if (component != null) {
            point.putAttr(Attr.TYPE, component.toString());
        }
        point.putAttr(Attr.CALL, method.getSignature());

        this.doc.attach(point);

        for (final Integer i : with.keySet()) {
            for (final TypeName type : with.get(i)) {
                final DetachedNode arg = new DetachedNode(Tag.WITH);
                if (i > 0) {
                    arg.putAttr(Attr.PARAM, i.toString());
                } else {
                    arg.putAttr(Attr.PARAM, "this");
                }
                arg.putAttr(Attr.TYPE, type.toString());
                this.doc.attach(arg);
            }
        }
    }

    Class<?> currentConfig;
    void config(Class<?> which, String key, String value, Class<?> type) {
        if (currentConfig != which) {
            currentConfig = which;
            final DetachedNode config = new DetachedNode(Tag.CONFIG);
            config.putAttr(Attr.OF, which.getCanonicalName());
            this.doc.attach(config);
        }

        final DetachedNode setting = new DetachedNode(Tag.SETTING);
        setting.putAttr(Attr.OF, key);
        if (value != null) {
            setting.putAttr(Attr.VALUE, value);
        }
        setting.putAttr(Attr.TYPE, type.getCanonicalName());
        this.doc.attach(setting);
    }

    void intent(Intent intent) {
        final DetachedNode tent = new DetachedNode(Tag.INTENT);

        tent.putAttr(Attr.NAME, intent.getAction().toString());
        if ( intent.getComponent() != null) {
            tent.putAttr(Attr.OF, intent.getComponent().toString());
        }
        tent.putAttr(Attr.RESOLVES, intent.getType().toString());

        this.doc.attach(tent);
    }
    
    void intentOverride(Intent intent) {
        final DetachedNode tent = new DetachedNode(Tag.OVERRIDE);

        tent.putAttr(Attr.NAME, intent.getAction().toString());
        if ( intent.getComponent() != null) {
            tent.putAttr(Attr.OF, intent.getComponent().toString());
        }
        tent.putAttr(Attr.RESOLVES, intent.getType().toString());

        this.doc.attach(tent);
    }

}
