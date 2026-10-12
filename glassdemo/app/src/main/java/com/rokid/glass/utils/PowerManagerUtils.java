package com.rokid.glass.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import com.rokid.utils.ContextUtil;

import java.lang.reflect.Method;

public class PowerManagerUtils {
    private static final String TAG = PowerManagerUtils.class.getSimpleName();
    public static void wakeUp() {
        Log.d(TAG,"wakeUp");
        try {
            // 获取 PowerManager 对象
            // Get the PowerManager object.
            PowerManager powerManager = (PowerManager) ContextUtil.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                throw new NullPointerException("PowerManager is null");
            }

            // 获取 wakeUp 方法
            // Get the wakeUp method.
            Method wakeUpMethod = PowerManager.class.getDeclaredMethod("wakeUp", long.class);
            // 设置为可访问
            // Make the method accessible.
            wakeUpMethod.setAccessible(true);

            // 调用 wakeUp 方法，传入当前的系统时间
            // Call wakeUp with the current system time.
            wakeUpMethod.invoke(powerManager, SystemClock.uptimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void goToSleep() {
        Log.d(TAG,"goToSleep");
        try {
            // 获取 PowerManager 实例
            // Get the PowerManager instance.
            PowerManager powerManager = (PowerManager) ContextUtil.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                throw new NullPointerException("PowerManager is null");
            }

            // 获取 goToSleep 方法
            // Get the goToSleep method.
            Method goToSleepMethod = PowerManager.class.getDeclaredMethod("goToSleep", long.class);
            // 设置为可访问
            // Make the method accessible.
            goToSleepMethod.setAccessible(true);

            // 调用 goToSleep 方法，传入当前时间
            // Call goToSleep with the current time.
            goToSleepMethod.invoke(powerManager, SystemClock.uptimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setScreenAutoSleepTime(long time) {
        Settings.System.putLong(ContextUtil.getApplicationContext().getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, time);
    }

    public static void updateUserActivity() {
        Log.d(TAG,"goToSleep");
        try {
            // 获取 PowerManager 实例
            // Get the PowerManager instance.
            PowerManager powerManager = (PowerManager) ContextUtil.getApplicationContext().getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                throw new NullPointerException("PowerManager is null");
            }
            // 获取 goToSleep 方法
            // Get the goToSleep method.
            @SuppressLint("DiscouragedPrivateApi") Method userActivityMethod = PowerManager.class.getDeclaredMethod("userActivity", long.class, boolean.class);
            // 设置为可访问
            // Make the method accessible.
            userActivityMethod.setAccessible(true);

            // 调用 goToSleep 方法，传入当前时间
            // Call goToSleep with the current time.
            userActivityMethod.invoke(powerManager, SystemClock.uptimeMillis(), true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
