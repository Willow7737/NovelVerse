package com.novelverse.app.ui.banner;

public class BannerConfig {
    private BannerType type           = BannerType.INFO;
    private String     title          = null;   // null → use type default
    private String     message        = "";
    private long       duration       = 3200L;
    private boolean    swipeToDismiss = true;
    private boolean    showProgress   = false;
    private OnBannerClickListener onClickListener;
    private Runnable              onDismissListener;

    public BannerConfig setType(BannerType type)           { this.type           = type;            return this; }
    public BannerConfig setTitle(String title)             { this.title          = title;           return this; }
    public BannerConfig setMessage(String message)         { this.message        = message;         return this; }
    public BannerConfig setDuration(long ms)               { this.duration       = ms;              return this; }
    public BannerConfig setSwipeToDismiss(boolean e)       { this.swipeToDismiss = e;               return this; }
    public BannerConfig setShowProgress(boolean s)         { this.showProgress   = s;               return this; }
    public BannerConfig setOnClickListener(OnBannerClickListener l) { this.onClickListener  = l; return this; }
    public BannerConfig setOnDismissListener(Runnable r)   { this.onDismissListener = r;           return this; }

    public BannerType           getType()              { return type; }
    public String               getTitle()             { return title; }
    public String               getMessage()           { return message; }
    public long                 getDuration()          { return duration; }
    public boolean              isSwipeToDismiss()     { return swipeToDismiss; }
    public boolean              isShowProgress()       { return showProgress; }
    public OnBannerClickListener getOnClickListener()  { return onClickListener; }
    public Runnable             getOnDismissListener() { return onDismissListener; }
}
