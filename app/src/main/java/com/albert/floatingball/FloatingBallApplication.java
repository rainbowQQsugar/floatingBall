package com.albert.floatingball;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.multidex.MultiDex;
import android.util.Log;
import android.widget.Toast;

import com.albert.floatingball.crashsupport.DebugSafeModeTipActivity;
import com.albert.floatingball.crashsupport.DebugSafeModeUI;
import com.albert.floatingball.crashsupport.CrashLog;
import com.albert.floatingball.utils.FloatingBallConstants;
import com.albert.floatingball.utils.SystemUtils;
import com.tencent.bugly.crashreport.CrashReport;
import com.wanjian.cockroach.Cockroach;
import com.wanjian.cockroach.ExceptionHandler;

public class FloatingBallApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initTencentBugly();//上传bug到bugly
        initCrashCatch();
    }

    private void initCrashCatch() {
        final Thread.UncaughtExceptionHandler sysExcepHandler = Thread.getDefaultUncaughtExceptionHandler();
        final Toast toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        DebugSafeModeUI.init(this);
        Cockroach.install(new ExceptionHandler() {
            @Override
            protected void onUncaughtExceptionHappened(Thread thread, Throwable throwable) {
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", throwable);
                CrashLog.saveCrashLog(getApplicationContext(), throwable);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        toast.setText(R.string.safe_mode_excep_tips);
                        toast.show();
                    }
                });
            }

            @Override
            protected void onBandageExceptionHappened(Throwable throwable) {
                throwable.printStackTrace();//打印警告级别log，该throwable可能是最开始的bug导致的，无需关心
                toast.setText("Cockroach Worked");
                toast.show();
            }

            @Override
            protected void onEnterSafeMode() {
                int tips = R.string.safe_mode_tips;
                Toast.makeText(FloatingBallApplication.this, getResources().getString(tips), Toast.LENGTH_LONG).show();
                DebugSafeModeUI.showSafeModeUI();

                if (BuildConfig.DEBUG) {
                    Intent intent = new Intent(FloatingBallApplication.this, DebugSafeModeTipActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }

            @Override
            protected void onMayBeBlackScreen(Throwable e) {
                Thread thread = Looper.getMainLooper().getThread();
                Log.e("AndroidRuntime", "--->onUncaughtExceptionHappened:" + thread + "<---", e);
                //黑屏时建议直接杀死app
                sysExcepHandler.uncaughtException(thread, new RuntimeException("black screen"));
            }

        });
    }

    private void initTencentBugly() {
        Context context = getApplicationContext();
        String packageName = context.getPackageName();
        String processName = SystemUtils.getProcessName(android.os.Process.myPid());
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        CrashReport.initCrashReport(getApplicationContext(), FloatingBallConstants.InitConstants.BUGLY_APPID, true, strategy);
    }


}
