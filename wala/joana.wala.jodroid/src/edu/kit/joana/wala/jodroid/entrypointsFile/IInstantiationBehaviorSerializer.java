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
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.Exactness;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.InstanceBehavior;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.Atom;

/**
 *  A Serializer specialized in reading IInstantiationBehavior-Objects.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-20-26
 */
public class IInstantiationBehaviorSerializer extends Serializer {
    public IInstantiationBehaviorSerializer(Marshall marshall) {
        super(marshall);
    }

    public void serialize(IInstantiationBehavior beh) {
        final ObjectStreamClass ser = ObjectStreamClass.lookup(beh.getClass());
        final ObjectStreamField[] fields = ser.getFields();

        { // Read out the default behavior
            final InstanceBehavior defaultBehvior = beh.getDafultBehavior();
            marshall.defaultBehaviour(defaultBehvior);
        }

        // Most certainly the data s in a Map...
        for (final ObjectStreamField osField: fields) {
            if ((! osField.isPrimitive()) && ( Map.class.isAssignableFrom(osField.getType()) )) {
                final Reflected<Map<?, ?>> map = Reflected.reflect(beh, osField).catchFirst(Map.class);
                final Map<?, ?> possiblyBehaviors = map.unbox();

                for (final Object key: possiblyBehaviors.keySet()) {
                    final ObjectStreamClass keySer = ObjectStreamClass.lookup(key.getClass());

                    if (key instanceof TypeName) {
                        final IInstantiationBehavior.InstanceBehavior behaviour = beh.getBehavior((TypeName)key, null, null, null);
                        final IInstantiationBehavior.Exactness exactness = beh.getExactness((TypeName)key, null, null, null);

                        marshall.behaviour((TypeName)key, null, null, null, behaviour, exactness);
                    } else if (key instanceof Atom) {
                        final Object val = possiblyBehaviors.get(key);
                        final ObjectStreamClass valSer = ObjectStreamClass.lookup(val.getClass());
                        final ObjectStreamField[] valFields = valSer.getFields();

                        Reflected<InstanceBehavior> behaviour = null;
                        Reflected<Exactness> exactness = null;
                        {
                            for (final ObjectStreamField valOsField : valFields) {
                                /* { // DEBUG 
                                    System.out.println("\t\tFIELD" + valOsField);
                                } // */
                                final Reflected<Field> valField = Reflected.reflect(val, valOsField);
                                if (behaviour == null) behaviour = valField.catchFirst(InstanceBehavior.class);
                                if (exactness == null) exactness = valField.catchFirst(Exactness.class);
                            }
                            assert (behaviour != null) : "null behavior";
                        }

                        marshall.behaviour((Atom)key, null, null, null, behaviour.unbox(), (exactness!=null)?exactness.unbox():null);
                    } else {
                        final ObjectStreamField[] keyFields = keySer.getFields();

                        final Set<Object> handled = new HashSet<Object>();
                        Reflected<MethodReference> possibleMRef = null;
                        Reflected<TypeName> typeName = null;
                        Reflected<Atom> possiblePackage = null;
                        Reflected<TypeName> possibleAsParameterTo = null;
                        { // Grab these
                            for (final ObjectStreamField keyOsField : keyFields) {
                                final Reflected<Field> keyField = Reflected.reflect(key, keyOsField);
                                // The order acutally metters: We don't want to extract an Atrom from a TypeName
                                if (possibleMRef == null) possibleMRef = keyField.catchFirst(MethodReference.class);
                                if (possibleMRef != null) { handled.add(possibleMRef.unbox()); continue; }
                                if (typeName == null) typeName = keyField.catchFirst(TypeName.class, handled);
                                if (typeName != null) { handled.add(typeName.unbox()); continue; }
                                if (possiblePackage == null) possiblePackage = keyField.catchFirst(Atom.class, handled);
                                if (possiblePackage != null) { handled.add(possiblePackage.unbox()); continue; }
                                if (possibleAsParameterTo == null) possibleAsParameterTo = keyField.catchFirst(TypeName.class, handled);
                                if (possibleAsParameterTo != null) { handled.add(possibleAsParameterTo.unbox()); continue; }
                            }
                        }
                        // TODO: Get the loaders to the types to be sure?

                        if (typeName != null) {
                            /* { // DEBUG
                                System.out.println("We fetched:" + handled);
                            } // */
                            // Prepare the parameters to getBehavior and getExactness
                            final TypeName mType = typeName.unbox();
                            //assert (possiblePackage == null) : "Now that was unexpected :/";
                            final TypeName mAsParameterTo = (possibleAsParameterTo != null)?possibleAsParameterTo.unbox():null;
                            final MethodReference mInCall = (possibleAsParameterTo != null)?possibleMRef.unbox():null;
                            final IInstantiationBehavior.InstanceBehavior behaviour = beh.getBehavior(mType, mAsParameterTo, mInCall, null);
                            final IInstantiationBehavior.Exactness exactness = beh.getExactness(mType, mAsParameterTo, mInCall, null);

                            marshall.behaviour(mType, mAsParameterTo, mInCall, null, behaviour, exactness);
                        } else if (possiblePackage != null) {
                            final Object val = possiblyBehaviors.get(key);
                            final ObjectStreamClass valSer = ObjectStreamClass.lookup(val.getClass());
                            final ObjectStreamField[] valFields = valSer.getFields();

                            final TypeName mAsParameterTo = (possibleAsParameterTo != null)?possibleAsParameterTo.unbox():null;
                            final MethodReference mInCall = (possibleAsParameterTo != null)?possibleMRef.unbox():null;

                            Reflected<InstanceBehavior> behaviour = null;
                            Reflected<Exactness> exactness = null;
                            {
                                for (final ObjectStreamField valOsField : valFields) {
                                    /* { // DEBUG
                                        System.out.println("\t\tFIELD" + valOsField);
                                    } // */
                                    final Reflected<Field> valField = Reflected.reflect(val, valOsField);
                                    if (behaviour == null) behaviour = valField.catchFirst(InstanceBehavior.class);
                                    if (exactness == null) exactness = valField.catchFirst(Exactness.class);
                                }
                                assert (behaviour != null) : "null behavior for " + possiblePackage;
                            }

                            marshall.behaviour(possiblePackage.unbox(), mAsParameterTo, mInCall, null, behaviour.unbox(), 
                                    (exactness!=null)?exactness.unbox():null);
                        } else {
                            // We are in the wrong Map :(
                        }
                        
                    }
                }
            }
        }

    }


}
