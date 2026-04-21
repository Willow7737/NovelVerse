package com.novelverse.app.presentation.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;

/**
 * Draws a dotted horizontal divider between RecyclerView list items.
 *
 * <p>Uses identical dash metrics to {@link DottedDividerView} so the
 * library list and home-feed section separators look the same.
 * Applied with {@code recyclerView.addItemDecoration(new DottedDividerDecoration(context))}.
 */
public class DottedDividerDecoration extends RecyclerView.ItemDecoration {

    private final Paint paint;
    private final int   dividerHeight;

    public DottedDividerDecoration(@NonNull Context ctx) {
        float density        = ctx.getResources().getDisplayMetrics().density;
        float strokeWidth    = ctx.getResources().getDimension(R.dimen.divider_stroke_width);
        float dashWidth      = ctx.getResources().getDimension(R.dimen.dash_width);
        float dashGap        = ctx.getResources().getDimension(R.dimen.dash_gap);
        dividerHeight        = ctx.getResources().getDimensionPixelSize(R.dimen.divider_height);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidth);
        paint.setColor(ContextCompat.getColor(ctx, R.color.divider));
        paint.setPathEffect(new DashPathEffect(new float[]{dashWidth, dashGap}, 0f));
    }

    @Override
    public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                               @NonNull View view,
                               @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        // Reserve space below each item except the last one
        int pos = parent.getChildAdapterPosition(view);
        int count = state.getItemCount();
        if (pos >= 0 && pos < count - 1) {
            outRect.bottom = dividerHeight;
        }
    }

    @Override
    public void onDraw(@NonNull Canvas canvas,
                       @NonNull RecyclerView parent,
                       @NonNull RecyclerView.State state) {
        int left  = parent.getPaddingLeft()  + (int)(16 * parent.getResources().getDisplayMetrics().density);
        int right = parent.getWidth() - parent.getPaddingRight()
                                      - (int)(16 * parent.getResources().getDisplayMetrics().density);

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            View child = parent.getChildAt(i);
            float y = child.getBottom() + (dividerHeight / 2f);
            canvas.drawLine(left, y, right, y, paint);
        }
    }
}
