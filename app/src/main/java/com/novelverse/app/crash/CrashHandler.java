package com.novelverse.app.crash;

import android.content.Context;
import android.content.SharedPreferences;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context context) {
        this.context = context;
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        String stackTrace = android.util.Log.getStackTraceString(throwable);

        try {
            SharedPreferences prefs =
                    context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE);

            prefs.edit()
                    .putBoolean("crashed", true)
                    .putString("stack_trace", stackTrace)
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start CrashActivity
        android.content.Intent intent = new android.content.Intent(context, CrashActivity.class);
        intent.putExtra("crash_log", stackTrace);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Kill current process
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
