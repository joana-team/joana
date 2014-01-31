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

import edu.kit.joana.wala.core.SDGBuilder.SDGBuilderConfig;
import com.ibm.wala.dalvik.util.AndroidEntryPointManager;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

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
 *  A Serializer specialized for the Config of the AndroidModel.
 *
 *  @see    com.ibm.wala.dalvik.util.AndroidEntryPointManager
 *  @see    com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.IInstantiationBehavior
 *  @author Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-20-26
 */
public class ConfigSerializer extends Serializer {
    public ConfigSerializer(Marshall marshall) {
        super(marshall);
    }

    public void serialize(AndroidEntryPointManager manager) {
        final ObjectStreamClass ser = ObjectStreamClass.lookup(manager.getClass());
        final ObjectStreamField[] fields = ser.getFields();

        for (final ObjectStreamField osField: fields) {
            Reflected<Field> field = Reflected.reflect(manager, osField);

            if ( field.getValue() != null) {
                marshall.config(AndroidEntryPointManager.class, osField.getName(), field.getValue().toString(), osField.getType());
            } else {
                marshall.config(AndroidEntryPointManager.class, osField.getName(), null, osField.getType());
            }
        }
    }

    public void serialize(SDGBuilderConfig scfg) {
        final ObjectStreamClass ser = ObjectStreamClass.lookup(SDGBuilderConfig.class);
        final ObjectStreamField[] fields = ser.getFields();

        for (final ObjectStreamField osField: fields) {
            Reflected<Field> field = Reflected.reflect(scfg, osField);

            if ( field.getValue() != null) {
                marshall.config(SDGBuilderConfig.class, osField.getName(), field.getValue().toString(), osField.getType());
            } else {
                marshall.config(SDGBuilderConfig.class, osField.getName(), null, osField.getType());
            }
        }
    }


}
