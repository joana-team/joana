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
package edu.kit.joana.wala.jodroid;

import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 *  configure log-levels for various classes.
 *
 *  Configures the loggers returned by slf4js LoggerFactory.
 *  @todo TODO: Get rid of this file and only use logback.xml
 *
 *  @author  Tobias Blaschke <code@tobiasblaschke.de>
 *  @since  2013-12-12
 */
public class LogLevels {
    private static final Level defaultLevel = Level.WARN;

    /**
     *  Classes to mute _completely_ even errors!
     */
    private static final Class[] defaultMute = new Class[] {
        // The ParameterAccessor dumps a lot of messages about not finding ceratin
        // classes. These are not really fatal, so zap them.
        com.ibm.wala.util.ssa.ParameterAccessor.class,
        com.ibm.wala.dalvik.ipa.callgraph.androidModel.parameters.Instantiator.class
    };

    private static final Class[] defaultError = new Class[] {
        // The IntentContextSelector currently warns on all null-values. This is a
        // bit annoying. This should rather be fixed than zaped 
        com.ibm.wala.dalvik.ipa.callgraph.propagation.cfa.IntentContextSelector.class,
        // This class currently disrespects the defaultLoglevel. It's here until I
        // figure out why.
        com.ibm.wala.dalvik.classLoader.DexFileModule.class,
        // "no instruction at ..." is not of particualr interest. ZAP!
    };

    /**
     *  Set the loglevel of cls to 
     */
    public static void debug(Class cls) {
        final Logger logger = (Logger) LoggerFactory.getLogger(cls);
        logger.setLevel(Level.DEBUG);
    }

    public static void setLevels() {
        final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(defaultLevel);

        for (Class muteMe: defaultMute) {
            final Logger logger = (Logger) LoggerFactory.getLogger(muteMe);
            logger.setLevel(Level.OFF);
        }

        for (Class muteMe: defaultError) {
            final Logger logger = (Logger) LoggerFactory.getLogger(muteMe);
            logger.setLevel(Level.ERROR);
        }
    }
}
