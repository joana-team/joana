package android;

import android.view.View;
import java.util.Queue;
import android.os.Bundle;
import android.app.FragmentManager;
import android.os.MessageQueue;

import android.hardware.input.InputManager;
import android.view.LayoutInflater;
/**
 *  Static fields for use with the StubsBuilder.
 *
 *  These fields are used with "replace_fields" and "singleton" from the subsBuilder.ini
 *  It is _not_ sufficient to mention fields here, the subsBuilder.ini has to be adapted
 *  too for the replacement to take place.
 */
public class StubFields {
    public static MessageQueue mQueue = (MessageQueue) new Object();
    public static String mWho = "";
    public static View mView = (View) new Object();
    public static Bundle mSavedFragmentState = (Bundle) new Object();
    public static boolean mRetaining = false;
    public static boolean mInLayout = false;
    public static FragmentManager mFragmentManager = (FragmentManager) new Object();
    public static String mTag = "";
    public static int mContainerId = 42;
    public static int mFragmentId = 23;
    public static boolean mFromLayout = false;

    public static InputManager singleton_InputManager_getInstance = (InputManager) new Object();
    public static LayoutInflater singleton_mWindow_getLayoutInflater = (LayoutInflater) new Object();
}
