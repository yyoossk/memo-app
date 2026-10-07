package com.yyoossk.memo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

/** やる事リスト（1 項目ずつ端末標準のアラームを設定できる） */
public class TodoActivity extends Activity {
    private static final int REQ_VOICE = 1;

    private NoteStore store;
    private Note note;
    private EditText titleEdit;
    private TextView progress;
    private LinearLayout itemsBox;
    private ScrollView scroll;
    private EditText input;
    private boolean changed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = NoteStore.get(this);
        String id = savedInstanceState != null ? savedInstanceState.getString("id")
                : getIntent().getStringExtra("id");
        note = store.find(id);
        if (note == null) {
            note = Note.create(Note.TODO);
            if (id != null) note.id = id;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        LinearLayout bar = Ui.topBar(this);
        bar.addView(Ui.spacer(this));
        ImageButton share = Ui.icon(this, R.drawable.ic_share, "送る・印刷", Ui.INK);
        share.setOnClickListener(v -> ShareUtil.showMenu(this, note, this::save));
        bar.addView(share);
        ImageButton more = Ui.icon(this, R.drawable.ic_more, "その他", Ui.INK);
        more.setOnClickListener(v -> showMore());
        bar.addView(more);
        root.addView(bar, Ui.matchWrap());

        titleEdit = new EditText(this);
        titleEdit.setHint("リストの名前（例：今日のやる事）");
        titleEdit.setText(note.title);
        titleEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleEdit.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleEdit.setTextColor(Ui.INK);
        titleEdit.setHintTextColor(0xFF9AA19A);
        titleEdit.setBackground(null);
        titleEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        titleEdit.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), 0);
        root.addView(titleEdit, Ui.matchWrap());

        progress = Ui.text(this, "", 13, Ui.BODY, false);
        progress.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 10));
        root.addView(progress, Ui.matchWrap());

        scroll = new ScrollView(this);
        itemsBox = new LinearLayout(this);
        itemsBox.setOrientation(LinearLayout.VERTICAL);
        itemsBox.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 16));
        scroll.addView(itemsBox, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout add = new LinearLayout(this);
        add.setOrientation(LinearLayout.HORIZONTAL);
        add.setGravity(Gravity.CENTER_VERTICAL);
        add.setBackground(Ui.round(this, Ui.CARD, 16));
        add.setElevation(Ui.dp(this, 2));
        add.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 4));
        input = new EditText(this);
        input.setHint("やる事を追加");
        input.setSingleLine(true);
        input.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        input.setTextColor(Ui.INK);
        input.setHintTextColor(Ui.MUTED);
        input.setBackground(null);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            addItem(input.getText().toString());
            return true;
        });
        add.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ImageButton mic = Ui.icon(this, R.drawable.ic_mic, "音声で追加", 0xFFFFFFFF);
        mic.setBackground(Ui.ripple(this, Ui.round(this, Ui.ACCENT, 12), 12));
        mic.setOnClickListener(v -> startVoice());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44));
        mp.setMarginEnd(Ui.dp(this, 6));
        add.addView(mic, mp);
        TextView addBtn = Ui.pill(this, "追加", false);
        addBtn.setOnClickListener(v -> addItem(input.getText().toString()));
        add.addView(addBtn, Ui.wrap());
        LinearLayout.LayoutParams ap = Ui.matchWrap();
        ap.setMargins(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 16));
        root.addView(add, ap);

        setContentView(root);
        render();
    }

    private void render() {
        itemsBox.removeAllViews();
        int total = note.items.size();
        int done = note.doneCount();
        progress.setText(total == 0 ? "下の欄からやる事を追加できます" : done + " / " + total + " 完了");

        for (Note.Item it : note.items) if (!it.done) itemsBox.addView(row(it));
        if (done > 0) {
            TextView h = Ui.text(this, "完了済み（" + done + "）", 13, Ui.MUTED, true);
            h.setPadding(Ui.dp(this, 6), Ui.dp(this, 14), 0, Ui.dp(this, 6));
            itemsBox.addView(h, Ui.matchWrap());
            for (Note.Item it : note.items) if (it.done) itemsBox.addView(row(it));
        }
    }

    private View row(Note.Item it) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(Ui.ripple(this, Ui.round(this, Ui.CARD, 14), 14));
        r.setPadding(Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        r.setMinimumHeight(Ui.dp(this, 56));
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.bottomMargin = Ui.dp(this, 8);
        r.setLayoutParams(lp);
        if (it.done) r.setAlpha(0.7f);

        CheckBox cb = new CheckBox(this);
        cb.setButtonTintList(ColorStateList.valueOf(Ui.ACCENT));
        cb.setChecked(it.done);
        cb.setContentDescription(it.text + "を完了");
        cb.setOnCheckedChangeListener((b, checked) -> {
            it.done = checked;
            changed = true;
            itemsBox.post(this::render);
        });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48));
        r.addView(cb, cp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = Ui.text(this, it.text, 16, it.done ? Ui.BODY : Ui.INK, false);
        if (it.done) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        col.addView(t, Ui.matchWrap());
        if (!it.done && it.hasAlarm() && it.days != 0) {
            TextView d = Ui.text(this, it.daysLabel(), 12, Ui.ALARM, false);
            col.addView(d, Ui.matchWrap());
        }
        LinearLayout.LayoutParams colp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        colp.setMarginEnd(Ui.dp(this, 8));
        r.addView(col, colp);

        if (!it.done) {
            TextView chip;
            if (it.hasAlarm()) {
                chip = Ui.text(this, it.timeLabel(), 14, Ui.ALARM, true);
                chip.setBackground(Ui.ripple(this, Ui.round(this, Ui.ALARM_SOFT, 18), 18));
                chip.setContentDescription(it.timeLabel() + " のアラーム");
            } else {
                chip = Ui.text(this, "＋ アラーム", 13, Ui.BODY, false);
                chip.setBackground(Ui.ripple(this, Ui.outline(this, Ui.CARD, 0xFF9AA19A, 18, true), 18));
            }
            chip.setGravity(Gravity.CENTER);
            chip.setMinHeight(Ui.dp(this, 40));
            chip.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> alarmMenu(it));
            r.addView(chip, Ui.wrap());
        }

        r.setClickable(true);
        r.setOnClickListener(v -> editItem(it));
        r.setOnLongClickListener(v -> {
            itemMenu(it);
            return true;
        });
        return r;
    }

    private void addItem(String text) {
        String s = text.trim();
        if (s.isEmpty()) return;
        Note.Item it = new Note.Item();
        it.text = s;
        note.items.add(it);
        changed = true;
        input.setText("");
        render();
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void editItem(Note.Item it) {
        EditText e = new EditText(this);
        e.setText(it.text);
        e.setSelection(e.getText().length());
        FrameLayout box = new FrameLayout(this);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        box.addView(e);
        new AlertDialog.Builder(this)
                .setTitle("やる事を編集")
                .setView(box)
                .setPositiveButton("保存", (d, w) -> {
                    String s = e.getText().toString().trim();
                    if (!s.isEmpty()) {
                        it.text = s;
                        changed = true;
                        render();
                    }
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void itemMenu(Note.Item it) {
        String[] items = {"編集", "削除"};
        new AlertDialog.Builder(this)
                .setTitle(it.text)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        editItem(it);
                    } else {
                        if (it.hasAlarm()) dismissAlarm(it);
                        note.items.remove(it);
                        changed = true;
                        render();
                    }
                })
                .show();
    }

    // ---- アラーム（端末標準の時計アプリを使う） ----

    private void alarmMenu(Note.Item it) {
        if (!it.hasAlarm()) {
            pickTime(it);
            return;
        }
        String[] items = {"時刻を変える", "アラームを解除"};
        new AlertDialog.Builder(this)
                .setTitle(it.text + "（" + it.timeLabel() + "）")
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        pickTime(it);
                    } else {
                        dismissAlarm(it);
                        it.hour = -1;
                        it.minute = -1;
                        it.days = 0;
                        changed = true;
                        render();
                    }
                })
                .show();
    }

    private void pickTime(Note.Item it) {
        int h = it.hour;
        int m = it.minute;
        if (h < 0) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.HOUR_OF_DAY, 1);
            h = c.get(Calendar.HOUR_OF_DAY);
            m = 0;
        }
        new TimePickerDialog(this, (view, hour, minute) -> pickDays(it, hour, minute), h, m, true).show();
    }

    private void pickDays(Note.Item it, int hour, int minute) {
        boolean[] checked = new boolean[7];
        for (int i = 0; i < 7; i++) checked[i] = (it.days & (1 << i)) != 0;
        new AlertDialog.Builder(this)
                .setTitle("繰り返す曜日（選ばなければ1回だけ）")
                .setMultiChoiceItems(Note.DAY_LABELS, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton("設定", (d, w) -> {
                    int days = 0;
                    for (int i = 0; i < 7; i++) if (checked[i]) days |= 1 << i;
                    if (it.hasAlarm()) dismissAlarm(it);
                    setAlarm(it, hour, minute, days);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private String alarmLabel(Note.Item it) {
        return it.text;
    }

    private void setAlarm(Note.Item it, int hour, int minute, int days) {
        Note.Item tmp = new Note.Item();
        tmp.days = days;
        Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
        i.putExtra(AlarmClock.EXTRA_HOUR, hour);
        i.putExtra(AlarmClock.EXTRA_MINUTES, minute);
        i.putExtra(AlarmClock.EXTRA_MESSAGE, alarmLabel(it));
        if (days != 0) i.putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS, tmp.calendarDays());
        i.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "時計アプリが見つかりません", Toast.LENGTH_LONG).show();
            return;
        }
        it.hour = hour;
        it.minute = minute;
        it.days = days;
        changed = true;
        render();
        Toast.makeText(this, "時計アプリに " + it.timeLabel() + " のアラームを設定しました",
                Toast.LENGTH_SHORT).show();
    }

    private void dismissAlarm(Note.Item it) {
        Intent i = new Intent(AlarmClock.ACTION_DISMISS_ALARM);
        i.putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE, AlarmClock.ALARM_SEARCH_MODE_LABEL);
        i.putExtra(AlarmClock.EXTRA_MESSAGE, alarmLabel(it));
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "時計アプリでアラームを解除してください", Toast.LENGTH_LONG).show();
        }
    }

    // ---- 音声入力 ----

    private void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "やる事を話してください");
        try {
            startActivityForResult(i, REQ_VOICE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "この端末では音声入力が使えません", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_VOICE || resultCode != RESULT_OK || data == null) return;
        ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (results != null && !results.isEmpty()) addItem(results.get(0));
    }

    // ---- その他・保存 ----

    private void showMore() {
        String[] items = {note.pinned ? "ピン留めを外す" : "ピン留め", "完了済みを消す", "リストを削除"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        note.pinned = !note.pinned;
                        Toast.makeText(this, note.pinned ? "ピン留めしました" : "ピン留めを外しました",
                                Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        note.items.removeIf(x -> x.done);
                        changed = true;
                        render();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage("このリストを削除しますか？\n（時計アプリのアラームは時計アプリで消してください）")
                                .setPositiveButton("削除", (d2, w) -> {
                                    store.remove(note);
                                    note.items.clear();
                                    note.title = "";
                                    titleEdit.setText("");
                                    finish();
                                })
                                .setNegativeButton("キャンセル", null)
                                .show();
                    }
                })
                .show();
    }

    private void save() {
        String newTitle = titleEdit.getText().toString();
        if (!newTitle.equals(note.title)) changed = true;
        note.title = newTitle;
        if (note.isEmpty()) {
            if (store.contains(note)) store.remove(note);
        } else if (changed || !store.contains(note)) {
            store.put(note);
        } else {
            store.saveQuietly();
        }
        changed = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("id", note.id);
    }
}
