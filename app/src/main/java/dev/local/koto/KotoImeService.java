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
import dev.local.koto.core.KeyboardLayout;
import dev.local.koto.core.FlickSensitivity;
import dev.local.koto.core.LearningCoordinator;
import dev.local.koto.core.LearningData;
import dev.local.koto.core.ConversionCandidate;
import dev.local.koto.core.CandidateList;
import dev.local.koto.core.PhraseConverter;
import dev.local.koto.core.PhraseRequests;
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
    private TextView status,preedit;
    private HorizontalScrollView candidateScroll;
    private List<ConversionCandidate> candidates = Collections.emptyList();
    private String candidateReading="";
    private LearningCoordinator learning;
    private LearningCoordinator.State candidateState;
    private ConversionCandidate previewSource;
    private PhraseRequests phraseRequests;
    private String phraseReading="";
    private List<String> phraseCandidates=Collections.emptyList();
    private final Runnable learningChanged=()-> { if (!destroyed && board!=null) { refreshCandidates(); updateEnterKey(); } };
    private int selected = -1, rowHeight,flickDistance;
    private boolean haptics;
    private SharedPreferences controls;
    private final SharedPreferences.OnSharedPreferenceChangeListener controlsChanged=(prefs,key)-> {
        if (!destroyed && "incognito".equals(key)) {
            syncPrivacy();
            if (board!=null) render();
        }
    };
    private KeyView enterKey;
    private EditorAction action = EditorAction.from(0,0,null);
    private ConnectionEditor editor;
    private int symbolPage;

    @Override public void onCreate() {
        super.onCreate();
        controls=getSharedPreferences("controls",MODE_PRIVATE);
        controls.registerOnSharedPreferenceChangeListener(controlsChanged);
        learning=LearningStore.get(this); learning.addListener(learningChanged);
        session.choices(this::recordChoice);
        loader.execute(() -> {
            SkkDictionary loaded = null;
            try { loaded = SkkDictionary.load(getAssets().open("skk.tsv")); }
            catch (IOException ignored) { /* Show a static failure message; never log input or exceptions. */ }
            final SkkDictionary result = loaded;
            main.post(() -> {
                if (destroyed) return;
                dictionary = result; dictionaryFailed = result == null;
                if (result!=null) phraseRequests=new PhraseRequests(new PhraseConverter(result),loader,
                        task->main.post(task),(reading,words)->{
                            if (!destroyed && CandidateList.allowsPhrases(session)
                                    && reading.equals(session.text()) && reading.equals(phraseReading)) {
                                phraseCandidates=words; refreshCandidates(); updateEnterKey();
                            }
                        });
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
        syncPrivacy();
        if (session.policy().learning && !session.incognito()) learning.maintain();
        action = editor.action(); symbolPage = 0;
        clearCandidateState();
        if (board != null) render();
    }
    @Override public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        syncPrivacy();
        if (board != null) render();
    }
    private boolean syncPrivacy() {
        boolean before=session.incognito();
        session.syncIncognito(()->LearningStore.privateMode(controls));
        if (before!=session.incognito()) {
            cancelKeyGestures(board); clearCandidateState();
        }
        return true;
    }
    @Override public void onFinishInputView(boolean finishingInput) {
        // Unconfirmed local input is cancelled on hide; no text is sent implicitly.
        cancelKeyGestures(board);
        session.discard(); clearCandidateState();
        super.onFinishInputView(finishingInput);
    }
    @Override public void onFinishInput() {
        cancelKeyGestures(board);
        session.finish(); editor = null; clearCandidateState(); super.onFinishInput();
    }
    private void clearCandidateState() {
        candidates = Collections.emptyList(); selected = -1;
        candidateReading=""; candidateState=null;
        // A same-session relayout may keep the local preview. Retain its
        // provenance so expiry/clear checks still run before rendering/commit.
        if (session.preview().isEmpty()) previewSource=null;
        phraseReading=""; phraseCandidates=Collections.emptyList();
        if (phraseRequests!=null) phraseRequests.cancel();
        if (candidateRow != null) candidateRow.removeAllViews();
        updatePreedit();
    }
    @Override public void onUpdateSelection(int oldStart, int oldEnd, int newStart, int newEnd, int start, int end) {
        super.onUpdateSelection(oldStart, oldEnd, newStart, newEnd, start, end);
        if (editor != null) editor.updateSelection(oldStart,oldEnd,newStart,newEnd,start,end);
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        // Preserve the local draft while relayout keeps this input session active.
        cancelKeyGestures(board);
        session.relayout(); clearCandidateState();
        super.onConfigurationChanged(configuration);
        if (board != null) render();
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
        KeyView view = new KeyView(this, label, flick, listener, haptics, repeat, accent,flickDistance);
        row.addView(view, new LinearLayout.LayoutParams(0, -1, weight));
        return view;
    }
    private void render() {
        if (board == null) return;
        syncPrivacy();
        SharedPreferences prefs = controls;
        haptics = prefs.getBoolean("haptics", false);
        flickDistance=FlickSensitivity.clamp(prefs.getInt("flick_distance",FlickSensitivity.DEFAULT_DP));
        rowHeight = Math.max(44, Math.min(76, prefs.getInt("row_height", 58)));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) rowHeight = 44;
        board.removeAllViews();
        status = new TextView(this); status.setTextColor(ACCENT); status.setTextSize(11);
        status.setGravity(Gravity.CENTER_VERTICAL); status.setPadding(dp(8), 0, 0, 0);
        board.addView(status, new LinearLayout.LayoutParams(-1, dp(22)));
        preedit=new TextView(this); preedit.setTextColor(INK); preedit.setTextSize(18);
        preedit.setMaxLines(2); preedit.setVerticalScrollBarEnabled(true);
        preedit.setMovementMethod(android.text.method.ScrollingMovementMethod.getInstance());
        preedit.setGravity(Gravity.CENTER_VERTICAL); preedit.setPadding(dp(8),0,dp(8),0);
        preedit.setBackgroundColor(0xff243542);
        board.addView(preedit,new LinearLayout.LayoutParams(-1,dp(rowHeight==44 ? 36 : 48)));
        candidateScroll = new HorizontalScrollView(this); candidateScroll.setHorizontalScrollBarEnabled(false);
        candidateRow = new LinearLayout(this); candidateRow.setOrientation(LinearLayout.HORIZONTAL);
        candidateScroll.addView(candidateRow, new HorizontalScrollView.LayoutParams(-2, -1));
        board.addView(candidateScroll, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout toolbar = row(board, 44);
        key(toolbar, "←", null, d -> { if (prepareEdit()) session.cursor(-1); changed(); }, 1, true, false);
        key(toolbar, "→", null, d -> { if (prepareEdit()) session.cursor(1); changed(); }, 1, true, false);
        KeyView cancel=key(toolbar,"取消",null,d->{ session.discard(); changed(); },1,false,false);
        cancel.setContentDescription("キーボード内の未確定入力をすべて取り消す");
        key(toolbar,session.incognito() ? "私用 ON" : "私用 OFF",null,d -> {
            prefs.edit().putBoolean("incognito",!LearningStore.privateMode(prefs)).apply();
            syncPrivacy(); render();
        },1.7f,false,session.incognito());
        key(toolbar, "設定", null, d -> startActivity(new Intent(this, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)), 1, false, false);
        keyArea = new LinearLayout(this); keyArea.setOrientation(LinearLayout.VERTICAL);
        board.addView(keyArea, new LinearLayout.LayoutParams(-1, -2));
        renderKeys(); refreshCandidates(); updateEnterKey();
    }
    private void renderKeys() {
        if (keyArea == null) return;
        keyArea.removeAllViews();
        if (session.mode() == InputSession.Mode.KANA) {
            for (int r=0;r<4;r++) layoutRow(KeyboardLayout.kanaRow(r));
        } else if (session.mode() == InputSession.Mode.ENGLISH) {
            for (int r=0;r<4;r++) layoutRow(KeyboardLayout.englishRow(r));
        } else {
            for (int row=0;row<3;row++) symbols(SymbolLayout.row(symbolPage,row));
            LinearLayout bottom = row(keyArea, rowHeight);
            key(bottom,symbolPage==0 ? "記号 2" : "記号 1",null,d -> {
                symbolPage=(symbolPage+1)%SymbolLayout.pages(); renderKeys(); updateEnterKey();
            },1.5f,false,false);
            key(bottom,"かな",null,d->changeMode(InputSession.Mode.KANA),1.2f,false,false);
            key(bottom,"ABC",null,d->changeMode(InputSession.Mode.ENGLISH),1.2f,false,false);
            for (String s : SymbolLayout.bottom()) character(bottom,s,1);
            key(bottom, "空白", null, d -> { session.type(" "); changed(); }, 3, false, false);
            enterKey = key(bottom, enterLabel(), null, d -> enter(), 2, false, true);
        }
        updateEnterKey();
    }
    private void layoutRow(KeyboardLayout.Key[] keys) {
        LinearLayout line=row(keyArea,rowHeight);
        for (KeyboardLayout.Key spec:keys) {
            KeyView.Listener press;
            switch (spec.role) {
                case SPACER: line.addView(new View(this),new LinearLayout.LayoutParams(0,-1,spec.weight)); continue;
                case CHAR: character(line,spec.value,spec.weight); continue;
                case KANA:
                    if (spec.kana>=0) {
                        key(line,spec.value,FlickLayout.key(spec.kana),d->{
                            if (prepareEdit()) session.type(FlickLayout.kana(spec.kana,d)); changed();
                        },spec.weight,false,false); continue;
                    }
                    press=d->changeMode(InputSession.Mode.KANA); break;
                case ENGLISH: press=d->changeMode(InputSession.Mode.ENGLISH); break;
                case SYMBOLS: press=d->changeMode(InputSession.Mode.SYMBOLS); break;
                case PICKER: press=d->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker(); break;
                case HIDE: press=d->requestHideSelf(0); break;
                case MODIFY: press=d->{ if (prepareEdit()) session.modify(); changed(); }; break;
                case DELETE: press=d->{ session.backspace(); changed(); }; break;
                case CONVERT: press=d->cycleCandidate(); break;
                case SPACE: press=d->space(); break;
                case SHIFT: press=d->{ session.shift(); renderKeys(); }; break;
                default: press=d->enter();
            }
            boolean repeat=spec.role==KeyboardLayout.Role.DELETE;
            String label=spec.role==KeyboardLayout.Role.ENTER ? enterLabel()
                    : spec.role==KeyboardLayout.Role.SHIFT && session.shifted() ? "⇧ ON" : spec.value;
            KeyView view=key(line,label,null,press,spec.weight,repeat,spec.role==KeyboardLayout.Role.ENTER);
            if (spec.role==KeyboardLayout.Role.ENTER) enterKey=view;
        }
    }
    private void character(LinearLayout line, String text, float weight) {
        String label = session.shifted() ? text.toUpperCase(java.util.Locale.ROOT) : text;
        key(line, label, null, d -> {
            boolean shifted = session.shifted();
            if (prepareEdit()) session.type(text); changed();
            if (shifted) renderKeys();
        }, weight, false, false);
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
    private void changeMode(InputSession.Mode mode) { if (prepareEdit()) session.mode(mode); symbolPage=0; render(); }
    private void updateEnterKey() {
        if (enterKey == null) return;
        enterKey.label(enterLabel());
        enterKey.setContentDescription(session.text().isEmpty() ? action.description() : "キーボード内の未確定入力を入力欄へ確定");
    }
    private void changed() {
        refreshCandidates();
        // Retain the active key's pointer across delete/cursor repeats.
        updateEnterKey();
    }
    private void cycleCandidate() {
        if (!session.canRequestConversion() || !prepareEdit()) return;
        if (!session.requestConversion()) return;
        refreshCandidates();
        if (candidates.isEmpty()) return;
        int next = (indexWord(session.preview()) + 1) % candidates.size();
        ConversionCandidate choice=candidates.get(next);
        if (session.preview(choice.word,session.revision())) previewSource=choice;
        changed();
    }
    private void enter() {
        if (prepareEdit()) session.enter(); changed();
    }
    private void space() {
        boolean shifted=session.shifted();
        if (prepareEdit()) {
            if (session.mode()==InputSession.Mode.KANA) session.space(); else session.type(" ");
        }
        changed(); if (shifted!=session.shifted()) renderKeys();
    }
    private int indexWord(String word) { for (int i=0;i<candidates.size();i++) if (candidates.get(i).word.equals(word)) return i; return -1; }
    private boolean currentLearned(ConversionCandidate candidate,String reading) {
        if (!session.policy().learning || session.incognito() || LearningStore.privateMode(controls)) return false;
        for (LearningData.Entry e:learning.state().suggest(reading,session.policy(),LearningStore.privateMode(controls),System.currentTimeMillis()))
            if (e.word.equals(candidate.word) && e.reading.equals(candidate.learningReading)) return true;
        return false;
    }
    private boolean prepareEdit() {
        if (!syncPrivacy()) return false;
        if (session.preview().isEmpty()) previewSource=null;
        if (previewSource!=null && previewSource.learned && !currentLearned(previewSource,session.text())) {
            if (!session.cancelPreview()) return false;
            previewSource=null;
            refreshCandidates();
        }
        return true;
    }
    private void recordChoice(String reading,String word,FieldPolicy policy) {
        if (!policy.learning || LearningStore.privateMode(controls)) return;
        if (!candidateReading.equals(reading) || candidateState!=learning.state()) return;
        int index=indexWord(word); if (index<0) return;
        ConversionCandidate candidate=candidates.get(index);
        if (candidate.eligible && dictionary!=null && dictionary.exactChoice(candidate.learningReading,word)
                && (!candidate.learned || currentLearned(candidate,reading)))
            learning.record(candidate.learningReading,word,policy,LearningStore.privateMode(controls),true,candidateState.generation);
    }
    private void refreshCandidates() {
        if (candidateRow == null || status == null) return;
        boolean privacyReady=syncPrivacy();
        candidateRow.removeAllViews();
        LearningCoordinator.State learnedState=learning.state();
        // The projection first cancels an expired/cleared learned preview.
        // Text and accessibility must describe the same next confirmation.
        updatePreedit();
        String learningLabel=learnedState.busy ? "学習データ処理中" : learnedState.failed ? "保存エラー · 学習停止"
                : session.incognito() || !session.policy().learning ? "私用 · 学習 OFF"
                : learnedState.days==0 ? "学習 OFF" : "端末内学習 " + learnedState.days + "日";
        status.setText(session.limitReached() ? "Koto · 256字まで · 確定してから続けてください"
            : editor!=null && editor.commitRejected() ? session.policy().candidates
                ? "Koto · 確定できません · 読みはキーボード内に保持" : "Koto · 入力欄へ送れませんでした"
            : session.policy().password ? "Koto · パスワード · 候補 OFF"
            : !session.policy().candidates ? "Koto · 直接入力 · 候補 OFF"
            : dictionaryFailed ? "Koto · 辞書を読み込めません · かな入力"
            : dictionary == null ? "Koto · 辞書を準備中 · 学習 OFF"
            : session.policy().explicitConversion ? "Koto · 手動辞書変換 · 自動候補 OFF · 学習 OFF"
            : session.policy().noPersonalizedLearning ? "Koto · プライベート入力 · 学習 OFF"
            : "Koto · 未確定 " + session.text().codePointCount(0,session.text().length()) + "/256 · " + learningLabel);
        String reading = session.text();
        if (!privacyReady || !session.hasCandidates()) {
            clearCandidateState();
            TextView hint = new TextView(this); hint.setTextColor(0xffb5c8c9); hint.setTextSize(13);
            hint.setPadding(dp(8), 0, 0, 0); hint.setGravity(Gravity.CENTER_VERTICAL);
            hint.setText(session.policy().password ? "候補表示・入力の保存はありません"
                : !session.policy().candidates ? session.policy().numeric
                    ? "数字・電話・日時は直接入力 · 辞書変換・学習 OFF" : "入力欄の種類が不明 · 直接入力のみ"
                : session.policy().ascii ? "英字優先 · 日本語は「かな」→読み→「変換」 · 学習 OFF"
                : session.policy().noSuggestions ? "自動候補 OFF · 読みを入力して「変換」 · 学習 OFF"
                : session.mode() != InputSession.Mode.KANA ? "英字・記号もキーボード内で編集 · 確定で入力欄へ"
                : "キーボード内の読み → 候補タップ / 確定で入力欄へ");
            candidateRow.addView(hint, new LinearLayout.LayoutParams(-2, -1)); return;
        }
        if (session.policy().explicitConversion) {
            phraseReading=""; phraseCandidates=Collections.emptyList();
            if (phraseRequests!=null) phraseRequests.cancel();
        } else if (phraseRequests!=null && !reading.equals(phraseReading)) {
            phraseReading=reading; phraseCandidates=Collections.emptyList(); phraseRequests.request(reading);
        }
        candidates=CandidateList.from(session,dictionary,
                ()->learnedState.suggest(reading,session.policy(),LearningStore.privateMode(controls),System.currentTimeMillis()),phraseCandidates);
        candidateReading=reading; candidateState=learnedState;
        if (session.policy().learning && !session.incognito()
                && learnedState.count(System.currentTimeMillis())<learnedState.entries.size()) learning.maintain();
        selected = indexWord(session.preview());
        final long revision = session.revision();
        for (int i = 0; i < candidates.size(); i++) {
            final ConversionCandidate candidate=candidates.get(i);
            final String choice = candidate.word, snapshot = reading;
            final LearningCoordinator.State stateAtRender=learnedState;
            Button button = new Button(this); button.setAllCaps(false);
            button.setText(choice+(candidate.phrase ? "\n分割" : candidate.learned ? "\n端末内" : ""));
            button.setContentDescription(choice+(candidate.phrase ? "、辞書による分割候補。学習保存しません" : ""));
            button.setTextSize(candidate.phrase || candidate.learned ? 15 : 18);
            button.setTypeface(Typeface.DEFAULT, i == selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setTextColor(i == selected ? BACKGROUND : INK);
            button.setBackgroundColor(i == selected ? ACCENT : BACKGROUND);
            button.setMinWidth(dp(64)); button.setMinimumWidth(dp(64)); button.setPadding(dp(12), 0, dp(12), 0);
            button.setFilterTouchesWhenObscured(true);
            button.setOnClickListener(v -> {
                if (prepareEdit() && snapshot.equals(session.text())
                        && (!candidate.learned || stateAtRender==learning.state() && currentLearned(candidate,snapshot))
                        && session.select(choice,revision)) changed();
                else refreshCandidates();
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
    private void updatePreedit() {
        if (preedit==null) return;
        boolean currentPreview=previewSource==null || !previewSource.learned || currentLearned(previewSource,session.text());
        InputSession.Preedit shown=session.preedit(currentPreview);
        if (!currentPreview) previewSource=null;
        if (!shown.visible) {
            preedit.setText(shown.text); preedit.setContentDescription(shown.description); preedit.setVisibility(View.GONE); return;
        }
        preedit.setVisibility(View.VISIBLE);
        preedit.setText(shown.text.isEmpty() ? "未確定の読み · 確定で入力欄へ" : shown.text);
        preedit.setContentDescription(shown.description);
        long revision=session.revision();
        preedit.post(()->{
            if (preedit!=null && revision==session.revision() && preedit.getLayout()!=null)
                preedit.scrollTo(0,Math.max(0,preedit.getLayout().getHeight()-preedit.getHeight()));
        });
    }
    @Override public void onDestroy() {
        destroyed = true; learning.removeListener(learningChanged); session.choices(null);
        controls.unregisterOnSharedPreferenceChangeListener(controlsChanged);
        cancelKeyGestures(board); session.finish(); editor = null; clearCandidateState();
        loader.shutdownNow(); main.removeCallbacksAndMessages(null);
        dictionary = null; board = null; keyArea = null; candidateRow = null; frame = null;
        super.onDestroy();
    }
}
