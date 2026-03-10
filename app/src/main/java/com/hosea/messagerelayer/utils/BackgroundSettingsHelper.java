package com.hosea.messagerelayer.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.blankj.utilcode.util.LogUtils;

/**
 * 国产手机后台保活引导工具类。
 * 针对不同品牌跳转到对应的"自启动管理"或"后台运行"设置页面。
 */
public class BackgroundSettingsHelper {

    private static final String TAG = "BackgroundSettingsHelper";
    private static final String PREFS_NAME = "bg_settings_prefs";
    private static final String KEY_HAS_GUIDED = "has_guided_background";

    /**
     * 是否已经引导过（每个设备只弹一次）
     */
    public static boolean hasGuided(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_HAS_GUIDED, false);
    }

    /**
     * 标记已引导
     */
    public static void markGuided(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_HAS_GUIDED, true).apply();
    }

    /**
     * 重置引导状态（方便用户从"关于"页面重新触发）
     */
    public static void resetGuided(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_HAS_GUIDED, false).apply();
    }

    /**
     * 获取当前品牌对应的引导文案
     */
    public static String getGuideMessage() {
        String brand = Build.MANUFACTURER.toLowerCase();
        if (brand.contains("huawei") || brand.contains("honor")) {
            return "检测到华为/荣耀手机，请在接下来的页面中：\n\n" +
                    "1. 找到本应用\n" +
                    "2. 关闭\"自动管理\"\n" +
                    "3. 手动打开\"允许自启动\"、\"允许后台活动\"、\"允许关联启动\"\n\n" +
                    "这样才能确保短信转发不被系统中断。";
        } else if (brand.contains("xiaomi") || brand.contains("redmi")) {
            return "检测到小米/红米手机，请在接下来的页面中：\n\n" +
                    "1. 找到本应用\n" +
                    "2. 开启\"自启动\"权限\n" +
                    "3. 同时建议在\"省电策略\"中设为\"无限制\"\n\n" +
                    "这样才能确保短信转发不被系统中断。";
        } else if (brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus")) {
            return "检测到 OPPO/Realme/一加手机，请在接下来的页面中：\n\n" +
                    "1. 找到本应用\n" +
                    "2. 允许\"自启动\"和\"后台运行\"\n\n" +
                    "这样才能确保短信转发不被系统中断。";
        } else if (brand.contains("vivo") || brand.contains("iqoo")) {
            return "检测到 vivo/iQOO 手机，请在接下来的页面中：\n\n" +
                    "1. 找到本应用\n" +
                    "2. 开启\"允许后台高耗电\"和\"自启动\"\n\n" +
                    "这样才能确保短信转发不被系统中断。";
        } else if (brand.contains("samsung")) {
            return "检测到三星手机，请在接下来的页面中：\n\n" +
                    "1. 找到本应用\n" +
                    "2. 在\"电池\"设置中选择\"不受限\"\n\n" +
                    "这样才能确保短信转发不被系统中断。";
        } else if (brand.contains("meizu")) {
            return "检测到魅族手机，请在接下来的页面中：\n\n" +
                    "1. 找到本应用\n" +
                    "2. 允许\"后台管理\"为\"保持后台运行\"\n\n" +
                    "这样才能确保短信转发不被系统中断。";
        } else {
            return "为确保短信能正常转发，请在接下来的页面中允许本应用\"自启动\"和\"后台运行\"。\n\n" +
                    "不同手机设置位置不同，一般在\"电池\"或\"应用管理\"中。";
        }
    }

    /**
     * 尝试跳转到厂商自启动/后台管理设置页面。
     * 返回 true 表示成功跳转，false 表示该品牌没有适配或跳转失败。
     */
    public static boolean openAutoStartSettings(Context context) {
        String brand = Build.MANUFACTURER.toLowerCase();
        LogUtils.i(TAG, "手机品牌: " + brand);

        // 按品牌逐一尝试
        Intent intent = null;

        try {
            if (brand.contains("huawei") || brand.contains("honor")) {
                intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                context.startActivity(intent);
                return true;
            }

            if (brand.contains("xiaomi") || brand.contains("redmi")) {
                intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                context.startActivity(intent);
                return true;
            }

            if (brand.contains("oppo") || brand.contains("realme") || brand.contains("oneplus")) {
                // ColorOS
                intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.startupapp.StartupAppListActivity"));
                context.startActivity(intent);
                return true;
            }

            if (brand.contains("vivo") || brand.contains("iqoo")) {
                intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.iqoo.secure",
                        "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"));
                context.startActivity(intent);
                return true;
            }

            if (brand.contains("samsung")) {
                intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"));
                context.startActivity(intent);
                return true;
            }

            if (brand.contains("meizu")) {
                intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.putExtra("packageName", context.getPackageName());
                context.startActivity(intent);
                return true;
            }

            // Letv / 乐视
            if (brand.contains("letv") || brand.contains("leeco")) {
                intent = new Intent();
                intent.setComponent(new ComponentName(
                        "com.letv.android.letvsafe",
                        "com.letv.android.letvsafe.AutobootManageActivity"));
                context.startActivity(intent);
                return true;
            }

        } catch (Exception e) {
            LogUtils.w(TAG, "跳转厂商设置页失败: " + e.getMessage());
        }

        // 兜底：跳转到系统应用详情页
        try {
            intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            LogUtils.e(TAG, "跳转应用详情也失败: " + e.getMessage());
        }

        return false;
    }
}
