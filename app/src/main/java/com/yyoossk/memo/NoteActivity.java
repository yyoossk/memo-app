package com.yyoossk.memo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/** メモの編集（罫線つき・音声入力） */
public class NoteActivity extends Activity {
    private static final int REQ_VOICE = 1;

    private NoteStore store;
    private Note note;
    private EditText titleEdit;
    private LinedEditText body;
    private TextView meta;
    private TextView count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = NoteStore.get(this);
        String id = savedInstanceState != null ? savedInstanceState.getString("id")
                : getIntent().getStringExtra("id");
        note = store.find(id);
        if (note == null) {
            note = Note.create(Note.TEXT);
            if (id != null) note.id = id;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CARD);

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
        titleEdit.setHint("タイトル");
        titleEdit.setText(note.title);
        titleEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleEdit.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleEdit.setTextColor(Ui.INK);
        titleEdit.setHintTextColor(0xFF9AA19A);
        titleEdit.setBackground(null);
        titleEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        titleEdit.setPadding(Ui.dp(this, 24), Ui.dp(this, 4), Ui.dp(this, 24), 0);
        root.addView(titleEdit, Ui.matchWrap());

        meta = Ui.text(this, "", 12, Ui.MUTED, false);
        meta.setPadding(Ui.dp(this, 24), Ui.dp(this, 2), Ui.dp(this, 24), Ui.dp(this, 4));
        root.addView(meta, Ui.matchWrap());

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        body = new LinedEditText(this);
        body.setText(note.body);
        body.setHint("ここにメモを書く");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        body.setTextColor(Ui.INK);
        body.setHintTextColor(0xFF9AA19A);
        body.setLineSpacing(Ui.dp(this, 14), 1f);
        body.setGravity(Gravity.TOP | Gravity.START);
        body.setBackground(null);
        body.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        body.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), Ui.dp(this, 24), Ui.dp(this, 24));
        sv.addView(body, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 10));
        bottom.setBackgroundColor(Ui.CARD);
        count = Ui.text(this, "", 13, Ui.MUTED, false);
        bottom.addView(count, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView voice = Ui.pill(this, "音声で入力", true);
        voice.setCompoundDrawablesRelativeWithIntrinsicBounds(tinted(R.drawable.ic_mic), null, null, null);
        voice.setCompoundDrawablePadding(Ui.dp(this, 6));
        voice.setOnClickListener(v -> startVoice());
        bottom.addView(voice, Ui.wrap());
        root.addView(bottom, Ui.matchWrap());

        body.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int c, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateInfo();
            }
        });
        updateInfo();

        setContentView(root);
        if (note.body.isEmpty() && note.title.isEmpty()) body.requestFocus();
    }

    private android.graphics.drawable.Drawable tinted(int res) {
        android.graphics.drawable.Drawable d = getDrawable(res);
        if (d != null) {
            d = d.mutate();
            d.setTint(0xFFFFFFFF);
        }
        return d;
    }

    private void updateInfo() {
        meta.setText(Ui.formatDate(note.updated));
        count.setText(body.getText().length() + " 文字");
    }

    private void startVoice() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP");
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "話してください");
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
        if (results == null || results.isEmpty()) return;
        String spoken = results.get(0);
        Editable e = body.getText();
        int s = Math.max(body.getSelectionStart(), 0);
        int en = Math.max(body.getSelectionEnd(), 0);
        int start = Math.min(s, en);
        int end = Math.max(s, en);
        e.replace(start, end, spoken);
        body.setSelection(Math.min(start + spoken.length(), e.length()));
        body.requestFocus();
    }

    private void showMore() {
        String[] items = {note.pinned ? "ピン留めを外す" : "ピン留め", "削除"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        note.pinned = !note.pinned;
                        Toast.makeText(this, note.pinned ? "ピン留めしました" : "ピン留めを外しました",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage("このメモを削除しますか？")
                                .setPositiveButton("削除", (d2, w) -> {
                                    store.remove(note);
                                    note.title = "";
                                    note.body = "";
                                    titleEdit.setText("");
                                    body.setText("");
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
        String newBody = body.getText().toString();
        boolean changed = !newTitle.equals(note.title) || !newBody.equals(note.body);
        note.title = newTitle;
        note.body = newBody;
        if (note.isEmpty()) {
            if (store.contains(note)) store.remove(note);
        } else if (changed || !store.contains(note)) {
            store.put(note);
        } else {
            store.saveQuietly();
        }
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
