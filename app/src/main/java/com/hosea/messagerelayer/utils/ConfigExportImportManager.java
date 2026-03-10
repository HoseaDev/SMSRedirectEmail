package com.hosea.messagerelayer.utils;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.bean.Contact;
import com.hosea.messagerelayer.bean.SmsBean;
import com.hosea.messagerelayer.confing.Constant;
import com.hosea.messagerelayer.utils.db.DataBaseManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 配置导入/导出管理器
 */
public class ConfigExportImportManager {

    private static final String TAG = "ConfigExportImport";

    /**
     * 导出所有配置为 JSON
     */
    public static JSONObject exportConfig(Context context) throws JSONException {
        NativeDataManager mgr = new NativeDataManager(context);
        DataBaseManager dbMgr = new DataBaseManager(context);

        JSONObject root = new JSONObject();
        root.put("version", Constant.CONFIG_EXPORT_VERSION);
        root.put("export_time", System.currentTimeMillis());

        // SharedPreferences 设置
        JSONObject settings = new JSONObject();
        settings.put(Constant.KEY_RECEIVER, mgr.getReceiver());
        settings.put(Constant.KEY_RELAY_SMS, mgr.getSmsRelay());
        settings.put(Constant.KEY_RELAY_INNER_SMS, mgr.getInnerRelay());
        settings.put(Constant.KEY_RELAY_EMAIL, mgr.getEmailRelay());
        settings.put(Constant.KEY_RELAY_WECHAT, mgr.getWeChatRelay());
        settings.put(Constant.KEY_OBJECT_MOBILE, mgr.getObjectMobile());
        settings.put(Constant.KEY_INNER_MOBILE, mgr.getInnerMobile());
        settings.put(Constant.KEY_INNER_MOBILE_RULE, mgr.getInnerRule());
        settings.put(Constant.KEY_EMAIL_SERVICER, mgr.getEmailServicer());
        settings.put(Constant.KEY_EMAIL_ACCOUNT, mgr.getEmailAccount());
        settings.put(Constant.KEY_EMAIL_PASSWORD, mgr.getEmailPassword());
        settings.put(Constant.KEY_EMAIL_HOST, mgr.getEmailHost());
        settings.put(Constant.KEY_EMAIL_PORT, mgr.getEmailPort());
        settings.put(Constant.KEY_EMAIL_SSL, mgr.getEmailSsl());
        settings.put(Constant.KEY_EMAIL_TO_ACCOUNT, mgr.getEmailToAccount());
        settings.put(Constant.KEY_EMAIL_SENDER_NAME, mgr.getEmailSenderName());
        settings.put(Constant.KEY_EMAIL_SUBJECT, mgr.getEmailSubject());
        settings.put(Constant.KEY_CONTENT_PREFIX, mgr.getContentPrefix());
        settings.put(Constant.KEY_CONTENT_SUFFIX, mgr.getContentSuffix());
        root.put("settings", settings);

        // 关键字列表
        Set<String> keywords = mgr.getKeywordSet();
        JSONArray keywordArray = new JSONArray();
        for (String kw : keywords) {
            keywordArray.put(kw);
        }
        root.put("keywords", keywordArray);

        // 联系人白名单
        ArrayList<Contact> contacts = dbMgr.getAllContact();
        JSONArray contactArray = new JSONArray();
        for (Contact c : contacts) {
            JSONObject cObj = new JSONObject();
            cObj.put("name", c.getContactName());
            cObj.put("mobile", c.getContactNum());
            contactArray.put(cObj);
        }
        root.put("contacts", contactArray);

        // 短信拦截名单
        ArrayList<SmsBean> intercepts = dbMgr.getSmsIntercept();
        JSONArray interceptArray = new JSONArray();
        for (SmsBean s : intercepts) {
            JSONObject sObj = new JSONObject();
            sObj.put("name", s.getName());
            sObj.put("mobile", s.getPhone());
            interceptArray.put(sObj);
        }
        root.put("sms_intercept", interceptArray);

        dbMgr.closeHelper();
        return root;
    }

    /**
     * 导入配置
     */
    public static void importConfig(Context context, JSONObject root) throws JSONException {
        int version = root.optInt("version", 0);
        if (version < 1) {
            throw new JSONException("无效的配置文件版本");
        }

        NativeDataManager mgr = new NativeDataManager(context);
        DataBaseManager dbMgr = new DataBaseManager(context);

        // 还原 SharedPreferences
        JSONObject settings = root.getJSONObject("settings");
        mgr.setReceiver(settings.optBoolean(Constant.KEY_RECEIVER, true));
        mgr.setSmsRelay(settings.optBoolean(Constant.KEY_RELAY_SMS, false));
        mgr.setInnerRelay(settings.optBoolean(Constant.KEY_RELAY_INNER_SMS, false));
        mgr.setEmailRelay(settings.optBoolean(Constant.KEY_RELAY_EMAIL, false));
        mgr.setWeChatRelay(settings.optBoolean(Constant.KEY_RELAY_WECHAT, false));

        if (settings.has(Constant.KEY_OBJECT_MOBILE))
            mgr.setObjectMobile(settings.getString(Constant.KEY_OBJECT_MOBILE));
        if (settings.has(Constant.KEY_INNER_MOBILE))
            mgr.setInnerMobile(settings.getString(Constant.KEY_INNER_MOBILE));
        if (settings.has(Constant.KEY_INNER_MOBILE_RULE))
            mgr.setInnerRule(settings.getString(Constant.KEY_INNER_MOBILE_RULE));
        if (settings.has(Constant.KEY_EMAIL_SERVICER))
            mgr.setEmailServicer(settings.getString(Constant.KEY_EMAIL_SERVICER));
        if (settings.has(Constant.KEY_EMAIL_ACCOUNT))
            mgr.setEmailAccount(settings.getString(Constant.KEY_EMAIL_ACCOUNT));
        if (settings.has(Constant.KEY_EMAIL_PASSWORD))
            mgr.setEmailPassword(settings.optString(Constant.KEY_EMAIL_PASSWORD, null));
        if (settings.has(Constant.KEY_EMAIL_HOST))
            mgr.setEmailHost(settings.optString(Constant.KEY_EMAIL_HOST, null));
        if (settings.has(Constant.KEY_EMAIL_PORT))
            mgr.setEmailPort(settings.optString(Constant.KEY_EMAIL_PORT, null));
        mgr.setEmailSsl(settings.optBoolean(Constant.KEY_EMAIL_SSL, true));
        if (settings.has(Constant.KEY_EMAIL_TO_ACCOUNT))
            mgr.setEmailToAccount(settings.getString(Constant.KEY_EMAIL_TO_ACCOUNT));
        if (settings.has(Constant.KEY_EMAIL_SENDER_NAME))
            mgr.setEmailSenderName(settings.getString(Constant.KEY_EMAIL_SENDER_NAME));
        if (settings.has(Constant.KEY_EMAIL_SUBJECT))
            mgr.setEmailSubject(settings.getString(Constant.KEY_EMAIL_SUBJECT));
        if (settings.has(Constant.KEY_CONTENT_PREFIX))
            mgr.setContentPrefix(settings.optString(Constant.KEY_CONTENT_PREFIX, null));
        if (settings.has(Constant.KEY_CONTENT_SUFFIX))
            mgr.setContentSuffix(settings.optString(Constant.KEY_CONTENT_SUFFIX, null));

        // 还原关键字
        if (root.has("keywords")) {
            JSONArray keywordArray = root.getJSONArray("keywords");
            Set<String> keywords = new HashSet<>();
            for (int i = 0; i < keywordArray.length(); i++) {
                keywords.add(keywordArray.getString(i));
            }
            mgr.setKeywordSet(keywords);
        }

        // 还原联系人（先清空再插入）
        dbMgr.deleteAll();
        if (root.has("contacts")) {
            JSONArray contactArray = root.getJSONArray("contacts");
            for (int i = 0; i < contactArray.length(); i++) {
                JSONObject cObj = contactArray.getJSONObject(i);
                Contact c = new Contact(cObj.getString("name"), cObj.getString("mobile"));
                dbMgr.addContact(c);
            }
        }

        // 还原拦截名单（先清空再插入）
        if (root.has("sms_intercept")) {
            // 先清空现有拦截
            ArrayList<SmsBean> existing = dbMgr.getSmsIntercept();
            for (SmsBean s : existing) {
                dbMgr.deleteSmsFromMobile(s.getPhone());
            }
            JSONArray interceptArray = root.getJSONArray("sms_intercept");
            for (int i = 0; i < interceptArray.length(); i++) {
                JSONObject sObj = interceptArray.getJSONObject(i);
                SmsBean s = new SmsBean();
                s.setName(sObj.getString("name"));
                s.setPhone(sObj.getString("mobile"));
                dbMgr.addSMSIntercept(s);
            }
        }

        dbMgr.closeHelper();
    }

    /**
     * 导出配置到 Downloads 文件夹，返回文件名
     */
    public static String exportToFile(Context context) throws Exception {
        JSONObject config = exportConfig(context);
        String jsonStr = config.toString(2);
        String fileName = "messagerelayer_config_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                + ".json";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用 MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                OutputStream os = context.getContentResolver().openOutputStream(uri);
                if (os == null) {
                    throw new Exception("无法打开输出流");
                }
                try {
                    os.write(jsonStr.getBytes("UTF-8"));
                } finally {
                    os.close();
                }
            } else {
                throw new Exception("无法创建文件");
            }
        } else {
            // Android 9 及以下直接写文件
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonStr.getBytes("UTF-8"));
            fos.close();
        }

        return fileName;
    }

    /**
     * 从 URI 读取 JSON 配置
     */
    public static JSONObject readFromUri(Context context, Uri uri) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(uri);
        if (is == null) {
            throw new Exception("无法打开文件");
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            return new JSONObject(sb.toString());
        } finally {
            is.close();
        }
    }
}
