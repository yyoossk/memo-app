package com.yyoossk.memo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** 指やペンで描くキャンバス */
public class DrawView extends View {
    static final int PAPER = 0xFFFBFBF9;
    static final int PEN = 0, MARKER = 1, ERASER = 2;

    private static final class Stroke {
        final Path path;
        final Paint paint;

        Stroke(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap base;
    private Path current;
    private float lastX, lastY;
    private boolean moved;
    private boolean dirty;

    int color = Ui.INK;
    float widthDp = 4;
    int tool = PEN;

    public DrawView(Context context) {
        super(context);
        dotPaint.setColor(0xFFD7DAD4);
    }

    void setBase(Bitmap bitmap) {
        base = bitmap;
        invalidate();
    }

    boolean isBlank() {
        return strokes.isEmpty() && base == null;
    }

    boolean isDirty() {
        return dirty;
    }

    void markSaved() {
        dirty = false;
    }

    boolean undo() {
        if (strokes.isEmpty()) return false;
        strokes.remove(strokes.size() - 1);
        dirty = true;
        invalidate();
        return true;
    }

    void clear() {
        strokes.clear();
        base = null;
        dirty = true;
        invalidate();
    }

    private Paint makePaint() {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        float w = Ui.dp(getContext(), widthDp);
        if (tool == ERASER) {
            p.setColor(PAPER);
            p.setStrokeWidth(w * 4);
        } else if (tool == MARKER) {
            p.setColor((color & 0x00FFFFFF) | 0x66000000);
            p.setStrokeWidth(w * 3);
        } else {
            p.setColor(color);
            p.setStrokeWidth(w);
        }
        return p;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(PAPER);
        float step = Ui.dp(getContext(), 22);
        float r = Ui.dp(getContext(), 1.2f);
        for (float y = step; y < getHeight(); y += step) {
            for (float x = step; x < getWidth(); x += step) {
                canvas.drawCircle(x, y, r, dotPaint);
            }
        }
        drawContent(canvas);
    }

    private void drawContent(Canvas canvas) {
        if (base != null) canvas.drawBitmap(base, 0, 0, null);
        for (Stroke s : strokes) canvas.drawPath(s.path, s.paint);
    }

    /** 保存用の画像（点線の方眼は含めない） */
    Bitmap export() {
        if (getWidth() <= 0 || getHeight() <= 0) return null;
        Bitmap b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.drawColor(PAPER);
        drawContent(c);
        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                current = new Path();
                current.moveTo(x, y);
                strokes.add(new Stroke(current, makePaint()));
                lastX = x;
                lastY = y;
                moved = false;
                dirty = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (current == null) return true;
                for (int h = 0; h < e.getHistorySize(); h++) {
                    addPoint(e.getHistoricalX(h), e.getHistoricalY(h));
                }
                addPoint(x, y);
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (current != null) {
                    if (!moved) current.lineTo(x + 0.5f, y + 0.5f);
                    else current.lineTo(x, y);
                }
                current = null;
                invalidate();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                current = null;
                return true;
            default:
                return true;
        }
    }

    private void addPoint(float x, float y) {
        current.quadTo(lastX, lastY, (lastX + x) / 2f, (lastY + y) / 2f);
        lastX = x;
        lastY = y;
        moved = true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
