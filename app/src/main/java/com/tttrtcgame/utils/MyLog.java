package com.tttrtcgame.utils;

import com.wushuangtech.utils.PviewLog;

/**
 * Created by wangzhiguo on 17/10/31.
 */

public class MyLog {
    private static boolean IS_DEBUG = true;
    public static final String TAG = "WSTECH";

    public static void i(String msg) {
        if (!IS_DEBUG) {
            return;
        }
        PviewLog.i(msg);
//        Log.i(TAG, msg);
    }

    public static void d(String msg) {
        if (!IS_DEBUG) {
            return;
        }
        PviewLog.d(msg);
//        Log.w(TAG, msg);
    }

    public static void w(String msg) {
        PviewLog.w(msg);
//        Log.w(TAG, msg);
    }

    public static void e(String msg) {
        PviewLog.e(msg);
//        Log.e(TAG, "[V2-TECH-ERROR]" + msg);
    }
}
