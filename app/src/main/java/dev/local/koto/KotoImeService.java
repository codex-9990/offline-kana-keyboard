/* SPDX-License-Identifier: GPL-2.0-or-later */
package dev.local.koto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import dev.local.koto.core.FieldPolicy;
import dev.local.koto.core.FlickLayout;
import dev.local.koto.core.InputSession;
import dev.local.koto.core.SkkDictionary;
import dev.local.koto.core.EditorAction;
import dev.local.koto.core.SymbolLayout;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class KotoImeService extends InputMethodService {
    private static final int BACKGROUND = 0xff17232c, INK = 0xfff4f4ee, ACCENT = 0xff8ddfc7;
    private final InputSession session = new InputSession();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private SkkDictionary dictionary;
    private boolean destroyed, dictionaryFailed;
    private FrameLayout frame;
    private LinearLayout board, candidateRow, keyArea;
    private TextView status;
    private HorizontalScrollView candidateScroll;
    private List<String> candidates = Collections.emptyList();
    private int selected = -1, rowHeight;
    private boolean haptics;
    private KeyView enterKey;
    private EditorAction action = EditorAction.from(0,0,null);
    private ConnectionEditor editor;
    private int symbolPage;

    @Override public void onCreate() {
        super.onCreate();
        loader.execute(() -> {
            SkkDictionary loaded = null;
            try { loaded = SkkDictionary.load(getAssets().open("skk.tsv")); }
            catch (IOException ignored) { /* Show a static failure message; never log input or exceptions. */ }
            final SkkDictionary result = loaded;
            main.post(() -> {
                if (destroyed) return;
                dictionary = result; dictionaryFailed = result == null;
                if (board != null) refreshCandidates();
            });
        });
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override public View onCreateInputView() {
        frame = new FrameLayout(this); frame.setBackgroundColor(BACKGROUND);
        frame.setFilterTouchesWhenObscured(true);
        frame.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        board = new LinearLayout(this); board.setOrientation(LinearLayout.VERTICAL);
        board.setMotionEventSplittingEnabled(false); board.setPadding(dp(4), dp(4), dp(4), dp(8));
        int width = Math.min(getResources().getDisplayMetrics().widthPixels, dp(640));
        frame.addView(board, new FrameLayout.LayoutParams(width, -2, Gravity.CENTER_HORIZONTAL));
        render(); return frame;
    }
    @Override public boolean onEvaluateFullscreenMode() { return false; }
    @Override public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        cancelKeyGestures(board);
        editor = new ConnectionEditor(getCurrentInputConnection(), info);
        session.start(editor, FieldPolicy.from(info.inputType, info.imeOptions));
        action = editor.action(); symbolPage = 0;
        selected = -1; candidates = Collections.emptyList();
        if (board != null) render();
    }
    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (board != null) render();
    }
    @Override public void onFinishInputView(boolean finishingInput) {
        // Finish the host's composing span without retaining its text while hidden.
        cancelKeyGestures(board);
        session.selectionMovedOutsideComposition(); clearCandidateState();
        super.onFinishInputView(finishingInput);
    }
    @Override public void onFinishInput() {
        cancelKeyGestures(board);
        session.finish(); editor = null; clearCandidateState(); super.onFinishInput();
    }
    private void clearCandidateState() {
        candidates = Collections.emptyList(); selected = -1;
        if (candidateRow != null) candidateRow.removeAllViews();
    }
    @Override public void onUpdateSelection(int oldStart, int oldEnd, int newStart, int newEnd, int start, int end) {
        super.onUpdateSelection(oldStart, oldEnd, newStart, newEnd, start, end);
        if (editor != null) editor.selection(newStart, newEnd);
        if (!session.text().isEmpty() && (start < 0 || newStart != end || newEnd != end)) {
            session.selectionMovedOutsideComposition(); clearCandidateState(); refreshCandidates();
        }
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        // The current composing span is already in the host; preserve it as committed text.
        cancelKeyGestures(board);
        session.selectionMovedOutsideComposition(); clearCandidateState();
        super.onConfigurationChanged(configuration);
    }
    private void cancelKeyGestures(View view) {
        if (view instanceof KeyView) ((KeyView) view).cancelGesture();
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) cancelKeyGestures(group.getChildAt(i));
        }
    }
    private LinearLayout row(LinearLayout parent, int height) {
        LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.HORIZONTAL);
        view.setMotionEventSplittingEnabled(false);
        parent.addView(view, new LinearLayout.LayoutParams(-1, dp(height))); return view;
    }
    private KeyView key(LinearLayout row, String label, String[] flick, KeyView.Listener listener,
                     float weight, boolean repeat, boolean accent) {
        KeyView view = new KeyView(this, label, flick, listener, haptics, repeat, accent);
        row.addView(view, new LinearLayout.LayoutParams(0, -1, weight));
        return view;
    }
    private void render() {
        if (board == null) return;
        SharedPreferences prefs = getSharedPreferences("controls", MODE_PRIVATE);
        haptics = prefs.getBoolean("haptics", false);
        rowHeight = Math.max(44, Math.min(76, prefs.getInt("row_height", 58)));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) rowHeight = 44;
        board.removeAllViews();
        status = new TextView(this); status.setTextColor(ACCENT); status.setTextSize(11);
        status.setGravity(Gravity.CENTER_VERTICAL); status.setPadding(dp(8), 0, 0, 0);
        board.addView(status, new LinearLayout.LayoutParams(-1, dp(22)));
        candidateScroll = new HorizontalScrollView(this); candidateScroll.setHorizontalScrollBarEnabled(false);
        candidateRow = new LinearLayout(this); candidateRow.setOrientation(LinearLayout.HORIZONTAL);
        candidateScroll.addView(candidateRow, new HorizontalScrollView.LayoutParams(-2, -1));
        board.addView(candidateScroll, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout toolbar = row(board, 44);
        key(toolbar, "かな", null, d -> changeMode(InputSession.Mode.KANA), 1, false, session.mode() == InputSession.Mode.KANA);
        key(toolbar, "ABC", null, d -> changeMode(InputSession.Mode.ENGLISH), 1, false, session.mode() == InputSession.Mode.ENGLISH);
        key(toolbar, "123", null, d -> changeMode(InputSession.Mode.SYMBOLS), 1, false, session.mode() == InputSession.Mode.SYMBOLS);
        key(toolbar, "←", null, d -> { session.cursor(-1); changed(); }, 1, true, false);
        key(toolbar, "→", null, d -> { session.cursor(1); changed(); }, 1, true, false);
        key(toolbar, "切替", null, d -> ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker(), 1, false, false);
        key(toolbar, "設定", null, d -> startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)), 1, false, false);
        keyArea = new LinearLayout(this); keyArea.setOrientation(LinearLayout.VERTICAL);
        board.addView(keyArea, new LinearLayout.LayoutParams(-1, -2));
        renderKeys(); refreshCandidates(); updateEnterKey();
    }
    private void renderKeys() {
        if (keyArea == null) return;
        keyArea.removeAllViews();
        if (session.mode() == InputSession.Mode.KANA) {
            for (int r = 0; r < 4; r++) {
                LinearLayout line = row(keyArea, rowHeight);
                for (int c = 0; c < 3; c++) {
                    final int position = r * 3 + c;
                    if (position == 9) key(line, "゛小", null, d -> { session.modify(); changed(); }, 1, false, false);
                    else key(line, FlickLayout.kana(position, 0), FlickLayout.key(position),
                            d -> { session.type(FlickLayout.kana(position, d)); changed(); }, 1, false, false);
                }
                switch (r) {
                    case 0: key(line, "⌫", null, d -> { session.backspace(); changed(); }, .85f, true, false); break;
                    case 1: key(line, "変換", null, d -> cycleCandidate(), .85f, false, false); break;
                    case 2: key(line, "空白", null, d -> space(), .85f, false, false); break;
                    default: enterKey = key(line, enterLabel(), null, d -> enter(), .85f, false, true);
                }
            }
        } else if (session.mode() == InputSession.Mode.ENGLISH) {
            letters(SymbolLayout.englishRow(0)); letters(SymbolLayout.englishRow(1));
            LinearLayout third = row(keyArea, rowHeight);
            key(third, session.shifted() ? "⇧ ON" : "⇧", null, d -> { session.shift(); renderKeys(); }, 1.3f, false, session.shifted());
            for (char c : SymbolLayout.englishRow(2).toCharArray()) character(third, String.valueOf(c), 1);
            key(third, "⌫", null, d -> { session.backspace(); changed(); }, 1.3f, true, false);
            LinearLayout bottom = row(keyArea, rowHeight);
            character(bottom, "@", 1); character(bottom, ",", 1);
            key(bottom, "space", null, d -> {
                boolean shifted = session.shifted(); session.type(" "); changed();
                if (shifted != session.shifted()) renderKeys();
            }, 4, false, false);
            character(bottom, ".", 1);
            enterKey = key(bottom, enterLabel(), null, d -> enter(), 2, false, true);
        } else {
            for (int row=0;row<3;row++) symbols(SymbolLayout.row(symbolPage,row));
            LinearLayout bottom = row(keyArea, rowHeight);
            key(bottom,symbolPage==0 ? "記号 2" : "記号 1",null,d -> {
                symbolPage=(symbolPage+1)%SymbolLayout.pages(); renderKeys(); updateEnterKey();
            },2,false,false);
            for (String s : SymbolLayout.bottom()) character(bottom,s,1);
            key(bottom, "空白", null, d -> { session.type(" "); changed(); }, 3, false, false);
            enterKey = key(bottom, enterLabel(), null, d -> enter(), 2, false, true);
        }
        updateEnterKey();
    }
    private void character(LinearLayout line, String text, float weight) {
        String label = session.shifted() ? text.toUpperCase(java.util.Locale.ROOT) : text;
        key(line, label, null, d -> {
            boolean shifted = session.shifted();
            session.type(text); changed();
            if (shifted) renderKeys();
        }, weight, false, false);
    }
    private void letters(String letters) {
        LinearLayout line = row(keyArea, rowHeight);
        for (char c : letters.toCharArray()) character(line, String.valueOf(c), 1);
    }
    private void symbols(String[] values) {
        LinearLayout line = row(keyArea, rowHeight);
        for (String text : values) {
            if (text.equals("⌫")) key(line, text, null, d -> { session.backspace(); changed(); }, 1, true, false);
            else character(line, text, 1);
        }
    }
    private String enterLabel() {
        if (!session.text().isEmpty()) return "確定";
        return action.label();
    }
    private void changeMode(InputSession.Mode mode) { session.mode(mode); symbolPage=0; render(); }
    private void updateEnterKey() {
        if (enterKey == null) return;
        enterKey.label(enterLabel());
        enterKey.setContentDescription(session.text().isEmpty() ? action.description() : "変換中の単語を確定");
    }
    private void changed() {
        refreshCandidates();
        // Retain the active key's pointer across delete/cursor repeats.
        updateEnterKey();
    }
    private void cycleCandidate() {
        if (!session.hasCandidates() || candidates.isEmpty()) return;
        int next = (candidates.indexOf(session.preview()) + 1) % candidates.size();
        session.preview(candidates.get(next), session.revision());
        changed();
    }
    private void enter() {
        session.enter(); changed();
    }
    private void space() {
        session.space(); changed();
    }
    private void refreshCandidates() {
        if (candidateRow == null || status == null) return;
        candidateRow.removeAllViews();
        status.setText(session.policy().password ? "Koto · パスワード · 候補 OFF"
            : session.policy().noPersonalizedLearning ? "Koto · プライベート入力 · 学習 OFF"
            : !session.policy().candidates ? "Koto · 直接入力 · 候補 OFF"
            : dictionaryFailed ? "Koto · 辞書を読み込めません · かな入力"
            : dictionary == null ? "Koto · 辞書を準備中 · 学習 OFF"
            : "Koto · SKK 単語変換 · 学習 OFF");
        String reading = session.text();
        if (!session.hasCandidates()) {
            clearCandidateState();
            TextView hint = new TextView(this); hint.setTextColor(0xffb5c8c9); hint.setTextSize(13);
            hint.setPadding(dp(8), 0, 0, 0); hint.setGravity(Gravity.CENTER_VERTICAL);
            hint.setText(session.policy().password ? "候補表示・入力の保存はありません"
                : !session.policy().candidates ? "この入力欄では候補を使いません"
                : session.mode() != InputSession.Mode.KANA ? "英字・記号は直接入力。かなで単語変換できます"
                : "単語を入力 → 候補をタップ / 変換 → 確定");
            candidateRow.addView(hint, new LinearLayout.LayoutParams(-2, -1)); return;
        }
        String katakana = FlickLayout.katakana(reading);
        candidates = dictionary == null
                ? (reading.equals(katakana) ? Collections.singletonList(reading) : Arrays.asList(reading, katakana))
                : dictionary.candidates(reading);
        selected = candidates.indexOf(session.preview());
        final long revision = session.revision();
        for (int i = 0; i < candidates.size(); i++) {
            final String choice = candidates.get(i), snapshot = reading;
            Button button = new Button(this); button.setAllCaps(false); button.setText(choice);
            button.setTextSize(18); button.setTypeface(Typeface.DEFAULT, i == selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setTextColor(i == selected ? BACKGROUND : INK);
            button.setBackgroundColor(i == selected ? ACCENT : BACKGROUND);
            button.setMinWidth(dp(64)); button.setMinimumWidth(dp(64)); button.setPadding(dp(12), 0, dp(12), 0);
            button.setFilterTouchesWhenObscured(true);
            button.setOnClickListener(v -> {
                if (snapshot.equals(session.text()) && session.select(choice, revision)) changed();
            });
            candidateRow.addView(button, new LinearLayout.LayoutParams(-2, -1));
        }
        if (selected < 0) candidateScroll.scrollTo(0, 0);
        else {
            int position = selected;
            candidateScroll.post(() -> {
                if (revision == session.revision() && candidateRow != null && position < candidateRow.getChildCount())
                    candidateScroll.smoothScrollTo(candidateRow.getChildAt(position).getLeft(), 0);
            });
        }
    }
    @Override public void onDestroy() {
        destroyed = true; cancelKeyGestures(board); session.finish(); editor = null; clearCandidateState();
        loader.shutdownNow(); main.removeCallbacksAndMessages(null);
        dictionary = null; board = null; keyArea = null; candidateRow = null; frame = null;
        super.onDestroy();
    }
}
