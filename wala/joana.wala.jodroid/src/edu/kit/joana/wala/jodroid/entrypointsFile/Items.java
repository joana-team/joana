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

import com.ibm.wala.dalvik.util.AndroidSettingFactory;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.lang.Class;
import java.lang.reflect.Constructor;

import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.dalvik.ipa.callgraph.impl.DexEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.InstanceBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.Exactness;


// LOCAL:
import edu.kit.joana.wala.jodroid.entrypointsFile.Tags.Tag;
import edu.kit.joana.wala.jodroid.entrypointsFile.Tags.Attr;
import edu.kit.joana.wala.jodroid.entrypointsFile.Tags.HistoryKey;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

/**
 * Tags have a ParserItem associated to them that handles the reading-process.
 *
 * For a Tag that needs special handling an ParserItem for it has to be created. But there are numerous 
 * Tags, that don't need special handling. For these the following Items exist:
 *
 * * FinalItem:
 *      A Tag that has no Child elements and whose data will get evaluated by a parent-tag
 * * NoOpItem:
 *      A Tag that may contain child Tags whose data gets evaluated elsewhere (by a parent-Tag) or which
 *      has no data to evaluate associated wit it.
 *      Can be used when just adding the "path" from Tag.ROOT to something that evaluates itself.
 *
 * @since   2013-10-13
 * @author  Tobias Blaschke <code@tobiasblaschke.de>
 */
class Items {
    //private static final Logger logger = LoggerFactory.getLogger(Items.class);
    private static final Logger logger = NOPLogger.NOP_LOGGER;

    private static WalaObjectFactory factory = null;
    private static Deserializer disel = null;

    public Items(final WalaObjectFactory factory, final Deserializer disel) {
        Items.factory = factory;
        Items.disel = disel;
    }

    /**
     *  Contains the "path" from Tag.ROOT that currently gets evaluated.
     *
     *  On an opening Tag the Tag gets pushed. It get's popped again once it's evaluated. 
     *  That is on the closing Tag or on the closing Tag of a parent: A Item does not remove its 
     *  own Tag from the Stack.
     */
    private static final Stack<Tag> parserStack = new Stack<Tag>();

    /**
     *  Contains either Attributes of a child or the evaluation-result of a child-Tag.
     *
     *  The Item that consumes an Attribute has to pop it.
     */
    private static final Map<HistoryKey, Stack<Object>> attributesHistory = new HashMap<HistoryKey, Stack<Object>>();  // No EnumMap possible :(

    /**
     *  Create an empty attributesHistory.
     */
    static {
        for (Attr attr : Attr.values()) {
            attributesHistory.put(attr, new Stack<Object>());
        }
        for (Tag tag : Tag.values()) {
            attributesHistory.put(tag, new Stack<Object>());
        }
    }

    /**
     *  Handling of a Tag.
     *
     *  Does (if not overridden) all the needed Push- and Pop-Operations. Items may however choose to leave some
     *  stuff on the Stack. In this case this data has to be poped by the handling parent, or the Tag may only
     *  occur once or bad things will happen.
     *
     *  _CAUTION_: This will be instantiated by an Enum, so if you write local Fields you will get surprising 
     *  results! You should mark all of them as final to be sure.
     */
    public static abstract class ParserItem {
        protected Tag self;
        /**
         *  Set the Tag this ParserItem-Instance is an Handler for.
         *
         *  This may only be set once!
         */
        public void setSelf(Tag self) {
            if (this.self != null) {
                throw new IllegalStateException("Self can only be set once!");
            }
            this.self = self;
        }

        public ParserItem() {
        }

        /**
         *  Remember attributes to the tag. 
         *
         *  The read attributes will be pushed to the attributesHistory.
         *
         *  Leave Parser-Stack alone! This is called by SAXHandler only!
         */
        public void enter(Attributes saxAttrs) {
            for (Attr relevant : self.getRelevantAttributes()) {
                String attr = saxAttrs.getValue(relevant.getName());
                if (attr == null) {
                    attr = saxAttrs.getValue("android:" + relevant.getName());
                }

                attributesHistory.get(relevant).push(attr);
                logger.debug("Pushing '" + attr + "' for " + relevant + " in " + self);
                // if there is no such value in saxAttrs it returns null 
            }
        }

        /**
         *  Remove all Attributes generated by self and self itself.
         *
         *  This is called by the consuming ParserItem.
         */
        public void popAttributes() {
             for (Attr relevant : self.getRelevantAttributes()) {
                 try {
                    logger.debug("Popping " + relevant + " of value " + attributesHistory.get(relevant).peek() + " in " + self);
                    attributesHistory.get(relevant).pop();
                 }  catch (java.util.EmptyStackException e) {
                    System.err.println(self + " failed to pop " + relevant);
                    throw e;
                 }
             }
             if (attributesHistory.containsKey(self) && attributesHistory.get(self) != null &&
                     (! attributesHistory.get(self).isEmpty())) {
                 try {
                     attributesHistory.get(self).pop();
                 } catch (java.util.EmptyStackException e) {
                    System.err.println("The Stack for " + self + " was Empty when trying to pop");
                    throw e;
                 }
             }
        }

        /**
         *  Consume subitems on the stack.
         *
         *  Do this by popping them, but leave self on the stack!
         *  For each Item popped call its popAttributes()!
         */
        public void leave() {
            while (parserStack.peek() != self) {
                final Set<Tag> allowedSubTags = self.getAllowedSubTags();
                Tag subTag = parserStack.pop();
                if (allowedSubTags.contains(subTag)) {
                    subTag.getHandler().popAttributes(); // hmmm....

                    logger.debug("New Stack: " + parserStack);
                    //parserStack.pop();
                } else {
                    throw new IllegalStateException(subTag + " is not allowed as sub-tag of " + self + " in Context:\n\t" + parserStack);
                }
            }
        }
    }

    /**
     *  An ParserItem that contains no sub-tags.
     *
     *  You can use it directly if you don't intend to do any computation on this Tag but remember its
     *  Attributes.
     */
    public static class FinalItem extends ParserItem {
        public FinalItem() {
            super();
        }
        @Override
        public void leave() {
            final Set<Tag> subs = self.getAllowedSubTags();
            if (!((subs == null) || subs.isEmpty())) {
                throw new IllegalArgumentException("FinalItem can not be applied to " + self + " as it contains sub-tags: " + 
                        self.getAllowedSubTags());
            }

            if (parserStack.peek() != self) {
                throw new IllegalStateException("Topstack is not " + self + " which is disallowed for a FinalItem!\n" +
                        "This is most certainly caused by an implementation mistake on a ParserItem. Stack is:\n\t" + parserStack);
            }
        }
    }

    /**
     *  Only extracts Attributes.
     *
     *  It's like FinalItem but may contain sub-tags.
     */
    public static class NoOpItem extends ParserItem {
        public NoOpItem() {
            super();
        }
    }


    /**
     *  Creates objects for its contained EntryPoints.
     */
    public static class SectionItem extends ParserItem {
        public SectionItem() {
            super();
        }
        @Override
        public void enter(Attributes saxAttrs) {
            boolean nameSet = false;
            for (Attr relevant : self.getRelevantAttributes()) {
                String attr = saxAttrs.getValue(relevant.getName());

                if (relevant == Attr.NAME) {
                    final ExecutionOrder order = factory.ExecutionOrder(attr);
                    attributesHistory.get(relevant).push(order);
                    logger.debug("Pushing '" + order + "' for " + relevant + " in " + self);
                    nameSet = true;
                } else {
                    attributesHistory.get(relevant).push(attr);
                    logger.debug("Pushing '" + attr + "' for " + relevant + " in " + self);
                }
            }

            if (! nameSet) {
                throw new IllegalStateException("The required attribute name is not set on a Section-Tag");
            }
        }
        @Override
        public void leave() {
            final Stack<DexEntryPoint> eps = new Stack<DexEntryPoint>();
            while (parserStack.peek() != self) {
                final Tag current = parserStack.pop();
                final Set<Tag> allowedTags = self.getAllowedSubTags();
                if (! allowedTags.contains(current)) {
                    throw new IllegalStateException("In " + self + ": Tag " + current + " not allowed in Context " + parserStack + "\n\t"+
                            "Allowed Tags: " + allowedTags);
                }

                assert (current == Tag.ENTRYPOINT);
                
                // As we use a Stack the EntryPoints are in reverse-order now - so let's read them an
                // take them from behinde
                eps.push((DexEntryPoint) attributesHistory.get(current).peek());
                current.getHandler().popAttributes();
            }

            { // Now for the entrypoints
                ExecutionOrder order = (ExecutionOrder) attributesHistory.get(Attr.NAME).peek();

                while (! eps.isEmpty()) {
                    final DexEntryPoint dep = eps.pop();
                    final AndroidEntryPoint aep = factory.AndroidEntryPoint(dep, order);
                    order = ExecutionOrder.directlyAfter(order);
                    disel.deserialize(aep);
                }
            }
        }
    }


    public static class EPItem extends FinalItem {
        @Override
        public void enter(Attributes saxAttrs) {
            for (Attr relevant : self.getRelevantAttributes()) {
                String attr = saxAttrs.getValue(relevant.getName());
                if (attr == null) {
                    attr = saxAttrs.getValue("android:" + relevant.getName());
                }

                if (relevant == Attr.CALL) {
                    final MethodReference mRef = factory.MethodReference(attr);
                    attributesHistory.get(relevant).push(mRef);
                    logger.debug("Pushing '" + mRef + "' for " + relevant + " in " + self);
                } else {
                    attributesHistory.get(relevant).push(attr);
                    logger.debug("Pushing '" + attr + "' for " + relevant + " in " + self);
                }
            }
        }
        @Override
        public void leave() {
            //super.leave();

            final Map<Integer, List<TypeReference>> with = new HashMap<Integer, List<TypeReference>>();
            { // Catch the with-tags
                while (parserStack.peek() != self) {
                    final Tag current = parserStack.pop();
                    final Set<Tag> allowedTags = self.getAllowedSubTags();
                    if (! allowedTags.contains(current)) {
                        throw new IllegalStateException("In " + self + ": Tag " + current + " not allowed in Context " + parserStack + "\n\t"+ "Allowed Tags: " + allowedTags);
                    }

                    assert (current == Tag.WITH);

                    final String paramString = (String) attributesHistory.get(Attr.PARAM).peek();
                    final int paramNo;
                    if (paramString.equals("this")) {
                        paramNo = 0;
                    } else {
                        paramNo = Integer.parseInt(paramString) - 1;
                    }

                    final TypeReference type = factory.TypeReference((String)attributesHistory.get(Attr.TYPE).peek());
                    
                    if (with.containsKey(paramNo)) {
                        with.get(paramNo).add(type);
                    } else {
                        final List<TypeReference> typeList = new ArrayList<TypeReference>();
                        typeList.add(type);
                        with.put(paramNo, typeList);
                    }

                    current.getHandler().popAttributes();
                }
            }
           
            // Generating a position-less EntryPoint for this...
            final String pack;
            final MethodReference mRef = (MethodReference) attributesHistory.get(Attr.CALL).peek();
            final DexEntryPoint ep = factory.entrypoint(mRef);

            for (Integer paramNo : with.keySet()) {
                TypeReference[] tRefs = new TypeReference[with.get(paramNo).size()];
                tRefs = with.get(paramNo).toArray(tRefs);
                ep.setParameterTypes(paramNo, tRefs);
            }

            attributesHistory.get(self).push(ep);
        }
    }

    public static class InstantiationItem extends ParserItem {
        public InstantiationItem() {
            super();
        }
        @Override
        public void enter(Attributes saxAttrs) {
            for (Attr relevant : self.getRelevantAttributes()) {
                String attr = saxAttrs.getValue(relevant.getName());

                if (attr != null) {
                    switch (relevant) {
                        case DEFAULT:
                            final InstanceBehavior beh = factory.InstanceBehavior(attr);
                            attributesHistory.get(relevant).push(beh);
                            break;
                        default:
                            attributesHistory.get(relevant).push(attr);
                            logger.warn("Don't know to handle " + relevant  + " to " + self);
                    }
                }
            }
        }
        @Override
        public void leave() {
            super.leave();
        
            final InstanceBehavior beh = (InstanceBehavior) attributesHistory.get(Attr.DEFAULT).peek();

            disel.deserialize(beh);
        }
    }

    public static class BehavioralItem extends FinalItem {
        // <behaviour of="CREATE" package="Ljava/lang" exactness="EXACT">
        // EnumSet.of(Attr.OF, Attr.TYPE, Attr.PACKAGE, Attr.EXACTNESS, Attr.TO, Attr.CALL, Attr.NAME),
        @Override
        public void enter(Attributes saxAttrs) {
            for (Attr relevant : self.getRelevantAttributes()) {
                String attr = saxAttrs.getValue(relevant.getName());

                if (attr != null) {
                    switch (relevant) {
                        case EXACTNESS:
                            final Exactness exactness = factory.Exactness(attr);
                            attributesHistory.get(relevant).push(exactness);
                            logger.debug("Pushing '" + attr + "' for " + relevant + " in " + self);
                            break;
                        case OF: 
                            final InstanceBehavior beh = factory.InstanceBehavior(attr);
                            attributesHistory.get(relevant).push(beh);
                            break;
                        case TYPE:
                            final TypeName type = factory.TypeName(attr);
                            attributesHistory.get(relevant).push(type);
                            break;
                        case PACKAGE:
                            final Atom pack = factory.Atom(attr);
                            attributesHistory.get(relevant).push(pack);
                            break;
                        case TO:
                            final TypeName toClass = factory.TypeName(attr);
                            attributesHistory.get(relevant).push(toClass);
                        case CALL:
                            final MethodReference inCall = factory.MethodReference(attr);
                            attributesHistory.get(relevant).push(inCall);
                        case NAME:
                        default:
                            attributesHistory.get(relevant).push(attr);
                            logger.warn("Don't know to handle " + relevant  + " to " + self);
                    }
                } else {
                    // We have to push the null
                    attributesHistory.get(relevant).push(attr);
                }
            }
        }
        @Override
        public void leave() {
            super.leave();
        
            final Exactness exactness = (Exactness) attributesHistory.get(Attr.EXACTNESS).peek();
            final InstanceBehavior beh = (InstanceBehavior) attributesHistory.get(Attr.OF).peek();
            final TypeName type = (TypeName) attributesHistory.get(Attr.TYPE).peek();
            final Atom pack = (Atom) attributesHistory.get(Attr.PACKAGE).peek();
            final TypeName toClass = (TypeName) attributesHistory.get(Attr.TO).peek();
            final MethodReference inCall = (MethodReference) attributesHistory.get(Attr.CALL).peek();

            disel.deserialize(exactness, beh, type, pack, toClass, inCall);
        }
    }


    public static class EPFileHandler extends DefaultHandler {
        private int unimportantDepth = 0;

        public EPFileHandler() {
            super();
            parserStack.push(Tag.ROOT);
        }

        @Override
        public void startElement(String uri, String name, String qName, Attributes attrs) {
            Tag tag = Tag.fromString(qName);
            if ((tag == Tag.UNIMPORTANT) || (unimportantDepth > 0)) {
                unimportantDepth++;
            } else {
                logger.debug("Handling " + tag + " made from " + qName);

                final ParserItem handler = tag.getHandler();
                if (handler != null) {
                    handler.enter(attrs);
                }
                parserStack.push(tag);

            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (unimportantDepth > 0) {
                unimportantDepth--;
            } else {
                final Tag tag = Tag.fromString(qName);
                final ParserItem handler = tag.getHandler();
                if (handler != null) {
                    handler.leave();
                }
            }
        }
    }
}
