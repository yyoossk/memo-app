package com.yyoossk.memo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** ホーム：メモ・やる事・手書きの一覧 */
public class MainActivity extends Activity {
    private static final String[] FILTER_LABELS = {"すべて", "メモ", "やる事", "手書き"};
    private static final int[] FILTER_TYPES = {-1, Note.TEXT, Note.TODO, Note.DRAW};

    private NoteStore store;
    private LinearLayout list;
    private EditText search;
    private int filter = -1;
    private final List<TextView> chips = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = NoteStore.get(this);

        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(Ui.BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        TextView header = Ui.text(this, "メモ", 26, Ui.INK, true);
        header.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 8));
        root.addView(header, Ui.matchWrap());

        search = new EditText(this);
        search.setHint("検索");
        search.setSingleLine(true);
        search.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        search.setTextColor(Ui.INK);
        search.setHintTextColor(Ui.MUTED);
        search.setBackground(Ui.round(this, Ui.CARD, 14));
        search.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        search.setMinHeight(Ui.dp(this, 48));
        LinearLayout.LayoutParams sp = Ui.matchWrap();
        sp.setMargins(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 12));
        root.addView(search, sp);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                refresh();
            }
        });

        LinearLayout chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), Ui.dp(this, 8));
        for (int i = 0; i < FILTER_LABELS.length; i++) {
            final int type = FILTER_TYPES[i];
            TextView chip = Ui.text(this, FILTER_LABELS[i], 14, Ui.INK, false);
            chip.setGravity(Gravity.CENTER);
            chip.setMinHeight(Ui.dp(this, 40));
            chip.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                filter = type;
                updateChips();
                refresh();
            });
            LinearLayout.LayoutParams cp = Ui.wrap();
            cp.setMarginEnd(Ui.dp(this, 8));
            chipRow.addView(chip, cp);
            chips.add(chip);
        }
        root.addView(chipRow, Ui.matchWrap());
        updateChips();

        ScrollView sv = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 110));
        sv.addView(list, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        frame.addView(root, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        ImageButton fab = new ImageButton(this);
        fab.setImageResource(R.drawable.ic_plus);
        fab.setColorFilter(0xFFFFFFFF);
        fab.setScaleType(ImageView.ScaleType.CENTER);
        fab.setBackground(Ui.ripple(this, Ui.round(this, Ui.ACCENT, 20), 20));
        fab.setElevation(Ui.dp(this, 6));
        fab.setContentDescription("新しく作る");
        fab.setOnClickListener(v -> showCreate());
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(Ui.dp(this, 60), Ui.dp(this, 60));
        fp.gravity = Gravity.BOTTOM | Gravity.END;
        fp.setMargins(0, 0, Ui.dp(this, 20), Ui.dp(this, 24));
        frame.addView(fab, fp);

        setContentView(frame);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void updateChips() {
        for (int i = 0; i < chips.size(); i++) {
            TextView chip = chips.get(i);
            boolean on = FILTER_TYPES[i] == filter;
            chip.setTextColor(on ? 0xFFFFFFFF : Ui.INK);
            chip.setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            chip.setBackground(Ui.ripple(this, on ? Ui.round(this, Ui.INK, 20)
                    : Ui.outline(this, Ui.CARD, Ui.BORDER, 20, false), 20));
        }
    }

    private void refresh() {
        list.removeAllViews();
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        int shown = 0;
        for (Note n : store.all()) {
            if (filter >= 0 && n.type != filter) continue;
            if (!n.matches(q)) continue;
            list.addView(card(n));
            shown++;
        }
        if (shown == 0) {
            String msg = q.isEmpty() ? "まだありません。\n右下の ＋ から作れます。" : "見つかりませんでした";
            TextView empty = Ui.text(this, msg, 15, Ui.MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setLineSpacing(Ui.dp(this, 4), 1f);
            empty.setPadding(0, Ui.dp(this, 48), 0, 0);
            list.addView(empty, Ui.matchWrap());
        }
    }

    private View card(Note n) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Ui.ripple(this, Ui.round(this, Ui.CARD, 16), 16));
        c.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        c.setClickable(true);
        c.setFocusable(true);
        LinearLayout.LayoutParams lp = Ui.matchWrap();
        lp.bottomMargin = Ui.dp(this, 10);
        c.setLayoutParams(lp);

        if (n.pinned) {
            TextView pin = Ui.text(this, "ピン留め", 12, Ui.ACCENT, true);
            c.addView(pin, Ui.wrap());
        }

        TextView title = Ui.text(this, n.displayTitle(), 16, Ui.INK, true);
        c.addView(title, Ui.matchWrap());

        if (n.type == Note.TEXT) {
            String preview = n.body.trim();
            if (!preview.isEmpty() && !n.title.trim().isEmpty()) {
                TextView p = Ui.text(this, preview, 14, Ui.BODY, false);
                p.setMaxLines(2);
                p.setEllipsize(TextUtils.TruncateAt.END);
                p.setLineSpacing(Ui.dp(this, 2), 1f);
                LinearLayout.LayoutParams pp = Ui.matchWrap();
                pp.topMargin = Ui.dp(this, 6);
                c.addView(p, pp);
            }
        } else if (n.type == Note.TODO) {
            int total = n.items.size();
            int done = n.doneCount();
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout track = new LinearLayout(this);
            track.setBackground(Ui.round(this, 0xFFDDE2DC, 3));
            View fill = new View(this);
            fill.setBackground(Ui.round(this, Ui.ACCENT, 3));
            track.setWeightSum(Math.max(total, 1));
            track.addView(fill, new LinearLayout.LayoutParams(0, Ui.dp(this, 6), done));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, Ui.dp(this, 6), 1f);
            row.addView(track, tp);
            TextView count = Ui.text(this, done + " / " + total, 13, Ui.BODY, false);
            count.setPadding(Ui.dp(this, 10), 0, 0, 0);
            row.addView(count, Ui.wrap());
            LinearLayout.LayoutParams rp = Ui.matchWrap();
            rp.topMargin = Ui.dp(this, 10);
            c.addView(row, rp);
        } else {
            Bitmap thumb = loadThumb(store.drawingFile(n));
            if (thumb != null) {
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(thumb);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackground(Ui.round(this, Ui.BG, 10));
                iv.setClipToOutline(true);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 120));
                ip.topMargin = Ui.dp(this, 8);
                c.addView(iv, ip);
            }
        }

        SpannableStringBuilder meta = new SpannableStringBuilder();
        meta.append(Ui.formatDate(n.updated)).append("・").append(n.typeLabel());
        int alarms = n.alarmCount();
        if (alarms > 0) {
            int start = meta.length();
            meta.append("・アラーム ").append(String.valueOf(alarms)).append("件");
            meta.setSpan(new ForegroundColorSpan(Ui.ALARM), start, meta.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        TextView m = Ui.text(this, "", 12, Ui.MUTED, false);
        m.setText(meta);
        LinearLayout.LayoutParams mp = Ui.matchWrap();
        mp.topMargin = Ui.dp(this, 8);
        c.addView(m, mp);

        c.setOnClickListener(v -> open(n));
        c.setOnLongClickListener(v -> {
            showActions(n);
            return true;
        });
        return c;
    }

    private Bitmap loadThumb(File f) {
        if (!f.exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = 3;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    }

    static Class<?> screenFor(int type) {
        if (type == Note.TODO) return TodoActivity.class;
        if (type == Note.DRAW) return DrawActivity.class;
        return NoteActivity.class;
    }

    private void open(Note n) {
        startActivity(new Intent(this, screenFor(n.type)).putExtra("id", n.id));
    }

    private void showCreate() {
        String[] items = {"メモ", "やる事リスト", "手書き"};
        int[] types = {Note.TEXT, Note.TODO, Note.DRAW};
        new AlertDialog.Builder(this)
                .setTitle("新しく作る")
                .setItems(items, (d, which) ->
                        startActivity(new Intent(this, screenFor(types[which]))))
                .show();
    }

    private void showActions(Note n) {
        String[] items = {n.pinned ? "ピン留めを外す" : "ピン留め", "LINE で送る", "他のアプリで送る", "印刷する", "削除"};
        new AlertDialog.Builder(this)
                .setTitle(n.displayTitle())
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            n.pinned = !n.pinned;
                            store.saveQuietly();
                            refresh();
                            break;
                        case 1:
                            ShareUtil.send(this, n, true);
                            break;
                        case 2:
                            ShareUtil.send(this, n, false);
                            break;
                        case 3:
                            ShareUtil.print(this, n);
                            break;
                        default:
                            confirmDelete(n);
                            break;
                    }
                })
                .show();
    }

    private void confirmDelete(Note n) {
        new AlertDialog.Builder(this)
                .setMessage("「" + n.displayTitle() + "」を削除しますか？")
                .setPositiveButton("削除", (d, w) -> {
                    store.remove(n);
                    refresh();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }
}
