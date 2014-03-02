package java.lang;

/**
 *  Only handle string-length.
 */
public final class StringBuilder
    implements java.io.Serializable, CharSequence {

    static final long serialVersionUID = 4383685877147921099L;
    private long length = 0;

    public StringBuilder() {
        this.length = 0;
    }

    public StringBuilder(int capacity) {
        this.length = capacity;
    }

    public StringBuilder(String str) {
        this.length = str.length();
    }

    public StringBuilder(CharSequence seq) {
        this.length = seq.length();
    }

    public StringBuilder append(Object obj) {
        this.length += String.valueOf(obj).length();
        return this;
    }

    public StringBuilder append(String str) {
        this.length += str.length();
        return this;
    }

    private StringBuilder append(StringBuilder sb) {
        this.length += sb.length;
        return this;
    }

    public StringBuilder append(StringBuffer sb) {
        this.length += sb.length();
        return this;
    }

    public StringBuilder append(CharSequence s) {
        this.length += s.length();
        return this;
    }

    public StringBuilder append(CharSequence s, int start, int end) {
        this.length += s.length() + start + end;
        return this;
    }

    public StringBuilder append(char str[]) {
        this.length += str.length;
        return this;
    }

    public StringBuilder append(char str[], int offset, int len) {
        this.length += str.length + offset + len;
        return this;
    }

    public StringBuilder append(boolean b) {
        this.length += (b)?1:2;
        return this;
    }

    public StringBuilder append(char c) {
        this.length += (int) c;
        return this;
    }

    public StringBuilder append(int i) {
        this.length += i;
        return this;
    }

    public StringBuilder append(long lng) {
        this.length += lng;
        return this;
    }

    public StringBuilder append(float f) {
        this.length += f;
        return this;
    }

    public StringBuilder append(double d) {
        this.length += d;
        return this;
    }

    public StringBuilder appendCodePoint(int codePoint) {
        this.length += codePoint;
        return this;
    }

    public StringBuilder delete(int start, int end) {
        return this;
    }

    public StringBuilder deleteCharAt(int index) {
        return this;
    }

    public StringBuilder replace(int start, int end, String str) {
        this.length += str.length() + start + end;
        return this;
    }

    public StringBuilder insert(int index, char str[], int offset,
                                int len)
    {
        this.length += str.length + index + offset + len;
        return this;
    }

    public StringBuilder insert(int offset, Object obj) {
        this.length += String.valueOf(obj).length() + offset;
        return this;
    }

    public StringBuilder insert(int offset, String str) {
        this.length += str.length() + offset;
        return this;
    }

    public StringBuilder insert(int offset, char str[]) {
        this.length += str.length + offset;
        return this;
    }

    public StringBuilder insert(int dstOffset, CharSequence s) {
        this.length += s.length() + dstOffset;
        return this;
    }

    public StringBuilder insert(int dstOffset, CharSequence s,
                                int start, int end) {
        this.length += s.length() + dstOffset;
        return this;
    }

    public StringBuilder insert(int offset, boolean b) {
        this.length += (b)?1:2;
        return this;
    }

    public StringBuilder insert(int offset, char c) {
        this.length += offset + (int)c;
        return this;
    }

    public StringBuilder insert(int offset, int i) {
        this.length += i + offset;
        return this;
    }

    public StringBuilder insert(int offset, long l) {
        this.length += l + offset;
        return this;
   }

    public StringBuilder insert(int offset, float f) {
        this.length += offset + f;
        return this;
    }

    public StringBuilder insert(int offset, double d) {
        this.length += offset + d;
        return this;
    }

    public int indexOf(String str) {
        return ((int)this.length) - str.length();
    }

    public int indexOf(String str, int fromIndex) {
        return ((int)this.length) - str.length() - fromIndex;
    }

    public int lastIndexOf(String str) {
        return ((int)this.length) - str.length();
    }

    public int lastIndexOf(String str, int fromIndex) {
        return ((int)this.length) - str.length() - fromIndex;
    }

    public StringBuilder reverse() {
        return this;
    }

    public String toString() {
        return String.valueOf(this.length);
    }

    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        s.writeInt((int) this.length);
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        this.length = s.readInt();
    }

    public char charAt(int index) {
        return (char) this.length;
    }

    public int length() {
        return (int) this.length;
    }

    public CharSequence subSequence(int start, int end) {
        return String.valueOf(this.length + start + end);
    }
}
