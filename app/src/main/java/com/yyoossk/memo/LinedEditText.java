package com.yyoossk.memo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.widget.EditText;

/** ノートのような罫線を引いた入力欄 */
public class LinedEditText extends EditText {
    private final Paint paint = new Paint();
    private final Rect rect = new Rect();

    public LinedEditText(Context context) {
        super(context);
        paint.setColor(Ui.RULE);
        paint.setStrokeWidth(Math.max(1, Ui.dp(context, 1)));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int lineHeight = getLineHeight();
        if (lineHeight > 0) {
            int baseline = getLineBounds(0, rect);
            int gap = Ui.dp(getContext(), 7);
            int left = getPaddingLeft();
            int right = getWidth() - getPaddingRight();
            int bottom = Math.max(getHeight(), getLineCount() * lineHeight + getPaddingTop());
            for (int y = baseline + gap; y < bottom; y += lineHeight) {
                canvas.drawLine(left, y, right, y, paint);
            }
        }
        super.onDraw(canvas);
    }
}
