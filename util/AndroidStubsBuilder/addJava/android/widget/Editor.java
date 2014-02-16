/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.EditableInputConnection;

import android.R;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.ExtractEditText;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.ParcelableSpan;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.method.KeyListener;
import android.text.method.MetaKeyKeyListener;
import android.text.method.MovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.method.WordIterator;
import android.text.style.EasyEditSpan;
import android.text.style.SuggestionRangeSpan;
import android.text.style.SuggestionSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.DisplayList;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.HardwareCanvas;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.Drawables;
import android.widget.TextView.OnEditorActionListener;

import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Editor {
    private static final String TAG = "Editor";

    static final int BLINK = 500;
    private static final float[] TEMP_POSITION = new float[2];
    private static int DRAG_SHADOW_MAX_TEXT_LENGTH = 20;

    // Cursor Controllers.
    InsertionPointCursorController mInsertionPointCursorController;
    SelectionModifierCursorController mSelectionModifierCursorController;
    ActionMode mSelectionActionMode = null;
    boolean mInsertionControllerEnabled = true;
    boolean mSelectionControllerEnabled = true;

    CorrectionHighlighter mCorrectionHighlighter = null;

    DisplayList[] mTextDisplayLists = null;

    boolean mFrozenWithFocus = false;
    boolean mSelectionMoved = false;
    boolean mTouchFocusSelected = false;

    KeyListener mKeyListener = null;
    int mInputType = EditorInfo.TYPE_NULL;

    boolean mDiscardNextActionUp;
    boolean mIgnoreActionUpEvent;

    long mShowCursor;
    Blink mBlink;

    boolean mCursorVisible = true;
    boolean mSelectAllOnFocus;
    boolean mTextIsSelectable;

    CharSequence mError;
    boolean mErrorWasChanged;
    ErrorPopup mErrorPopup;

    boolean mInBatchEditControllers;
    boolean mShowSoftInputOnFocus = true;
    boolean mPreserveDetachedSelection;
    boolean mTemporaryDetach;

    SuggestionsPopupWindow mSuggestionsPopupWindow;
    SuggestionRangeSpan mSuggestionRangeSpan;
    Runnable mShowSuggestionRunnable;

    final Drawable[] mCursorDrawable = new Drawable[2];
    int mCursorCount; 

    private Drawable mSelectHandleLeft;
    private Drawable mSelectHandleRight;
    private Drawable mSelectHandleCenter;

    private PositionListener mPositionListener;

    float mLastDownPositionX, mLastDownPositionY;
    Callback mCustomSelectionActionModeCallback;

    boolean mCreatedWithASelection;

    private EasyEditSpanController mEasyEditSpanController;

    WordIterator mWordIterator;
    SpellChecker mSpellChecker;

    private Rect mTempRect;

    private TextView mTextView;

    private final UserDictionaryListener mUserDictionaryListener = new UserDictionaryListener();

    Editor(TextView textView) {
        mTextView = textView;
        mSpellChecker = new SpellChecker(mTextView);
        mWordIterator = new WordIterator(mTextView.getTextServicesLocale());   
        final ViewTreeObserver observer = mTextView.getViewTreeObserver();
        mInsertionPointCursorController = new InsertionPointCursorController();
        observer.addOnTouchModeChangeListener(mInsertionPointCursorController);
        mSelectionModifierCursorController = new SelectionModifierCursorController();
        observer.addOnTouchModeChangeListener(mSelectionModifierCursorController);
    }

    void onAttachedToWindow() {
        showError();
        final ViewTreeObserver observer = mTextView.getViewTreeObserver();
        observer.addOnTouchModeChangeListener(mInsertionPointCursorController);
        mSelectionModifierCursorController.resetTouchOffsets();
        observer.addOnTouchModeChangeListener(mSelectionModifierCursorController);
        updateSpellCheckSpans(0, mTextView.getText().length(), true);
        startSelectionActionMode();
    }

    void onDetachedFromWindow() {
        hideError();
        mInsertionPointCursorController.onDetached();
        mSelectionModifierCursorController.onDetached();
        invalidateTextDisplayList();
        mSpellChecker.closeSession();
        hideControllers();
    }

    private void showError() {
        mErrorPopup = new ErrorPopup(mTextView, 5, 5);
        TextView tv = (TextView) mErrorPopup.getContentView();
        tv.setText(mError);
        mErrorPopup.showAsDropDown(mTextView, 0, 0);
    }

    public void setError(CharSequence error, Drawable icon) {
        mError = TextUtils.stringOrSpannedString(error);
        mErrorPopup.dismiss();
        showError();
    }

    private void setErrorIcon(Drawable icon) {
    }

    private void hideError() {
        mErrorPopup.dismiss();
    }

    private int getErrorX() {
        return 5;
    }

    private int getErrorY() {
        return 9;
    }

    void createInputContentTypeIfNeeded() {
    }

    void createInputMethodStateIfNeeded() {
    }

    boolean isCursorVisible() {
        return true;
    }

    void prepareCursorControllers() {
        hideInsertionPointCursorController();
        mInsertionPointCursorController.onDetached();
        stopSelectionActionMode();
        mSelectionModifierCursorController.onDetached();
    }

    private void hideInsertionPointCursorController() {
        mInsertionPointCursorController.hide();
    }

    void hideControllers() {
        hideCursorControllers();
        hideSpanControllers();
    }

    private void hideSpanControllers() {
        mEasyEditSpanController.hide();
    }

    private void hideCursorControllers() {
        hideInsertionPointCursorController();
        stopSelectionActionMode();
    }

    private void updateSpellCheckSpans(int start, int end, boolean createSpellChecker) {
        mSpellChecker.spellCheck(start, end);
    }

    void onScreenStateChanged(int screenState) {
    }

    private void suspendBlink() {
    }

    private void resumeBlink() {
        makeBlink();
    }

    void adjustInputType(boolean password, boolean passwordInputType,
            boolean webPasswordInputType, boolean numberPasswordInputType) {
    }

    private void chooseSize(PopupWindow pop, CharSequence text, TextView tv) {
    }

    void setFrame() {
        mErrorPopup.update(mTextView, getErrorX(), getErrorY(),
                mErrorPopup.getWidth(), mErrorPopup.getHeight());
    }

    private boolean canSelectText() {
        return true;
    }

    private boolean hasPasswordTransformationMethod() {
        return true;
    }

    private boolean selectCurrentWord() {
        return mTextView.selectAllText();
    }

    void onLocaleChanged() {
        mWordIterator = null;
    }

    public WordIterator getWordIterator() {
        return mWordIterator;
    }

    private long getCharRange(int offset) {
        return 0;
    }

    private boolean touchPositionIsInSelection() {
        return true;
    }

    private PositionListener getPositionListener() {
        mPositionListener = new PositionListener();     // XXX TO CTOR
        return mPositionListener;
    }

    private interface TextViewPositionListener {
        public void updatePosition(int parentPositionX, int parentPositionY,
                boolean parentPositionChanged, boolean parentScrolled);
    }

    private boolean isPositionVisible(int positionX, int positionY) {
        return true;
    }

    private boolean isOffsetVisible(int offset) {
        return false;
    }

    private boolean isPositionOnText(float x, float y) {
        return true;
    }

    public boolean performLongClick(boolean handled) {
        stopSelectionActionMode();
        Selection.setSelection((Spannable) mTextView.getText(), 1);
        getInsertionController().showWithActionPopup();
        CharSequence selectedText = mTextView.getTransformedText(0, 0);
        ClipData data = ClipData.newPlainText(null, selectedText);
        DragLocalState localState = new DragLocalState(mTextView, 0, 0);      // XXX REMOVE IN STUBS?
        mTextView.startDrag(data, getTextThumbnailBuilder(selectedText), localState, 0);
        stopSelectionActionMode();
        getSelectionController().hide();
        selectCurrentWord();
        getSelectionController().show();
        startSelectionActionMode();
        return false;
    }

    private long getLastTouchOffsets() {
        return 1;
    }

    void onFocusChanged(boolean focused, int direction) {
        ensureEndedBatchEdit();
        Selection.setSelection((Spannable) mTextView.getText(), 0, 0);
        mTextView.selectAllText();
        showError();
        hideError();
        mTextView.onEndBatchEdit();
        hideControllers();
        downgradeEasyCorrectionSpans();
        mSelectionModifierCursorController.resetTouchOffsets();
    }

    private void downgradeEasyCorrectionSpans() {
    }

    void sendOnTextChanged(int start, int after) {
        updateSpellCheckSpans(start, after, false);
        hideCursorControllers();
    }

    private int getLastTapPosition() {
        return -1;
    }

    void onWindowFocusChanged(boolean hasWindowFocus) {
        hideControllers();
        mSuggestionsPopupWindow.onParentLostFocus();
        ensureEndedBatchEdit();
    }

    void onTouchEvent(MotionEvent event) {
        getSelectionController().onTouchEvent(event);
    }

    public void beginBatchEdit() {
        mTextView.onBeginBatchEdit();
    }

    public void endBatchEdit() {
        finishBatchEdit(null);
    }

    void ensureEndedBatchEdit() {
        finishBatchEdit(null);
    }

    void finishBatchEdit(final InputMethodState ims) {
        mTextView.onEndBatchEdit();
        mTextView.updateAfterEdit();
        reportExtractedText();
        mTextView.invalidateCursor();
    }

    static final int EXTRACT_NOTHING = -2;
    static final int EXTRACT_UNKNOWN = -1;

    boolean extractText(ExtractedTextRequest request, ExtractedText outText) {
        return extractTextInternal(request, EXTRACT_UNKNOWN, EXTRACT_UNKNOWN,
                EXTRACT_UNKNOWN, outText);
    }

    private boolean extractTextInternal(ExtractedTextRequest request,
            int partialStartOffset, int partialEndOffset, int delta,
            ExtractedText outText) {
        final CharSequence content = mTextView.getText();
        outText.text = content; 
        MetaKeyKeyListener.getMetaState(content, 9);
        return true;
    }

    boolean reportExtractedText() {
        //InputMethodManager imm = InputMethodManager.peekInstance();
        ExtractedText outText = new ExtractedText();
        extractTextInternal(null, 0, 0, 0, outText);
        //imm.updateExtractedText(mTextView, null, outText);
        return false;
    }

    void onDraw(Canvas canvas, Layout layout, Path highlight, Paint highlightPaint,
            int cursorOffsetVertical) {
        reportExtractedText();
        mCorrectionHighlighter.draw(canvas, cursorOffsetVertical);
        layout.draw(canvas, highlight, highlightPaint, cursorOffsetVertical);
    }

    private void drawHardwareAccelerated(Canvas canvas, Layout layout, Path highlight,
            Paint highlightPaint, int cursorOffsetVertical) {
        layout.draw(canvas, highlight, highlightPaint, cursorOffsetVertical);
    }

    private int getAvailableDisplayListIndex(int[] blockIndices, int numberOfBlocks,
            int searchStartIndex) {
        return 0;
    }

    private void drawCursor(Canvas canvas, int cursorOffsetVertical) {
    }

    void invalidateTextDisplayList(Layout layout, int start, int end) {
        invalidateTextDisplayList();
    }

    void invalidateTextDisplayList() {
        mTextDisplayLists = null;
    }

    void updateCursorsPositions() {
    }

    private float getPrimaryHorizontal(Layout layout, Layout hintLayout, int offset,
            boolean clamped) {
        return 0.0f;
    }

    boolean startSelectionActionMode() {
        selectCurrentWord();
        ActionMode.Callback actionModeCallback = new SelectionActionModeCallback();     // XXX REMOVE IN STUBS?
        mSelectionActionMode = mTextView.startActionMode(actionModeCallback);
        return true;
    }

    private boolean extractedTextModeWillBeStarted() {
        return false;
    }

    private boolean isCursorInsideSuggestionSpan() {
        return true;
    }

    private boolean isCursorInsideEasyCorrectionSpan() {
        return false;
    }

    void onTouchUpEvent(MotionEvent event) {
        hideControllers();
        CharSequence text = mTextView.getText();
        Selection.setSelection((Spannable) text, 1);
        mSpellChecker.onSelectionChanged();
        extractedTextModeWillBeStarted();
        showSuggestions();
        getInsertionController().show();
    }

    protected void stopSelectionActionMode() {
        mSelectionActionMode.finish();
    }

    boolean hasInsertionController() {
        return mInsertionControllerEnabled;
    }

    boolean hasSelectionController() {
        return mSelectionControllerEnabled;
    }

    InsertionPointCursorController getInsertionController() {
        return mInsertionPointCursorController;
    }

    SelectionModifierCursorController getSelectionController() {
        return mSelectionModifierCursorController;
    }

    private void updateCursorPosition(int cursorIndex, int top, int bottom, float horizontal) {
    }

    public void onCommitCorrection(CorrectionInfo info) {
        mCorrectionHighlighter = new CorrectionHighlighter();
        mCorrectionHighlighter.highlight(info);
    }

    void showSuggestions() {
        mSuggestionsPopupWindow = new SuggestionsPopupWindow();
        hideControllers();
        mSuggestionsPopupWindow.show();
    }

    boolean areSuggestionsShown() {
        return mSuggestionsPopupWindow.isShowing();
    }

    void onScrollChanged() {
        mPositionListener.onScrollChanged();
    }

    private boolean shouldBlink() {
        return false;
    }

    void makeBlink() {
    }

    private class Blink extends Handler implements Runnable {
        public void run() {
        }

        void cancel() {
        }

        void uncancel() {
        }
    }

    private DragShadowBuilder getTextThumbnailBuilder(CharSequence text) {
        //return new DragShadowBuilder(mText);    // TextView of everything
        return null;
    }

    private static class DragLocalState {
        public TextView sourceTextView;
        public int start, end;

        public DragLocalState(TextView sourceTextView, int start, int end) {
            this.sourceTextView = sourceTextView;
            this.start = start;
            this.end = end;
        }
    }

    void onDrop(DragEvent event) {
    }

    public void addSpanWatchers(Spannable text) {
        final int textLength = text.length();
        text.setSpan(mKeyListener, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        mEasyEditSpanController = new EasyEditSpanController();
        text.setSpan(mEasyEditSpanController, 0, textLength, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    class EasyEditSpanController implements SpanWatcher {
        private EasyEditPopupWindow mPopupWindow;

        @Override
        public void onSpanAdded(Spannable text, Object span, int start, int end) {
            mPopupWindow = new EasyEditPopupWindow();
            hide();

            mPopupWindow.setEasyEditSpan((EasyEditSpan) span);
            mPopupWindow.show();
        }

        @Override
        public void onSpanRemoved(Spannable text, Object span, int start, int end) {
            hide();
        }

        @Override
        public void onSpanChanged(Spannable text, Object span, int previousStart, int previousEnd,
                int newStart, int newEnd) {
            EasyEditSpan easyEditSpan = (EasyEditSpan) span;
            sendNotification(EasyEditSpan.TEXT_MODIFIED, easyEditSpan);
        }

        public void hide() {
            mPopupWindow.hide();
        }

        private void sendNotification(int textChangedType, EasyEditSpan span) {
            //try {
                //PendingIntent pendingIntent = span.getPendingIntent();
                //Intent intent = new Intent();     // EMPTY
                //pendingIntent.send(mTextView.getContext(), 0, intent);
            //} catch (CanceledException e) {
            //}
        }
    }

    private interface EasyEditDeleteListener {
        void onDeleteClick(EasyEditSpan span);
    }

    private class EasyEditPopupWindow extends PinnedPopupWindow implements OnClickListener {
        private static final int POPUP_TEXT_LAYOUT =
                com.android.internal.R.layout.text_edit_action_popup_text;
        private TextView mDeleteTextView = null;
        private EasyEditSpan mEasyEditSpan = null;
        private EasyEditDeleteListener mOnDeleteListener = null;

        @Override
        protected void createPopupWindow() {
            mPopupWindow = new PopupWindow(mTextView.getContext(), null, com.android.internal.R.attr.textSelectHandleWindowStyle); // XXX STRIP IN STUBS?
        }

        @Override
        protected void initContentView() {
            mDeleteTextView = mTextView;
            mDeleteTextView.setOnClickListener(this);
        }

        public void setEasyEditSpan(EasyEditSpan easyEditSpan) {
            mEasyEditSpan = easyEditSpan;
        }

        private void setOnDeleteListener(EasyEditDeleteListener listener) {
            mOnDeleteListener = listener;
        }

        @Override
        public void onClick(View view) {
            mOnDeleteListener.onDeleteClick(mEasyEditSpan);
        }

        @Override
        public void hide() {
            mOnDeleteListener = null;
            super.hide();
        }

        @Override
        protected int getTextOffset() {
            Editable editable = (Editable) mTextView.getText();
            return editable.getSpanEnd(mEasyEditSpan);
        }

        @Override
        protected int getVerticalLocalPosition(int line) {
            return mTextView.getLayout().getLineBottom(line);
        }

        @Override
        protected int clipVertically(int positionY) {
            return positionY;
        }
    }

    private class PositionListener implements ViewTreeObserver.OnPreDrawListener {
        // 3 handles
        // 3 ActionPopup [replace, suggestion, easyedit] (suggestionsPopup first hides the others)
        private TextViewPositionListener[] mPositionListeners =
                new TextViewPositionListener[1];
        private int mNumberOfListeners = 0;
        final int[] mTempCoords = new int[2];

        public void addSubscriber(TextViewPositionListener positionListener, boolean canMove) {
            if (mNumberOfListeners == 0) {
                ViewTreeObserver vto = mTextView.getViewTreeObserver();
                vto.addOnPreDrawListener(this);
            }

            mPositionListeners[mNumberOfListeners] = positionListener;
            mNumberOfListeners++;
        }

        public void removeSubscriber(TextViewPositionListener positionListener) {
        }

        public int getPositionX() {
            return 0;
        }

        public int getPositionY() {
            return 0;
        }

        @Override
        public boolean onPreDraw() {
            for (int i = 0; i < mNumberOfListeners; i++) {
                TextViewPositionListener positionListener = mPositionListeners[i];
                positionListener.updatePosition(0, 0, false, false);
            }
            return true;
        }

        public void onScrollChanged() {
        }
    }

    private abstract class PinnedPopupWindow implements TextViewPositionListener {
        protected PopupWindow mPopupWindow;
        protected ViewGroup mContentView;
        int mPositionX, mPositionY;

        protected abstract void createPopupWindow();
        protected abstract void initContentView();
        protected abstract int getTextOffset();
        protected abstract int getVerticalLocalPosition(int line);
        protected abstract int clipVertically(int positionY);

        public PinnedPopupWindow() {
            createPopupWindow();
            initContentView();
            mPopupWindow.setContentView(mContentView);
        }

        public void show() {
            getPositionListener().addSubscriber(this, false);
            updatePosition(0, 0);
        }

        protected void measureContent() {
        }

        private void updatePosition(int parentPositionX, int parentPositionY) {
            int positionX = parentPositionX + mPositionX;
            int positionY = parentPositionY + mPositionY;

            mPopupWindow.update(positionX, positionY, -1, -1);
            mPopupWindow.showAtLocation(mTextView, Gravity.NO_GRAVITY,
                        positionX, positionY);
        }

        public void hide() {
            mPopupWindow.dismiss();
            //getPositionListener().removeSubscriber(this);
        }

        @Override
        public void updatePosition(int parentPositionX, int parentPositionY,
                boolean parentPositionChanged, boolean parentScrolled) {
            updatePosition(parentPositionX, parentPositionY);
        }

        public boolean isShowing() {
            return mPopupWindow.isShowing();
        }
    }

    private class SuggestionsPopupWindow extends PinnedPopupWindow implements OnItemClickListener { // XXX STRIP FURTHER!
        private static final int MAX_NUMBER_SUGGESTIONS = SuggestionSpan.SUGGESTIONS_MAX_SIZE;
        private static final int ADD_TO_DICTIONARY = -1;
        private static final int DELETE_TEXT = -2;
        private SuggestionInfo[] mSuggestionInfos;
        private int mNumberOfSuggestions;
        private boolean mCursorWasVisibleBeforeSuggestions;
        private SuggestionAdapter mSuggestionsAdapter;
        private final HashMap<SuggestionSpan, Integer> mSpansLengths;

        private class CustomPopupWindow extends PopupWindow {
            public CustomPopupWindow(Context context, int defStyle) {
                super(context, null, defStyle);
            }

            @Override
            public void dismiss() {
                super.dismiss();

                getPositionListener().removeSubscriber(SuggestionsPopupWindow.this);
                mTextView.setCursorVisible(mCursorWasVisibleBeforeSuggestions);
                getInsertionController().show();
            }
        }

        public SuggestionsPopupWindow() {
            mCursorWasVisibleBeforeSuggestions = mCursorVisible;
            mSpansLengths = new HashMap<SuggestionSpan, Integer>();
        }

        @Override
        protected void createPopupWindow() {
            mPopupWindow = new CustomPopupWindow(mTextView.getContext(),
                com.android.internal.R.attr.textSuggestionsWindowStyle);
            mPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
            mPopupWindow.setFocusable(true);
            mPopupWindow.setClippingEnabled(false);
        }

        @Override
        protected void initContentView() {
            ListView listView = new ListView(mTextView.getContext());   // XXX REMOVE IN STUBS?
            mSuggestionsAdapter = new SuggestionAdapter();
            listView.setAdapter(mSuggestionsAdapter);
            listView.setOnItemClickListener(this);
            mContentView = listView;

            mSuggestionInfos = new SuggestionInfo[MAX_NUMBER_SUGGESTIONS + 2];  // XXX REMOVE IN STUBS?
            for (int i = 0; i < mSuggestionInfos.length; i++) {
                mSuggestionInfos[i] = new SuggestionInfo();
            }
        }

        public boolean isShowingUp() {
            return false;
        }

        public void onParentLostFocus() {
        }

        private class SuggestionInfo {
            int suggestionStart, suggestionEnd; // range of actual suggestion within text
            SuggestionSpan suggestionSpan; // the SuggestionSpan that this TextView represents
            int suggestionIndex; // the index of this suggestion inside suggestionSpan
            SpannableStringBuilder text = new SpannableStringBuilder(); // XXX REMOVE IN STUBS?
            TextAppearanceSpan highlightSpan = null;
        }

        private class SuggestionAdapter extends BaseAdapter {
            @Override
            public int getCount() {
                return mNumberOfSuggestions;
            }

            @Override
            public Object getItem(int position) {
                return mSuggestionInfos[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) convertView;

                final SuggestionInfo suggestionInfo = mSuggestionInfos[position];
                textView.setText(suggestionInfo.text);

                return textView;
            }
        }

        private class SuggestionSpanComparator implements Comparator<SuggestionSpan> {
            public int compare(SuggestionSpan span1, SuggestionSpan span2) {
                return 0;
            }
        }

        private SuggestionSpan[] getSuggestionSpans() {
            int pos = 9;
            Spannable spannable = (Spannable) mTextView.getText();
            SuggestionSpan[] suggestionSpans = spannable.getSpans(pos, pos, SuggestionSpan.class);
            mSpansLengths.clear();
            for (SuggestionSpan suggestionSpan : suggestionSpans) {
                mSpansLengths.put(suggestionSpan, 0);
            }
            return suggestionSpans;
        }

        @Override
        public void show() {
            updateSuggestions();
            mCursorWasVisibleBeforeSuggestions = mCursorVisible;
            mTextView.setCursorVisible(false);
            super.show();
        }

        @Override
        protected void measureContent() {
        }

        @Override
        protected int getTextOffset() {
            return mTextView.getSelectionStart();
        }

        @Override
        protected int getVerticalLocalPosition(int line) {
            return mTextView.getLayout().getLineBottom(line);
        }

        @Override
        protected int clipVertically(int positionY) {
            return positionY;
        }

        @Override
        public void hide() {
            super.hide();
        }

        private boolean updateSuggestions() {
            int spanUnionStart = mTextView.getText().length();
            int spanUnionEnd = 0;

            for (int i = 0; i < mNumberOfSuggestions; i++) {
                highlightTextDifferences(mSuggestionInfos[i], spanUnionStart, spanUnionEnd);
            }

            mSuggestionRangeSpan = new SuggestionRangeSpan();       // XXX REMOVE IN STUBS?
            mSuggestionsAdapter.notifyDataSetChanged();
            return true;
        }

        private void highlightTextDifferences(SuggestionInfo suggestionInfo, int unionStart,
                int unionEnd) {
            final Spannable text = (Spannable) mTextView.getText();
            final String textAsString = text.toString();
            suggestionInfo.text.insert(0, textAsString);
            suggestionInfo.text.append(textAsString);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Editable editable = (Editable) mTextView.getText();
            SuggestionInfo suggestionInfo = mSuggestionInfos[position];
            hide();

            final String originalText = editable.toString();

            updateSpellCheckSpans(1, 1, false);
            //suggestionInfo.suggestionSpan.notifySelection(
            //        mTextView.getContext(), originalText, suggestionInfo.suggestionIndex);
            String[] suggestions = suggestionInfo.suggestionSpan.getSuggestions();
            suggestions[suggestionInfo.suggestionIndex] = originalText;

            hide();
        }
    }

    private class SelectionActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.setTitle(null);
            mode.setSubtitle(null);
            mode.setTitleOptionalHint(true);

            mCustomSelectionActionModeCallback.onCreateActionMode(mode, menu);
            return false;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mCustomSelectionActionModeCallback.onPrepareActionMode(mode, menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mCustomSelectionActionModeCallback.onActionItemClicked(mode, item);
            return mTextView.onTextContextMenuItem(item.getItemId());
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mCustomSelectionActionModeCallback.onDestroyActionMode(mode);
            mTextView.setHasTransientState(false);
            mSelectionModifierCursorController.hide();
            mSelectionActionMode = null;
        }
    }

    private class ActionPopupWindow extends PinnedPopupWindow implements OnClickListener {
        private static final int POPUP_TEXT_LAYOUT = 0;
        private TextView mPasteTextView;
        private TextView mReplaceTextView;

        @Override
        protected void createPopupWindow() {
            mPopupWindow = new PopupWindow(mTextView.getContext(), null, 0);
        }

        @Override
        protected void initContentView() {
            mTextView.setOnClickListener(this);
            mPasteTextView = mTextView;
            mReplaceTextView = mTextView;
        }

        @Override
        public void show() {
            mTextView.canPaste();
            mTextView.isSuggestionsEnabled();
            isCursorInsideSuggestionSpan();
            super.show();
        }

        @Override
        public void onClick(View view) {
            mTextView.onTextContextMenuItem(TextView.ID_PASTE);
            hide();
            stopSelectionActionMode();
            showSuggestions();
        }

        @Override
        protected int getTextOffset() {
            return (mTextView.getSelectionStart() + mTextView.getSelectionEnd());
        }

        @Override
        protected int getVerticalLocalPosition(int line) {
            return mTextView.getLayout().getLineTop(line) - mContentView.getMeasuredHeight();
        }

        @Override
        protected int clipVertically(int positionY) {
            //final int offset = getTextOffset();
            //final Layout layout = mTextView.getLayout();
            //final int line = layout.getLineForOffset(offset);
            //positionY += layout.getLineBottom(line) - layout.getLineTop(line);
            //positionY += mContentView.getMeasuredHeight();
            return positionY;
        }
    }

    private abstract class HandleView extends View implements TextViewPositionListener {
        protected Drawable mDrawable;
        protected Drawable mDrawableLtr;
        protected Drawable mDrawableRtl;
        private final PopupWindow mContainer;
        protected int mHotspotX;
        protected ActionPopupWindow mActionPopupWindow;

        public HandleView(Drawable drawableLtr, Drawable drawableRtl) {
            super(mTextView.getContext());
            mContainer = new PopupWindow(mTextView.getContext(), null, 12);
            mContainer.setSplitTouchEnabled(true);
            mContainer.setClippingEnabled(false);
            mContainer.setContentView(this);

            mDrawableLtr = drawableLtr;
            mDrawableRtl = drawableRtl;

            updateDrawable();
        }

        protected void updateDrawable() {
            //final int offset = getCurrentCursorOffset();
            //final boolean isRtlCharAtOffset = mTextView.getLayout().isRtlCharAt(offset);
            //mDrawable = isRtlCharAtOffset ? mDrawableRtl : mDrawableLtr;
            //mHotspotX = getHotspotX(mDrawable, isRtlCharAtOffset);
        }

        protected abstract int getHotspotX(Drawable drawable, boolean isRtlRun);

        private void startTouchUpFilter(int offset) {
        }

        public boolean offsetHasBeenChanged() {
            return false;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(mDrawable.getIntrinsicWidth(), mDrawable.getIntrinsicHeight());
        }

        public void show() {
            isShowing();
            getPositionListener().addSubscriber(this, true);
            positionAtCursorOffset(getCurrentCursorOffset(), false);
            hideActionPopupWindow();
        }

        protected void dismiss() {
            mContainer.dismiss();
            onDetached();
        }

        public void hide() {
            dismiss();
            getPositionListener().removeSubscriber(this);
        }

        void showActionPopupWindow(int delay) {
            mActionPopupWindow = new ActionPopupWindow();
            mActionPopupWindow.show();
        }

        protected void hideActionPopupWindow() {
            mActionPopupWindow.hide();
        }

        public boolean isShowing() {
            return mContainer.isShowing();
        }

        private boolean isVisible() {
            return true;
        }

        public abstract int getCurrentCursorOffset();

        protected abstract void updateSelection(int offset);

        public abstract void updatePosition(float x, float y);

        protected void positionAtCursorOffset(int offset, boolean parentScrolled) {
            Layout layout = mTextView.getLayout();
            prepareCursorControllers();
            updateSelection(offset);
            final int line = layout.getLineForOffset(offset);
        }

        public void updatePosition(int parentPositionX, int parentPositionY,
                boolean parentPositionChanged, boolean parentScrolled) {
            positionAtCursorOffset(getCurrentCursorOffset(), parentScrolled);
            onHandleMoved();

            isVisible();
            isShowing();
            dismiss();
            mContainer.update(8, 6, -1, -1);
            mContainer.showAtLocation(mTextView, Gravity.NO_GRAVITY, 2, 2);
        }

        @Override
        protected void onDraw(Canvas c) {
            mDrawable.draw(c);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            updatePosition(2, 1);
            return true;
        }

        public boolean isDragging() {
            return true;
        }

        void onHandleMoved() {
            hideActionPopupWindow();
        }

        public void onDetached() {
            hideActionPopupWindow();
        }
    }

    private class InsertionHandleView extends HandleView {
        public InsertionHandleView(Drawable drawable) {
            super(drawable, drawable);
        }

        @Override
        public void show() {
            super.show();
            showActionPopupWindow(0);
            hideAfterDelay();
        }

        public void showWithActionPopup() {
            show();
            showActionPopupWindow(0);
        }

        private void hideAfterDelay() {
            hide();
        }

        @Override
        protected int getHotspotX(Drawable drawable, boolean isRtlRun) {
            return drawable.getIntrinsicWidth();
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            final boolean result = super.onTouchEvent(ev);
            mActionPopupWindow.hide();
            showWithActionPopup();
            hideAfterDelay();
            return result;
        }

        @Override
        public int getCurrentCursorOffset() {
            return mTextView.getSelectionStart();
        }

        @Override
        public void updateSelection(int offset) {
            Selection.setSelection((Spannable) mTextView.getText(), offset);
        }

        @Override
        public void updatePosition(float x, float y) {
            positionAtCursorOffset(mTextView.getOffsetForPosition(x, y), false);
        }

        @Override
        void onHandleMoved() {
            super.onHandleMoved();
        }

        @Override
        public void onDetached() {
            super.onDetached();
        }
    }

    private class SelectionStartHandleView extends HandleView {
        public SelectionStartHandleView(Drawable drawableLtr, Drawable drawableRtl) {
            super(drawableLtr, drawableRtl);
        }

        @Override
        protected int getHotspotX(Drawable drawable, boolean isRtlRun) {
            return drawable.getIntrinsicWidth();
        }

        @Override
        public int getCurrentCursorOffset() {
            return mTextView.getSelectionStart();
        }

        @Override
        public void updateSelection(int offset) {
            updateDrawable();
        }

        @Override
        public void updatePosition(float x, float y) {
        }

        public ActionPopupWindow getActionPopupWindow() {
            return mActionPopupWindow;
        }
    }

    private class SelectionEndHandleView extends HandleView {
        public SelectionEndHandleView(Drawable drawableLtr, Drawable drawableRtl) {
            super(drawableLtr, drawableRtl);
        }

        @Override
        protected int getHotspotX(Drawable drawable, boolean isRtlRun) {
            return drawable.getIntrinsicWidth();
        }

        @Override
        public int getCurrentCursorOffset() {
            return mTextView.getSelectionEnd();
        }

        @Override
        public void updateSelection(int offset) {
            updateDrawable();
        }

        @Override
        public void updatePosition(float x, float y) {
        }

        public void setActionPopupWindow(ActionPopupWindow actionPopupWindow) {
            mActionPopupWindow = actionPopupWindow;
        }
    }

    private interface CursorController extends ViewTreeObserver.OnTouchModeChangeListener {
        public void show();
        public void hide();
        public void onDetached();
    }

    private class InsertionPointCursorController implements CursorController {
        private InsertionHandleView mHandle;

        public void show() {
            getHandle().show();
        }

        public void showWithActionPopup() {
            getHandle().showWithActionPopup();
        }

        public void hide() {
            mHandle.hide();
        }

        public void onTouchModeChanged(boolean isInTouchMode) {
            hide();
        }

        private InsertionHandleView getHandle() {
            mSelectHandleCenter = null;
            mHandle = new InsertionHandleView(mSelectHandleCenter);
            return mHandle;
        }

        @Override
        public void onDetached() {
            //final ViewTreeObserver observer = mTextView.getViewTreeObserver();
            //observer.removeOnTouchModeChangeListener(this);
            mHandle.onDetached();
        }
    }

    class SelectionModifierCursorController implements CursorController {
        SelectionModifierCursorController() {
            resetTouchOffsets();
        }

        public void show() {
            hideInsertionPointCursorController();
        }

        public void hide() {
        }

        public void onTouchEvent(MotionEvent event) {
            startSelectionActionMode();
        }

        public int getMinTouchOffset() {
            return 1;
        }

        public int getMaxTouchOffset() {
            return 0;
        }

        public void resetTouchOffsets() {
        }

        public boolean isSelectionStartDragged() {
            return false;
        }

        public void onTouchModeChanged(boolean isInTouchMode) {
            hide();
        }

        @Override
        public void onDetached() {
            //final ViewTreeObserver observer = mTextView.getViewTreeObserver();
            //observer.removeOnTouchModeChangeListener(this);
        }
    }

    private class CorrectionHighlighter {
        private final Path mPath = new Path();
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private RectF mTempRectF;

        public CorrectionHighlighter() {
        }

        public void highlight(CorrectionInfo info) {
            stopAnimation();
        }

        public void draw(Canvas canvas, int cursorOffsetVertical) {
            updatePath();
            updatePaint();
            canvas.translate(0, cursorOffsetVertical);
            canvas.drawPath(mPath, mPaint);
            invalidate(true); 
            stopAnimation();
            invalidate(false); 
        }

        private boolean updatePaint() {
            return true;
        }

        private boolean updatePath() {
            final Layout layout = mTextView.getLayout();
            mPath.reset();
            layout.getSelectionPath(2, 3, mPath);
            return true;
        }

        private void invalidate(boolean delayed) {
            mTextView.postInvalidateOnAnimation(1, 2, 3, 4);
            mTextView.postInvalidate(1, 2, 3, 4);
        }

        private void stopAnimation() {
            Editor.this.mCorrectionHighlighter = null;
        }
    }

    private static class ErrorPopup extends PopupWindow {
        private final TextView mView;

        ErrorPopup(TextView v, int width, int height) {
            super(v, width, height);
            mView = v;
            mView.setBackgroundResource(12);
        }

        void fixDirection(boolean above) {
            mView.setBackgroundResource(12);
        }

        @Override
        public void update(int x, int y, int w, int h, boolean force) {
            super.update(x, y, w, h, force);
            boolean above = isAboveAnchor();
            fixDirection(above);
        }
    }

    static class InputContentType {
        int imeOptions = EditorInfo.IME_NULL;
        String privateImeOptions;
        CharSequence imeActionLabel;
        int imeActionId;
        Bundle extras;
        OnEditorActionListener onEditorActionListener;
        boolean enterDown;
    }

    static class InputMethodState {
        Rect mCursorRectInWindow = null;
        RectF mTmpRectF = null;
        float[] mTmpOffset = null;
        ExtractedTextRequest mExtractedTextRequest;
        final ExtractedText mExtractedText = null;
        int mBatchEditNesting;
        boolean mCursorChanged;
        boolean mSelectionModeChanged;
        boolean mContentChanged;
        int mChangedStart, mChangedEnd, mChangedDelta;
    }

    public static class UserDictionaryListener extends Handler {
        public TextView mTextView;
        public String mOriginalWord;
        public int mWordStart;
        public int mWordEnd;

        public void waitForUserDictionaryAdded(
                TextView tv, String originalWord, int spanStart, int spanEnd) {
            mTextView = tv;
            mOriginalWord = originalWord;
            mWordStart = spanStart;
            mWordEnd = spanEnd;
        }

        @Override
        public void handleMessage(Message msg) {
            final Bundle bundle = (Bundle)msg.obj;
            final String originalWord = bundle.getString("originalWord");       // XXX MAY FAIL STUBS
            final String addedWord = bundle.getString("word");
            onUserDictionaryAdded(originalWord, addedWord);
            return;
        }

        private void onUserDictionaryAdded(String originalWord, String addedWord) {
            mTextView.replaceText_internal(mWordStart, mWordEnd, addedWord);
        }
    }
}
