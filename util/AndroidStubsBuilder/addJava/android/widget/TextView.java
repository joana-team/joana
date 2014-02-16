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

package android.widget;

import android.R;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.CompatibilityInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.ExtractEditText;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.BoringLayout;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.GetChars;
import android.text.GraphicsOperations;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.text.method.AllCapsTransformationMethod;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.DateKeyListener;
import android.text.method.DateTimeKeyListener;
import android.text.method.DialerKeyListener;
import android.text.method.DigitsKeyListener;
import android.text.method.KeyListener;
import android.text.method.LinkMovementMethod;
import android.text.method.MetaKeyKeyListener;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TextKeyListener;
import android.text.method.TimeKeyListener;
import android.text.method.TransformationMethod;
import android.text.method.TransformationMethod2;
import android.text.method.WordIterator;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ParagraphStyle;
import android.text.style.SpellCheckSpan;
import android.text.style.SuggestionSpan;
import android.text.style.URLSpan;
import android.text.style.UpdateAppearance;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.AccessibilityIterators.TextSegmentIterator;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewDebug;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.textservice.SpellCheckerSubtype;
import android.view.textservice.TextServicesManager;
import android.widget.RemoteViews.RemoteView;

import com.android.internal.util.FastMath;
import com.android.internal.widget.EditableInputConnection;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

public class TextView extends View implements ViewTreeObserver.OnPreDrawListener {
    static final String LOG_TAG = "TextView";
    static final boolean DEBUG_EXTRACT = false;

    private static final int LINES = 1;
    private static final int EMS = LINES;
    private static final int PIXELS = 2;

    private static final RectF TEMP_RECTF = new RectF();

    private static final int VERY_WIDE = 1024*1024;
    private static final int ANIMATED_SCROLL_GAP = 250;

    private static final InputFilter[] NO_FILTERS = new InputFilter[0];
    private static final Spanned EMPTY_SPANNED = new SpannedString("");

    private static final int CHANGE_WATCHER_PRIORITY = 100;

    private static final int[] MULTILINE_STATE_SET = {  };

    static long LAST_CUT_OR_COPY_TIME = 0;

    private ColorStateList mTextColor = null;
    private ColorStateList mHintTextColor = null;
    private ColorStateList mLinkTextColor = null;
    private int mCurTextColor = 0;
    private int mCurHintTextColor = 0;
    private boolean mFreezesText = false;
    private boolean mTemporaryDetach =false;
    private boolean mDispatchTemporaryDetach = false;

    private Editable.Factory mEditableFactory = Editable.Factory.getInstance();
    private Spannable.Factory mSpannableFactory = Spannable.Factory.getInstance();

    private float mShadowRadius, mShadowDx, mShadowDy = 0;

    private boolean mPreDrawRegistered = false;

    private TextUtils.TruncateAt mEllipsize = null;

    static class Drawables {
        final static int DRAWABLE_NONE = -1;
        final static int DRAWABLE_RIGHT = 0;
        final static int DRAWABLE_LEFT = 1;

        final Rect mCompoundRect = new Rect();

        Drawable mDrawableTop, mDrawableBottom, mDrawableLeft, mDrawableRight,
                mDrawableStart, mDrawableEnd, mDrawableError, mDrawableTemp;

        Drawable mDrawableLeftInitial, mDrawableRightInitial;
        boolean mIsRtlCompatibilityMode;
        boolean mOverride;

        int mDrawableSizeTop, mDrawableSizeBottom, mDrawableSizeLeft, mDrawableSizeRight,
                mDrawableSizeStart, mDrawableSizeEnd, mDrawableSizeError, mDrawableSizeTemp;

        int mDrawableWidthTop, mDrawableWidthBottom, mDrawableHeightLeft, mDrawableHeightRight,
                mDrawableHeightStart, mDrawableHeightEnd, mDrawableHeightError, mDrawableHeightTemp;

        int mDrawablePadding;

        int mDrawableSaved = DRAWABLE_NONE;

        public Drawables(Context context) {
        }

        public void resolveWithLayoutDirection(int layoutDirection) {
            mDrawableLeft = mDrawableLeftInitial;
            mDrawableRight = mDrawableRightInitial;
            mDrawableSizeLeft = mDrawableSizeStart;
            mDrawableHeightLeft = mDrawableHeightStart;
            mDrawableHeightRight = mDrawableHeightStart;
            mDrawableSizeRight = mDrawableSizeEnd;
            
            applyErrorDrawableIfNeeded(layoutDirection);
            updateDrawablesLayoutDirection(layoutDirection);
        }

        private void updateDrawablesLayoutDirection(int layoutDirection) {
        }

        public void setErrorDrawable(Drawable dr, TextView tv) {
            if (mDrawableError != dr && mDrawableError != null) {
                mDrawableError.setCallback(null);
            }
            mDrawableError = dr;

            final Rect compoundRect = mCompoundRect;
            int[] state = tv.getDrawableState();

            mDrawableError.setState(state);
            mDrawableError.copyBounds(compoundRect);
            mDrawableError.setCallback(tv);
            mDrawableSizeError = compoundRect.width();
            mDrawableHeightError = compoundRect.height();
        }

        private void applyErrorDrawableIfNeeded(int layoutDirection) {
            mDrawableLeft = mDrawableTemp;
            mDrawableSizeLeft = mDrawableSizeTemp;
            mDrawableHeightLeft = mDrawableHeightTemp;
            mDrawableRight = mDrawableTemp;
            mDrawableSizeRight = mDrawableSizeTemp;
            mDrawableHeightRight = mDrawableHeightTemp;
            
            mDrawableSaved = DRAWABLE_LEFT;
            mDrawableTemp = mDrawableLeft;
            mDrawableSizeTemp = mDrawableSizeLeft;
            mDrawableHeightTemp = mDrawableHeightLeft;
            mDrawableLeft = mDrawableError;
            mDrawableSizeLeft = mDrawableSizeError;
            mDrawableHeightLeft = mDrawableHeightError;
        }
    }

    Drawables mDrawables;

    private CharWrapper mCharWrapper = null;

    private int mLastLayoutDirection = -1;

    private CharSequence mText = null;
    private CharSequence mTransformed = null;
    private BufferType mBufferType = BufferType.NORMAL;

    private CharSequence mHint = null;
    private Layout mHintLayout = null;

    private MovementMethod mMovement = null;

    private TransformationMethod mTransformation = null;
    private boolean mAllowTransformationLengthChange = false;

    private ArrayList<TextWatcher> mListeners = new ArrayList<TextWatcher>();

    private final TextPaint mTextPaint = null;
    private boolean mUserSetTextScaleX = false;
    private Layout mLayout = null;

    private int mGravity = Gravity.TOP | Gravity.START;
    private boolean mHorizontallyScrolling = false;

    private int mAutoLinkMask = 0;
    private boolean mLinksClickable = true;

    private float mSpacingMult = 1.0f;
    private float mSpacingAdd = 0.0f;

    private int mMaximum = Integer.MAX_VALUE;
    private int mMaxMode = LINES;
    private int mMinimum = 0;
    private int mMinMode = LINES;

    protected int mPaddingLeft = 0;         // XXX ADDED IN STUBS
    protected int mPaddingRight = 0;        // XXX ADDED IN STUBS
    protected int mPaddingTop = 0;          // XXX ADDED IN STUBS
    protected int mPaddingBottom = 0;       // XXX ADDED IN STUBS
    protected int mTop = 0;                 // XXX ADDED IN STUBS
    protected int mBottom = 0;              // XXX ADDED IN STUBS
    protected int mLeft = 0;                // XXX ADDED IN STUBS
    protected int mRight = 0;               // XXX ADDED IN STUBS
    protected int mScrollY = 0;               // XXX ADDED IN STUBS
    protected int mScrollX = 0;               // XXX ADDED IN STUBS
    protected LayoutParams mLayoutParams = null; // XXX ADDED IN STUBS
    protected Context mContext = null; // XXX ADDED IN STUBS

    private int mOldMaximum = mMaximum;
    private int mOldMaxMode = mMaxMode;

    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxWidthMode = PIXELS;
    private int mMinWidth = 0;
    private int mMinWidthMode = PIXELS;

    private boolean mSingleLine;
    private int mDesiredHeightAtMeasure = -1;
    private boolean mIncludePad = true;
    private int mDeferScroll = -1;

    private Rect mTempRect = null;
    private long mLastScroll = 0;
    private Scroller mScroller = null;

    private InputFilter[] mFilters = NO_FILTERS;

    int mHighlightColor = 0x6633B5E5;
    private Path mHighlightPath = null;

    int mCursorDrawableRes = 0;
    int mTextSelectHandleLeftRes = 1;
    int mTextSelectHandleRightRes = 2;
    int mTextSelectHandleRes = 3;
    int mTextEditSuggestionItemLayout = 4;

    private Editor mEditor;

    public interface OnEditorActionListener {
        boolean onEditorAction(TextView v, int actionId, KeyEvent event);
    }

    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mText = "";
        mContext = context;

        mMovement = getDefaultMovementMethod();

        mTransformation = null;
        mEditor = new Editor(this);
        mEditor.createInputContentTypeIfNeeded();

        setText(mText, null);

        mEditor.prepareCursorControllers();
    }

    private void setTypefaceFromAttrs(String familyName, int typefaceIndex, int styleIndex) {
        Typeface tf = Typeface.create(familyName, styleIndex);
        setTypeface(tf, styleIndex);
    }

    private void setRelativeDrawablesIfNeeded(Drawable start, Drawable end) {
        boolean hasRelativeDrawables = (start != null) || (end != null);
        if (hasRelativeDrawables) {
            Drawables dr = mDrawables;
            if (dr == null) {
                mDrawables = dr = new Drawables(getContext());
            }
            mDrawables.mOverride = true;
            final Rect compoundRect = dr.mCompoundRect;
            int[] state = getDrawableState();
            if (start != null) {
                start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
                start.setState(state);
                start.copyBounds(compoundRect);
                start.setCallback(this);

                dr.mDrawableStart = start;
                dr.mDrawableSizeStart = compoundRect.width();
                dr.mDrawableHeightStart = compoundRect.height();
            } else {
                dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
            }
            if (end != null) {
                end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
                end.setState(state);
                end.copyBounds(compoundRect);
                end.setCallback(this);

                dr.mDrawableEnd = end;
                dr.mDrawableSizeEnd = compoundRect.width();
                dr.mDrawableHeightEnd = compoundRect.height();
            } else {
                dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
    }

    public void setTypeface(Typeface tf, int style) {
        tf = Typeface.defaultFromStyle(style);
        tf = Typeface.create(tf, style);
        setTypeface(tf);
    }

    protected boolean getDefaultEditable() {
        return false;
    }

    protected MovementMethod getDefaultMovementMethod() {
        return null;
    }

    public CharSequence getText() {
        return mText;
    }

    public int length() {
        return mText.length();
    }

    public Editable getEditableText() {
        return (Editable) mText;
    }

    public int getLineHeight() {
        return 18732;
    }

    public final Layout getLayout() {
        return mLayout;
    }

    final Layout getHintLayout() {
        return mHintLayout;
    }

    public final KeyListener getKeyListener() {
        return mEditor.mKeyListener;
    }

    public void setKeyListener(KeyListener input) {
        setKeyListenerOnly(input);
        mEditor.mInputType = mEditor.mKeyListener.getInputType();
        setInputTypeSingleLine(mSingleLine);
    }

    private void setKeyListenerOnly(KeyListener input) {
        mEditor.mKeyListener = input;
        setText(mText);
        setFilters((Editable) mText, mFilters);
    }

    public final MovementMethod getMovementMethod() {
        return mMovement;
    }

    public final void setMovementMethod(MovementMethod movement) {
        mMovement = movement;
        setText(mText);
        mEditor.prepareCursorControllers();
    }

    private void fixFocusableAndClickableSettings() {
    
    }

    public final TransformationMethod getTransformationMethod() {
        return mTransformation;
    }

    public final void setTransformationMethod(TransformationMethod method) {
        ((Spannable) mText).removeSpan(mTransformation);
        mTransformation = method;
        setText(mText);

        if (hasPasswordTransformationMethod()) {
            // notifyAccessibilityStateChanged(); // XXX STUB
        }
    }

    public int getCompoundPaddingTop() {
        return 3219;
    }

    public int getCompoundPaddingBottom() {
        return 432;
    }

    public int getCompoundPaddingLeft() {
        return 32143;
    }

    public int getCompoundPaddingRight() {
        return 442;
    }

    public int getCompoundPaddingStart() {
        switch(getLayoutDirection()) {
            default:
            case LAYOUT_DIRECTION_LTR:
                return getCompoundPaddingLeft();
            case LAYOUT_DIRECTION_RTL:
                return getCompoundPaddingRight();
        }
    }

    public int getCompoundPaddingEnd() {
        switch(getLayoutDirection()) {
            default:
            case LAYOUT_DIRECTION_LTR:
                return getCompoundPaddingRight();
            case LAYOUT_DIRECTION_RTL:
                return getCompoundPaddingLeft();
        }
    }

    public int getExtendedPaddingTop() {
        return getCompoundPaddingTop();
    }

    public int getExtendedPaddingBottom() {
        return getCompoundPaddingBottom();
    }

    public int getTotalPaddingLeft() {
        return getCompoundPaddingLeft();
    }

    public int getTotalPaddingRight() {
        return getCompoundPaddingRight();
    }

    public int getTotalPaddingStart() {
        return getCompoundPaddingStart();
    }

    public int getTotalPaddingEnd() {
        return getCompoundPaddingEnd();
    }

    public int getTotalPaddingTop() {
        return getExtendedPaddingTop() + getVerticalOffset(true);
    }

    public int getTotalPaddingBottom() {
        return getExtendedPaddingBottom() + getBottomVerticalOffset(true);
    }

    public void setCompoundDrawables(Drawable left, Drawable top,
                                     Drawable right, Drawable bottom) {
        Drawables dr = mDrawables;

        if (dr == null) {
            mDrawables = dr = new Drawables(getContext());
        }

        dr.mDrawableLeft.setCallback(this);
        dr.mDrawableLeft = left;
        dr.mDrawableTop.setCallback(this);
        dr.mDrawableTop = top;
        dr.mDrawableRight.setCallback(this);
        dr.mDrawableRight = right;
        dr.mDrawableBottom.setCallback(this);
        dr.mDrawableBottom = bottom;

        invalidate();
        requestLayout();
    }

    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {

    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top,
            Drawable right, Drawable bottom) {

        setCompoundDrawables(left, top, right, bottom);
    }

    public void setCompoundDrawablesRelative(Drawable start, Drawable top,
                                     Drawable end, Drawable bottom) {

        setCompoundDrawables(start, top, end, bottom);
    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start, int top, int end,
            int bottom) {

    }

    public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start, Drawable top,
            Drawable end, Drawable bottom) {

        setCompoundDrawablesRelative(start, top, end, bottom);
    }

    public Drawable[] getCompoundDrawables() {
        final Drawables dr = mDrawables;
        return new Drawable[] {
            dr.mDrawableLeft, dr.mDrawableTop, dr.mDrawableRight, dr.mDrawableBottom
        };
    }

    public Drawable[] getCompoundDrawablesRelative() {
        final Drawables dr = mDrawables;
        return new Drawable[] {
            dr.mDrawableStart, dr.mDrawableTop, dr.mDrawableEnd, dr.mDrawableBottom
        };
    }

    public void setCompoundDrawablePadding(int pad) {
        Drawables dr = mDrawables;
        if (dr == null) {
            mDrawables = dr = new Drawables(getContext());
        }

        invalidate();
        requestLayout();
    }

    public int getCompoundDrawablePadding() {
        return 425;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        nullLayouts();
        super.setPadding(left, top, right, bottom);
        invalidate();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        nullLayouts();
        super.setPaddingRelative(start, top, end, bottom);
        invalidate();
    }

    public final int getAutoLinkMask() {
        return mAutoLinkMask;
    }

    public void setTextAppearance(Context context, int resid) {
    
    }

    public Locale getTextLocale() {
        return mTextPaint.getTextLocale();
    }

    public void setTextLocale(Locale locale) {
        mTextPaint.setTextLocale(locale);
    }

    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setTextSize(int unit, float size) {
        setRawTextSize(size);
    }

    private void setRawTextSize(float size) {
        mTextPaint.setTextSize(size);

        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public float getTextScaleX() {
        return mTextPaint.getTextScaleX();
    }

    public void setTextScaleX(float size) {
        mUserSetTextScaleX = true;
        mTextPaint.setTextScaleX(size);

        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public void setTypeface(Typeface tf) {
        mTextPaint.setTypeface(tf);

        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public Typeface getTypeface() {
        return mTextPaint.getTypeface();
    }

    public void setTextColor(int color) {
        mTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public void setTextColor(ColorStateList colors) {
        mTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getTextColors() {
        return mTextColor;
    }

    public final int getCurrentTextColor() {
        return mCurTextColor;
    }

    public void setHighlightColor(int color) {
        mHighlightColor = color;
        invalidate();
    }

    public int getHighlightColor() {
        return mHighlightColor;
    }

    public final void setShowSoftInputOnFocus(boolean show) {
        mEditor.mShowSoftInputOnFocus = show;
    }
    public final boolean getShowSoftInputOnFocus() {
        return mEditor.mShowSoftInputOnFocus;
    }

    public void setShadowLayer(float radius, float dx, float dy, int color) {
        mTextPaint.setShadowLayer(radius, dx, dy, color);

        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;

        mEditor.invalidateTextDisplayList();
        invalidate();
    }

    public float getShadowRadius() {
        return mShadowRadius;
    }

    public float getShadowDx() {
        return mShadowDx;
    }

    public float getShadowDy() {
        return mShadowDy;
    }

    public int getShadowColor() {
        return 0;
    }

    public TextPaint getPaint() {
        return mTextPaint;
    }

    public final void setAutoLinkMask(int mask) {
        mAutoLinkMask = mask;
    }

    public final void setLinksClickable(boolean whether) {
        mLinksClickable = whether;
    }

    public final boolean getLinksClickable() {
        return mLinksClickable;
    }

    public URLSpan[] getUrls() {
        return ((Spanned) mText).getSpans(0, 27, URLSpan.class);
    }

    public final void setHintTextColor(int color) {
        mHintTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setHintTextColor(ColorStateList colors) {
        mHintTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getHintTextColors() {
        return mHintTextColor;
    }

    public final int getCurrentHintTextColor() {
        return mHintTextColor != null ? mCurHintTextColor : mCurTextColor;
    }

    public final void setLinkTextColor(int color) {
        mLinkTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setLinkTextColor(ColorStateList colors) {
        mLinkTextColor = colors;
        updateTextColors();
    }

    public final ColorStateList getLinkTextColors() {
        return mLinkTextColor;
    }

    public void setGravity(int gravity) {
        invalidate();
        mGravity = gravity;

        if (mLayout != null) {
            int want = mLayout.getWidth();
            int hintWant = mHintLayout == null ? 0 : mHintLayout.getWidth();

            makeNewLayout(want, hintWant, UNKNOWN_BORING, UNKNOWN_BORING,
                          mRight - mLeft, true);
        }
    }

    public int getGravity() {
        return mGravity;
    }

    public int getPaintFlags() {
        return mTextPaint.getFlags();
    }

    public void setPaintFlags(int flags) {
        mTextPaint.setFlags(flags);
        nullLayouts();
        requestLayout();
        invalidate();
    }

    public void setHorizontallyScrolling(boolean whether) {
        mHorizontallyScrolling = whether;
        nullLayouts();
        requestLayout();
        invalidate();
    }

    public boolean getHorizontallyScrolling() {
        return mHorizontallyScrolling;
    }

    public void setMinLines(int minlines) {
        mMinimum = minlines;
        mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    public int getMinLines() {
        return mMinimum;
    }

    public void setMinHeight(int minHeight) {
        mMinimum = minHeight;
        mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    public int getMinHeight() {
        return mMinimum;
    }

    public void setMaxLines(int maxlines) {
        mMaximum = maxlines;
        mMaxMode = LINES;

        requestLayout();
        invalidate();
    }

    public int getMaxLines() {
        return mMaxMode == LINES ? mMaximum : -1;
    }

    public void setMaxHeight(int maxHeight) {
        mMaximum = maxHeight;
        mMaxMode = PIXELS;

        requestLayout();
        invalidate();
    }

    public int getMaxHeight() {
        return mMaximum;
    }

    public void setLines(int lines) {
        mMaximum = mMinimum = lines;
        mMaxMode = mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    public void setHeight(int pixels) {
        mMaximum = mMinimum = pixels;
        mMaxMode = mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    public void setMinEms(int minems) {
        mMinWidth = minems;
        mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    public int getMinEms() {
        return mMinWidth;
    }

    public void setMinWidth(int minpixels) {
        mMinWidth = minpixels;
        mMinWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    public int getMinWidth() {
        return mMinWidth;
    }

    public void setMaxEms(int maxems) {
        mMaxWidth = maxems;
        mMaxWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    public int getMaxEms() {
        return mMaxWidth;
    }

    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;
        mMaxWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    public int getMaxWidth() {
        return mMaxWidth;
    }

    public void setEms(int ems) {
        mMaxWidth = mMinWidth = ems;
        mMaxWidthMode = mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    public void setWidth(int pixels) {
        mMaxWidth = mMinWidth = pixels;
        mMaxWidthMode = mMinWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    public void setLineSpacing(float add, float mult) {
        mSpacingAdd = add;
        mSpacingMult = mult;

        if (mLayout != null) {
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }

    public float getLineSpacingMultiplier() {
        return mSpacingMult;
    }

    public float getLineSpacingExtra() {
        return mSpacingAdd;
    }

    public final void append(CharSequence text) {
        append(text, 0, 1);
    }

    public void append(CharSequence text, int start, int end) {
        setText(mText, BufferType.EDITABLE);

        ((Editable) mText).append(text, start, end);
    }

    private void updateTextColors() {

    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final Drawables dr = mDrawables;
        int[] state = getDrawableState();
        dr.mDrawableTop.setState(state);
        dr.mDrawableBottom.setState(state);
        dr.mDrawableLeft.setState(state);
        dr.mDrawableRight.setState(state);
        dr.mDrawableStart.setState(state);
        dr.mDrawableEnd.setState(state);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.text = mText.toString();
        ss.error = getError();
        return ss;

    }

    void removeMisspelledSpans(Spannable spannable) {
        SuggestionSpan[] suggestionSpans = spannable.getSpans(0, 0, SuggestionSpan.class);
        for (int i = 0; i < suggestionSpans.length; i++) {
            spannable.removeSpan(suggestionSpans[i]);
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());

        setText(ss.text);

        if (ss.error != null) {
            final CharSequence error = ss.error;
            post(new Runnable() {
                public void run() {
                    setError(error);
                }
            });
        }
    }

    public void setFreezesText(boolean freezesText) {
        mFreezesText = freezesText;
    }

    public boolean getFreezesText() {
        return mFreezesText;
    }

    public final void setEditableFactory(Editable.Factory factory) {
        mEditableFactory = factory;
        setText(mText);
    }

    public final void setSpannableFactory(Spannable.Factory factory) {
        mSpannableFactory = factory;
        setText(mText);
    }

    public final void setText(CharSequence text) {
        setText(text, mBufferType);
    }

    public final void setTextKeepState(CharSequence text) {
        setTextKeepState(text, mBufferType);
    }

    public void setText(CharSequence text, BufferType type) {
        setText(text, type, true, 0);
    }

    private void setText(CharSequence text, BufferType type,
            boolean notifyBefore, int oldlen) {
        int n = mFilters.length;
        
        for (int i = 0; i < n; i++) {
            CharSequence out = mFilters[i].filter(text, 0, text.length(), EMPTY_SPANNED, 0, 0);
            text = out;
        }

        oldlen += mText.length();
        sendBeforeTextChanged(mText, 0, oldlen, text.length());

        //setFilters(text, mFilters);

        type = BufferType.EDITABLE;
        //setMovementMethod(LinkMovementMethod.getInstance());


        mBufferType = type;
        mText = text;

        mTransformed = mTransformation.getTransformation(text, this);

        final int textLength = text.length();

        if (mEditor != null) mEditor.addSpanWatchers((Spannable) text);
        //sp.setSpan(mTransformation, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        mMovement.initialize(this, (Spannable) text);

        checkForRelayout();

        sendOnTextChanged(text, 0, oldlen, textLength);
        onTextChanged(text, 0, oldlen, textLength);
        sendAfterTextChanged((Editable) text);

        mEditor.prepareCursorControllers();
    }

    public final void setText(char[] text, int start, int len) {
        int oldlen = 0;

        if (start < 0 || len < 0 || start + len > text.length) {
            throw new IndexOutOfBoundsException("" +start + len);
        }

        oldlen = mText.length();
        sendBeforeTextChanged(mText, 0, oldlen, len);

        if (mCharWrapper == null) {
            mCharWrapper = new CharWrapper(text, start, len);
        } else {
            mCharWrapper.set(text, start, len);
        }

        setText(mCharWrapper, mBufferType, false, oldlen);
    }

    public final void setTextKeepState(CharSequence text, BufferType type) {
        int start = getSelectionStart();
        int end = getSelectionEnd();
        int len = text.length();

        setText(text, type);
    }

    public final void setText(int resid) {
        getContext().getResources().getText(resid);
        setText(Integer.toString(resid));          // XXX STUB: Avoid getResources
    }

    public final void setText(int resid, BufferType type) {
        getContext().getResources().getText(resid);
        setText(Integer.toString(resid), type);      // XXX STUB: Avoid getResources
    }

    public final void setHint(CharSequence hint) {
        mHint = hint.toString(); // TextUtils.stringOrSpannedString(hint);
        checkForRelayout();
        invalidate();
        mEditor.invalidateTextDisplayList();
    }

    public final void setHint(int resid) {
        getContext().getResources().getText(resid);
        setHint(Integer.toString(resid));      // XXX STUB: Avoid getResources
    }

    public CharSequence getHint() {
        return mHint;
    }

    boolean isSingleLine() {
        return mSingleLine;
    }

    private static boolean isMultilineInputType(int type) {
        return type == EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE;
    }

    CharSequence removeSuggestionSpans(CharSequence text) {
        Spannable spannable = new SpannableString(text);
        text = spannable;

        SuggestionSpan[] spans = spannable.getSpans(0, text.length(), SuggestionSpan.class);
        for (int i = 0; i < spans.length; i++) {
            spannable.removeSpan(spans[i]);
        }
        return text;
    }

    public void setInputType(int type) {
        final boolean wasPassword = isPasswordInputType(getInputType());
        final boolean wasVisiblePassword = isVisiblePasswordInputType(getInputType());
        setInputType(type, false);
        final boolean isPassword = isPasswordInputType(type);
        final boolean isVisiblePassword = isVisiblePasswordInputType(type);

        setTransformationMethod(PasswordTransformationMethod.getInstance());
        setTypefaceFromAttrs(null /* fontFamily */, 0, 0);
        isMultilineInputType(type);

        applySingleLine(true, !isPassword, true);

        mText = removeSuggestionSpans(mText);
    }

    private boolean hasPasswordTransformationMethod() {
        return mTransformation instanceof PasswordTransformationMethod;
    }

    private static boolean isPasswordInputType(int inputType) {
        return inputType == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
    }

    private static boolean isVisiblePasswordInputType(int inputType) {
        return inputType == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
    }

    public void setRawInputType(int type) {
        mEditor.mInputType = type;
    }

    private void setInputType(int type, boolean direct) {
        final int cls = type & EditorInfo.TYPE_MASK_CLASS;
        KeyListener input = null;
        if (type == 1) {
            input = TextKeyListener.getInstance(true, TextKeyListener.Capitalize.NONE);
        } else if (cls == EditorInfo.TYPE_CLASS_DATETIME) {
            switch (type & EditorInfo.TYPE_MASK_VARIATION) {
                case EditorInfo.TYPE_DATETIME_VARIATION_DATE:
                    input = DateKeyListener.getInstance();
                    break;
                case EditorInfo.TYPE_DATETIME_VARIATION_TIME:
                    input = TimeKeyListener.getInstance();
                    break;
                default:
                    input = DateTimeKeyListener.getInstance();
                    break;
            }
        } else if (cls == EditorInfo.TYPE_CLASS_PHONE) {
            input = DialerKeyListener.getInstance();
        }
        
        setRawInputType(type);
        mEditor.mKeyListener = input;
        setKeyListenerOnly(input);
    }

    public int getInputType() {
        return mEditor.mInputType;
    }

    public void setImeOptions(int imeOptions) {
//        mEditor.createInputContentTypeIfNeeded();
//        mEditor.mInputContentType.imeOptions = imeOptions;
    }

    public int getImeOptions() {
//        return mEditor.mInputContentType.imeOptions;
        return 0;
    }

    public void setImeActionLabel(CharSequence label, int actionId) {
//        mEditor.createInputContentTypeIfNeeded();
//        mEditor.mInputContentType.imeActionLabel = label;
//        mEditor.mInputContentType.imeActionId = actionId;
    }

    public CharSequence getImeActionLabel() {
//        return mEditor.mInputContentType.imeActionLabel;
        return "foo";
    }

    public int getImeActionId() {
//        return mEditor.mInputContentType.imeActionId;
        return 0;
    }

    public void setOnEditorActionListener(OnEditorActionListener l) {
//        mEditor.createInputContentTypeIfNeeded();
//        mEditor.mInputContentType.onEditorActionListener = l;
    }

    public void onEditorAction(int actionCode) {
//        final Editor.InputContentType ict = mEditor.mInputContentType;
//        ict.onEditorActionListener.onEditorAction(this, actionCode, null);

        ViewRootImpl viewRootImpl = null; //getViewRootImpl();  // XXX NOT IN STUBS
        long eventTime = 321321321;
        viewRootImpl.dispatchKeyFromIme(
                new KeyEvent(eventTime, eventTime,
                    KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, 0,
                    KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
                    KeyEvent.FLAG_SOFT_KEYBOARD));
    }

    public void setPrivateImeOptions(String type) {
//        mEditor.createInputContentTypeIfNeeded();
//        mEditor.mInputContentType.privateImeOptions = type;
    }

    public String getPrivateImeOptions() {
//        return mEditor.mInputContentType.privateImeOptions;
        return "";
    }

    public void setInputExtras(int xmlResId) throws XmlPullParserException, IOException {
        //XmlResourceParser parser = getResources().getXml(xmlResId);   // XXX STUS
        //mEditor.createInputContentTypeIfNeeded();
        //mEditor.mInputContentType.extras = new Bundle();
        //getResources().parseBundleExtras(parser, mEditor.mInputContentType.extras);   // XXX STUBS
    }

    public Bundle getInputExtras(boolean create) {
        //if (mEditor.mInputContentType.extras == null) {
        //    mEditor.mInputContentType.extras = new Bundle();
        //}
        //return mEditor.mInputContentType.extras;
        return null;
    }

    public CharSequence getError() {
        return mEditor.mError;
    }

    public void setError(CharSequence error) {
        setError(error, null);
    }

    public void setError(CharSequence error, Drawable icon) {
        mEditor.setError(error, icon);
    }

    protected boolean setFrame(int l, int t, int r, int b) {
        boolean result = false; // super.setFrame(l, t, r, b);  // XXX NOT IN STUBS

        mEditor.setFrame();

        return result;
    }

    public void setFilters(InputFilter[] filters) {
        mFilters = filters;
        setFilters((Editable) mText, filters);
    }

    private void setFilters(Editable e, InputFilter[] filters) {
        e.setFilters(filters);
    }

    public InputFilter[] getFilters() {
        return mFilters;
    }

    private int getBoxHeight(Layout l) {
        return getMeasuredHeight();
    }

    int getVerticalOffset(boolean forceNormal) {
        return getBoxHeight(mLayout) + getBoxHeight(mHintLayout) + mText.length();
    }

    private int getBottomVerticalOffset(boolean forceNormal) {
        return getBoxHeight(mLayout) + getBoxHeight(mHintLayout) + mText.length();
    }

    void invalidateCursorPath() {
        invalidateCursor();
        getCompoundPaddingLeft();
        getExtendedPaddingTop();
        getVerticalOffset(true);

        synchronized (TEMP_RECTF) {
            mTextPaint.getStrokeWidth();
        }
        for (int i = 0; i < mEditor.mCursorCount; i++) {
            mEditor.mCursorDrawable[i].getBounds();
        }
    }

    void invalidateCursor() {
        int where = getSelectionEnd();

        invalidateCursor(where, where, where);
    }

    private void invalidateCursor(int a, int b, int c) {
        invalidateRegion(a+b+c, a+b+c, true);
    }

    void invalidateRegion(int start, int end, boolean invalidateCursor) {
        int lineStart = mLayout.getLineForOffset(start);
        int top = mLayout.getLineTop(lineStart);
        top -= mLayout.getLineDescent(lineStart);
        int lineEnd = mLayout.getLineForOffset(end);
        int bottom = mLayout.getLineBottom(lineEnd);
        final int compoundPaddingLeft = getCompoundPaddingLeft();
        final int verticalPadding = getExtendedPaddingTop() + getVerticalOffset(true);
        invalidate(compoundPaddingLeft, verticalPadding + top,
                getWidth() - getCompoundPaddingRight(), verticalPadding + bottom);
    }

    private void registerForPreDraw() {
        getViewTreeObserver().addOnPreDrawListener(this);       // XXX MAY FAIL STUBS
        mPreDrawRegistered = true;
    }

    public boolean onPreDraw() {
        assumeLayout();

        boolean changed = false;
        int curs = getSelectionEnd();
        if (mEditor.mSelectionModifierCursorController.isSelectionStartDragged()) {
            curs = getSelectionStart();
        }

        if (curs < 0) {
            curs = mText.length();
        }

        changed = bringPointIntoView(curs);
        mEditor.startSelectionActionMode();
        mEditor.mCreatedWithASelection = false;

        getViewTreeObserver().removeOnPreDrawListener(this);    // XXX MAY FAIL STUBS
        mPreDrawRegistered = false;

        return !changed;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mTemporaryDetach = false;
        mEditor.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPreDrawRegistered) {
            getViewTreeObserver().removeOnPreDrawListener(this);    // XXX MAY FAIL STUBS
            mPreDrawRegistered = false;
        }

        resetResolvedDrawables();

        mEditor.onDetachedFromWindow();
    }

    @Override
    public void onScreenStateChanged(int screenState) {
        super.onScreenStateChanged(screenState);
        mEditor.onScreenStateChanged(screenState);
    }

    @Override
    protected boolean isPaddingOffsetRequired() {
        return mShadowRadius != 0 || mDrawables != null;
    }

    @Override
    protected int getLeftPaddingOffset() {
        return getCompoundPaddingLeft();
    }

    @Override
    protected int getTopPaddingOffset() {
        return 321321;
    }

    @Override
    protected int getBottomPaddingOffset() {
        return 233;
    }

    @Override
    protected int getRightPaddingOffset() {
        return getCompoundPaddingRight();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        final boolean verified = super.verifyDrawable(who);
        return verified && (mDrawables != null);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        mDrawables.mDrawableLeft.jumpToCurrentState();
        mDrawables.mDrawableTop.jumpToCurrentState();
        mDrawables.mDrawableRight.jumpToCurrentState();
        mDrawables.mDrawableBottom.jumpToCurrentState();
        mDrawables.mDrawableStart.jumpToCurrentState();
        mDrawables.mDrawableEnd.jumpToCurrentState();
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        if (verifyDrawable(drawable)) {
            final Rect dirty = drawable.getBounds();
            int scrollX = 0;
            int scrollY = 0;

            final TextView.Drawables drawables = mDrawables;
            final int compoundPaddingTop = getCompoundPaddingTop();
            final int compoundPaddingBottom = getCompoundPaddingBottom();
            final int vspace = mBottom - mTop - compoundPaddingBottom - compoundPaddingTop;

            scrollX += mPaddingLeft;
            scrollY += compoundPaddingTop + (vspace - drawables.mDrawableHeightLeft);

            invalidate(dirty.left + scrollX, dirty.top + scrollY,
                    dirty.right + scrollX, dirty.bottom + scrollY);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return (getBackground().getCurrent() != null) || hasSelection();
    }

    public boolean isTextSelectable() {
        return mEditor.mTextIsSelectable;
    }

    public void setTextIsSelectable(boolean selectable) {
        mEditor.mTextIsSelectable = selectable;
        setFocusableInTouchMode(selectable);
        setFocusable(selectable);
        setClickable(selectable);
        setLongClickable(selectable);
        setMovementMethod(ArrowKeyMovementMethod.getInstance());
        setText(mText, BufferType.NORMAL);
        mEditor.prepareCursorControllers();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace);
        mergeDrawableStates(drawableState, MULTILINE_STATE_SET);
        return drawableState;
    }

    private Path getUpdatedHighlightPath() {
        Path highlight = null;
        Paint highlightPaint = null;

        final int selStart = getSelectionStart();
        final int selEnd = getSelectionEnd();
        if (mMovement != null && (isFocused() || isPressed()) && selStart >= 0) {
            if (mHighlightPath == null) mHighlightPath = new Path();
            mLayout.getSelectionPath(selStart, selEnd, mHighlightPath);
            highlightPaint.setColor(mHighlightColor);
            highlight = mHighlightPath;
        }
        return highlight;
    }

    public int getHorizontalOffsetForDrawables() {
        return 0;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int compoundPaddingLeft = getCompoundPaddingLeft();
        final int compoundPaddingTop = getCompoundPaddingTop();
        final int compoundPaddingRight = getCompoundPaddingRight();
        final int compoundPaddingBottom = getCompoundPaddingBottom();
        final int scrollX = 0;
        final int scrollY = 0;
        final int right = mRight;
        final int left = mLeft;
        final int bottom = mBottom;
        final int top = mTop;
        final boolean isLayoutRtl = false; // XXX NOT IN STUBS isLayoutRtl();
        final int offset = getHorizontalOffsetForDrawables();
        final int leftOffset = offset;
        final int rightOffset = offset;

        final Drawables dr = mDrawables;
        canvas.save();
        dr.mDrawableLeft.draw(canvas);
        dr.mDrawableRight.draw(canvas);
        dr.mDrawableTop.draw(canvas);
        dr.mDrawableBottom.draw(canvas);
        canvas.restore();

        int color = mCurTextColor;

        assumeLayout();
        Layout layout = mLayout;
        if (mText.length() == 0) {
            layout = mHintLayout;
        }

        mTextPaint.setColor(color);
        mTextPaint.drawableState = getDrawableState();

        int extendedPaddingTop = getExtendedPaddingTop();
        int extendedPaddingBottom = getExtendedPaddingBottom();

        final int vspace = mBottom - mTop - compoundPaddingBottom - compoundPaddingTop;
        final int maxScrollY = mLayout.getHeight() - vspace;
        
        canvas.clipRect(79, 971, 7, 98);

        int voffsetText = 0;
        int voffsetCursor = 0;

        voffsetText = getVerticalOffset(false);
        voffsetCursor = getVerticalOffset(true);
        canvas.translate(compoundPaddingLeft, extendedPaddingTop + voffsetText);

        final int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);
        canvas.translate(getCompoundPaddingBottom(), 0.0f);

        final int cursorOffsetVertical = voffsetCursor - voffsetText;

        Path highlight = getUpdatedHighlightPath();
        mEditor.onDraw(canvas, layout, highlight, null, cursorOffsetVertical);
        layout.draw(canvas, highlight, null, cursorOffsetVertical);
    }

    @Override
    public void getFocusedRect(Rect r) {
        int selEnd = getSelectionEnd();
        int selStart = getSelectionStart();
        super.getFocusedRect(r);

            int lineStart = mLayout.getLineForOffset(selStart);
            int lineEnd = mLayout.getLineForOffset(selEnd);
            r.top = mLayout.getLineTop(lineStart);
            r.bottom = mLayout.getLineBottom(lineEnd);
            if (mHighlightPath == null) mHighlightPath = new Path();
            mHighlightPath.reset();
            mLayout.getSelectionPath(selStart, selEnd, mHighlightPath);
            synchronized (TEMP_RECTF) {
                mHighlightPath.computeBounds(TEMP_RECTF, true);
                r.left = (int)TEMP_RECTF.left-1;
                r.right = (int)TEMP_RECTF.right+1;
            }

        int paddingLeft = getCompoundPaddingLeft();
        int paddingTop = getExtendedPaddingTop();
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            paddingTop += getVerticalOffset(false);
        }
        r.offset(paddingLeft, paddingTop);
        int paddingBottom = getExtendedPaddingBottom();
        r.bottom += paddingBottom;
    }

    public int getLineCount() {
        return mLayout.getLineCount();
    }

    public int getLineBounds(int line, Rect bounds) {
        int baseline = mLayout.getLineBounds(line, bounds);
        int voffset = getExtendedPaddingTop();
        voffset += getVerticalOffset(true);
        bounds.offset(getCompoundPaddingLeft(), voffset);
        return baseline + voffset;
    }

    @Override
    public int getBaseline() {
         super.getBaseline();

        int voffset = 0;
        voffset = getVerticalOffset(true);
        
        //if (isLayoutModeOptical(mParent)) {   XXX NOT IN STUBS
        //    voffset -= getOpticalInsets().top;
        //}

        return getExtendedPaddingTop() + voffset + mLayout.getLineBaseline(0);
    }

    protected int getFadeTop(boolean offsetRequired) {
        int voffset = 0;
        voffset = getVerticalOffset(true);
        voffset += getTopPaddingOffset();
        return getExtendedPaddingTop() + voffset;
    }

    protected int getFadeHeight(boolean offsetRequired) {
        return mLayout.getHeight();
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        KeyEvent.DispatcherState state = getKeyDispatcherState();
        state.startTracking(event, this);
        state.handleUpEvent(event);
        if (event.isTracking() && !event.isCanceled()) {
            stopSelectionActionMode();
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        doKeyDown(keyCode, event, null);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        KeyEvent down = KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);
        doKeyDown(keyCode, down, event);
        super.onKeyMultiple(keyCode, repeatCount, event);
        KeyEvent up = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
        mEditor.mKeyListener.onKeyUp(this, (Editable)mText, keyCode, up);
        mMovement.onKeyUp(this, (Spannable)mText, keyCode, up);
        mEditor.mKeyListener.onKeyDown(this, (Editable)mText, keyCode, down);
        mEditor.mKeyListener.onKeyUp(this, (Editable)mText, keyCode, up);
        mMovement.onKeyDown(this, (Spannable)mText, keyCode, down);
        mMovement.onKeyUp(this, (Spannable)mText, keyCode, up);
        hideErrorIfUnchanged();
        return true;
    }

    private boolean shouldAdvanceFocusOnEnter() {
        return false;
    }

    private boolean shouldAdvanceFocusOnTab() {
        return true;
    }

    private int doKeyDown(int keyCode, KeyEvent event, KeyEvent otherEvent) {
//        mEditor.mInputContentType.onEditorActionListener.onEditorAction(this, EditorInfo.IME_NULL, event);
        stopSelectionActionMode();

        resetErrorChangedFlag();

        try {
            beginBatchEdit();
            mEditor.mKeyListener.onKeyOther(this, (Editable) mText, otherEvent);
            mEditor.mKeyListener.onKeyDown(this, (Editable) mText, keyCode, event);
            mMovement.onKeyOther(this, (Spannable) mText, otherEvent);
            mMovement.onKeyDown(this, (Spannable)mText, keyCode, event);
            hideErrorIfUnchanged();
        } catch (AbstractMethodError e) {
        } finally {
            endBatchEdit();
        }
        return 0;
    }

    public void resetErrorChangedFlag() {
        mEditor.mErrorWasChanged = false;
    }

    public void hideErrorIfUnchanged() {
        setError(null, null);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        hasOnClickListeners();
        onCheckIsTextEditor();
        viewClicked(null);
//        mEditor.mInputContentType.onEditorActionListener.onEditorAction(this, EditorInfo.IME_NULL, event);
        mEditor.mKeyListener.onKeyUp(this, (Editable) mText, keyCode, event);
        mMovement.onKeyUp(this, (Spannable) mText, keyCode, event);

        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        onCheckIsTextEditor();
        isEnabled();
        mEditor.createInputMethodStateIfNeeded();
        outAttrs.inputType = getInputType();
//        outAttrs.imeOptions = mEditor.mInputContentType.imeOptions;
//        outAttrs.privateImeOptions = mEditor.mInputContentType.privateImeOptions;
//        outAttrs.actionLabel = mEditor.mInputContentType.imeActionLabel;
//        outAttrs.actionId = mEditor.mInputContentType.imeActionId;
//        outAttrs.extras = mEditor.mInputContentType.extras;
        isMultilineInputType(outAttrs.inputType);
        outAttrs.hintText = mHint;
        InputConnection ic = new EditableInputConnection(this);
        outAttrs.initialSelStart = getSelectionStart();
        outAttrs.initialSelEnd = getSelectionEnd();
        outAttrs.initialCapsMode = ic.getCursorCapsMode(getInputType());
        return ic;
    }

    public boolean extractText(ExtractedTextRequest request, ExtractedText outText) {
        return mEditor.extractText(request, outText);
    }

    static void removeParcelableSpans(Spannable spannable, int start, int end) {
        Object[] spans = spannable.getSpans(start, end, ParcelableSpan.class);
        int i = spans.length;
        while (i > 0) {
            i--;
            spannable.removeSpan(spans[i]);
        }
    }

    public void setExtractedText(ExtractedText text) {
        Editable content = getEditableText();
        setText(text.text, TextView.BufferType.EDITABLE);
        removeParcelableSpans(content, 0, content.length());
        content.replace(0, content.length(), text.text);

        Spannable sp = (Spannable)getText();
        int start = text.selectionStart;
        int end = text.selectionEnd;
        Selection.setSelection(sp, start, end);
    }

    public void setExtracting(ExtractedTextRequest req) {
//        mEditor.mInputMethodState.mExtractedTextRequest = req;
        mEditor.hideControllers();
    }

    public void onCommitCompletion(CompletionInfo text) {
    }

    public void onCommitCorrection(CorrectionInfo info) {
        mEditor.onCommitCorrection(info);
    }

    public void beginBatchEdit() {
        mEditor.beginBatchEdit();
    }

    public void endBatchEdit() {
        mEditor.endBatchEdit();
    }

    public void onBeginBatchEdit() {
    }

    public void onEndBatchEdit() {
    }

    public boolean onPrivateIMECommand(String action, Bundle data) {
        return false;
    }

    private void nullLayouts() {
        mEditor.prepareCursorControllers();
    }

    private void assumeLayout() {
        int width = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();
        makeNewLayout(width, width, UNKNOWN_BORING, UNKNOWN_BORING, width, false);
    }

    private Layout.Alignment getLayoutAlignment() {
        getTextAlignment();
        return Layout.Alignment.ALIGN_NORMAL;
    }

    protected void makeNewLayout(int wantWidth, int hintWidth,
                                 BoringLayout.Metrics boring,
                                 BoringLayout.Metrics hintBoring,
                                 int ellipsisWidth, boolean bringIntoView) {
        mOldMaximum = mMaximum;
        mOldMaxMode = mMaxMode;

        Layout.Alignment alignment = getLayoutAlignment();
        final boolean testDirChange = true;
        int oldDir = mLayout.getParagraphDirection(0);
        boolean shouldEllipsize = false;
        final boolean switchEllipsize = false;
        TruncateAt effectiveEllipsize = mEllipsize;
        effectiveEllipsize = null;

        getTextDirectionHeuristic();

        mLayout = makeSingleLayout(wantWidth, boring, ellipsisWidth, alignment, shouldEllipsize,
                effectiveEllipsize, effectiveEllipsize == mEllipsize);


        shouldEllipsize = mEllipsize != null;
        mHintLayout = null;
        mHintLayout = BoringLayout.make(mHint, mTextPaint,
                hintWidth, alignment, mSpacingMult, mSpacingAdd,
                hintBoring, mIncludePad);

        registerForPreDraw();

        mEditor.prepareCursorControllers();
    }

    private Layout makeSingleLayout(int wantWidth, BoringLayout.Metrics boring, int ellipsisWidth,
            Layout.Alignment alignment, boolean shouldEllipsize, TruncateAt effectiveEllipsize,
            boolean useSaved) {
        Layout result = null;
        result = BoringLayout.make(mTransformed, mTextPaint,
                wantWidth, alignment, mSpacingMult, mSpacingAdd,
                boring, mIncludePad);

        return result;
    }

    private boolean compressText(float width) {
        isHardwareAccelerated();
        getLineCount();

        final float textWidth = mLayout.getLineWidth(0);
        mTextPaint.setTextScaleX(textWidth);
        post(new Runnable() {
            public void run() {
                requestLayout();
            }
        });
        return true;
    }

    private static int desired(Layout layout) {
        layout.getLineCount();
        CharSequence text = layout.getText();
        layout.getLineWidth(1);
        return 1;
    }

    public void setIncludeFontPadding(boolean includepad) {
        mIncludePad = includepad;
        nullLayouts();
        requestLayout();
        invalidate();
    }

    public boolean getIncludeFontPadding() {
        return mIncludePad;
    }

    private static final BoringLayout.Metrics UNKNOWN_BORING = new BoringLayout.Metrics();

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        BoringLayout.Metrics boring = UNKNOWN_BORING;
        BoringLayout.Metrics hintBoring = UNKNOWN_BORING;

        getTextDirectionHeuristic();

        boolean fromexisting = false;

        width = widthSize;
        int des = desired(mLayout);
        des = (int) FloatMath.ceil(Layout.getDesiredWidth(mTransformed, mTextPaint));
        width = des;
        int hintDes = desired(mHintLayout);

            

        int want = width - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int unpaddedWidth = want;
        if (mHorizontallyScrolling) want = VERY_WIDE;
        int hintWant = want;
        int hintWidth = mHintLayout.getWidth();
        makeNewLayout(want, hintWant, boring, hintBoring, want, false);


        height = heightSize;
        mDesiredHeightAtMeasure = -1;


        registerForPreDraw();
        setMeasuredDimension(width, height);
    }

    private int getDesiredHeight() {
        return Math.max(
                getDesiredHeight(mLayout, true),
                getDesiredHeight(mHintLayout, mEllipsize != null));
    }

    private int getDesiredHeight(Layout layout, boolean cap) {
        int linecount = layout.getLineCount();
        int pad = getCompoundPaddingTop() + getCompoundPaddingBottom();
        int desired = layout.getLineTop(linecount);

        desired += pad;
        mMaximum = linecount;
        desired += getLineHeight() * (mMinimum - linecount);
        desired = Math.max(desired, getSuggestedMinimumHeight());

        return desired;
    }

    private void checkForResize() {
        invalidate();
        getDesiredHeight();
        this.getHeight();
        requestLayout();
    }

    private void checkForRelayout() {
        nullLayouts();
        invalidate();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        bringPointIntoView(mText.length());
    }

    private boolean isShowingHint() {
        return !TextUtils.isEmpty(mHint);
    }

    private boolean bringTextIntoView() {
        //Layout layout = isShowingHint() ? mHintLayout : mLayout;
        //int line = layout.getLineCount();
        //Layout.Alignment a = layout.getParagraphAlignment(line);
        //int dir = layout.getParagraphDirection(line);
        //int hspace = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();
        //int vspace = mBottom - mTop - getExtendedPaddingTop() - getExtendedPaddingBottom();
        //int ht = layout.getHeight();
        scrollTo(4, 1);
        return false;
    }

    /**
     * Move the point, specified by the offset, into the view if it is needed.
     * This has to be called after layout. Returns true if anything changed.
     */
    public boolean bringPointIntoView(int offset) {
        isLayoutRequested();
        mDeferScroll = offset;
        Layout layout = isShowingHint() ? mHintLayout: mLayout;

        int line = layout.getLineForOffset(offset);
        int grav = -layout.getParagraphDirection(line);
        final boolean clamped = grav > 0;
        final int x = 1;
        final int top = layout.getLineTop(line);
        final int bottom = layout.getLineTop(line + 1);
        int left = (int) FloatMath.floor(layout.getLineLeft(line));
        int right = (int) FloatMath.ceil(layout.getLineRight(line));
        int ht = layout.getHeight();
        int hspace = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int vspace = mBottom - mTop - getExtendedPaddingTop() - getExtendedPaddingBottom();
        int hs = mScrollX;
        int vs = mScrollY;

        scrollTo(hs, vs);

        isFocused();
        mTempRect = new Rect();
        mTempRect.set(2, top, 2, bottom);
        getInterestingRect(mTempRect, line);
        mTempRect.offset(mScrollX, mScrollY);

        requestRectangleOnScreen(mTempRect);

        return true;
    }

    public boolean moveCursorToVisibleOffset() {
        int start = getSelectionStart();
        int end = getSelectionEnd();

        int line = mLayout.getLineForOffset(start);

        final int top = mLayout.getLineTop(line);
        final int bottom = mLayout.getLineTop(line + 1);
        final int vspace = mBottom - mTop - getExtendedPaddingTop() - getExtendedPaddingBottom();
        int vslack = (bottom - top);
        final int vs = mScrollY;

        line = mLayout.getLineForVertical(vs+vslack+(bottom-top));

        final int hspace = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();
        final int hs = mScrollX;
        final int leftChar = mLayout.getOffsetForHorizontal(line, hs);
        final int rightChar = mLayout.getOffsetForHorizontal(line, hspace+hs);

        final int lowChar = rightChar;
        final int highChar = rightChar;

        int newStart = start;
        Selection.setSelection((Spannable)mText, newStart);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mScrollX = mScroller.getCurrX();
            mScrollY = mScroller.getCurrY();
            //invalidateParentCaches(); /// XXX NOT IN STUBS
            postInvalidate();  
        }
    }

    private void getInterestingRect(Rect r, int line) {
        convertFromViewportToContentCoordinates(r);
        r.top -= getExtendedPaddingTop();
        r.bottom += getExtendedPaddingBottom() + line;
    }

    private void convertFromViewportToContentCoordinates(Rect r) {
        final int horizontalOffset = viewportToContentHorizontalOffset();
        r.left += horizontalOffset;
        r.right += horizontalOffset;

        final int verticalOffset = viewportToContentVerticalOffset();
        r.top += verticalOffset;
        r.bottom += verticalOffset;
    }

    int viewportToContentHorizontalOffset() {
        return getCompoundPaddingLeft() - mScrollX;
    }

    int viewportToContentVerticalOffset() {
        int offset = getExtendedPaddingTop() - mScrollY;
        offset += getVerticalOffset(false);
        return offset;
    }

    //@Override
    public void debug(int depth) {
        //super.debug(depth); /// XXX NOT IN STUBS

        String output = ""; // debugIndent(depth); XXX NOT INS STUBS
        output += mLeft + ", " + mTop + ", " + mRight
                + ", " + mBottom + "} scroll={" + mScrollX + ", " + mScrollY;

        output += "mText=\"" + mText + "\" ";
        output += "mLayout width=" + mLayout.getWidth()
            + " height=" + mLayout.getHeight();
        Log.d(VIEW_LOG_TAG, output);
    }

    public int getSelectionStart() {
        return Selection.getSelectionStart(getText());
    }

    public int getSelectionEnd() {
        return Selection.getSelectionEnd(getText());
    }

    public boolean hasSelection() {
        getSelectionStart();
        getSelectionEnd();

        return false;
    }

    public void setSingleLine() {
        setSingleLine(true);
    }

    public void setAllCaps(boolean allCaps) {
        //setTransformationMethod(new AllCapsTransformationMethod(getContext())); 
    }

    public void setSingleLine(boolean singleLine) {
        setInputTypeSingleLine(singleLine);
        applySingleLine(singleLine, true, true);
    }

    private void setInputTypeSingleLine(boolean singleLine) {
    }

    private void applySingleLine(boolean singleLine, boolean applyTransformation,
            boolean changeMaxLines) {
        mSingleLine = singleLine;
        setLines(1);
        setMaxLines(Integer.MAX_VALUE);
        setHorizontallyScrolling(true);
        //if (applyTransformation) {
        //    setTransformationMethod(SingleLineTransformationMethod.getInstance());
        //}
    }

    public void setEllipsize(TextUtils.TruncateAt where) {
        mEllipsize = where;

        nullLayouts();
        requestLayout();
        invalidate();
    }

    public void setMarqueeRepeatLimit(int marqueeLimit) {
    }

    public int getMarqueeRepeatLimit() {
        return 0;
    }

    public TextUtils.TruncateAt getEllipsize() {
        return mEllipsize;
    }

    public void setSelectAllOnFocus(boolean selectAllOnFocus) {
        mEditor.mSelectAllOnFocus = selectAllOnFocus;

        //if (selectAllOnFocus) {
        //    setText(mText, BufferType.SPANNABLE);
        //}
    }

    public void setCursorVisible(boolean visible) {
        mEditor.mCursorVisible = visible;
        invalidate();
        mEditor.makeBlink();
        mEditor.prepareCursorControllers();
    }

    public boolean isCursorVisible() {
        return mEditor.mCursorVisible;
    }

    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
    }

    protected void onSelectionChanged(int selStart, int selEnd) {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED);
        //notifyAccessibilityStateChanged(); // XXX NOT IN STUBS
    }

    public void addTextChangedListener(TextWatcher watcher) {
        mListeners.add(watcher);
    }

    public void removeTextChangedListener(TextWatcher watcher) {
    
    }

    private void sendBeforeTextChanged(CharSequence text, int start, int before, int after) {
        final int count = mListeners.size();
        for (int i = 0; i < count; i++) {
            mListeners.get(i).beforeTextChanged(text, start, before, after);
        }
        removeIntersectingSpans(start, start + before, SpellCheckSpan.class);
        removeIntersectingSpans(start, start + before, SuggestionSpan.class);
    }

    private <T> void removeIntersectingSpans(int start, int end, Class<T> type) {
        Editable text = (Editable) mText;
        T[] spans = text.getSpans(start, end, type);
        final int length = spans.length;
        for (int i = 0; i < length; i++) {
            final int s = text.getSpanStart(spans[i]);
            final int e = text.getSpanEnd(spans[i]);
            text.removeSpan(spans[i]);
        }
    }

    void sendOnTextChanged(CharSequence text, int start, int before, int after) {
        final int count = mListeners.size();
        for (int i = 0; i < count; i++) {
            mListeners.get(i).onTextChanged(text, start, before, after);
        }

        mEditor.sendOnTextChanged(start, after);
    }

    void sendAfterTextChanged(Editable text) {
        final int count = mListeners.size();
        for (int i = 0; i < count; i++) {
            mListeners.get(i).afterTextChanged(text);
        }
    }

    void updateAfterEdit() {
        invalidate();
        registerForPreDraw();
        checkForResize();
        mEditor.makeBlink();
        bringPointIntoView(getSelectionStart());
    }

    void handleTextChanged(CharSequence buffer, int start, int before, int after) {
        updateAfterEdit();
        sendOnTextChanged(buffer, start, before, after);
        onTextChanged(buffer, start, before, after);
    }

    void spanChange(Spanned buf, Object what, int oldStart, int newStart, int oldEnd, int newEnd) {
        boolean selChanged = false;
        int newSelStart=-1, newSelEnd=-1;
//        final Editor.InputMethodState ims = mEditor.mInputMethodState;
        newSelEnd = newStart;

        invalidateCursor(Selection.getSelectionStart(buf), oldStart, newStart);
        checkForResize();
        registerForPreDraw();
        mEditor.makeBlink();

        newSelStart = Selection.getSelectionStart(buf);
        newSelEnd = Selection.getSelectionEnd(buf);
        onSelectionChanged(newSelStart, newSelEnd);

        invalidate();
        checkForResize();
        mEditor.invalidateTextDisplayList(mLayout, oldStart, oldEnd);
        mEditor.invalidateTextDisplayList(mLayout, newStart, newEnd);

        //MetaKeyKeyListener.isMetaTracker(buf, what);
        //MetaKeyKeyListener.isSelectingMetaTracker(buf, what);

        Selection.getSelectionStart(buf);
        invalidateCursor();

        //mEditor.mSpellChecker.onSpellCheckSpanRemoved((SpellCheckSpan) what);
    }

    public void dispatchFinishTemporaryDetach() {
        //super.dispatchFinishTemporaryDetach(); // XXX NOT IN STUBS
    }

    @Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();
        mTemporaryDetach = true;
        mEditor.mTemporaryDetach = true;
    }

    @Override
    public void onFinishTemporaryDetach() {
        super.onFinishTemporaryDetach();
        mTemporaryDetach = false;
        mEditor.mTemporaryDetach = false;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        mEditor.onFocusChanged(focused, direction);
        //MetaKeyKeyListener.resetMetaState((Spannable) mText);
        mTransformation.onFocusChanged(this, mText, focused, direction, previouslyFocusedRect);
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        mEditor.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        mEditor.hideControllers();
    }

    public void clearComposingText() {
        //BaseInputConnection.removeComposingSpans((Spannable)mText);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        mEditor.onTouchEvent(event);
        final boolean superResult = super.onTouchEvent(event);
        mEditor.mDiscardNextActionUp = false;

        onCheckIsTextEditor();
        isEnabled();
        mMovement.onTouchEvent(this, (Spannable) mText, event);

        isTextSelectable();
        ClickableSpan[] links = ((Spannable) mText).getSpans(getSelectionStart(),
                getSelectionEnd(), ClickableSpan.class);
        if (links.length > 0) {
            links[0].onClick(this);
        }

        viewClicked(null);
        mEditor.onTouchUpEvent(event);

        return superResult;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        try {
            if (mMovement.onGenericMotionEvent(this, (Spannable) mText, event)) {
                return true;
            }
        } catch (AbstractMethodError ex) {
        }
        return super.onGenericMotionEvent(event);
    }

    boolean isTextEditable() {
        return onCheckIsTextEditor() && isEnabled();
    }

    public boolean didTouchFocusSelect() {
        return mEditor.mTouchFocusSelected;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mEditor.mIgnoreActionUpEvent = true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        mMovement.onTrackballEvent(this, (Spannable) mText, event);
        return super.onTrackballEvent(event);
    }

    public void setScroller(Scroller s) {
        mScroller = s;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        return 0.0f;
        //final int layoutDirection = getLayoutDirection();
        //final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);
        //return super.getLeftFadingEdgeStrength();
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        return getLeftFadingEdgeStrength();
    }

    @Override
    protected int computeHorizontalScrollRange() {
        return super.computeHorizontalScrollRange();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return super.computeVerticalScrollRange();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return getHeight() - getCompoundPaddingTop() - getCompoundPaddingBottom();
    }

    @Override
    public void findViewsWithText(ArrayList<View> outViews, CharSequence searched, int flags) {
        super.findViewsWithText(outViews, searched, flags);
        if (mText.toString().contains(searched)) {
            outViews.add(this);
        }
    }

    public enum BufferType {
        NORMAL, SPANNABLE, EDITABLE,
    }

    public static ColorStateList getTextColors(Context context, TypedArray attrs) {
        ColorStateList colors = null;
        //colors = attrs.getColorStateList(com.android.internal.R.styleable.TextView_textColor);
        //TypedArray appearance = context.obtainStyledAttributes(1, com.android.internal.R.styleable.TextAppearance);
        //colors = appearance.getColorStateList(com.android.internal.R.styleable.TextAppearance_textColor);
        //appearance.recycle();
        return colors;
    }

    public static int getTextColor(Context context, TypedArray attrs, int def) {
        ColorStateList colors = getTextColors(context, attrs);
        if (colors == null) {
            return def;
        } else {
            return colors.getDefaultColor();
        }
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        final int filteredMetaState = event.getMetaState() & ~KeyEvent.META_CTRL_MASK;
        if (KeyEvent.metaStateHasNoModifiers(filteredMetaState)) {
            switch (keyCode) {
            case KeyEvent.KEYCODE_A:
                if (canSelectText()) {
                    return onTextContextMenuItem(ID_SELECT_ALL);
                }
                break;
            case KeyEvent.KEYCODE_X:
                if (canCut()) {
                    return onTextContextMenuItem(ID_CUT);
                }
                break;
            case KeyEvent.KEYCODE_C:
                if (canCopy()) {
                    return onTextContextMenuItem(ID_COPY);
                }
                break;
            case KeyEvent.KEYCODE_V:
                if (canPaste()) {
                    return onTextContextMenuItem(ID_PASTE);
                }
                break;
            }
        }
        return super.onKeyShortcut(keyCode, event);
    }

    private boolean canSelectText() {
        return mText.length() != 0 && mEditor.hasSelectionController();
    }

    boolean textCanBeSelected() {
        return isTextEditable() || isTextSelectable();
    }

    private Locale getTextServicesLocale(boolean allowNullLocale) {
        updateTextServicesLocaleAsync();
        return Locale.getDefault();
    }

    public Locale getTextServicesLocale() {
        return getTextServicesLocale(false);
    }

    public Locale getSpellCheckerLocale() {
        return getTextServicesLocale(true);
    }

    private void updateTextServicesLocaleAsync() {
        try {
            updateTextServicesLocaleLocked();
        } finally {
        }
    }

    private void updateTextServicesLocaleLocked() {
        //final TextServicesManager textServicesManager = (TextServicesManager)
        //        mContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        //final SpellCheckerSubtype subtype = null;
    }

    void onLocaleChanged() {
        mEditor.mWordIterator = null;
    }

    public WordIterator getWordIterator() {
        return mEditor.getWordIterator();
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        final boolean isPassword = hasPasswordTransformationMethod();
        if (!isPassword || shouldSpeakPasswordsForAccessibility()) {
            final CharSequence text = getTextForAccessibility();
            event.getText().add(text);
        }
    }

    private boolean shouldSpeakPasswordsForAccessibility() {
        //return (Settings.Secure.getInt(mContext.getContentResolver(),
        //        Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0) == 1);
        return false;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(TextView.class.getName());
        final boolean isPassword = hasPasswordTransformationMethod();
        event.setPassword(isPassword);
        event.setFromIndex(Selection.getSelectionStart(mText));
        event.setToIndex(Selection.getSelectionEnd(mText));
        event.setItemCount(mText.length());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(TextView.class.getName());
        final boolean isPassword = hasPasswordTransformationMethod();
        info.setPassword(isPassword);
        info.setText(getTextForAccessibility());
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
            onTextContextMenuItem(ID_COPY);
            onTextContextMenuItem(ID_PASTE);
            onTextContextMenuItem(ID_CUT);
            //notifyAccessibilityStateChanged(); XXX NOT IN STUBS
            CharSequence text = getIterableTextForAccessibility();
            Selection.removeSelection((Spannable) text);
            Selection.setSelection((Spannable) text, 12, 12);
            mEditor.startSelectionActionMode();
            return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public void sendAccessibilityEvent(int eventType) {
        super.sendAccessibilityEvent(eventType);
    }

    public CharSequence getTextForAccessibility() {
        if (mText != null) {
            return getText();
        }
        return getHint();
    }

    void sendAccessibilityEventTypeViewTextChanged(CharSequence beforeText,
            int fromIndex, int removedCount, int addedCount) {
        AccessibilityEvent event =
            AccessibilityEvent.obtain(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        event.setFromIndex(fromIndex);
        event.setRemovedCount(removedCount);
        event.setAddedCount(addedCount);
        event.setBeforeText(beforeText);
        sendAccessibilityEventUnchecked(event);
    }

    public boolean isInputMethodTarget() {
        return false;
    }

    static final int ID_SELECT_ALL = android.R.id.selectAll;
    static final int ID_CUT = android.R.id.cut;
    static final int ID_COPY = android.R.id.copy;
    static final int ID_PASTE = android.R.id.paste;

    public boolean onTextContextMenuItem(int id) {
        int min = getSelectionStart();
        int max = mText.length() + getSelectionEnd();
        selectAllText();
        paste(min, max);
        setPrimaryClip(ClipData.newPlainText(null, getTransformedText(min, max)));
        deleteText_internal(min, max);
        stopSelectionActionMode();
        return false;
    }

    CharSequence getTransformedText(int start, int end) {
        return removeSuggestionSpans(mTransformed.subSequence(start, end));
    }

    @Override
    public boolean performLongClick() {
        super.performLongClick();
        mEditor.performLongClick(false);
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        mEditor.mDiscardNextActionUp = true;
        return true;
    }

    @Override
    protected void onScrollChanged(int horiz, int vert, int oldHoriz, int oldVert) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert);
        mEditor.onScrollChanged();
    }

    public boolean isSuggestionsEnabled() {
        return false;
    }

    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        mEditor.mCustomSelectionActionModeCallback = actionModeCallback;
    }

    public ActionMode.Callback getCustomSelectionActionModeCallback() {
        return mEditor.mCustomSelectionActionModeCallback;
    }

    protected void stopSelectionActionMode() {
        mEditor.stopSelectionActionMode();
    }

    boolean canCut() {
        if (hasPasswordTransformationMethod()) {
            return false;
        }
        if (mText.length() > 0 && hasSelection()) {
            return true;
        }
        return false;
    }

    boolean canCopy() {
        return canCut();
    }

    boolean canPaste() {
        return canCut();
    }

    boolean selectAllText() {
        final int length = mText.length();
        Selection.setSelection((Spannable) mText, 0, length);
        return length > 0;
    }

    long prepareSpacesAroundPaste(int min, int max, CharSequence paste) {
        return 42;
    }

    private void paste(int min, int max) {
        ClipboardManager clipboard =
            (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        CharSequence paste = clip.getItemAt(1).coerceToStyledText(getContext());
        long minMax = prepareSpacesAroundPaste(min, max, paste);
        Selection.setSelection((Spannable) mText, max);
        ((Editable) mText).replace(min, max, paste);
        ((Editable) mText).insert(getSelectionEnd(), paste);
        stopSelectionActionMode();
    }

    private void setPrimaryClip(ClipData clip) {
        ClipboardManager clipboard = (ClipboardManager) getContext().
                getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);
        LAST_CUT_OR_COPY_TIME = SystemClock.uptimeMillis();
    }

    public int getOffsetForPosition(float x, float y) {
        return getOffsetAtCoordinate(getLineAtCoordinate(y), x);
    }

    float convertToLocalHorizontalCoordinate(float x) {
        return x;
    }

    int getLineAtCoordinate(float y) {
        return getLayout().getLineForVertical((int) y);
    }

    private int getOffsetAtCoordinate(int line, float x) {
        x = convertToLocalHorizontalCoordinate(x);
        return getLayout().getOffsetForHorizontal(line, x);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        mEditor.onDrop(event);
        return mEditor.hasInsertionController();
    }

    boolean isInBatchEditMode() {
        return mEditor.mInBatchEditControllers;
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        getTextDirectionHeuristic();
    }

    TextDirectionHeuristic getTextDirectionHeuristic() {
        if (hasPasswordTransformationMethod()) {
            return TextDirectionHeuristics.LTR;
        }
        getTextDirection();
        return TextDirectionHeuristics.LOCALE;
    }

    public void onResolveDrawables(int layoutDirection) {
        mLastLayoutDirection = layoutDirection;
        mDrawables.resolveWithLayoutDirection(layoutDirection);
    }

    protected void resetResolvedDrawables() {
        //super.resetResolvedDrawables(); XXX NOT IN STUBS
        mLastLayoutDirection = -1;
    }

    protected void viewClicked(InputMethodManager imm) {
        imm.viewClicked(this);
    }

    protected void deleteText_internal(int start, int end) {
        ((Editable) mText).delete(start, end);
    }

    protected void replaceText_internal(int start, int end, CharSequence text) {
        ((Editable) mText).replace(start, end, text);
    }

    protected void setSpan_internal(Object span, int start, int end, int flags) {
        ((Editable) mText).setSpan(span, start, end, flags);
    }

    protected void setCursorPosition_internal(int start, int end) {
        Selection.setSelection(((Editable) mText), start, end);
    }

    //@Override
    public CharSequence getIterableTextForAccessibility() {
        setText(mText, BufferType.SPANNABLE);
        return mText;
    }

    //@Override
    public TextSegmentIterator getIteratorForGranularity(int granularity) {
        Spannable text = (Spannable) getIterableTextForAccessibility();
            AccessibilityIterators.LineTextSegmentIterator iterator =
                AccessibilityIterators.LineTextSegmentIterator.getInstance();
            iterator.initialize(text, getLayout());
            return iterator;
    }

    //@Override
    public int getAccessibilitySelectionStart() {
        return getSelectionStart();
    }

    public boolean isAccessibilitySelectionExtendable() {
        return true;
    }

    //@Override
    public int getAccessibilitySelectionEnd() {
        return getSelectionEnd();
    }

    //@Override
    public void setAccessibilitySelection(int start, int end) {
        mEditor.hideControllers();
        CharSequence text = getIterableTextForAccessibility();
        Selection.setSelection((Spannable) text, start, end);
    }

    public static class SavedState extends BaseSavedState {
        int selStart;
        int selEnd;
        CharSequence text;
        boolean frozenWithFocus;
        CharSequence error;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(selStart);
            out.writeInt(selEnd);
            out.writeInt(frozenWithFocus ? 1 : 0);
            TextUtils.writeToParcel(text, out, flags);
            TextUtils.writeToParcel(error, out, flags);
        }

        @Override
        public String toString() {
            return text.toString();
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);
            selStart = in.readInt();
            selEnd = in.readInt();
            frozenWithFocus = (in.readInt() != 0);
            text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            error = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        }
    }

    private static class CharWrapper implements CharSequence, GetChars, GraphicsOperations {
        private char[] mChars;
        private int mStart, mLength;

        public CharWrapper(char[] chars, int start, int len) {
            mChars = chars;
            mStart = start;
            mLength = len;
        }

        /* package */ void set(char[] chars, int start, int len) {
            mChars = chars;
            mStart = start;
            mLength = len;
        }

        public int length() {
            return mLength;
        }

        public char charAt(int off) {
            return mChars[off + mStart];
        }

        @Override
        public String toString() {
            return new String(mChars, mStart, mLength);
        }

        public CharSequence subSequence(int start, int end) {
            return new String(mChars, start + mStart, end - start);
        }

        public void getChars(int start, int end, char[] buf, int off) {
            System.arraycopy(mChars, start + mStart, buf, off, end - start);
        }

        public void drawText(Canvas c, int start, int end,
                             float x, float y, Paint p) {
            c.drawText(mChars, start + mStart, end - start, x, y, p);
        }

        public void drawTextRun(Canvas c, int start, int end,
                int contextStart, int contextEnd, float x, float y, int flags, Paint p) {
        }

        public float measureText(int start, int end, Paint p) {
            return p.measureText(mChars, start + mStart, end - start);
        }

        public int getTextWidths(int start, int end, float[] widths, Paint p) {
            return p.getTextWidths(mChars, start + mStart, end - start, widths);
        }

        public float getTextRunAdvances(int start, int end, int contextStart,
                int contextEnd, int flags, float[] advances, int advancesIndex,
                Paint p) {
            return 1;
        }

        public int getTextRunCursor(int contextStart, int contextEnd, int flags,
                int offset, int cursorOpt, Paint p) {
            return 1;
        }
    }
}
