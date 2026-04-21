package com.novelverse.app.presentation.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.novelverse.app.R;

public class DottedDividerView extends View {

    private final Paint paint;

    public DottedDividerView(Context context) {
        this(context, null);
    }

    public DottedDividerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DottedDividerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(getResources().getDimension(R.dimen.divider_stroke_width));
        paint.setColor(ContextCompat.getColor(context, R.color.divider));
        paint.setPathEffect(
                new DashPathEffect(
                        new float[] {
                            getResources().getDimension(R.dimen.dash_width),
                            getResources().getDimension(R.dimen.dash_gap)
                        },
                        0f));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerY = getHeight() / 2f;
        canvas.drawLine(0f, centerY, getWidth(), centerY, paint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredHeight = getResources().getDimensionPixelSize(R.dimen.divider_height);

        int width = resolveSizeAndState(MeasureSpec.getSize(widthMeasureSpec), widthMeasureSpec, 0);
        int height = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0);

        setMeasuredDimension(width, height);
    }
}
