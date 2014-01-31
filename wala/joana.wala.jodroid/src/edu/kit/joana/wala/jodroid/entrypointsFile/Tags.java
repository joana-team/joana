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

import edu.kit.joana.wala.jodroid.entrypointsFile.Items.*;

import java.util.Collections;
import java.util.Set;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;


/**
 * All Tags and Attributes that should be processed in a XML-File.
 *
 * A Tag is actually processed by its associated ParserItem. If a Tag not listed here is
 * encountered in the XML-File the complete subtree under it will be ignored. Attributes
 * that are not mentioned in a Tag will be ignored in it as well.
 *
 * To add a new Tag to the read-ones simply add it (and the path to it) to the Tag-Enum and
 * associate an Item with it.
 *
 * To read a new Attribute add it to the Attr-Enum, add it to the relevant attributes of the
 * corresponding Tag and alter the ParserItem which the Attribute should be evaluated on
 * (this is either the one associated with the Tag itself or one associated to a parent Tag).
 *
 * @see edu.kit.joana.wala.jodroid.entrypointsFile.Items
 *
 * @since   2013-10-26
 * @author  Tobias Blaschke <code@tobiasblaschke.de>
 */
public class Tags {

    /**
     *  Internal helper needed for delayed initialization.
     *
     *  The Tags-Enum cannot refer to itself until all Enum-Items are added to it. Thus the
     *  definition of allowed Sub-Tags has to be delayed.
     */
    private interface ISubTags {
        public Set<Tag> getSubTags();
    }

    /**
     *  A HistoryKey is something that may be put on the parsers stack.
     */
    public interface HistoryKey {} ;

    /**
     *  Tags in this Enum are not ignored when reading an XML-File.
     *
     *  A Tag consists of a String representating the Tag-Name in the XML-File, a allowed 
     *  disposition, a Set of Attributes relevant in it and a ParserItem used to read in
     *  the tag.
     *
     *  The String-representation has to be _all_ lower case in order to be readable from the
     *  XML-File. "Abstract" tags have at least one upper case letter in it.
     */
    @SuppressWarnings("unchecked")
    public enum Tag implements HistoryKey {
        ROOT("ROOT",
                new ISubTags() { public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.MODEL); }},
                null,
                NoOpItem.class),
        MODEL("model",
                new ISubTags() { public Set<Tag> getSubTags() {
                    return EnumSet.of(Tag.META, Tag.INSTANTIATION, Tag.ENTRYPOINTS, Tag.INTENTS, Tag.CONFIG); }},
                null,
                NoOpItem.class),
            META("meta",
                    new ISubTags() { public Set<Tag> getSubTags() {
                        return Collections.EMPTY_SET; }},
                    EnumSet.of(Attr.NAME),  // XXX??
                    NoOpItem.class),
            CONFIG("config",
                    new ISubTags() { public Set<Tag> getSubTags() {
                        return EnumSet.of(Tag.SETTING); }},
                    EnumSet.of(Attr.OF),  // which config
                    NoOpItem.class),
                SETTING("setting",
                        new ISubTags() { public Set<Tag> getSubTags() {
                            return Collections.EMPTY_SET; }},
                        EnumSet.of(Attr.VALUE, Attr.NAME),
                        FinalItem.class),
            INSTANTIATION("instantiation",
                    new ISubTags() { public Set<Tag> getSubTags() {
                        return EnumSet.of(Tag.BEHAVIOUR); }},
                    EnumSet.of(Attr.DEFAULT),
                    InstantiationItem.class),
                BEHAVIOUR("behaviour",
                        new ISubTags() { public Set<Tag> getSubTags() {
                            return Collections.EMPTY_SET; }},
                        EnumSet.of(Attr.OF, Attr.TYPE, Attr.PACKAGE, Attr.EXACTNESS, Attr.TO, Attr.CALL, Attr.NAME),
                        BehavioralItem.class),
            ENTRYPOINTS("entrypoints",
                    new ISubTags() { public Set<Tag> getSubTags() {
                        return EnumSet.of(Tag.SECTION); }},
                    null,
                    NoOpItem.class),
                SECTION("section",
                        new ISubTags() { public Set<Tag> getSubTags() {
                            return EnumSet.of(Tag.ENTRYPOINT); }},
                        EnumSet.of(Attr.NAME),
                        SectionItem.class),
                    ENTRYPOINT("entrypoint",
                            new ISubTags() { public Set<Tag> getSubTags() {
                                return EnumSet.of(Tag.WITH); }},
                            EnumSet.of(Attr.TYPE, Attr.CALL),
                            EPItem.class),
                        WITH("with",
                            new ISubTags() { public Set<Tag> getSubTags() {
                                return Collections.EMPTY_SET; }},
                            EnumSet.of(Attr.PARAM, Attr.TYPE),
                            FinalItem.class),
            INTENTS("intents",
                    new ISubTags() { public Set<Tag> getSubTags() {
                        return  EnumSet.of(Tag.INTENT); }},
                    EnumSet.of(Attr.NAME, Attr.TO),
                    NoOpItem.class),
                INTENT("intent",
                        new ISubTags() { public Set<Tag> getSubTags() {
                            return EnumSet.of(Tag.OVERRIDE); }},
                        EnumSet.of(Attr.NAME, Attr.OF, Attr.RESOLVES),
                        NoOpItem.class),
                    OVERRIDE("override",
                            new ISubTags() { public Set<Tag> getSubTags() {
                                return Collections.EMPTY_SET; }},
                            EnumSet.of(Attr.NAME, Attr.OF, Attr.RESOLVES),
                            FinalItem.class),



        UNIMPORTANT("UNIMPORTANT",
                null,
                Collections.EMPTY_SET,
                null);

        private final String tagName;
        private final Set<Attr> relevantAttributes;
        private final ISubTags allowedSubTagsHolder;
        private final ParserItem item;
        private Set<Tag> allowedSubTags;    // Delay init
        private static final Map<String, Tag> reverseMap = new HashMap<String, Tag>();// HashMapFactory.make(9);
        
        Tag (String tagName, ISubTags allowedSubTags, Set<Attr> relevant, Class<? extends ParserItem> item) {
            this.tagName = tagName;
            this.relevantAttributes = relevant;
            this.allowedSubTagsHolder = allowedSubTags;
            if (item != null) {
                try {
                    this.item = item.newInstance();
                    this.item.setSelf(this);
                } catch (java.lang.InstantiationException e) {
                    e.getCause().printStackTrace();
                    throw new IllegalStateException("InstantiationException was thrown");
                } catch (java.lang.IllegalAccessException e) {
                    e.printStackTrace();
                    if (e.getCause() != null) {
                        e.getCause().printStackTrace();
                    }
                    throw new IllegalStateException("IllegalAccessException was thrown");
                }
            } else {
                this.item = null;
            }
        }

        static {
            for (Tag tag: Tag.values()) {
                reverseMap.put(tag.tagName, tag);
            }
        }
    
        /**
         *  Return the ParserItem to evaluate the Tag when reading in a XML-File.
         */
        public ParserItem getHandler() {
            return this.item;
        }

        /**
         *  _Dangerous_ function that returns _only_ the first Tag found this Tag may be child of.
         *
         *  @todo We should return a Set here
         */
        public Tag parent() {
            for (Tag t : Tag.values()) {
                if (t.getAllowedSubTags() == null) continue;
                if (t.getAllowedSubTags().contains(this)) {
                    return t;
                }
            }
            throw new IllegalStateException("No parent found");
        }

        /**
         *  The Tags that may appear as a child of this Tag.
         */
        public Set<Tag> getAllowedSubTags() {
            if (this.allowedSubTagsHolder == null) {
                return null;
            } else if (this.allowedSubTags == null) {
               this.allowedSubTags = allowedSubTagsHolder.getSubTags();
            }

            return Collections.unmodifiableSet(this.allowedSubTags);
        }

        /**
         *  The Attributes read in when parsing the Tag.
         *
         *  The read attributes get thrown on a Stak, thus they don't need to be evaluated
         *  in the Tag itself but may also be handled by a parent.
         *
         *  The handling Item has to pop them after it has evaluated them else it gets a big 
         *  mess.
         */
        public Set<Attr> getRelevantAttributes() {
            if (this.relevantAttributes == null) {
                return Collections.EMPTY_SET;
            } else {
                return Collections.unmodifiableSet(this.relevantAttributes);
            }
        }

        /**
         *  The given Attr is in {@link #getRelevantAttributes()}.
         */
        public boolean isRelevant(Attr attr) {
            return relevantAttributes.contains(attr);
        }

        /**
         *  All Tags in this Enum but UNIMPORTANT are relevant.
         */
        public boolean isRelevant() {
            return (this != Tag.UNIMPORTANT);
        }

        /**
         *  Match the Tag-Name in the XML-File against the one associated to the Enums Tag.
         *
         *  If no Tag in this Enum matches Tag.UNIMPORTANT is returned and the parser will ignore the
         *  tree under this tag.
         *
         *  Matching is case insensitive of course.
         */
        public static Tag fromString(String tag) {
            tag = tag.toLowerCase();
            
            if (reverseMap.containsKey(tag)) {
                return reverseMap.get(tag);
            } else {
                return Tag.UNIMPORTANT;
            }
        }

        /**
         *  The Tag appears in the XML File using this name.
         */
        public String getName() {
            return this.tagName;
        }
    }


    /**
     *  Attributes that may appear in a Tags.Tag.
     *
     *  In order to evaluate a new attribute it has to be added to the Tags it may appear in and 
     *  at least one ParserItem has to be adapted.
     *
     *  Values read for the single Attrs will get pushed to the attributesHistory (in Items).
     */
    public enum Attr implements HistoryKey {
        PARAM("param"),
        OF("of"),
        TYPE("type"),
        PACKAGE("package"),
        EXACTNESS("exactness"),
        TO("to"),
        CALL("call"),
        NAME("name"),
        RESOLVES("resolves"),
        VALUE("value"),
        DEFAULT("default");

        private final String attrName;
        Attr(String attrName) {
            this.attrName = attrName;
        }

        /**
         *  Shorthand for Tag.isRelevant(this).
         */
        public boolean isRelevantIn(Tag tag) {
            return tag.isRelevant(this);
        }

        /**
         *  How the Attr will appear in the XML-File.
         */
        public String getName() {
            return this.attrName;
        }
    }
}
