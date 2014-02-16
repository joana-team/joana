/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import android.util.Log;
import android.util.SparseArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class Bundle implements Parcelable, Cloneable {
    public static final Bundle EMPTY;

    static {
        EMPTY = new Bundle();
        EMPTY.mMap = Collections.unmodifiableMap(new HashMap<String, Object>());
    }

    /* package */ Map<String, Object> mMap = null;
    /* package */ Parcel mParcelledData = null;

    private boolean mHasFds = false;
    private boolean mFdsKnown = true;
    private boolean mAllowFds = true;
    private ClassLoader mClassLoader;

    public Bundle() {
        mMap = new HashMap<String, Object>();
        mClassLoader = getClass().getClassLoader();
    }

    /*Bundle(Parcel parcelledData) {
        readFromParcel(parcelledData);
    }

    Bundle(Parcel parcelledData, int length) {
        readFromParcelInner(parcelledData, length);
    }*/

    public Bundle(ClassLoader loader) {
        mMap = new HashMap<String, Object>();
        mClassLoader = loader;
    }

    public Bundle(int capacity) {
        mMap = new HashMap<String, Object>(capacity);
        mClassLoader = getClass().getClassLoader();
    }
    
    public Bundle(Bundle b) {
        mMap = b.mMap;
        mHasFds = b.mHasFds;
        mFdsKnown = b.mFdsKnown;
        mClassLoader = b.mClassLoader;
    }

    public static Bundle forPair(String key, String value) {
        Bundle b = new Bundle(1);
        b.putString(key, value);
        return b;
    }

    public String getPairValue() {
        return (String) mMap.values().iterator().next();
    }

    public void setClassLoader(ClassLoader loader) {
        mClassLoader = loader;
    }

    public ClassLoader getClassLoader() {
        return mClassLoader;
    }

    public boolean setAllowFds(boolean allowFds) {
        boolean orig = mAllowFds;
        mAllowFds = allowFds;
        return orig;
    }

    public Object clone() {
        return new Bundle(this);
    }

    public boolean isParcelled() {
        return mParcelledData != null;
    }

    public int size() {
        return mMap.size();
    }

    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    public void clear() {
    }

    public boolean containsKey(String key) {
        return mMap.containsKey(key);
    }

    public Object get(String key) {
        return mMap.get(key);
    }

    public void remove(String key) {
        mMap.remove(key);
    }

    public void putAll(Bundle map) {
        mMap.putAll(map.mMap);
    }

    public Set<String> keySet() {
        return mMap.keySet();
    }

    public boolean hasFileDescriptors() {
        return mMap.size() > 0;
    }
    
    public void putBoolean(String key, boolean value) {
        mMap.put(key, value);
    }

    public void putByte(String key, byte value) {
        mMap.put(key, value);
    }

    public void putChar(String key, char value) {
        mMap.put(key, value);
    }

    public void putShort(String key, short value) {
        mMap.put(key, value);
    }

    public void putInt(String key, int value) {
        mMap.put(key, value);
    }

    public void putLong(String key, long value) {
        mMap.put(key, value);
    }

    public void putFloat(String key, float value) {
        mMap.put(key, value);
    }

    public void putDouble(String key, double value) {
        mMap.put(key, value);
    }

    public void putString(String key, String value) {
        mMap.put(key, value);
    }

    public void putCharSequence(String key, CharSequence value) {
        mMap.put(key, value);
    }

    public void putParcelable(String key, Parcelable value) {
        mMap.put(key, value);
    }

    public void putParcelableArray(String key, Parcelable[] value) {
        mMap.put(key, value);
    }

    public void putParcelableArrayList(String key,
        ArrayList<? extends Parcelable> value) {
        mMap.put(key, value);
    }

    public void putSparseParcelableArray(String key,
            SparseArray<? extends Parcelable> value) {
        mMap.put(key, value);
    }

    public void putIntegerArrayList(String key, ArrayList<Integer> value) {
        mMap.put(key, value);
    }

    public void putStringArrayList(String key, ArrayList<String> value) {
        mMap.put(key, value);
    }
    
    public void putCharSequenceArrayList(String key, ArrayList<CharSequence> value) {
        mMap.put(key, value);
    }

    public void putSerializable(String key, Serializable value) {
        mMap.put(key, value);
    }

    public void putBooleanArray(String key, boolean[] value) {
        mMap.put(key, value);
    }

    public void putByteArray(String key, byte[] value) {
        mMap.put(key, value);
    }

    public void putShortArray(String key, short[] value) {
        mMap.put(key, value);
    }

    public void putCharArray(String key, char[] value) {
        mMap.put(key, value);
    }

    public void putIntArray(String key, int[] value) {
        mMap.put(key, value);
    }

    public void putLongArray(String key, long[] value) {
        mMap.put(key, value);
    }

    public void putFloatArray(String key, float[] value) {
        mMap.put(key, value);
    }

    public void putDoubleArray(String key, double[] value) {
        mMap.put(key, value);
    }

    public void putStringArray(String key, String[] value) {
        mMap.put(key, value);
    }

    public void putCharSequenceArray(String key, CharSequence[] value) {
        mMap.put(key, value);
    }

    public void putBundle(String key, Bundle value) {
        mMap.put(key, value);
    }

    public void putBinder(String key, IBinder value) {
        mMap.put(key, value);
    }

    public void putIBinder(String key, IBinder value) {
        mMap.put(key, value);
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    private void typeWarning(String key, Object value, String className,
        Object defaultValue, ClassCastException e) {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        sb.append(className);
        sb.append(value.getClass().getName());
        sb.append(defaultValue);
        Log.w("", sb.toString());
    }

    private void typeWarning(String key, Object value, String className,
        ClassCastException e) {
        typeWarning(key, value, className, "<null>", e);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return (Boolean) mMap.get(key);
    }

    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    public Byte getByte(String key, byte defaultValue) {
        return (Byte) mMap.get(key);
    }

    public char getChar(String key) {
        return getChar(key, (char) 0);
    }

    public char getChar(String key, char defaultValue) {
        return (Character) mMap.get(key);
    }

    public short getShort(String key) {
        return getShort(key, (short) 0);
    }

    public short getShort(String key, short defaultValue) {
        return (Short) mMap.get(key);
    }

    public int getInt(String key) {
        return getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        return (Integer)  mMap.get(key);
    }

    public long getLong(String key) {
        return getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue) {
        return (Long) mMap.get(key);
    }

    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    public float getFloat(String key, float defaultValue) {
        return (Float) mMap.get(key);
    }

    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    public double getDouble(String key, double defaultValue) {
        return (Double) mMap.get(key);
    }

    public String getString(String key) {
        return (String) mMap.get(key);
    }

    public String getString(String key, String defaultValue) {
        return getString(key);
    }

    public CharSequence getCharSequence(String key) {
        return (CharSequence) mMap.get(key);
    }

    public CharSequence getCharSequence(String key, CharSequence defaultValue) {
        return getCharSequence(key);
    }

    public Bundle getBundle(String key) {
        return (Bundle) mMap.get(key);
    }

    public <T extends Parcelable> T getParcelable(String key) {
        return (T) mMap.get(key);
    }

    public Parcelable[] getParcelableArray(String key) {
        return (Parcelable[]) mMap.get(key);
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayList(String key) {
        return (ArrayList<T>) mMap.get(key);
    }

    public <T extends Parcelable> SparseArray<T> getSparseParcelableArray(String key) {
        return (SparseArray<T>) mMap.get(key);
    }

    public Serializable getSerializable(String key) {
        return (Serializable) mMap.get(key);
    }

    public ArrayList<Integer> getIntegerArrayList(String key) {
        return (ArrayList<Integer>) mMap.get(key);
    }

    public ArrayList<String> getStringArrayList(String key) {
        return (ArrayList<String>) mMap.get(key);
    }

    public ArrayList<CharSequence> getCharSequenceArrayList(String key) {
        return (ArrayList<CharSequence>) mMap.get(key);
    }

    public boolean[] getBooleanArray(String key) {
        return (boolean[]) mMap.get(key);
    }

    public byte[] getByteArray(String key) {
        return (byte[]) mMap.get(key);
    }

    public short[] getShortArray(String key) {
        return (short[]) mMap.get(key);
    }

    public char[] getCharArray(String key) {
        return (char[]) mMap.get(key);
    }

    public int[] getIntArray(String key) {
        return (int[]) mMap.get(key);
    }

    public long[] getLongArray(String key) {
        return (long[]) mMap.get(key);
    }

    public float[] getFloatArray(String key) {
        return (float[]) mMap.get(key);
    }

    public double[] getDoubleArray(String key) {
        return (double[]) mMap.get(key);
    }

    public String[] getStringArray(String key) {
        return (String[]) mMap.get(key);
    }

    public CharSequence[] getCharSequenceArray(String key) {
        return (CharSequence[]) mMap.get(key);
    }

    public IBinder getBinder(String key) {
        return (IBinder) mMap.get(key);
    }

    public IBinder getIBinder(String key) {
        return (IBinder) mMap.get(key);
    }

    public static final Parcelable.Creator<Bundle> CREATOR =
        new Parcelable.Creator<Bundle>() {
        public Bundle createFromParcel(Parcel in) {
            return in.readBundle();
        }

        public Bundle[] newArray(int size) {
            return new Bundle[size];
        }
    };

    public int describeContents() {
        if (hasFileDescriptors()) {
            return 1;
        }
        return 0;
    }
    
    public void writeToParcel(Parcel parcel, int flags) {
        throw new UnsupportedOperationException("STUBS!");
    }

    public void readFromParcel(Parcel parcel) {
        throw new UnsupportedOperationException("STUBS!");
    }

    void readFromParcelInner(Parcel parcel, int length) {
        throw new UnsupportedOperationException("STUBS!");
    }

    @Override
    public synchronized String toString() {
        return  mMap.toString();
    }
}
