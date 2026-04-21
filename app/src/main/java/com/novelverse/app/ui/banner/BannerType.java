package com.novelverse.app.ui.banner;

import com.novelverse.app.R;

public enum BannerType {
    ERROR   (R.drawable.ic_error,   R.color.banner_error,   "Error"),
    WARNING (R.drawable.ic_warning, R.color.banner_warning, "Warning"),
    INFO    (R.drawable.ic_info,    R.color.banner_info,    "Info"),
    SUCCESS (R.drawable.ic_success, R.color.banner_success, "Success");

    private final int iconRes;
    private final int colorRes;
    private final String defaultTitle;

    BannerType(int iconRes, int colorRes, String defaultTitle) {
        this.iconRes      = iconRes;
        this.colorRes     = colorRes;
        this.defaultTitle = defaultTitle;
    }

    public int    getIconRes()      { return iconRes; }
    public int    getColorRes()     { return colorRes; }
    public String getDefaultTitle() { return defaultTitle; }
}
