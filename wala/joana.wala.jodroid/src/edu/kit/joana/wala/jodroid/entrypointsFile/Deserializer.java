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

import java.util.List;
import java.util.Map;

import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.Exactness;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior.InstanceBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.LoadedInstantiationBehavior;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.wala.jodroid.entrypointsFile.Reader.Target;

class Deserializer {
    final Map<Target, Object> targets;

    public Deserializer(final Map<Target, Object> targets) {
        this.targets = targets;
    }


    public void deserialize(AndroidEntryPoint aep) {
        if (targets.containsKey(Target.ENTRYPOINTS)) {
            final List eps = (List) targets.get(Target.ENTRYPOINTS);
            eps.add(aep);
        }
    }

    public void deserialize(final InstanceBehavior beh) {
        Object obj = targets.get(Target.INSTANTIATION);
        if (obj instanceof LoadedInstantiationBehavior) {
            final LoadedInstantiationBehavior lb = (LoadedInstantiationBehavior) obj;
            lb.setDefaultBehavior(beh);
        } else {
            throw new UnsupportedOperationException("Can only deserialize to a LoadedInstantiationBehavior, got " +
                    obj.getClass());
        }
    }

    /**
     *  That should not be the right way to do it.
     */
    public void deserialize(final Exactness exactness, final InstanceBehavior beh, final TypeName type, final Atom pack,
            final TypeName toClass, final MethodReference inCall) {

        if (targets.containsKey(Target.INSTANTIATION)) {
            Object obj = targets.get(Target.INSTANTIATION);

            if (obj instanceof LoadedInstantiationBehavior) {
                final LoadedInstantiationBehavior lb = (LoadedInstantiationBehavior) obj;

                if (type != null) {
                    lb.setBehavior(type, toClass, inCall, "", beh, exactness);
                    //System.out.println("Setting Behavior of Type " + type + " to " + beh);
                } else {
                    lb.setBehavior(pack, toClass, inCall, "", beh, exactness);
                    //System.out.println("Setting Behavior of Pack " + pack + " to " + beh);
                }
            } else {
                throw new UnsupportedOperationException("Can only deserialize to a LoadedInstantiationBehavior, got " +
                        obj.getClass());
            }
        }
    }
}
