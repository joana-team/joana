package edu.kit.joana.SuSi2Joana;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.List;
import java.util.ArrayList;

import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.NullProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SuSiFile {
    private static final Logger logger = LoggerFactory.getLogger(SuSiFile.class);
    private IProgressMonitor mon;
    public List<Source> sources = new ArrayList<Source>(); 
    public List<Sink> sinks = new ArrayList<Sink>(); 

    public static abstract class Entry {
        public final String clazz;
        public final String method;
        public final String cathegory;
        private static final Pattern reClassMethod = Pattern.compile("\\s*<([\\w\\d\\.\\$]+):\\s*[\\w\\d\\.\\$\\[\\]]+\\s*((<init>)?[\\w\\d\\(\\)\\.\\$\\[\\]\\s,]+)>");
        private static final Pattern reCathegory = Pattern.compile("\\(([\\w_]*)\\)$");

        public Entry(String line) {
            final Matcher matcher = Entry.reClassMethod.matcher(line);
            if ( matcher.find() ) {
                this.clazz = matcher.group(1);
                this.method = matcher.group(2);
                final Matcher cmatcher = Entry.reCathegory.matcher(line);
                if ( cmatcher.find() ) {
                    this.cathegory = cmatcher.group(1);
                } else {
                    this.cathegory = null;
                }
            } else {
                logger.warn("Unable to parse: " + line);
                this.clazz = null;
                this.method = null;
                this.cathegory = null;
            }
        }

        public abstract String getType();

        @Override
        public String toString() {
            return "<SuSi " + this.getType() + " " + this.clazz + "." + this.method + " />";
        }

        @Override
        public int hashCode() {
            return this.getType().hashCode() * this.clazz.hashCode() * this.method.hashCode();
        }
    }

    public static class Source extends Entry {
        public Source(String line) {
            super(line);
        }

        @Override
        public String getType() {
            return "source";
        }
    }

    public static class Sink extends Entry {
        public Sink(String line) {
            super(line);
        }

        @Override
        public String getType() {
            return "sink";
        }
    }

    public SuSiFile(IProgressMonitor mon) {
        if (mon == null) {
            this.mon = new NullProgressMonitor();
        } else {
            this.mon = mon;
        }
    }

    public void readSources(File sources) throws IOException {
        this.mon.beginTask("Reading SuSi-sources", IProgressMonitor.UNKNOWN);
        final BufferedReader br = new BufferedReader(new FileReader(sources));
        String line;
        int fails = 0;

        while ((line = br.readLine()) != null) {
            if (line.contains("<")) {
                Source s = new Source(line);
                if ( s.method != null ) {
                    this.sources.add(s);
                } else {
                    fails++;
                }
            }
        }
        br.close();
        this.mon.done();
        logger.info("Read " + this.sources.size() + " sources, " + fails + " failed");
    }

    public void readSinks(File sinks) throws IOException {
        this.mon.beginTask("Reading SuSi-sinks", IProgressMonitor.UNKNOWN);
        final BufferedReader br = new BufferedReader(new FileReader(sinks));
        String line;
        int fails = 0;

        while ((line = br.readLine()) != null) {
            if (line.contains("<")) {
                Sink s = new Sink(line);
                if ( s.method != null ) {
                    this.sinks.add(s);
                } else {
                    fails++;
                }
            }
        }
        br.close();
        this.mon.done();
        logger.info("Read " + this.sinks.size() + " sinks, " + fails + " failed");

    }

}
