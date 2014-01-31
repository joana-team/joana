/**
 *  The starting-point for analyzing an Android-App using Joana.
 *
 *  After building there should be a "joana.wala.jodroid.jar" in the dist-folder. This may
 *  be invoked from the command line.
 *
 *  Handling an AndroidApp usully takes these steps:
 *
 *  1. Invoke JoDroid with the --scan option
 *
 *      A Configruation ".ntrP"-File is created. Adapt this to yout needs.
 *
 *  2. Invoke JoDroid with the --run and the --ep-file *ntrP options
 *
 *      This will generate a SDG in a *.pdg-File
 *
 *  3. Load this file into IFC-Console
 */
package edu.kit.joana.wala.jodroid;
