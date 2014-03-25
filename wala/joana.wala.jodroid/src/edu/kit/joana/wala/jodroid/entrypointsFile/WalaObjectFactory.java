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

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.Exactness;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.InstanceBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.ExecutionOrder;
import com.ibm.wala.dalvik.ipa.callgraph.impl.DexEntryPoint;
import com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.Intent;
import com.ibm.wala.dalvik.util.AndroidSettingFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

import edu.kit.joana.wala.jodroid.entrypointsFile.Exceptions.StringUnpackException;

/**
 *  Create an Object suitable for use in Wala from its String-Representation.
 *
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-10-27
 */
class WalaObjectFactory extends AndroidSettingFactory {
    final IClassHierarchy cha;

    public WalaObjectFactory(final IClassHierarchy cha) {
        this.cha = cha;
    }

    /**
     *  The position information will be added later.
     */
    public DexEntryPoint entrypoint(MethodReference method) {
        return new DexEntryPoint(method, this.cha);
    } 

    public AndroidEntryPoint AndroidEntryPoint(DexEntryPoint dep, ExecutionOrder order) {
        return new AndroidEntryPoint(order, dep.getMethod(), dep.getClassHierarchy());
    }

    public static ExecutionOrder ExecutionOrder(String ord) {
        if (ord.contains(".")) {
            ord = ord.substring(ord.lastIndexOf(".") + 1);
        }

        return new ExecutionOrder(ord);
    }

    public static MethodReference MethodReference (String method) {
        return StringStuff.makeMethodReference(method);
        /*
        final TypeName clazz;
        {
            final String typeString;
            final String half = method.substring(0, method.lastIndexOf("("));
            if (half.contains(".")) {
                // Converte the dotted to normal
                final typePart = half.substring(0, half.lastIndexOf("."));
                typeString = StringStuff.deployment2CanonicalTypeString(typePart);
            } else {
                assert (half.startsWith("L"));
                typeString = half.substring(0, half.lastIndexOf("/"));
            }

            clazz = TypeName(typeString);
        }
        final Selector selector;
        {
            final int selectorStart;
        }
        //MethodReference     findOrCreate (TypeReference tref, Selector selector)*/
    }

    public TypeName TypeName(final String name) throws StringUnpackException {
        try {
            final TypeName ret = TypeName.findOrCreate(name);
            if (ret == null) {
                throw new StringUnpackException(name, TypeName.class, "Got back null.");
            } else {
                return ret;
            }
        } catch (Exception e) {
            throw new StringUnpackException(name, TypeName.class, e);
        }
    }

    public TypeReference TypeReference(final String name) throws StringUnpackException {
        final TypeName type = this.TypeName(name);
        return TypeReference.find(ClassLoaderReference.Application, type); // XXX Always Application?
    }

    public static Atom Atom(final String atom) throws StringUnpackException {
        try {
            final Atom ret = Atom.findOrCreateUnicodeAtom(atom);
            if (ret == null) {
                throw new StringUnpackException(atom, Atom.class, "Got back null.");
            } else {
                return ret;
            }
        } catch (Exception e) {
            throw new StringUnpackException(atom, Atom.class, e);
        }
    }

    public static Exactness Exactness(final String exactness) {
        try {
            final Exactness ret = Exactness.valueOf(exactness.toUpperCase());
            if (ret == null) {
                throw new StringUnpackException(exactness, Exactness.class, "Got back null.");
            } else {
                return ret;
            }
        } catch (Exception e) {
            throw new StringUnpackException(exactness, Exactness.class, e);
        }
    }

    public static InstanceBehavior InstanceBehavior(final String behaviour) {
        try {
            final InstanceBehavior ret = InstanceBehavior.valueOf(behaviour.toUpperCase());
            if (ret == null) {
                throw new StringUnpackException(behaviour, InstanceBehavior.class, "Got back null.");
            } else {
                return ret;
            }
        } catch (Exception e) {
            throw new StringUnpackException(behaviour, InstanceBehavior.class, e);
        }
    }

    public static Intent Intent(final String action, final String resolves) {
        Intent.IntentType res;
        { // parse resolves
            try {
                res = Intent.IntentType.valueOf(resolves.toUpperCase());  
            } catch (IllegalArgumentException e) {
                //logger.error("Unable to parse resolves value " + resolves + " for intent " + 
                //        action + "! Using UNKNOWN_TARGET.");
                res = Intent.IntentType.UNKNOWN_TARGET;
            }
        }

        return Intent(action, res);
    }

    public static Intent Intent(final String action, final Intent.IntentType resolves) {
        final Intent intent;

        switch (resolves) {
            case BROADCAST:
                intent = new UnknownIntent(action);
                break;
            case EXTERNAL_TARGET:
                intent = new ExternalIntent(action);
                break;
            case IGNORE:
                intent = new IgnoreIntent(action);
                break;
            case INTERNAL_TARGET:
                intent =  new InternalIntent(action);
                break;
            case STANDARD_ACTION:
                intent = new StandardIntent(action);
                break;
            case SYSTEM_SERVICE:
                throw new UnsupportedOperationException("The IntentType " + resolves + " cannot be handed in this implementation!");
            case UNKNOWN_TARGET:
                intent = new UnknownIntent(action);
                break;
            default:
                throw new UnsupportedOperationException("The IntentType " + resolves + " cannot be handed in this implementation!");
        }

        return intent;
    }
}
