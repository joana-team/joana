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

package android.content;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.util.XmlUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class Intent implements Parcelable, Cloneable {
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_DEFAULT = ACTION_VIEW;
    public static final String ACTION_ATTACH_DATA = "android.intent.action.ATTACH_DATA";
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public static final String ACTION_INSERT_OR_EDIT = "android.intent.action.INSERT_OR_EDIT";
    public static final String ACTION_PICK = "android.intent.action.PICK";
    public static final String ACTION_CREATE_SHORTCUT = "android.intent.action.CREATE_SHORTCUT";
    public static final String EXTRA_SHORTCUT_INTENT = "android.intent.extra.shortcut.INTENT";
    public static final String EXTRA_SHORTCUT_NAME = "android.intent.extra.shortcut.NAME";
    public static final String EXTRA_SHORTCUT_ICON = "android.intent.extra.shortcut.ICON";
    public static final String EXTRA_SHORTCUT_ICON_RESOURCE = "android.intent.extra.shortcut.ICON_RESOURCE";

    public static class ShortcutIconResource implements Parcelable {
        public String packageName;
        public String resourceName;
        
        public static ShortcutIconResource fromContext(Context context, int resourceId) {
            ShortcutIconResource icon = new ShortcutIconResource();
            icon.packageName = context.getPackageName();
            icon.resourceName = context.getResources().getResourceName(resourceId);
            return icon;
        }

        public static final Parcelable.Creator<ShortcutIconResource> CREATOR =
            new Parcelable.Creator<ShortcutIconResource>() {

                public ShortcutIconResource createFromParcel(Parcel source) {
                    ShortcutIconResource icon = new ShortcutIconResource();
                    icon.packageName = source.readString();
                    icon.resourceName = source.readString();
                    return icon;
                }

                public ShortcutIconResource[] newArray(int size) {
                    return new ShortcutIconResource[size];
                }
            };

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(packageName);
            dest.writeString(resourceName);
        }

        @Override
        public String toString() {
            return resourceName;
        }
    }

    public static final String ACTION_CHOOSER = "android.intent.action.CHOOSER";

    public static Intent createChooser(Intent target, CharSequence title) {
        Intent intent = new Intent(ACTION_CHOOSER);
        intent.putExtra(EXTRA_INTENT, target);
        if (title != null) {
            intent.putExtra(EXTRA_TITLE, title);
        }

        // Migrate any clip data and flags from target.
        int permFlags = target.getFlags()
                & (FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION);
        if (permFlags != 0) {
            ClipData targetClipData = target.getClipData();
            if (targetClipData == null && target.getData() != null) {
                ClipData.Item item = new ClipData.Item(target.getData());
                String[] mimeTypes;
                if (target.getType() != null) {
                    mimeTypes = new String[] { target.getType() };
                } else {
                    mimeTypes = new String[] { };
                }
                targetClipData = new ClipData(null, mimeTypes, item);
            }
            if (targetClipData != null) {
                intent.setClipData(targetClipData);
                intent.addFlags(permFlags);
            }
        }

        return intent;
    }

    
    public static final String ACTION_GET_CONTENT = "android.intent.action.GET_CONTENT";
    public static final String ACTION_DIAL = "android.intent.action.DIAL";
    public static final String ACTION_CALL = "android.intent.action.CALL";
    public static final String ACTION_CALL_EMERGENCY = "android.intent.action.CALL_EMERGENCY";
    public static final String ACTION_CALL_PRIVILEGED = "android.intent.action.CALL_PRIVILEGED";
    public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";
    public static final String ACTION_ANSWER = "android.intent.action.ANSWER";
    public static final String ACTION_INSERT = "android.intent.action.INSERT";
    public static final String ACTION_PASTE = "android.intent.action.PASTE";
    public static final String ACTION_DELETE = "android.intent.action.DELETE";
    public static final String ACTION_RUN = "android.intent.action.RUN";
    public static final String ACTION_SYNC = "android.intent.action.SYNC";
    public static final String ACTION_PICK_ACTIVITY = "android.intent.action.PICK_ACTIVITY";
    public static final String ACTION_SEARCH = "android.intent.action.SEARCH";
    public static final String ACTION_SYSTEM_TUTORIAL = "android.intent.action.SYSTEM_TUTORIAL";
    public static final String ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH";
    public static final String ACTION_ASSIST = "android.intent.action.ASSIST";
    public static final String ACTION_VOICE_ASSIST = "android.intent.action.VOICE_ASSIST";
    public static final String EXTRA_ASSIST_PACKAGE = "android.intent.extra.ASSIST_PACKAGE";
    public static final String EXTRA_ASSIST_CONTEXT = "android.intent.extra.ASSIST_CONTEXT";
    public static final String ACTION_ALL_APPS = "android.intent.action.ALL_APPS";
    public static final String ACTION_SET_WALLPAPER = "android.intent.action.SET_WALLPAPER";
    public static final String ACTION_BUG_REPORT = "android.intent.action.BUG_REPORT";
    public static final String ACTION_FACTORY_TEST = "android.intent.action.FACTORY_TEST";
    public static final String ACTION_CALL_BUTTON = "android.intent.action.CALL_BUTTON";
    public static final String ACTION_VOICE_COMMAND = "android.intent.action.VOICE_COMMAND";
    public static final String ACTION_SEARCH_LONG_PRESS = "android.intent.action.SEARCH_LONG_PRESS";
    public static final String ACTION_APP_ERROR = "android.intent.action.APP_ERROR";
    public static final String ACTION_POWER_USAGE_SUMMARY = "android.intent.action.POWER_USAGE_SUMMARY";
    public static final String ACTION_UPGRADE_SETUP = "android.intent.action.UPGRADE_SETUP";
    public static final String ACTION_MANAGE_NETWORK_USAGE = "android.intent.action.MANAGE_NETWORK_USAGE";
    public static final String ACTION_INSTALL_PACKAGE = "android.intent.action.INSTALL_PACKAGE";
    public static final String EXTRA_INSTALLER_PACKAGE_NAME = "android.intent.extra.INSTALLER_PACKAGE_NAME";
    public static final String EXTRA_NOT_UNKNOWN_SOURCE = "android.intent.extra.NOT_UNKNOWN_SOURCE";
    public static final String EXTRA_ORIGINATING_URI = "android.intent.extra.ORIGINATING_URI";
    public static final String EXTRA_REFERRER = "android.intent.extra.REFERRER";
    public static final String EXTRA_ORIGINATING_UID = "android.intent.extra.ORIGINATING_UID";
    public static final String EXTRA_ALLOW_REPLACE = "android.intent.extra.ALLOW_REPLACE";
    public static final String EXTRA_RETURN_RESULT = "android.intent.extra.RETURN_RESULT";
    public static final String EXTRA_INSTALL_RESULT = "android.intent.extra.INSTALL_RESULT";
    public static final String ACTION_UNINSTALL_PACKAGE = "android.intent.action.UNINSTALL_PACKAGE";
    public static final String EXTRA_UNINSTALL_ALL_USERS = "android.intent.extra.UNINSTALL_ALL_USERS";
    public static final String METADATA_SETUP_VERSION = "android.SETUP_VERSION";
    public static final String ACTION_SCREEN_OFF = "android.intent.action.SCREEN_OFF";
    public static final String ACTION_SCREEN_ON = "android.intent.action.SCREEN_ON";
    public static final String ACTION_DREAMING_STOPPED = "android.intent.action.DREAMING_STOPPED";
    public static final String ACTION_DREAMING_STARTED = "android.intent.action.DREAMING_STARTED";
    public static final String ACTION_USER_PRESENT = "android.intent.action.USER_PRESENT";
    public static final String ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    public static final String ACTION_TIME_CHANGED = "android.intent.action.TIME_SET";
    public static final String ACTION_DATE_CHANGED = "android.intent.action.DATE_CHANGED";
    public static final String ACTION_TIMEZONE_CHANGED = "android.intent.action.TIMEZONE_CHANGED";
    public static final String ACTION_CLEAR_DNS_CACHE = "android.intent.action.CLEAR_DNS_CACHE";
    public static final String ACTION_ALARM_CHANGED = "android.intent.action.ALARM_CHANGED";
    public static final String ACTION_SYNC_STATE_CHANGED = "android.intent.action.SYNC_STATE_CHANGED";
    public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String ACTION_CLOSE_SYSTEM_DIALOGS = "android.intent.action.CLOSE_SYSTEM_DIALOGS";
    public static final String ACTION_PACKAGE_INSTALL = "android.intent.action.PACKAGE_INSTALL";
    public static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    public static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";
    public static final String ACTION_MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED";
    public static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    public static final String ACTION_PACKAGE_FULLY_REMOVED = "android.intent.action.PACKAGE_FULLY_REMOVED";
    public static final String ACTION_PACKAGE_CHANGED = "android.intent.action.PACKAGE_CHANGED";
    public static final String ACTION_QUERY_PACKAGE_RESTART = "android.intent.action.QUERY_PACKAGE_RESTART";
    public static final String ACTION_PACKAGE_RESTARTED = "android.intent.action.PACKAGE_RESTARTED";
    public static final String ACTION_PACKAGE_DATA_CLEARED = "android.intent.action.PACKAGE_DATA_CLEARED";
    public static final String ACTION_UID_REMOVED = "android.intent.action.UID_REMOVED";
    public static final String ACTION_PACKAGE_FIRST_LAUNCH = "android.intent.action.PACKAGE_FIRST_LAUNCH";
    public static final String ACTION_PACKAGE_NEEDS_VERIFICATION = "android.intent.action.PACKAGE_NEEDS_VERIFICATION";
    public static final String ACTION_PACKAGE_VERIFIED = "android.intent.action.PACKAGE_VERIFIED";
    public static final String ACTION_EXTERNAL_APPLICATIONS_AVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_AVAILABLE";
    public static final String ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE = "android.intent.action.EXTERNAL_APPLICATIONS_UNAVAILABLE";
    public static final String ACTION_WALLPAPER_CHANGED = "android.intent.action.WALLPAPER_CHANGED";
    public static final String ACTION_CONFIGURATION_CHANGED = "android.intent.action.CONFIGURATION_CHANGED";
    public static final String ACTION_LOCALE_CHANGED = "android.intent.action.LOCALE_CHANGED";
    public static final String ACTION_BATTERY_CHANGED = "android.intent.action.BATTERY_CHANGED";
    public static final String ACTION_BATTERY_LOW = "android.intent.action.BATTERY_LOW";
    public static final String ACTION_BATTERY_OKAY = "android.intent.action.BATTERY_OKAY";
    public static final String ACTION_POWER_CONNECTED = "android.intent.action.ACTION_POWER_CONNECTED";
    public static final String ACTION_POWER_DISCONNECTED = "android.intent.action.ACTION_POWER_DISCONNECTED";
    public static final String ACTION_SHUTDOWN = "android.intent.action.ACTION_SHUTDOWN";
    public static final String ACTION_REQUEST_SHUTDOWN = "android.intent.action.ACTION_REQUEST_SHUTDOWN";
    public static final String ACTION_DEVICE_STORAGE_LOW = "android.intent.action.DEVICE_STORAGE_LOW";
    public static final String ACTION_DEVICE_STORAGE_OK = "android.intent.action.DEVICE_STORAGE_OK";
    public static final String ACTION_DEVICE_STORAGE_FULL = "android.intent.action.DEVICE_STORAGE_FULL";
    public static final String ACTION_DEVICE_STORAGE_NOT_FULL = "android.intent.action.DEVICE_STORAGE_NOT_FULL";
    public static final String ACTION_MANAGE_PACKAGE_STORAGE = "android.intent.action.MANAGE_PACKAGE_STORAGE";
    public static final String ACTION_UMS_CONNECTED = "android.intent.action.UMS_CONNECTED";
    public static final String ACTION_UMS_DISCONNECTED = "android.intent.action.UMS_DISCONNECTED";
    public static final String ACTION_MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED";
    public static final String ACTION_MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED";
    public static final String ACTION_MEDIA_CHECKING = "android.intent.action.MEDIA_CHECKING";
    public static final String ACTION_MEDIA_NOFS = "android.intent.action.MEDIA_NOFS";
    public static final String ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
    public static final String ACTION_MEDIA_SHARED = "android.intent.action.MEDIA_SHARED";
    public static final String ACTION_MEDIA_UNSHARED = "android.intent.action.MEDIA_UNSHARED";
    public static final String ACTION_MEDIA_BAD_REMOVAL = "android.intent.action.MEDIA_BAD_REMOVAL";
    public static final String ACTION_MEDIA_UNMOUNTABLE = "android.intent.action.MEDIA_UNMOUNTABLE";
    public static final String ACTION_MEDIA_EJECT = "android.intent.action.MEDIA_EJECT";
    public static final String ACTION_MEDIA_SCANNER_STARTED = "android.intent.action.MEDIA_SCANNER_STARTED";
    public static final String ACTION_MEDIA_SCANNER_FINISHED = "android.intent.action.MEDIA_SCANNER_FINISHED";
    public static final String ACTION_MEDIA_SCANNER_SCAN_FILE = "android.intent.action.MEDIA_SCANNER_SCAN_FILE";
    public static final String ACTION_MEDIA_BUTTON = "android.intent.action.MEDIA_BUTTON";
    public static final String ACTION_CAMERA_BUTTON = "android.intent.action.CAMERA_BUTTON";
    public static final String ACTION_GTALK_SERVICE_CONNECTED = "android.intent.action.GTALK_CONNECTED";
    public static final String ACTION_GTALK_SERVICE_DISCONNECTED = "android.intent.action.GTALK_DISCONNECTED";
    public static final String ACTION_INPUT_METHOD_CHANGED = "android.intent.action.INPUT_METHOD_CHANGED";
    public static final String ACTION_AIRPLANE_MODE_CHANGED = "android.intent.action.AIRPLANE_MODE";
    public static final String ACTION_PROVIDER_CHANGED = "android.intent.action.PROVIDER_CHANGED";
    public static final String ACTION_HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    public static final String ACTION_ANALOG_AUDIO_DOCK_PLUG = "android.intent.action.ANALOG_AUDIO_DOCK_PLUG";
    public static final String ACTION_DIGITAL_AUDIO_DOCK_PLUG = "android.intent.action.DIGITAL_AUDIO_DOCK_PLUG";
    public static final String ACTION_HDMI_AUDIO_PLUG = "android.intent.action.HDMI_AUDIO_PLUG";
    public static final String ACTION_USB_AUDIO_ACCESSORY_PLUG = "android.intent.action.USB_AUDIO_ACCESSORY_PLUG";
    public static final String ACTION_USB_AUDIO_DEVICE_PLUG = "android.intent.action.USB_AUDIO_DEVICE_PLUG";
    public static final String ACTION_ADVANCED_SETTINGS_CHANGED = "android.intent.action.ADVANCED_SETTINGS";
    public static final String ACTION_NEW_OUTGOING_CALL = "android.intent.action.NEW_OUTGOING_CALL";
    public static final String ACTION_REBOOT = "android.intent.action.REBOOT";
    public static final String ACTION_DOCK_EVENT = "android.intent.action.DOCK_EVENT";
    public static final String ACTION_IDLE_MAINTENANCE_START = "android.intent.action.ACTION_IDLE_MAINTENANCE_START";
    public static final String ACTION_IDLE_MAINTENANCE_END = "android.intent.action.ACTION_IDLE_MAINTENANCE_END";
    public static final String ACTION_REMOTE_INTENT = "com.google.android.c2dm.intent.RECEIVE";
    public static final String ACTION_PRE_BOOT_COMPLETED = "android.intent.action.PRE_BOOT_COMPLETED";
    public static final String ACTION_GET_RESTRICTION_ENTRIES = "android.intent.action.GET_RESTRICTION_ENTRIES";
    public static final String ACTION_USER_INITIALIZE = "android.intent.action.USER_INITIALIZE";
    public static final String ACTION_USER_FOREGROUND = "android.intent.action.USER_FOREGROUND";
    public static final String ACTION_USER_BACKGROUND = "android.intent.action.USER_BACKGROUND";
    public static final String ACTION_USER_ADDED = "android.intent.action.USER_ADDED";
    public static final String ACTION_USER_STARTED = "android.intent.action.USER_STARTED";
    public static final String ACTION_USER_STARTING = "android.intent.action.USER_STARTING";
    public static final String ACTION_USER_STOPPING = "android.intent.action.USER_STOPPING";
    public static final String ACTION_USER_STOPPED = "android.intent.action.USER_STOPPED";
    public static final String ACTION_USER_REMOVED = "android.intent.action.USER_REMOVED";
    public static final String ACTION_USER_SWITCHED = "android.intent.action.USER_SWITCHED";
    public static final String ACTION_USER_INFO_CHANGED = "android.intent.action.USER_INFO_CHANGED";
    public static final String ACTION_QUICK_CLOCK = "android.intent.action.QUICK_CLOCK";
    public static final String ACTION_SHOW_BRIGHTNESS_DIALOG = "android.intent.action.SHOW_BRIGHTNESS_DIALOG";
    public static final String ACTION_GLOBAL_BUTTON = "android.intent.action.GLOBAL_BUTTON";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";
    public static final String CATEGORY_ALTERNATIVE = "android.intent.category.ALTERNATIVE";
    public static final String CATEGORY_SELECTED_ALTERNATIVE = "android.intent.category.SELECTED_ALTERNATIVE";
    public static final String CATEGORY_TAB = "android.intent.category.TAB";
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_INFO = "android.intent.category.INFO";
    public static final String CATEGORY_HOME = "android.intent.category.HOME";
    public static final String CATEGORY_PREFERENCE = "android.intent.category.PREFERENCE";
    public static final String CATEGORY_DEVELOPMENT_PREFERENCE = "android.intent.category.DEVELOPMENT_PREFERENCE";
    public static final String CATEGORY_EMBED = "android.intent.category.EMBED";
    public static final String CATEGORY_APP_MARKET = "android.intent.category.APP_MARKET";
    public static final String CATEGORY_MONKEY = "android.intent.category.MONKEY";
    public static final String CATEGORY_TEST = "android.intent.category.TEST";
    public static final String CATEGORY_UNIT_TEST = "android.intent.category.UNIT_TEST";
    public static final String CATEGORY_SAMPLE_CODE = "android.intent.category.SAMPLE_CODE";
    public static final String CATEGORY_OPENABLE = "android.intent.category.OPENABLE";
    public static final String CATEGORY_FRAMEWORK_INSTRUMENTATION_TEST = "android.intent.category.FRAMEWORK_INSTRUMENTATION_TEST";
    public static final String CATEGORY_CAR_DOCK = "android.intent.category.CAR_DOCK";
    public static final String CATEGORY_DESK_DOCK = "android.intent.category.DESK_DOCK";
    public static final String CATEGORY_LE_DESK_DOCK = "android.intent.category.LE_DESK_DOCK";
    public static final String CATEGORY_HE_DESK_DOCK = "android.intent.category.HE_DESK_DOCK";
    public static final String CATEGORY_CAR_MODE = "android.intent.category.CAR_MODE";
    public static final String CATEGORY_APP_BROWSER = "android.intent.category.APP_BROWSER";
    public static final String CATEGORY_APP_CALCULATOR = "android.intent.category.APP_CALCULATOR";
    public static final String CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR";
    public static final String CATEGORY_APP_CONTACTS = "android.intent.category.APP_CONTACTS";
    public static final String CATEGORY_APP_EMAIL = "android.intent.category.APP_EMAIL";
    public static final String CATEGORY_APP_GALLERY = "android.intent.category.APP_GALLERY";
    public static final String CATEGORY_APP_MAPS = "android.intent.category.APP_MAPS";
    public static final String CATEGORY_APP_MESSAGING = "android.intent.category.APP_MESSAGING";
    public static final String CATEGORY_APP_MUSIC = "android.intent.category.APP_MUSIC";
    public static final String EXTRA_TEMPLATE = "android.intent.extra.TEMPLATE";
    public static final String EXTRA_TEXT = "android.intent.extra.TEXT";
    public static final String EXTRA_HTML_TEXT = "android.intent.extra.HTML_TEXT";
    public static final String EXTRA_STREAM = "android.intent.extra.STREAM";
    public static final String EXTRA_EMAIL       = "android.intent.extra.EMAIL";
    public static final String EXTRA_CC       = "android.intent.extra.CC";
    public static final String EXTRA_BCC      = "android.intent.extra.BCC";
    public static final String EXTRA_SUBJECT  = "android.intent.extra.SUBJECT";
    public static final String EXTRA_INTENT = "android.intent.extra.INTENT";
    public static final String EXTRA_TITLE = "android.intent.extra.TITLE";
    public static final String EXTRA_INITIAL_INTENTS = "android.intent.extra.INITIAL_INTENTS";
    public static final String EXTRA_KEY_EVENT = "android.intent.extra.KEY_EVENT";
    public static final String EXTRA_KEY_CONFIRM = "android.intent.extra.KEY_CONFIRM";
    public static final String EXTRA_DONT_KILL_APP = "android.intent.extra.DONT_KILL_APP";
    public static final String EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER";
    public static final String EXTRA_UID = "android.intent.extra.UID";
    public static final String EXTRA_PACKAGES = "android.intent.extra.PACKAGES";
    public static final String EXTRA_DATA_REMOVED = "android.intent.extra.DATA_REMOVED";
    public static final String EXTRA_REMOVED_FOR_ALL_USERS = "android.intent.extra.REMOVED_FOR_ALL_USERS";
    public static final String EXTRA_REPLACING = "android.intent.extra.REPLACING";
    public static final String EXTRA_ALARM_COUNT = "android.intent.extra.ALARM_COUNT";
    public static final String EXTRA_DOCK_STATE = "android.intent.extra.DOCK_STATE";
    public static final int EXTRA_DOCK_STATE_UNDOCKED = 0;
    public static final int EXTRA_DOCK_STATE_DESK = 1;
    public static final int EXTRA_DOCK_STATE_CAR = 2;
    public static final int EXTRA_DOCK_STATE_LE_DESK = 3;
    public static final int EXTRA_DOCK_STATE_HE_DESK = 4;
    public static final String METADATA_DOCK_HOME = "android.dock_home";
    public static final String EXTRA_BUG_REPORT = "android.intent.extra.BUG_REPORT";
    public static final String EXTRA_REMOTE_INTENT_TOKEN = "android.intent.extra.remote_intent_token";
    public static final String EXTRA_CHANGED_COMPONENT_NAME = "android.intent.extra.changed_component_name";
    public static final String EXTRA_CHANGED_COMPONENT_NAME_LIST = "android.intent.extra.changed_component_name_list";
    public static final String EXTRA_CHANGED_PACKAGE_LIST = "android.intent.extra.changed_package_list";
    public static final String EXTRA_CHANGED_UID_LIST = "android.intent.extra.changed_uid_list";
    public static final String EXTRA_CLIENT_LABEL = "android.intent.extra.client_label";
    public static final String EXTRA_CLIENT_INTENT = "android.intent.extra.client_intent";
    public static final String EXTRA_LOCAL_ONLY = "android.intent.extra.LOCAL_ONLY";
    public static final String EXTRA_ALLOW_MULTIPLE = "android.intent.extra.ALLOW_MULTIPLE";
    public static final String EXTRA_USER_HANDLE = "android.intent.extra.user_handle";
    public static final String EXTRA_RESTRICTIONS_LIST = "android.intent.extra.restrictions_list";
    public static final String EXTRA_RESTRICTIONS_BUNDLE = "android.intent.extra.restrictions_bundle";
    public static final String EXTRA_RESTRICTIONS_INTENT = "android.intent.extra.restrictions_intent";
    public static final int FLAG_GRANT_READ_URI_PERMISSION = 0x00000001;
    public static final int FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002;
    public static final int FLAG_FROM_BACKGROUND = 0x00000004;
    public static final int FLAG_DEBUG_LOG_RESOLUTION = 0x00000008;
    public static final int FLAG_EXCLUDE_STOPPED_PACKAGES = 0x00000010;
    public static final int FLAG_INCLUDE_STOPPED_PACKAGES = 0x00000020;
    public static final int FLAG_ACTIVITY_NO_HISTORY = 0x40000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    public static final int FLAG_ACTIVITY_NEW_TASK = 0x10000000;
    public static final int FLAG_ACTIVITY_MULTIPLE_TASK = 0x08000000;
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_FORWARD_RESULT = 0x02000000;
    public static final int FLAG_ACTIVITY_PREVIOUS_IS_TOP = 0x01000000;
    public static final int FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS = 0x00800000;
    public static final int FLAG_ACTIVITY_BROUGHT_TO_FRONT = 0x00400000;
    public static final int FLAG_ACTIVITY_RESET_TASK_IF_NEEDED = 0x00200000;
    public static final int FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY = 0x00100000;
    public static final int FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET = 0x00080000;
    public static final int FLAG_ACTIVITY_NO_USER_ACTION = 0x00040000;
    public static final int FLAG_ACTIVITY_REORDER_TO_FRONT = 0X00020000;
    public static final int FLAG_ACTIVITY_NO_ANIMATION = 0X00010000;
    public static final int FLAG_ACTIVITY_CLEAR_TASK = 0X00008000;
    public static final int FLAG_ACTIVITY_TASK_ON_HOME = 0X00004000;
    public static final int FLAG_RECEIVER_REGISTERED_ONLY = 0x40000000;
    public static final int FLAG_RECEIVER_REPLACE_PENDING = 0x20000000;
    public static final int FLAG_RECEIVER_FOREGROUND = 0x10000000;
    public static final int FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT = 0x08000000;
    public static final int FLAG_RECEIVER_BOOT_UPGRADE = 0x04000000;
    public static final int IMMUTABLE_FLAGS = 1;
    public static final int URI_INTENT_SCHEME = 1<<0;


    private String mAction;
    private Uri mData;
    private String mType;
    private String mPackage;
    private ComponentName mComponent;
    private int mFlags;
    private HashSet<String> mCategories;
    private Bundle mExtras;
    //private Rect mSourceBounds;
    private Intent mSelector;
    //private ClipData mClipData;

    // ---------------------------------------------------------------------

    public Intent() {
        this.mAction = "NONE";
        this.mData = null;
        this.mType = "NONE";
        this.mPackage = "NONE";
        this.mComponent = null;
        this.mFlags = 0;
        this.mCategories = new HashSet<String>();
        this.mExtras = new Bundle();
        //this.mSourceBounds = new Rect(o.mSourceBounds);
        this.mSelector = new Intent();
        //this.mClipData = new ClipData(o.mClipData);
    }

    public Intent(Intent o) {
        this.mAction = o.mAction;
        this.mData = o.mData;
        this.mType = o.mType;
        this.mPackage = o.mPackage;
        this.mComponent = o.mComponent;
        this.mFlags = o.mFlags;
        this.mCategories = o.mCategories;
        this.mExtras = o.mExtras;
        //this.mSourceBounds = o.mSourceBounds;
        this.mSelector = o.mSelector;
        //this.mClipData = o.mClipData;
    }

    @Override
    public Object clone() {
        return new Intent(this);
    }

    private Intent(Intent o, boolean all) {
        this.mAction = o.mAction;
        this.mData = o.mData;
        this.mType = o.mType;
        this.mPackage = o.mPackage;
        this.mComponent = o.mComponent;
        this.mCategories = o.mCategories;
    }

    public Intent cloneFilter() {
        return new Intent(this, false);
    }

    public Intent(String action) {
        this.mAction = action;
    }

    public Intent(String action, Uri uri) {
        this.mAction = action;
        mData = uri;
    }

    public Intent(Context packageContext, Class<?> cls) {
        this.mAction = cls.toString();
        this.mData = null;
        this.mType = "NONE";
        this.mPackage = "NONE";
        this.mComponent = null;
        this.mFlags = 0;
        this.mCategories = new HashSet<String>();
        this.mExtras = new Bundle();
        //this.mSourceBounds = new Rect(o.mSourceBounds);
        this.mSelector = new Intent();
        //this.mClipData = new ClipData(o.mClipData);
    }

    public Intent(String action, Uri uri, Context packageContext, Class<?> cls) {
        this(packageContext, cls);
        mAction = action;
        mData = uri;
    }

    public static Intent makeMainActivity(ComponentName mainActivity) {
        Intent intent = new Intent(ACTION_MAIN);
        intent.setComponent(mainActivity);
        intent.addCategory(CATEGORY_LAUNCHER);
        return intent;
    }

    public static Intent makeMainSelectorActivity(String selectorAction,
            String selectorCategory) {
        Intent intent = new Intent(ACTION_MAIN);
        intent.addCategory(CATEGORY_LAUNCHER);
        Intent selector = new Intent();
        selector.setAction(selectorAction);
        selector.addCategory(selectorCategory);
        intent.setSelector(selector);
        return intent;
    }

    public static Intent makeRestartActivityTask(ComponentName mainActivity) {
        Intent intent = makeMainActivity(mainActivity);
        return intent;
    }

    public static Intent getIntent(String uri) throws URISyntaxException {
        return parseUri(uri, 0);
    }

    public static Intent parseUri(String uri, int flags) throws URISyntaxException {
        throw new UnsupportedOperationException("STUB");
    }

    public static Intent getIntentOld(String uri) throws URISyntaxException {
        throw new UnsupportedOperationException("STUB");
    }

    public String getAction() {
        return mAction;
    }

    public Uri getData() {
        return mData;
    }
    public String getDataString() {
        return mData.toString();
    }

    public String getScheme() {
        return mData.getScheme();
    }

    public String getType() {
        return mType;
    }

    public String resolveType(Context context) {
        return resolveType(context.getContentResolver());
    }

    public String resolveType(ContentResolver resolver) {
        return mType;
    }

    public String resolveTypeIfNeeded(ContentResolver resolver) {
        if (mComponent != null) {
            return mType;
        }
        return resolveType(resolver);
    }

    public boolean hasCategory(String category) {
        return mCategories != null && mCategories.contains(category);
    }

    public Set<String> getCategories() {
        return mCategories;
    }

    public Intent getSelector() {
        return mSelector;
    }

    public ClipData getClipData() {
        throw new UnsupportedOperationException("STUB!");
        //return mClipData;
    }

    public void setExtrasClassLoader(ClassLoader loader) {
        mExtras.setClassLoader(loader);
    }

    public boolean hasExtra(String name) {
        return mExtras.containsKey(name);
    }

    public boolean hasFileDescriptors() {
        return mExtras.hasFileDescriptors();
    }

    public void setAllowFds(boolean allowFds) {
    }

    public Object getExtra(String name) {
        return getExtra(name, null);
    }

    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return mExtras.getBoolean(name, defaultValue);
    }

    public byte getByteExtra(String name, byte defaultValue) {
        return mExtras.getByte(name, defaultValue);
    }

    public short getShortExtra(String name, short defaultValue) {
        return mExtras.getShort(name, defaultValue);
    }

    public char getCharExtra(String name, char defaultValue) {
        return mExtras.getChar(name, defaultValue);
    }

    public int getIntExtra(String name, int defaultValue) {
        return mExtras.getInt(name, defaultValue);
    }

    public long getLongExtra(String name, long defaultValue) {
        return mExtras.getLong(name, defaultValue);
    }

    public float getFloatExtra(String name, float defaultValue) {
        return mExtras.getFloat(name, defaultValue);
    }

    public double getDoubleExtra(String name, double defaultValue) {
        return mExtras.getDouble(name, defaultValue);
    }

    public String getStringExtra(String name) {
        return mExtras.getString(name);
    }

    public CharSequence getCharSequenceExtra(String name) {
        return mExtras.getCharSequence(name);
    }

    public <T extends Parcelable> T getParcelableExtra(String name) {
        return mExtras.<T>getParcelable(name);
    }

    public Parcelable[] getParcelableArrayExtra(String name) {
        return mExtras.getParcelableArray(name);
    }

    public <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(String name) {
        return mExtras.<T>getParcelableArrayList(name);
    }

    public Serializable getSerializableExtra(String name) {
        return mExtras.getSerializable(name);
    }

    public ArrayList<Integer> getIntegerArrayListExtra(String name) {
        return mExtras.getIntegerArrayList(name);
    }

    public ArrayList<String> getStringArrayListExtra(String name) {
        return mExtras.getStringArrayList(name);
    }

    public ArrayList<CharSequence> getCharSequenceArrayListExtra(String name) {
        return mExtras.getCharSequenceArrayList(name);
    }

    public boolean[] getBooleanArrayExtra(String name) {
        return mExtras.getBooleanArray(name);
    }

    public byte[] getByteArrayExtra(String name) {
        return mExtras.getByteArray(name);
    }

    public short[] getShortArrayExtra(String name) {
        return mExtras.getShortArray(name);
    }

    public char[] getCharArrayExtra(String name) {
        return mExtras.getCharArray(name);
    }

    public int[] getIntArrayExtra(String name) {
        return mExtras.getIntArray(name);
    }

    public long[] getLongArrayExtra(String name) {
        return mExtras.getLongArray(name);
    }

    public float[] getFloatArrayExtra(String name) {
        return mExtras.getFloatArray(name);
    }

    public double[] getDoubleArrayExtra(String name) {
        return mExtras.getDoubleArray(name);
    }

    public String[] getStringArrayExtra(String name) {
        return mExtras.getStringArray(name);
    }

    public CharSequence[] getCharSequenceArrayExtra(String name) {
        return mExtras.getCharSequenceArray(name);
    }

    public Bundle getBundleExtra(String name) {
        return mExtras.getBundle(name);
    }

    public IBinder getIBinderExtra(String name) {
        return null;
    }

    public Object getExtra(String name, Object defaultValue) {
        return mExtras.get(name);
    }

    public Bundle getExtras() {
        return mExtras;
    }

    public int getFlags() {
        return mFlags;
    }

    public boolean isExcludingStopped() {
        return false;
    }

    public String getPackage() {
        return mPackage;
    }
    public ComponentName getComponent() {
        return mComponent;
    }

    public Rect getSourceBounds() {
        throw new UnsupportedOperationException("STUB!");
    }

    public ComponentName resolveActivity(PackageManager pm) {
        return mComponent;
    }

    public ActivityInfo resolveActivityInfo(PackageManager pm, int flags) {
        throw new IllegalStateException("STUB!");
    }

    public Intent setAction(String action) {
        mAction = action;
        return this;
    }

    public Intent setData(Uri data) {
        mData = data;
        mType = null;
        return this;
    }

    public Intent setDataAndNormalize(Uri data) {
        return setData(data.normalizeScheme());
    }

    public Intent setType(String type) {
        mData = null;
        mType = type;
        return this;
    }

    public Intent setTypeAndNormalize(String type) {
        return setType(type);
    }

    public Intent setDataAndType(Uri data, String type) {
        mData = data;
        mType = type;
        return this;
    }

    public Intent setDataAndTypeAndNormalize(Uri data, String type) {
        return setDataAndType(data, type);
    }

    public Intent addCategory(String category) {
        mCategories.add(category.intern());
        return this;
    }

    public void removeCategory(String category) {
    }

    public void setSelector(Intent selector) {
        mSelector = selector;
    }

    public void setClipData(ClipData clip) {
//        mClipData = clip;
    }

    public Intent putExtra(String name, boolean value) {
        mExtras.putBoolean(name, value);
        return this;
    }

    public Intent putExtra(String name, byte value) {
        mExtras.putByte(name, value);
        return this;
    }

    public Intent putExtra(String name, char value) {
        mExtras.putChar(name, value);
        return this;
    }

    public Intent putExtra(String name, short value) {
        mExtras.putShort(name, value);
        return this;
    }

    public Intent putExtra(String name, int value) {
        mExtras.putInt(name, value);
        return this;
    }

    public Intent putExtra(String name, long value) {
        mExtras.putLong(name, value);
        return this;
    }

    public Intent putExtra(String name, float value) {
        mExtras.putFloat(name, value);
        return this;
    }

    public Intent putExtra(String name, double value) {
        mExtras.putDouble(name, value);
        return this;
    }

    public Intent putExtra(String name, String value) {
        mExtras.putString(name, value);
        return this;
    }

    public Intent putExtra(String name, CharSequence value) {
        mExtras.putCharSequence(name, value);
        return this;
    }

    public Intent putExtra(String name, Parcelable value) {
        mExtras.putParcelable(name, value);
        return this;
    }

    public Intent putExtra(String name, Parcelable[] value) {
        mExtras.putParcelableArray(name, value);
        return this;
    }

    public Intent putParcelableArrayListExtra(String name, ArrayList<? extends Parcelable> value) {
        mExtras.putParcelableArrayList(name, value);
        return this;
    }

    public Intent putIntegerArrayListExtra(String name, ArrayList<Integer> value) {
        mExtras.putIntegerArrayList(name, value);
        return this;
    }

    public Intent putStringArrayListExtra(String name, ArrayList<String> value) {
        mExtras.putStringArrayList(name, value);
        return this;
    }

    public Intent putCharSequenceArrayListExtra(String name, ArrayList<CharSequence> value) {
        mExtras.putCharSequenceArrayList(name, value);
        return this;
    }

    public Intent putExtra(String name, Serializable value) {
        mExtras.putSerializable(name, value);
        return this;
    }

    public Intent putExtra(String name, boolean[] value) {
        mExtras.putBooleanArray(name, value);
        return this;
    }

    public Intent putExtra(String name, byte[] value) {
        mExtras.putByteArray(name, value);
        return this;
    }

    public Intent putExtra(String name, short[] value) {
        mExtras.putShortArray(name, value);
        return this;
    }

    public Intent putExtra(String name, char[] value) {
        mExtras.putCharArray(name, value);
        return this;
    }

    public Intent putExtra(String name, int[] value) {
        mExtras.putIntArray(name, value);
        return this;
    }

    public Intent putExtra(String name, long[] value) {
        mExtras.putLongArray(name, value);
        return this;
    }

    public Intent putExtra(String name, float[] value) {
        mExtras.putFloatArray(name, value);
        return this;
    }

    public Intent putExtra(String name, double[] value) {
        mExtras.putDoubleArray(name, value);
        return this;
    }

    public Intent putExtra(String name, String[] value) {
        mExtras.putStringArray(name, value);
        return this;
    }

    public Intent putExtra(String name, CharSequence[] value) {
        mExtras.putCharSequenceArray(name, value);
        return this;
    }

    public Intent putExtra(String name, Bundle value) {
        mExtras.putBundle(name, value);
        return this;
    }

    public Intent putExtra(String name, IBinder value) {
        //mExtras.putIBinder(name, value);
        return this;
    }

    public Intent putExtras(Intent src) {
        mExtras.putAll(src.mExtras);
        return this;
    }

    public Intent putExtras(Bundle extras) {
        mExtras.putAll(extras);
        return this;
    }

    public Intent replaceExtras(Intent src) {
        mExtras = src.mExtras != null ? src.mExtras : null;
        return this;
    }

    public Intent replaceExtras(Bundle extras) {
        mExtras = extras;
        return this;
    }

    public void removeExtra(String name) {
    }

    public Intent setFlags(int flags) {
        return this;
    }

    public Intent addFlags(int flags) {
        return this;
    }

    public Intent setPackage(String packageName) {
        mPackage = packageName;
        return this;
    }

    public Intent setComponent(ComponentName component) {
        mComponent = component;
        return this;
    }

    public Intent setClassName(Context packageContext, String className) {  // STUBS TODO
        mComponent = new ComponentName(packageContext, className);
        return this;
    }

    public Intent setClassName(String packageName, String className) {      // STUBS TODO
        mComponent = new ComponentName(packageName, className);
        return this;
    }

    public Intent setClass(Context packageContext, Class<?> cls) {          // STUBS TODO
        mComponent = new ComponentName(packageContext, cls);
        return this;
    }

    public void setSourceBounds(Rect r) {
        throw new UnsupportedOperationException("STUB!");
    }

    public static final int FILL_IN_ACTION = 1<<0;
    public static final int FILL_IN_DATA = 1<<1;
    public static final int FILL_IN_CATEGORIES = 1<<2;
    public static final int FILL_IN_COMPONENT = 1<<3;
    public static final int FILL_IN_PACKAGE = 1<<4;
    public static final int FILL_IN_SOURCE_BOUNDS = 1<<5;
    public static final int FILL_IN_SELECTOR = 1<<6;
    public static final int FILL_IN_CLIP_DATA = 1<<7;

    public int fillIn(Intent other, int flags) {
        mAction = other.mAction;
        mData = other.mData;
        mType = other.mType;
        mCategories = other.mCategories;
        mPackage = other.mPackage;
        mSelector = new Intent(other.mSelector);
        //mClipData = other.mClipData;
        mComponent = other.mComponent;
        //mSourceBounds = new Rect(other.mSourceBounds);
        mExtras.putAll(other.mExtras);
        return 0;
    }

    public static final class FilterComparison {
        private final Intent mIntent;

        public FilterComparison(Intent intent) {
            mIntent = intent;
        }

        public Intent getIntent() {
            return mIntent;
        }

        @Override
        public boolean equals(Object obj) {
            return mIntent.equals(obj);
        }

        @Override
        public int hashCode() {
            return mIntent.hashCode();
        }
    }

    public boolean filterEquals(Intent other) {
        return true;
    }

    public int filterHashCode() {
        int code = 0;
        code += mAction.hashCode();
        code += mData.hashCode();
        code += mType.hashCode();
        code += mPackage.hashCode();
        code += mComponent.hashCode();
        code += mCategories.hashCode();
        return code;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(128);
        toShortString(b, true, true, true, false);

        return b.toString();
    }

    public String toInsecureString() {
        return toString();
    }

    public String toInsecureStringWithClip() {
        return toString();
    }

    public String toShortString(boolean secure, boolean comp, boolean extras, boolean clip) {
         return toString();
    }

    /** @hide */
    public void toShortString(StringBuilder b, boolean secure, boolean comp, boolean extras,
            boolean clip) {
        b.append(mAction);
        b.append(mCategories.toString());
        b.append(mData);
        b.append(mType);
        b.append(mPackage);
        b.append(mComponent.toString());
        //b.append("bnds=").append(mSourceBounds.toString());
        //mClipData.toShortString(b);
        b.append(mExtras.toString());
        mSelector.toShortString(b, secure, comp, extras, clip);
    }

    public String toURI() {
        return toUri(0);
    }

    public String toUri(int flags) {
        return toString();
    }


    public int describeContents() {
        return mExtras.describeContents();
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(toString());
        out.writeBundle(mExtras);
    }

    public static final Parcelable.Creator<Intent> CREATOR
            = new Parcelable.Creator<Intent>() {
        public Intent createFromParcel(Parcel in) {
            return new Intent(in);
        }
        public Intent[] newArray(int size) {
            return new Intent[size];
        }
    };

    protected Intent(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        throw new UnsupportedOperationException("STUB!");
    }

    public static Intent parseIntent(Resources resources, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        throw new UnsupportedOperationException("STUB!");
    }

    public static String normalizeMimeType(String type) {
        return type;
    }

    public void prepareToLeaveProcess() {
        setAllowFds(false);
        mSelector.prepareToLeaveProcess();
    }

    public boolean migrateExtraStreamToClipData() {
        throw new UnsupportedOperationException("STUB");
    }

    private static ClipData.Item makeClipItem(ArrayList<Uri> streams, ArrayList<CharSequence> texts,
            ArrayList<String> htmlTexts, int which) {
        throw new UnsupportedOperationException("STUB");
    }
}
