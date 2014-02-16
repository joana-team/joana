/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.app;

import com.android.internal.app.ActionBarImpl;
import com.android.internal.policy.PolicyManager;

import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.UserHandle;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

public class Activity extends ContextThemeWrapper
        implements LayoutInflater.Factory2,
        Window.Callback, KeyEvent.Callback,
        OnCreateContextMenuListener, ComponentCallbacks2 {
    public static final int RESULT_CANCELED    = 0;
    public static final int RESULT_OK           = -1;
    public static final int RESULT_FIRST_USER   = 1;

    private Instrumentation mInstrumentation;
    private IBinder mToken;
    private int mIdent;
    /*package*/ String mEmbeddedID;
    private Application mApplication;
    /*package*/ Intent mIntent;
    private ComponentName mComponent;
    /*package*/ ActivityInfo mActivityInfo;
    /*package*/ ActivityThread mMainThread;
    Activity mParent;
    boolean mCalled;
    boolean mCheckedForLoaderManager;
    boolean mLoadersStarted;
    /*package*/ boolean mResumed;
    private boolean mStopped;
    boolean mFinished;
    boolean mStartedActivity;
    private boolean mDestroyed;
    /*package*/ boolean mTemporaryPause = false;
    /*package*/ boolean mChangingConfigurations = false;
    /*package*/ int mConfigChangeFlags;
    /*package*/ Configuration mCurrentConfig;
    private SearchManager mSearchManager;
    private MenuInflater mMenuInflater;

    static final class NonConfigurationInstances {
        Object activity;
        HashMap<String, Object> children;
        ArrayList<Fragment> fragments;
        HashMap<String, LoaderManagerImpl> loaders;
    }
    /* package */ NonConfigurationInstances mLastNonConfigurationInstances;
    
    private Window mWindow;

    private WindowManager mWindowManager;
    /*package*/ View mDecor = null;
    /*package*/ boolean mWindowAdded = false;
    /*package*/ boolean mVisibleFromServer = false;
    /*package*/ boolean mVisibleFromClient = true;
    /*package*/ ActionBarImpl mActionBar = null;
    private boolean mEnableDefaultActionBarUp;

    private CharSequence mTitle;
    private int mTitleColor = 0;

    final FragmentManagerImpl mFragments = new FragmentManagerImpl();
    final FragmentContainer mContainer = new FragmentContainer() {
        @Override
        public View findViewById(int id) {
            return Activity.this.findViewById(id);
        }
    };
    
    HashMap<String, LoaderManagerImpl> mAllLoaderManagers;
    LoaderManagerImpl mLoaderManager;
    
    private static final class ManagedCursor {
        ManagedCursor(Cursor cursor) {
            mCursor = cursor;
            mReleased = false;
            mUpdated = false;
        }

        private final Cursor mCursor;
        private boolean mReleased;
        private boolean mUpdated;
    }
    private final ArrayList<ManagedCursor> mManagedCursors =
        new ArrayList<ManagedCursor>();

    int mResultCode = RESULT_CANCELED;
    Intent mResultData = null;

    protected static final int[] FOCUSED_STATE_SET = {com.android.internal.R.attr.state_focused};

    public Activity() {
        this.mLoaderManager = new LoaderManagerImpl("STUB", this, true);
        this.mIntent = new Intent();
    }


    private Thread mUiThread;
    final Handler mHandler = new Handler();

    public Intent getIntent() {
        return mIntent;
    }

    public void setIntent(Intent newIntent) {
        mIntent = newIntent;
    }

    public final Application getApplication() {
        return mApplication;
    }

    public final boolean isChild() {
        return mParent != null;
    }
    
    public final Activity getParent() {
        return mParent;
    }

    public WindowManager getWindowManager() {
        return mWindowManager;
    }

    public Window getWindow() {
        return mWindow;
    }

    public LoaderManager getLoaderManager() {
        return mLoaderManager;
    }
    
    LoaderManagerImpl getLoaderManager(String who, boolean started, boolean create) {
        return (LoaderManagerImpl) mLoaderManager;
    }
    
    public View getCurrentFocus() {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onCreate(Bundle savedInstanceState) {
        throw new UnsupportedOperationException("STUB!");
    }

    final void performRestoreInstanceState(Bundle savedInstanceState) {
        onRestoreInstanceState(savedInstanceState);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    private void restoreManagedDialogs(Bundle savedInstanceState) {
        throw new UnsupportedOperationException("STUB!");
    }

    private Dialog createDialog(Integer dialogId, Bundle state, Bundle args) {
        final Dialog dialog = onCreateDialog(dialogId, args);
        //dialog.dispatchOnCreate(state);
        return dialog;
    }

    private static String savedDialogKeyFor(int key) {
        throw new UnsupportedOperationException("STUB!");
    }

    private static String savedDialogArgsKeyFor(int key) {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onStart() {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onRestart() {
    }

    protected void onResume() {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onPostResume() {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onNewIntent(Intent intent) {
    }

    final void performSaveInstanceState(Bundle outState) {
        onSaveInstanceState(outState);
        saveManagedDialogs(outState);
    }

    protected void onSaveInstanceState(Bundle outState) {
        throw new UnsupportedOperationException("STUB!");
    }

    private void saveManagedDialogs(Bundle outState) {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onPause() {
    }

    protected void onUserLeaveHint() {
    }
    
    public boolean onCreateThumbnail(Bitmap outBitmap, Canvas canvas) {
        return false;
    }

    public CharSequence onCreateDescription() {
        return null;
    }

    public void onProvideAssistData(Bundle data) {
    }

    protected void onStop() {
    }

    protected void onDestroy() {
        throw new UnsupportedOperationException("STUB!");
    }

    public void onConfigurationChanged(Configuration newConfig) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public int getChangingConfigurations() {
        return 0;
    }
    
    public Object getLastNonConfigurationInstance() {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public Object onRetainNonConfigurationInstance() {
        return null;
    }
    
    HashMap<String, Object> getLastNonConfigurationChildInstances() {
        throw new UnsupportedOperationException("STUB!");
    }
    
    HashMap<String,Object> onRetainNonConfigurationChildInstances() {
        return null;
    }
    
    NonConfigurationInstances retainNonConfigurationInstances() {
        Object activity = onRetainNonConfigurationInstance();
        HashMap<String, Object> children = onRetainNonConfigurationChildInstances();
        ArrayList<Fragment> fragments = mFragments.retainNonConfig();
        
        NonConfigurationInstances nci = new NonConfigurationInstances();
        nci.activity = activity;
        nci.children = children;
        nci.fragments = fragments;
        return nci;
    }

    public void onLowMemory() {
        throw new UnsupportedOperationException("STUB!");
    }

    public void onTrimMemory(int level) {
        throw new UnsupportedOperationException("STUB!");
    }

    public FragmentManager getFragmentManager() {
        return mFragments;
    }

    void invalidateFragment(String who) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public void onAttachFragment(Fragment fragment) {
    }
    
    public final Cursor managedQuery(Uri uri, String[] projection, String selection,
            String sortOrder) {
        Cursor c = getContentResolver().query(uri, projection, selection, null, sortOrder);
        return c;
    }

    public final Cursor managedQuery(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        Cursor c = getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
        return c;
    }

    public void startManagingCursor(Cursor c) {
    }

    public void stopManagingCursor(Cursor c) {
    }

    public void setPersistent(boolean isPersistent) {
    }

    public View findViewById(int id) {                          // TODO
        throw new UnsupportedOperationException("STUB!");
    }
    
    public ActionBar getActionBar() {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public void setContentView(int layoutResID) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void setContentView(View view) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void setContentView(View view, ViewGroup.LayoutParams params) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void addContentView(View view, ViewGroup.LayoutParams params) {
        getWindow().addContentView(view, params);
    }

    public void setFinishOnTouchOutside(boolean finish) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    static public final int DEFAULT_KEYS_DISABLE = 0;
    static public final int DEFAULT_KEYS_DIALER = 1;
    static public final int DEFAULT_KEYS_SHORTCUT = 2;
    static public final int DEFAULT_KEYS_SEARCH_LOCAL = 3;
    static public final int DEFAULT_KEYS_SEARCH_GLOBAL = 4;

    public final void setDefaultKeyMode(int mode) {
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        onBackPressed();
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return false;
    }
    
    public void onBackPressed() {
        finish();
    }

    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
    
    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }

    public void onUserInteraction() {
    }
    
    public void onWindowAttributesChanged(WindowManager.LayoutParams params) {
    }

    public void onContentChanged() {
    }

    public void onWindowFocusChanged(boolean hasFocus) {
    }
    
    public void onAttachedToWindow() {
    }
    
    public void onDetachedFromWindow() {
    }
    
    public boolean hasWindowFocus() {
        return false;
    }
    
    public boolean dispatchKeyEvent(KeyEvent event) {
        onUserInteraction();
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        onUserInteraction();
        return onKeyShortcut(event.getKeyCode(), event);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        return onTouchEvent(ev);
    }
    
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        onUserInteraction();
        return onTrackballEvent(ev);
    }

    public boolean dispatchGenericMotionEvent(MotionEvent ev) {
        onUserInteraction();
        return onGenericMotionEvent(ev);
    }

    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        throw new UnsupportedOperationException("STUB!");
    }

    public View onCreatePanelView(int featureId) {
        return null;
    }

    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null) {
            boolean goforit = onPrepareOptionsMenu(menu);
            goforit |= mFragments.dispatchPrepareOptionsMenu(menu);
            return goforit;
        }
        return true;
    }

    public boolean onMenuOpened(int featureId, Menu menu) {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        CharSequence titleCondensed = item.getTitleCondensed();
        throw new UnsupportedOperationException("STUB!");
    }
    
    public void onPanelClosed(int featureId, Menu menu) {
        throw new UnsupportedOperationException("STUB!");                
    }

    public void invalidateOptionsMenu() {
    }
    
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    public boolean onNavigateUp() {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean onNavigateUpFromChild(Activity child) {
        return onNavigateUp();
    }

    public void onCreateNavigateUpTaskStack(TaskStackBuilder builder) {
        builder.addParentStack(this);
    }

    public void onPrepareNavigateUpTaskStack(TaskStackBuilder builder) {
    }

    public void onOptionsMenuClosed(Menu menu) {
        mParent.onOptionsMenuClosed(menu);
    }
    
    public void openOptionsMenu() {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public void closeOptionsMenu() {
        throw new UnsupportedOperationException("STUB!");
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    }

    public void registerForContextMenu(View view) {
        view.setOnCreateContextMenuListener(this);
    }
    
    public void unregisterForContextMenu(View view) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public void openContextMenu(View view) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public void closeContextMenu() {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public boolean onContextItemSelected(MenuItem item) {
        return mParent.onContextItemSelected(item);
    }

    public void onContextMenuClosed(Menu menu) {
        mParent.onContextMenuClosed(menu);
    }

    @Deprecated
    protected Dialog onCreateDialog(int id) {
        return null;
    }

    protected Dialog onCreateDialog(int id, Bundle args) {
        return onCreateDialog(id);
    }

    protected void onPrepareDialog(int id, Dialog dialog) {
        throw new UnsupportedOperationException("STUB!");
    }

    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void showDialog(int id) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final boolean showDialog(int id, Bundle args) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void dismissDialog(int id) {
        throw new UnsupportedOperationException("STUB!");
    }

    private IllegalArgumentException missingDialog(int id) {
        return new IllegalArgumentException("no dialog with id " + id + " was ever "
                + "shown via Activity#showDialog");
    }

    public final void removeDialog(int id) {
    }

    public boolean onSearchRequested() {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public void startSearch(String initialQuery, boolean selectInitialQuery, 
            Bundle appSearchData, boolean globalSearch) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void triggerSearch(String query, Bundle appSearchData) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void takeKeyEvents(boolean get) {
    }

    public final boolean requestWindowFeature(int featureId) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void setFeatureDrawableResource(int featureId, int resId) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void setFeatureDrawableUri(int featureId, Uri uri) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void setFeatureDrawable(int featureId, Drawable drawable) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void setFeatureDrawableAlpha(int featureId, int alpha) {
        throw new UnsupportedOperationException("STUB!");
    }

    public LayoutInflater getLayoutInflater() {
        return null;
    }

    public MenuInflater getMenuInflater() {
        return null;
    }

    //@Override
    //protected void onApplyThemeResource(Resources.Theme theme, int resid,
    //        boolean first) {
    //    throw new UnsupportedOperationException("STUB!");
    //}

    public void startActivityForResult(Intent intent, int requestCode) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivityAsUser(Intent intent, UserHandle user) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivityAsUser(Intent intent, Bundle options, UserHandle user) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startIntentSenderForResult(IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startIntentSenderForResult(IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            Bundle options) throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("STUB!");
    }

    private void startIntentSenderForResultInner(IntentSender intent, int requestCode,
            Intent fillInIntent, int flagsMask, int flagsValues, Activity activity,
            Bundle options)
            throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivity(Intent intent) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivity(Intent intent, Bundle options) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivities(Intent[] intents) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivities(Intent[] intents, Bundle options) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startIntentSender(IntentSender intent,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags)
            throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startIntentSender(IntentSender intent,
            Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
            Bundle options) throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean startActivityIfNeeded(Intent intent, int requestCode) {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean startActivityIfNeeded(Intent intent, int requestCode, Bundle options) {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean startNextMatchingActivity(Intent intent) {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean startNextMatchingActivity(Intent intent, Bundle options) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivityFromChild(Activity child, Intent intent,
            int requestCode) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivityFromChild(Activity child, Intent intent, 
            int requestCode, Bundle options) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startActivityFromFragment(Fragment fragment, Intent intent, 
            int requestCode) {
        throw new UnsupportedOperationException("STUB!"); 
    }

    public void startActivityFromFragment(Fragment fragment, Intent intent, 
            int requestCode, Bundle options) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startIntentSenderFromChild(Activity child, IntentSender intent,
            int requestCode, Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags)
            throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("STUB!");
    }

    public void startIntentSenderFromChild(Activity child, IntentSender intent,
            int requestCode, Intent fillInIntent, int flagsMask, int flagsValues,
            int extraFlags, Bundle options)
            throws IntentSender.SendIntentException {
        throw new UnsupportedOperationException("STUB!");
    }

    public void overridePendingTransition(int enterAnim, int exitAnim) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public final void setResult(int resultCode) {
        mResultCode = resultCode;
        mResultData = null;
    }

    public final void setResult(int resultCode, Intent data) {
        mResultCode = resultCode;
        mResultData = data;
        System.out.println(data.getExtras().toString());
    }

    public String getCallingPackage() {
        throw new UnsupportedOperationException("STUB!");
    }

    public ComponentName getCallingActivity() {             // STUBS TODO
        throw new UnsupportedOperationException("STUB!");
    }

    public void setVisible(boolean visible) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    void makeVisible() {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public boolean isFinishing() {
        return false;
    }

    public boolean isDestroyed() {
        return false;
    }

    public boolean isChangingConfigurations() {
        return false;
    }

    public void recreate() {
        throw new UnsupportedOperationException("STUB!");
    }

    public void finish() {
        throw new UnsupportedOperationException("STUB!");
    }

    public void finishAffinity() {
        throw new UnsupportedOperationException("STUB!");
    }

    public void finishFromChild(Activity child) {
        finish();
    }

    public void finishActivity(int requestCode) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void finishActivityFromChild(Activity child, int requestCode) {
        throw new UnsupportedOperationException("STUB!"); 
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public PendingIntent createPendingResult(int requestCode, Intent data,
            int flags) {
        throw new UnsupportedOperationException("STUB!");
    }

    int mOri = 0;
    public void setRequestedOrientation(int requestedOrientation) {
        mOri = requestedOrientation;
    }
    
    public int getRequestedOrientation() {
        return mOri;
    }
    
    public int getTaskId() {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean isTaskRoot() {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean moveTaskToBack(boolean nonRoot) {
        throw new UnsupportedOperationException("STUB!");
    }

    public String getLocalClassName() {
        return mComponent.getClassName();
    }
    
    public ComponentName getComponentName()
    {
        return mComponent;
    }

    public SharedPreferences getPreferences(int mode) {             
        return getSharedPreferences(getLocalClassName(), mode);
    }
    
    private void ensureSearchManager() {
    }
    
    @Override
    public Object getSystemService(String name) {
        throw new UnsupportedOperationException("STUB!"); 
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        onTitleChanged(title, mTitleColor);
        mParent.onChildTitleChanged(this, title);
    }

    public void setTitle(int titleId) {
        setTitle("");
    }

    public void setTitleColor(int textColor) {
        onTitleChanged(mTitle, textColor);
    }

    public final CharSequence getTitle() {
        throw new UnsupportedOperationException("STUB!");
    }

    public final int getTitleColor() {
        return 0;
    }

    protected void onTitleChanged(CharSequence title, int color) {
    }

    protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
    }

    public final void setProgressBarVisibility(boolean visible) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void setProgressBarIndeterminateVisibility(boolean visible) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public final void setProgressBarIndeterminate(boolean indeterminate) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public final void setProgress(int progress) {
        throw new UnsupportedOperationException("STUB!");
    }
    
    public final void setSecondaryProgress(int secondaryProgress) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final void setVolumeControlStream(int streamType) {
        throw new UnsupportedOperationException("STUB!");
    }

    public final int getVolumeControlStream() {
        return 0;
    }
    
    public final void runOnUiThread(Runnable action) {
        action.run();
    }

    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return null;
    }

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        Fragment fragment = Fragment.instantiate(this, name);
        //fragment.mFragmentManager = mFragments;
        //fragment.onInflate(this, attrs, fragment.mSavedFragmentState);
        mFragments.addFragment(fragment, true);
        return null;
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        throw new UnsupportedOperationException("STUB!");
    }

    public boolean isImmersive() {
        throw new UnsupportedOperationException("STUB!");
    }

    public void setImmersive(boolean i) {
        throw new UnsupportedOperationException("STUB!");
    }

    public ActionMode startActionMode(ActionMode.Callback callback) {
        throw new UnsupportedOperationException("STUB!");
    }

    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        throw new UnsupportedOperationException("STUB!");
    }

    public void onActionModeStarted(ActionMode mode) {
    }

    public void onActionModeFinished(ActionMode mode) {
    }

    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return true;
    }

    public boolean navigateUpTo(Intent upIntent) {
        ComponentName destInfo = upIntent.getComponent();
        upIntent = new Intent(upIntent);
        upIntent.setComponent(destInfo);
        mResultData.prepareToLeaveProcess();
        return true;
    }

    public boolean navigateUpToFromChild(Activity child, Intent upIntent) {
        return navigateUpTo(upIntent);
    }

    public Intent getParentActivityIntent() {
        throw new UnsupportedOperationException("STUB!");
    }

    // ------------------ Internal API ------------------
    
    final void setParent(Activity parent) {
        mParent = parent;
    }

    final void attach(Context context, ActivityThread aThread, Instrumentation instr, IBinder token,
            Application application, Intent intent, ActivityInfo info, CharSequence title, 
            Activity parent, String id, NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config) {
        attach(context, aThread, instr, token, 0, application, intent, info, title, parent, id,
            lastNonConfigurationInstances, config);
    }
    
    final void attach(Context context, ActivityThread aThread,
            Instrumentation instr, IBinder token, int ident,
            Application application, Intent intent, ActivityInfo info,
            CharSequence title, Activity parent, String id,
            NonConfigurationInstances lastNonConfigurationInstances,
            Configuration config) {
        attachBaseContext(context);
        mFragments.attachActivity(this, mContainer, null);
        
        /*mWindow = PolicyManager.makeNewWindow(this);
        mWindow.setCallback(this);
        mWindow.getLayoutInflater().setPrivateFactory(this);
        mWindow.setSoftInputMode(info.softInputMode);
        mWindow.setUiOptions(info.uiOptions);
        mUiThread = Thread.currentThread();
        mMainThread = aThread;
        mInstrumentation = instr;*/
        mToken = token;
        //mIdent = ident;
        mApplication = application;
        mIntent = intent;
        mComponent = intent.getComponent();
        //mActivityInfo = info;
        mTitle = title;
        mParent = parent;
        //mEmbeddedID = id;
        mLastNonConfigurationInstances = lastNonConfigurationInstances;

        //mWindow.setWindowManager(
        //        (WindowManager)context.getSystemService(Context.WINDOW_SERVICE),
        //        mToken, mComponent.flattenToString(),
        //        (info.flags & ActivityInfo.FLAG_HARDWARE_ACCELERATED) != 0);
        // mWindow.setContainer(mParent.getWindow());
        //mWindowManager = mWindow.getWindowManager();
        //mCurrentConfig = config;
    }

    /** @hide */
    public final IBinder getActivityToken() {
        return mParent != null ? mParent.getActivityToken() : mToken;
    }

    final void performCreate(Bundle icicle) {
        onCreate(icicle);
        mFragments.dispatchActivityCreated();
    }
    
    final void performStart() {
        mFragments.noteStateNotSaved();
        mFragments.execPendingActions();
        mInstrumentation.callActivityOnStart(this);
        mFragments.dispatchStart();
        
            /* LoaderManagerImpl loaders[] = new LoaderManagerImpl[mAllLoaderManagers.size()];
            mAllLoaderManagers.values().toArray(loaders);
            if (loaders != null) {
                for (int i=0; i<loaders.length; i++) {
                    LoaderManagerImpl lm = loaders[i];
                    lm.finishRetain();
                    lm.doReportStart();
                }
            } // */
    }
    
    final void performRestart() {
        mFragments.noteStateNotSaved();
        // WindowManagerGlobal.getInstance().setStoppedState(mToken, false);
        mInstrumentation.callActivityOnRestart(this);
        performStart();
    }
    
    final void performResume() {
        performRestart();
        
        mFragments.execPendingActions();
        mInstrumentation.callActivityOnResume(this);

        mFragments.dispatchResume();
        mFragments.execPendingActions();
        
        onPostResume();
    }

    final void performPause() {
        mFragments.dispatchPause();
        onPause();
    }
    
    final void performUserLeaving() {
        onUserInteraction();
        onUserLeaveHint();
    }
    
    final void performStop() {
        mLoaderManager.doStop();
        mLoaderManager.doRetain();
        // mWindow.closeAllPanels();
        // WindowManagerGlobal.getInstance().setStoppedState(mToken, true);
        mFragments.dispatchStop();
        mInstrumentation.callActivityOnStop(this);
    
        final int N = mManagedCursors.size();
        for (int i=0; i<N; i++) {
            ManagedCursor mc = mManagedCursors.get(i);
            if (!mc.mReleased) {
                mc.mCursor.deactivate();
            }
        }
    }

    final void performDestroy() {
        mDestroyed = true;
        mFragments.dispatchDestroy();
        onDestroy();
        mLoaderManager.doDestroy();
    }
    
    public final boolean isResumed() {
        return mResumed;
    }

    void dispatchActivityResult(String who, int requestCode, 
        int resultCode, Intent data) {
        mFragments.noteStateNotSaved();
        
        onActivityResult(requestCode, resultCode, data);
        
        Fragment frag = mFragments.findFragmentByWho(who);
        frag.onActivityResult(requestCode, resultCode, data);
    }
}
