package com.hosea.messagerelayer.utils;

import android.util.Log;

import com.blankj.utilcode.util.LogUtils;
import com.hosea.messagerelayer.bean.EmailMessage;
import com.hosea.messagerelayer.confing.Constant;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by WHF on 2017/3/25.
 */

public class EmailRelayerManager {

    public static final int CODE_SUCCESS = 0x1;
    public static final int CODE_FAILE = 0x0;

    private static final String PORT_SSL = "465";

    private static final String HOST_QQ = "smtp.qq.com";
    private static final String HOST_163 = "smtp.163.com";
    private static final String HOST_126 = "smtp.126.com";
    private static final String HOST_GMAIL = "smtp.gmail.com";
    private static final String HOST_OUTLOOK = "smtp.outlook.com";

    //发送短信至目标邮件
    public static int relayEmail(NativeDataManager dataManager, String title, String content) {
        Properties props = new Properties();
        User user = getSenderUser(dataManager);
        EmailMessage emailMessage = creatEmailMessage(title, content, dataManager);
        setHost(dataManager, props);

        //是否开启SSL
        if (dataManager.getEmailSsl()) {
            if (dataManager.getEmailServicer() == Constant.EMAIL_SERVICER_OTHER) {
                setSslMode(props, PORT_SSL);
            } else {
                String port = dataManager.getEmailPort();
                if (port != null) {
                    setSslMode(props, port);
                }
            }
        }

        setSenderToPro(props, user);
        props.put("mail.smtp.auth", true);//如果不设置，则报553错误
        props.put("mail.transport.protocol", "smtp");

        //getDefaultInstace得到的始终是该方法初次创建的缺省的对象，getInstace每次获取新对象
        Session session = Session.getInstance(props
                , new SmtpAuthenticator(user));
        session.setDebug(true);

        try {
            MimeMessage message = creatMessage(session, emailMessage);
            Transport.send(message);
            return CODE_SUCCESS;
        } catch (MessagingException e) {
            e.printStackTrace();
            return CODE_FAILE;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return CODE_FAILE;
        }
    }

    /**
     * 创建邮件消息对象
     *
     * @param session
     * @param emailMessage
     * @return
     * @throws UnsupportedEncodingException
     * @throws MessagingException
     */
    private static MimeMessage creatMessage(Session session, EmailMessage emailMessage)
            throws UnsupportedEncodingException, MessagingException {

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(emailMessage.getSenderAccount()
                , emailMessage.getSenderName(), "UTF-8"));//发件人
        message.setRecipients(MimeMessage.RecipientType.TO, emailMessage.getReceiverAccount());//收件人
        message.setSubject(emailMessage.getSubject());//主题
        message.setContent(emailMessage.getContent(), "text/html;charset=UTF-8");
        return message;
    }

    /**
     * SMTP 服务器的端口 (非 SSL 连接的端口一般默认为 25, 可以不添加, 如果
     * 开启了 SSL 连接,需要改为对应邮箱的 SMTP 服务器的端口, 具体可查看对应
     * 邮箱服务的帮助,QQ邮箱的SMTP(SLL)端口为465或587, 其他邮箱自行去查看)
     *
     * @param props
     * @param smtpPort
     * @return
     */
    private static void setSslMode(Properties props, String smtpPort) {
        props.setProperty("mail.smtp.port", smtpPort);
        props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.socketFactory.port", smtpPort);
    }

    /**
     * 从本地数据获取发送方账号和密码
     *
     * @param dataManager
     * @return
     */
    private static User getSenderUser(NativeDataManager dataManager) {
        return new User(dataManager.getEmailAccount(), dataManager.getEmailPassword());
    }

    /**
     * 将发送发的账号和密码设置给配置文件
     *
     * @param properties
     * @param user
     * @return
     */
    private static void setSenderToPro(Properties properties, User user) {
        properties.put("mail.smtp.username", user.account);
        properties.put("mail.smtp.password", user.password);
    }

    /**
     * 设置主机
     *
     * @param dataManager
     * @param props
     * @return
     */
    private static void setHost(NativeDataManager dataManager, Properties props) {

        switch (dataManager.getEmailServicer()) {
            case Constant.EMAIL_SERVICER_QQ:
                props.put("mail.smtp.host", HOST_QQ);
                break;
            case Constant.EMAIL_SERVICER_163:
                props.put("mail.smtp.host", HOST_163);
                break;
            case Constant.EMAIL_SERVICER_126:
                props.put("mail.smtp.host", HOST_126);
                break;
            case Constant.EMAIL_SERVICER_OUTLOOK:
                props.put("mail.smtp.host", HOST_OUTLOOK);
                break;
            case Constant.EMAIL_SERVICER_GMAIL:
                props.put("mail.smtp.host", HOST_GMAIL);
                break;
            case Constant.EMAIL_SERVICER_OTHER:
                String host = dataManager.getEmailHost();
                if (host != null) {
                    props.put("mail.smtp.host", host);
                }
                break;
        }
    }


    /**
     * 登录认证
     */
    private static class SmtpAuthenticator extends Authenticator {
        String mUsername;
        String mPassword;

        public SmtpAuthenticator(User user) {
            super();
            this.mUsername = user.account;
            this.mPassword = user.password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            if ((mUsername != null) && (mUsername.length() > 0) && (mPassword != null)
                    && (mPassword.length() > 0)) {
                return new PasswordAuthentication(mUsername, mPassword);
            }
            return null;
        }
    }

    /**
     * 发送方账户密码实体类
     */
    private static class User {
        String account;
        String password;

        User(String account, String password) {
            this.account = account;
            this.password = password;
        }
    }

    /**
     * 封装消息实体
     *
     * @param content
     * @param dataManager
     * @return
     */
    private static EmailMessage creatEmailMessage(String title, String content, NativeDataManager dataManager) {
        EmailMessage message = new EmailMessage();
        message.setContent(content);
        message.setSenderAccount(dataManager.getEmailAccount());
        message.setSenderName(dataManager.getEmailSenderName());
        message.setReceiverAccount(dataManager.getEmailToAccount());
         LogUtils.i("EmailRelayerManager", "real_length()" + getRealLength(content) + "");
         LogUtils.i("EmailRelayerManager", "content.length():" + content.length() + "");
        //小于中文加英文大于240个字符就显示原主题.
        if (getRealLength(content) < 240) {
            String[] split = content.split("<br>");
            if (split.length > 2) {
                message.setSubject(split[1].trim());
            } else {
                //发送测试配置.
                message.setSubject(title);
            }
        } else {
//            message.setSubject(dataManager.getEmailSubject());
            message.setSubject(title);
        }
        return message;
    }


    public static int getRealLength(String str) {
        int m = 0;
        char arr[] = str.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char c = arr[i];
            // 中文字符(根据Unicode范围判断),中文字符长度为2
            if ((c >= 0x0391 && c <= 0xFFE5)) {
                m = m + 2;
            } else if ((c >= 0x0000 && c <= 0x00FF)) // 英文字符
            {
                m = m + 1;
            }
        }
        return m;

    }

}
