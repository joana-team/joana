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

import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint.IExecutionOrder;
import com.ibm.wala.dalvik.util.AndroidComponent;
import com.ibm.wala.classLoader.IMethod;

import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import edu.kit.joana.wala.jodroid.entrypointsFile.Serializer;
import edu.kit.joana.wala.jodroid.entrypointsFile.Serializer.Reflected;
import java.lang.reflect.Field;

import java.io.FileOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.io.ObjectOutput;
import java.io.IOError;

/**
 *  A Serializer specialized in reading Entrypoints.
 *
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-20-26
 */
public class EntrypointSerializer extends Serializer {
    public EntrypointSerializer(Marshall marshall) {
        super(marshall);
    }

    public void serialize(final List<? extends Entrypoint> entrypoints) {
        for (final Entrypoint ep : entrypoints) {
            if (ep instanceof AndroidEntryPoint) {
                final AndroidEntryPoint aep = (AndroidEntryPoint) ep;

                // this.out.print(section.generate(((AndroidEntryPoint)ep).getSection()));
                final IExecutionOrder section = aep.getSection();
                final IMethod method = aep.getMethod();
                final AndroidComponent component = aep.getComponent();

                final Map<Integer, List<TypeName>> with = new HashMap<Integer, List<TypeName>>();
                final int argCount = aep.getNumberOfParameters();
                for (int i = 0; i < argCount; ++i) {
                    final int paramNo;
                    if (method.isStatic()) {
                        paramNo = i + 1;
                    } else if (i == 0) {
                        paramNo = -1;   // will be translated to "this"
                    } else {
                        paramNo = i + 1;
                    }
                    final TypeReference[] args = aep.getParameterTypes(i);
                    if ((args.length > 1) || (! args[0].equals(method.getParameterType(i)))) {
                        final List<TypeName> argNames = new ArrayList<TypeName>();
                        for (final TypeReference type: args) {
                            argNames.add(type.getName());
                        }
                        with.put(i, argNames);
                    }
                }

                marshall.entrypoint(section, method, component, with);
            } else {
                throw new UnsupportedOperationException("Can only serialize AndroidEntryPoints, " + ep.getClass() + " given.");
            }
        }
    }


}
