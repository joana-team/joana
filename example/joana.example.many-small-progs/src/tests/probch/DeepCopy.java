/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests.probch;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Utility for making deep copies (vs. clone()'s shallow copies) of
 * objects. Objects are first serialized and then deserialized. Error
 * checking is fairly minimal in this implementation. If an object is
 * encountered that cannot be serialized (or that references an object
 * that cannot be serialized) an error is printed to System.err and
 * null is returned. Depending on your specific application, it might
 * make more sense to have copy(...) re-throw the exception.
 *
 * @see http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
 */
public final class DeepCopy {

    /**
     * Returns a copy of the object, or null if the object cannot
     * be serialized.
     */
    public static Object copy(Object orig) {
        Object obj = null;
        try {
            // Write the object out to a byte array
            FastByteArrayOutputStream fbos =
                    new FastByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(fbos);
            out.writeObject(orig);
            out.flush();
            out.close();

            // Retrieve an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in =
                new ObjectInputStream(fbos.getInputStream());
            obj = in.readObject();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }

    /**
     * ByteArrayInputStream implementation that does not synchronize methods.
     */
    static class FastByteArrayInputStream extends InputStream {
        /**
         * Our byte buffer
         */
        protected byte[] buf = null;

        /**
         * Number of bytes that we can read from the buffer
         */
        protected int count = 0;

        /**
         * Number of bytes that have been read from the buffer
         */
        protected int pos = 0;

        public FastByteArrayInputStream(byte[] buf, int count) {
            this.buf = buf;
            this.count = count;
        }

        public final int available() {
            return count - pos;
        }

        public final int read() {
            return (pos < count) ? (buf[pos++] & 0xff) : -1;
        }

        public final int read(byte[] b, int off, int len) {
            if (pos >= count)
                return -1;

            if ((pos + len) > count)
                len = (count - pos);

            System.arraycopy(buf, pos, b, off, len);
            pos += len;
            return len;
        }

        public final long skip(long n) {
            if ((pos + n) > count)
                n = count - pos;
            if (n < 0)
                return 0;
            pos += n;
            return n;
        }

    }

    /**
     * ByteArrayOutputStream implementation that doesn't synchronize methods
     * and doesn't copy the data on toByteArray().
     */
    static class FastByteArrayOutputStream extends OutputStream {
        /**
         * Buffer and size
         */
        protected byte[] buf = null;
        protected int size = 0;

        /**
         * Constructs a stream with buffer capacity size 5K
         */
        public FastByteArrayOutputStream() {
            this(5 * 1024);
        }

        /**
         * Constructs a stream with the given initial size
         */
        public FastByteArrayOutputStream(int initSize) {
            this.size = 0;
            this.buf = new byte[initSize];
        }

        /**
         * Ensures that we have a large enough buffer for the given size.
         */
        private void verifyBufferSize(int sz) {
            if (sz > buf.length) {
                byte[] old = buf;
                buf = new byte[Math.max(sz, 2 * buf.length )];
                System.arraycopy(old, 0, buf, 0, old.length);
                old = null;
            }
        }

        public int getSize() {
            return size;
        }

        /**
         * Returns the byte array containing the written data. Note that this
         * array will almost always be larger than the amount of data actually
         * written.
         */
        public byte[] getByteArray() {
            return buf;
        }

        public final void write(byte b[]) {
            verifyBufferSize(size + b.length);
            System.arraycopy(b, 0, buf, size, b.length);
            size += b.length;
        }

        public final void write(byte b[], int off, int len) {
            verifyBufferSize(size + len);
            System.arraycopy(b, off, buf, size, len);
            size += len;
        }

        public final void write(int b) {
            verifyBufferSize(size + 1);
            buf[size++] = (byte) b;
        }

        public void reset() {
            size = 0;
        }

        /**
         * Returns a ByteArrayInputStream for reading back the written data
         */
        public InputStream getInputStream() {
            return new FastByteArrayInputStream(buf, size);
        }

    }
}
