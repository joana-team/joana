package edu.kit.joana.SuSi2Joana;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.Set;
import java.util.HashSet;

public class IfcScript {
    private final Set<Source> seenSources = new HashSet<Source>();
    private final Set<Sink> seenSinks = new HashSet<Sink>();
    private final ILatticeBuilder lattice;


    public static abstract class Entry {
        public final String method;
        public final String annot;
        public Entry(String method, String annot) {
            this.method = method;
            this.annot = annot;
        }
    }
    public static class Source extends Entry {
        public Source(String method, String annot) {
            super(method, annot);
        }
        @Override
        public int hashCode() {
            return this.method.hashCode();
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof Source) {
                final Source o = (Source) other;
                return o.method.equals(this.method) && o.annot.equals(this.annot);
            } else if (other instanceof String) {
                return this.method.equals(other);
            } else {
                return false;
            }
        }
        @Override
        public String toString() {
            return "source " + this.method + " " + this.annot;
        }
    }
    public static class Sink extends Entry {
        public Sink(String method, String annot) {
            super(method, annot);
        }
        @Override
        public int hashCode() {
            return this.method.hashCode();
        }
        @Override
        public boolean equals(Object other) {
            if (other instanceof Sink) {
                final Sink o = (Sink) other;
                return o.method.equals(this.method) && o.annot.equals(this.annot);
            } else if (other instanceof String) {
                return this.method.equals(other);
            } else {
                return false;
            }
        }
        @Override
        public String toString() {
            return "sink " + this.method + " " + this.annot;
        }
    }

    public IfcScript(ILatticeBuilder lattice) {
        this.lattice = lattice;
    }

    public void addSource(String method, String cathegory) {
        final Source source = new Source(method, this.lattice.getSource(cathegory));
        this.seenSources.add(source);
    }

    public void addSink(String method, String cathegory) {
        final Sink sink = new Sink(method, this.lattice.getSink(cathegory));
        this.seenSinks.add(sink);
    }

    public void write(final PrintWriter out) throws IOException {
        //final PrintStream ps = new PrintStream(File);

        for (Source src : this.seenSources) {
            out.println(src.toString());
        }
        out.println();
        for (Sink snk : this.seenSinks) {
            out.println(snk.toString());
        }

        //ps.close();
    }
}
