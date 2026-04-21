package com.novelverse.app.presentation.common.views;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.novelverse.app.R;
import com.novelverse.app.data.local.entities.UserCurrencyEntity;

import java.io.IOException;
import java.io.InputStream;

/**
 * Compound view: [ink_bottle] 1,234  [golden_quill] 56
 * Inflate via XML: <com.novelverse.app.presentation.common.views.CurrencyHudView>
 */
public class CurrencyHudView extends LinearLayout {

    private ImageView inkIcon;
    private ImageView quillIcon;
    private TextView  inkText;
    private TextView  quillText;

    public CurrencyHudView(Context context) {
        super(context); init(context);
    }
    public CurrencyHudView(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public CurrencyHudView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(android.view.Gravity.CENTER_VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_currency_hud, this, true);
        inkIcon   = findViewById(R.id.ink_icon);
        quillIcon = findViewById(R.id.quill_icon);
        inkText   = findViewById(R.id.ink_balance_text);
        quillText = findViewById(R.id.quill_balance_text);

        // Load asset PNGs
        loadAsset(context, inkIcon,   "gamification_icons/ink_bottle.png");
        loadAsset(context, quillIcon, "gamification_icons/golden_quill.png");
    }

    public void bind(UserCurrencyEntity currency) {
        if (currency == null) {
            setText(inkText, "—");
            setText(quillText, "—");
            return;
        }
        setText(inkText,   formatBalance(currency.getInkBalance()));
        setText(quillText, formatBalance(currency.getQuillBalance()));
    }

    public void setInk(int balance)   { setText(inkText,   formatBalance(balance)); }
    public void setQuill(int balance) { setText(quillText, formatBalance(balance)); }

    private void setText(TextView tv, String v) { if (tv != null) tv.setText(v); }

    private String formatBalance(int v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000f);
        if (v >= 1_000)     return String.format("%.1fK", v / 1_000f);
        return String.valueOf(v);
    }

    private void loadAsset(Context ctx, ImageView iv, String path) {
        if (iv == null) return;
        try {
            AssetManager assets = ctx.getAssets();
            InputStream is = assets.open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            iv.setImageBitmap(bmp);
        } catch (IOException ignored) {}
    }
}
