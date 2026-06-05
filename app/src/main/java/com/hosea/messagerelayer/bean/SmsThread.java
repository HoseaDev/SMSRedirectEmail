package com.hosea.messagerelayer.bean;

/**
 * 短信会话线程数据模型
 * 用于收件箱列表，代表与某一联系人/号码的全部对话
 */
public class SmsThread {

    /** 系统短信数据库的 thread_id */
    private long threadId;

    /** 对方号码 */
    private String address;

    /** 联系人名称，未存储到通讯录时为 null */
    private String contactName;

    /** 该会话最后一条短信内容摘要 */
    private String lastBody;

    /** 最后一条短信的时间戳（毫秒） */
    private long lastDate;

    /** 未读短信数量 */
    private int unreadCount;

    /**
     * 收到短信的 SIM 卡槽索引
     * 0 = 卡1，1 = 卡2，-1 = 未知（发出的消息或系统不支持双卡）
     */
    private int simSlot;

    // ----------------------------- getter / setter -----------------------------

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getLastBody() {
        return lastBody;
    }

    public void setLastBody(String lastBody) {
        this.lastBody = lastBody;
    }

    public long getLastDate() {
        return lastDate;
    }

    public void setLastDate(long lastDate) {
        this.lastDate = lastDate;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getSimSlot() {
        return simSlot;
    }

    public void setSimSlot(int simSlot) {
        this.simSlot = simSlot;
    }

    // ----------------------------- 业务方法 ------------------------------------

    /**
     * 获取用于界面显示的名称：
     * 联系人名不为空时返回联系人名，否则返回号码
     */
    public String getDisplayName() {
        if (contactName != null && !contactName.isEmpty()) {
            return contactName;
        }
        return address != null ? address : "";
    }
}
