package com.yyoossk.memo;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 画面部品を作るための小さな道具箱 */
final class Ui {
    static final int INK = 0xFF1B1F24;
    static final int BODY = 0xFF4A525C;
    static final int MUTED = 0xFF5B636D;
    static final int BG = 0xFFF4F5F2;
    static final int CARD = 0xFFFFFFFF;
    static final int BORDER = 0xFFD5D8D2;
    static final int DIVIDER = 0xFFEEF0EC;
    static final int ACCENT = 0xFF0F766E;
    static final int ACCENT_SOFT = 0xFFE8F3F1;
    static final int ALARM = 0xFF9A3412;
    static final int ALARM_SOFT = 0xFFFDEDE3;
    static final int RULE = 0xFFDCE3EA;

    private Ui() {
    }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    static GradientDrawable outline(Context c, int fill, int stroke, float radiusDp, boolean dashed) {
        GradientDrawable g = round(c, fill, radiusDp);
        if (dashed) {
            g.setStroke(dp(c, 1.5f), stroke, dp(c, 4), dp(c, 3));
        } else {
            g.setStroke(dp(c, 1), stroke);
        }
        return g;
    }

    static Drawable ripple(Context c, Drawable content, float radiusDp) {
        return new RippleDrawable(ColorStateList.valueOf(0x1F000000), content,
                round(c, 0xFFFFFFFF, radiusDp));
    }

    static TextView text(Context c, String s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    /** 押せる丸いボタン（文字） */
    static TextView pill(Context c, String label, boolean primary) {
        TextView t = text(c, label, 15, primary ? 0xFFFFFFFF : INK, primary);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c, 44));
        t.setMinWidth(dp(c, 44));
        t.setPadding(dp(c, 18), 0, dp(c, 18), 0);
        Drawable bg = primary ? round(c, ACCENT, 22) : outline(c, CARD, BORDER, 22, false);
        t.setBackground(ripple(c, bg, 22));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /** アイコンだけのボタン */
    static ImageButton icon(Context c, int res, String description, int tint) {
        ImageButton b = new ImageButton(c);
        b.setImageResource(res);
        b.setColorFilter(tint);
        b.setScaleType(ImageView.ScaleType.CENTER);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x1F000000), null, null));
        b.setContentDescription(description);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        return b;
    }

    /** 戻るボタン付きの上部バー。右側のボタンは呼び出し側で追加する */
    static LinearLayout topBar(Activity a) {
        LinearLayout bar = new LinearLayout(a);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(a, 4), 0, dp(a, 4), 0);
        bar.setMinimumHeight(dp(a, 56));
        ImageButton back = icon(a, R.drawable.ic_back, "戻る", INK);
        back.setOnClickListener(v -> a.finish());
        bar.addView(back);
        return bar;
    }

    static View spacer(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    static String formatDate(long time) {
        Date d = new Date(time);
        if (DateUtils.isToday(time)) {
            return "今日 " + new SimpleDateFormat("H:mm", Locale.JAPAN).format(d);
        }
        return new SimpleDateFormat("M月d日 H:mm", Locale.JAPAN).format(d);
    }
}
