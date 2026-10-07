package com.yyoossk.memo;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/** 手書き */
public class DrawActivity extends Activity {
    private static final int[] COLORS = {0xFF1B1F24, 0xFF0F766E, 0xFFC2410C, 0xFF1D4ED8, 0xFFB42318, 0xFFCA8A04};
    private static final String[] COLOR_NAMES = {"黒", "緑", "橙", "青", "赤", "黄"};
    private static final String[] TOOL_LABELS = {"ペン", "マーカー", "消しゴム"};

    private NoteStore store;
    private Note note;
    private EditText titleEdit;
    private DrawView draw;
    private final List<TextView> toolViews = new ArrayList<>();
    private final List<View> colorViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = NoteStore.get(this);
        String id = savedInstanceState != null ? savedInstanceState.getString("id")
                : getIntent().getStringExtra("id");
        note = store.find(id);
        if (note == null) {
            note = Note.create(Note.DRAW);
            if (id != null) note.id = id;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.CARD);

        LinearLayout bar = Ui.topBar(this);
        titleEdit = new EditText(this);
        titleEdit.setHint("手書き");
        titleEdit.setText(note.title);
        titleEdit.setSingleLine(true);
        titleEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleEdit.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleEdit.setTextColor(Ui.INK);
        titleEdit.setHintTextColor(0xFF9AA19A);
        titleEdit.setBackground(null);
        bar.addView(titleEdit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        ImageButton undo = Ui.icon(this, R.drawable.ic_undo, "元に戻す", Ui.INK);
        undo.setOnClickListener(v -> draw.undo());
        bar.addView(undo);
        ImageButton share = Ui.icon(this, R.drawable.ic_share, "送る・印刷", Ui.INK);
        share.setOnClickListener(v -> ShareUtil.showMenu(this, note, this::save));
        bar.addView(share);
        ImageButton more = Ui.icon(this, R.drawable.ic_more, "その他", Ui.INK);
        more.setOnClickListener(v -> showMore());
        bar.addView(more);
        root.addView(bar, Ui.matchWrap());

        draw = new DrawView(this);
        File f = store.drawingFile(note);
        if (f.exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inScaled = false;
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            if (b != null) draw.setBase(b);
        }
        root.addView(draw, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 14));
        panel.setBackgroundColor(Ui.CARD);

        LinearLayout tools = new LinearLayout(this);
        tools.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < TOOL_LABELS.length; i++) {
            final int tool = i;
            TextView t = Ui.text(this, TOOL_LABELS[i], 15, Ui.INK, false);
            t.setGravity(Gravity.CENTER);
            t.setMinHeight(Ui.dp(this, 44));
            t.setClickable(true);
            t.setFocusable(true);
            t.setOnClickListener(v -> {
                draw.tool = tool;
                updateTools();
            });
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) tp.setMarginStart(Ui.dp(this, 8));
            tools.addView(t, tp);
            toolViews.add(t);
        }
        panel.addView(tools, Ui.matchWrap());

        LinearLayout colors = new LinearLayout(this);
        colors.setOrientation(LinearLayout.HORIZONTAL);
        colors.setGravity(Gravity.CENTER_VERTICAL);
        for (int i = 0; i < COLORS.length; i++) {
            final int color = COLORS[i];
            View sw = new View(this);
            sw.setContentDescription(COLOR_NAMES[i]);
            sw.setClickable(true);
            sw.setFocusable(true);
            sw.setOnClickListener(v -> {
                draw.color = color;
                if (draw.tool == DrawView.ERASER) draw.tool = DrawView.PEN;
                updateTools();
            });
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(this, 36), Ui.dp(this, 36));
            cp.setMarginEnd(Ui.dp(this, 10));
            colors.addView(sw, cp);
            colorViews.add(sw);
        }
        LinearLayout.LayoutParams colp = Ui.matchWrap();
        colp.topMargin = Ui.dp(this, 12);
        panel.addView(colors, colp);

        LinearLayout widthRow = new LinearLayout(this);
        widthRow.setOrientation(LinearLayout.HORIZONTAL);
        widthRow.setGravity(Gravity.CENTER_VERTICAL);
        widthRow.addView(Ui.text(this, "太さ", 14, Ui.BODY, false), Ui.wrap());
        SeekBar seek = new SeekBar(this);
        seek.setMax(18);
        seek.setProgress(Math.round(draw.widthDp) - 2);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                draw.widthDp = progress + 2;
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        widthRow.addView(seek, new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f));
        LinearLayout.LayoutParams wp = Ui.matchWrap();
        wp.topMargin = Ui.dp(this, 6);
        panel.addView(widthRow, wp);

        root.addView(panel, Ui.matchWrap());
        setContentView(root);
        updateTools();
    }

    private void updateTools() {
        for (int i = 0; i < toolViews.size(); i++) {
            TextView t = toolViews.get(i);
            boolean on = draw.tool == i;
            t.setTextColor(on ? Ui.ACCENT : Ui.INK);
            t.setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            t.setBackground(Ui.ripple(this, on ? Ui.round(this, Ui.ACCENT_SOFT, 14)
                    : Ui.outline(this, Ui.CARD, Ui.BORDER, 14, false), 14));
        }
        for (int i = 0; i < colorViews.size(); i++) {
            boolean on = COLORS[i] == draw.color && draw.tool != DrawView.ERASER;
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            g.setColor(COLORS[i]);
            if (on) g.setStroke(Ui.dp(this, 3), 0xFF9AA19A);
            colorViews.get(i).setBackground(g);
        }
    }

    private void showMore() {
        String[] items = {note.pinned ? "ピン留めを外す" : "ピン留め", "全部消す", "削除"};
        new AlertDialog.Builder(this)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        note.pinned = !note.pinned;
                        Toast.makeText(this, note.pinned ? "ピン留めしました" : "ピン留めを外しました",
                                Toast.LENGTH_SHORT).show();
                        if (store.contains(note)) store.saveQuietly();
                    } else if (which == 1) {
                        new AlertDialog.Builder(this)
                                .setMessage("描いたものを全部消しますか？")
                                .setPositiveButton("消す", (d2, w) -> draw.clear())
                                .setNegativeButton("キャンセル", null)
                                .show();
                    } else {
                        new AlertDialog.Builder(this)
                                .setMessage("この手書きを削除しますか？")
                                .setPositiveButton("削除", (d2, w) -> {
                                    store.remove(note);
                                    draw.clear();
                                    draw.markSaved();
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
        boolean titleChanged = !newTitle.equals(note.title);
        note.title = newTitle;
        if (draw.isBlank()) {
            if (store.contains(note) && draw.hasChanges()) store.remove(note);
            draw.markSaved();
            return;
        }
        if (draw.hasChanges()) {
            Bitmap b = draw.export();
            if (b != null) {
                File f = store.drawingFile(note);
                try (OutputStream out = new FileOutputStream(f)) {
                    b.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    Log.e("DrawActivity", "save failed", e);
                    Toast.makeText(this, "保存できませんでした", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            draw.markSaved();
            store.put(note);
        } else if (titleChanged || !store.contains(note)) {
            store.put(note);
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
