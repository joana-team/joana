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
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;
import com.ibm.wala.ipa.callgraph.Entrypoint;

import edu.kit.joana.wala.core.SDGBuilder.SDGBuilderConfig;

/**
 *  Extract the data to write from WALAs structures.
 *
 *  All Serializers are specially crafted to only read out specific Data from WALA. They are
 *  _not_ intended to be used as generic Serializers that handle all classes implementing
 *  Serializable!
 *
 *  This superclass of the various specialized Serializers dispatches to them and adds some
 *  generic stuff for them.
 *
 *  When writing a new Serializer it should output its data to this.marshal in order to
 *  produce a single document.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-26
 */
public class Serializer {
    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);
   

    /**
     *  The Marshal-Object has the Document to write to associated to it.
     */
    final protected Marshall marshall;

    public Serializer(Marshall marshall) {
        this.marshall = marshall;
    }
   
    /**
     *  Dispatches to a specialized serializer based on the objects type.
     */
    public void serialize(Serializable object) {
        //final ObjectStreamClass serializeMe = ObjectStreamClass.lookup(object.getClass());
        //logger.info("Serializing: " + serializeMe.getName());

        if (object instanceof IInstantiationBehavior) {
            final IInstantiationBehaviorSerializer ser = new IInstantiationBehaviorSerializer(this.marshall);
            ser.serialize((IInstantiationBehavior) object);
            //serializeInstantiation(object, serializeMe);
        } else if (object instanceof AndroidEntryPointManager) {
            final ConfigSerializer ser = new ConfigSerializer(this.marshall); 
            ser.serialize((AndroidEntryPointManager) object);
        } else if (object instanceof SDGBuilderConfig) {
            final ConfigSerializer ser = new ConfigSerializer(this.marshall); 
            ser.serialize((SDGBuilderConfig) object);
        } else {
            throw new UnsupportedOperationException("I can't serialize " + object.getClass());
        }
    }

    public void serialize(Collection<? extends Entrypoint> object) {
        if (object.isEmpty()) {
            logger.warn("Got an empty collection to serialize");
            return;
        }

        if (object instanceof List) {
            final List<? extends Entrypoint> list = (List<? extends Entrypoint>) object;
            final Object first = list.get(0);

            if (first instanceof Entrypoint) {
                final EntrypointSerializer ser = new EntrypointSerializer(this.marshall);
                ser.serialize((List<? extends Entrypoint>) object);
            } else {
                throw new UnsupportedOperationException("I can't serialize a List of " + first.getClass());
            }
        }
    }

    public void serialize(Map<?, ?> map) { 
        if (map.isEmpty()) {
            logger.warn("Got an empty collection to serialize");
            return;
        }

        final Object first = map.keySet().iterator().next();

        if (first instanceof Intent) {
            final IntentSerializer ser = new IntentSerializer(this.marshall);
            ser.serializeInternal((Map<? extends Intent, ? extends Intent>) map);
        } else {
            throw new UnsupportedOperationException("I cant serialize a Map of " + first.getClass());
        }
    }

    /**
     *  Represents "something T" to read an actual value from.
     *
     *  In most cases that "something" is a java.lang.reflect.Field at first. It however can also be the
     *  contents of that field. The T may however _not_ be the Class to read the Fileds from.
     *
     *  See a specialized Serializers serialize function for usage examples.
     */
    public static class Reflected<T> {
        /**
         *  The thing boxed by Reflected&lt;T&gt;.
         */
        private T base;
        /**
         *  The Thing this.base resides in.
         *
         *  If T is an reflect.Fild, container is an ObjectStreamField.
         */
        private Object container;

        /**
         *  Use the reflect-Method to create a new Reflected.
         *
         *  The constructor can't dispatch on T, so a static method (reflect) is used instead.
         */
        private Reflected(final T base, final Object container) {
            this.base = base;
            this.container = container;
        }

        /**
         *  Remove Reflected, use {@link getValue()} to get the contents of T.
         *
         *  unbox() does not make much sense on a T of Field, in this case {@link getValue()} in most cases
         *  makes more sense.
         *
         *  It does however make sense on other T, where this.container is the Field.
         */
        public T unbox() {
            return base;
        }

        /**
         *  This "constructor" reads an ObjectStreamField.
         */
        public static Reflected<Field> reflect(final Object object, final ObjectStreamField osField) {
            try {
                final Field fd = object.getClass().getDeclaredField(osField.getName());
                return new Reflected<Field>(fd, object);
            } catch (java.lang.NoSuchFieldException e) {
                throw new IllegalStateException("this does not happen", e);
            }
        }

        /**
         *  Shorthand for catchFirst(cls, EMPTY_SET).
         */
        @SuppressWarnings("unchecked")
        public <U> Reflected<U> catchFirst(final Class<?> cls) {
            return catchFirst(cls, Collections.EMPTY_SET);
        }

        /**
         *  Return first instance of cls in the tree under this.
         *
         *  If this boxes an instance assignable to cls this is returned.
         *  If this boxes a reflect.Field pointing to a type assignable to cls, the Fields contents get read
         *  similar to getValue() but the result gets boxed in a Reflected again.
         *  If neither is true a breadth-first search is started on the boxed object. 
         *
         *  The parameter butNot contains Objects whose subtrees are not entered during the search,
         *  thus can be used to retrieve all cls-Objects.
         *
         *  @param  cls The U.class to search for
         *  @param  butNot The objects not to enter during the search (defunct?)
         *  @return the boxed cls-Instace or null if not found
         *  @todo   check in what situations butNot is defunct.
         */
        public <U> Reflected<U> catchFirst(final Class<?> cls, final Set<Object> butNot) {
            final Object val = getValue();
            if (val == null) {
                // something was primitive
                logger.debug(this.base.toString());
                logger.debug(this.container.toString());
                return null;
            }
            if ((cls.isAssignableFrom(val.getClass())) && (! butNot.contains(val))) {
                return new Reflected<U>((U)val, this.base);  // XXX: ??!!
                // TODO: Don't enter an iterable?
            } else if (val instanceof Serializable) {
                final ObjectStreamClass ser = ObjectStreamClass.lookup(val.getClass());
                final ObjectStreamField[] fields = ser.getFields();
                final List<Reflected<?>> recurseOn = new ArrayList<Reflected<?>>();

                // breadth first...
                for (final ObjectStreamField osField: fields) {
                    if ((cls.isAssignableFrom(osField.getType())) && (! butNot.contains(val))) {
                        logger.debug("Found");
                        final Reflected<Field> field = reflect(val, osField);
                        return new Reflected<U>((U) field.getValue(), field);
                    } else {
                        logger.debug("It's not " + osField + " in " + ser);
                        if (! osField.isPrimitive() ) {
                            final Reflected<Field> field = reflect(val, osField);
                            recurseOn.add(field);
                        }
                    }
                }
                
                // private void writeObject(java.io.ObjectOutputStream stream)
                try {
                    final java.lang.reflect.Method writeObject = val.getClass().getDeclaredMethod("writeObject", java.io.ObjectOutputStream.class);
                    writeObject.setAccessible(true);
                    final SubObjectCollector collector = new SubObjectCollector();
                    writeObject.invoke(val, collector);
                    final Set<Object> findings = collector.getFindings(); 
                    if (! findings.isEmpty()) {
                        final Iterator<Object> it = findings.iterator();
                        while (it.hasNext()) {
                            final Object obj = it.next();
                            if ((cls.isAssignableFrom(obj.getClass())) && (! butNot.contains(val))) {
                                // found it
                                return new Reflected<U>((U)obj, val);   // XXX: val?!
                            } else {
                                logger.debug("Add to recursion: " + obj);
                                final Reflected<Object> field = new Reflected<Object>(obj, val);
                                recurseOn.add(field);
                            }
                        }
                    }
                } catch (java.lang.NoSuchMethodException e) {
                    // Is ok
                    logger.info("No special handling");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Go depth now...
                for (Reflected<?> refl: recurseOn) {
                    logger.info("Recursing on " + refl);
                    final Reflected<U> ret = refl.catchFirst(cls, butNot);
                    if (ret != null) {
                        return ret;
                    }
                }
                return null;
            } else {
                throw new UnsupportedOperationException("Cannot recurse :( Not serializable " + this.base + " of " + this.base.getClass());
            }
        }

        /**
         *  Get the value present in an Instance.
         */
        public Object getValue() {
            try {
                if (base instanceof Field) {
                    final Field field = (Field) base;
                    field.setAccessible(true);
                    return (field).get(this.container);
                } else {
                    // TODO: Dangerous:
                    return this.base;
                    //throw new UnsupportedOperationException("Can't get the value of a " + base.getClass() + " yet :(");
                }
            } catch (java.lang.IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String toString() {
            return "<Reflected<" + this.base.getClass()  +"> of " + this.base + ">";
        }
    }
}
