package com.hosea.messagerelayer.utils.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.hosea.messagerelayer.bean.ForwardingLog;

import java.util.ArrayList;

/**
 * 转发日志数据库操作类
 */
public class ForwardingLogManager {

    public static final String TABLE_NAME = "forwarding_log";
    public static final String COL_ID = "id";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_SENDER_MOBILE = "sender_mobile";
    public static final String COL_RELAY_TYPE = "relay_type";
    public static final String COL_CONTENT_PREVIEW = "content_preview";
    public static final String COL_STATUS = "status";
    public static final String COL_ERROR_MSG = "error_msg";

    public static final String CREATE_TABLE_SQL = "CREATE TABLE " + TABLE_NAME + " ("
            + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COL_TIMESTAMP + " INTEGER, "
            + COL_SENDER_MOBILE + " VARCHAR(20), "
            + COL_RELAY_TYPE + " VARCHAR(10), "
            + COL_CONTENT_PREVIEW + " TEXT, "
            + COL_STATUS + " INTEGER, "
            + COL_ERROR_MSG + " TEXT)";

    private DataBaseHelper mHelper;

    public ForwardingLogManager(Context context) {
        mHelper = new DataBaseHelper(context);
    }

    /**
     * 便捷方法：记录一条转发日志
     */
    public static void logRelay(Context context, String senderMobile, String relayType,
                                String content, int status, String errorMsg) {
        DataBaseHelper helper = new DataBaseHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TIMESTAMP, System.currentTimeMillis());
        values.put(COL_SENDER_MOBILE, senderMobile);
        values.put(COL_RELAY_TYPE, relayType);
        // 截取前100个字符作为预览
        String preview = (content != null && content.length() > 100)
                ? content.substring(0, 100) : content;
        values.put(COL_CONTENT_PREVIEW, preview);
        values.put(COL_STATUS, status);
        values.put(COL_ERROR_MSG, errorMsg);
        db.insert(TABLE_NAME, null, values);
        helper.close();
    }

    /**
     * 分页查询日志，按时间倒序
     */
    public ArrayList<ForwardingLog> queryLogs(int limit, int offset) {
        ArrayList<ForwardingLog> logs = new ArrayList<>();
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null,
                COL_TIMESTAMP + " DESC", offset + "," + limit);
        while (cursor.moveToNext()) {
            ForwardingLog log = new ForwardingLog();
            log.setId(cursor.getLong(cursor.getColumnIndex(COL_ID)));
            log.setTimestamp(cursor.getLong(cursor.getColumnIndex(COL_TIMESTAMP)));
            log.setSenderMobile(cursor.getString(cursor.getColumnIndex(COL_SENDER_MOBILE)));
            log.setRelayType(cursor.getString(cursor.getColumnIndex(COL_RELAY_TYPE)));
            log.setContentPreview(cursor.getString(cursor.getColumnIndex(COL_CONTENT_PREVIEW)));
            log.setStatus(cursor.getInt(cursor.getColumnIndex(COL_STATUS)));
            log.setErrorMsg(cursor.getString(cursor.getColumnIndex(COL_ERROR_MSG)));
            logs.add(log);
        }
        cursor.close();
        return logs;
    }

    /**
     * 清空所有日志
     */
    public void clearAllLogs() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public void closeHelper() {
        mHelper.close();
    }
}
