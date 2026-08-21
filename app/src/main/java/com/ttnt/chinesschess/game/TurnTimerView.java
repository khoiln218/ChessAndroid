package com.ttnt.chinesschess.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.ttnt.chinesschess.R;

/**
 * The circular turn clock drawn on each side's panel: a ring that empties as the side's three
 * minutes run out, with the remaining time in the middle. While the AI is searching, an extra
 * segment sweeps around the ring - that is what replaced the modal "Thinking..." dialog.
 */
public class TurnTimerView extends View {

    /** One full turn of the thinking segment, in milliseconds. */
    private static final long SPIN_PERIOD = 1200L;

    private final int trackColor;
    private final int idleColor;
    private final int okColor;
    private final int warnColor;
    private final int dangerColor;
    private final int thinkingColor;

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private float remaining = 1f;
    private String label = "";
    private boolean active;
    private boolean thinking;

    public TurnTimerView(Context context) {
        this(context, null);
    }

    public TurnTimerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        trackColor = ContextCompat.getColor(context, R.color.timerTrack);
        idleColor = ContextCompat.getColor(context, R.color.timerIdle);
        okColor = ContextCompat.getColor(context, R.color.timerOk);
        warnColor = ContextCompat.getColor(context, R.color.timerWarn);
        dangerColor = ContextCompat.getColor(context, R.color.timerDanger);
        thinkingColor = ContextCompat.getColor(context, R.color.gold);
        textPaint.setColor(ContextCompat.getColor(context, R.color.sideBlack));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    /**
     * @param remaining fraction of the turn left, 1 = full clock, 0 = out of time
     * @param label     the countdown to print inside the ring
     * @param active    whether this side is the one on the clock
     */
    public void setTime(float remaining, String label, boolean active) {
        this.remaining = Math.max(0f, Math.min(1f, remaining));
        this.label = label;
        this.active = active;
        invalidate();
    }

    /** Turns the sweeping segment on while the AI searches for its move. */
    public void setThinking(boolean thinking) {
        if (this.thinking != thinking) {
            this.thinking = thinking;
            invalidate();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float stroke = size * 0.1f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = size / 2f - stroke / 2f;
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        ringPaint.setStrokeWidth(stroke);
        ringPaint.setColor(trackColor);
        canvas.drawCircle(cx, cy, radius, ringPaint);

        ringPaint.setColor(active ? timeColor() : idleColor);
        canvas.drawArc(oval, -90f, 360f * remaining, false, ringPaint);

        if (thinking) {
            long phase = SystemClock.uptimeMillis() % SPIN_PERIOD;
            ringPaint.setStrokeWidth(stroke / 2f);
            ringPaint.setColor(thinkingColor);
            canvas.drawArc(oval, -90f + 360f * phase / SPIN_PERIOD, 60f, false, ringPaint);
            postInvalidateOnAnimation();
        }

        if (!label.isEmpty()) {
            textPaint.setTextSize(size * 0.26f);
            float baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(label, cx, baseline, textPaint);
        }
    }

    private int timeColor() {
        if (remaining > 0.5f) return okColor;
        if (remaining > 0.2f) return warnColor;
        return dangerColor;
    }
}
