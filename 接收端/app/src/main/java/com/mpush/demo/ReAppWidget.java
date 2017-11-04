package com.mpush.demo;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mpush.api.Constants;
import com.mpush.client.ClientConfig;
import com.mpush.xbw.BuildConfig;
import com.mpush.xbw.MPush;
import com.mpush.xbw.MPushService;
import com.mpush.xbw.Notifications;
import com.mpush.xbw.R;

import org.json.JSONObject;

/**
 * Implementation of App Widget functionality.
 */
public class ReAppWidget extends AppWidgetProvider {
    public static final String CLICK_ACTION = "android.appwidget.action.APPWIDGET_UPDATE";
    public String alloc="http://182.254.146.68:9999";
    public String userid="xubowen";
    boolean islight=false;
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        Intent intent = new Intent(CLICK_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,R.id.button, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.button, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (CLICK_ACTION.equals(intent.getAction())) {
            Notifications.I.init(context);
            MPush.I.bindAccount(userid, "mpush:" + (int) (Math.random() * 10));
            initPush(alloc, userid,context);
            MPush.I.checkInit(context).startPush();
            Toast.makeText(context, "就绪", Toast.LENGTH_SHORT).show();
        }
        if (MPushService.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
            byte[] bytes = intent.getByteArrayExtra(MPushService.EXTRA_PUSH_MESSAGE);
            int messageId = intent.getIntExtra(MPushService.EXTRA_PUSH_MESSAGE_ID, 0);
            String message = new String(bytes, Constants.UTF_8);

            //Toast.makeText(context, "收到新的通知：" + message, Toast.LENGTH_SHORT).show();

            if (messageId > 0) MPush.I.ack(messageId);
            if (TextUtils.isEmpty(message)) return;
            NotificationDO ndo = fromJson(message);
            if (ndo != null) {
                Intent it = new Intent(context, ReAppWidget.class);
                it.setAction(MPushService.ACTION_NOTIFICATION_OPENED);
                if (ndo.getExtras() != null) it.putExtra("my_extra", ndo.getExtras().toString());
                if (TextUtils.isEmpty(ndo.getTitle())) ndo.setTitle("嘿嘿");
                if (TextUtils.isEmpty(ndo.getTicker())) ndo.setTicker(ndo.getTitle());
                if (TextUtils.isEmpty(ndo.getContent())) ndo.setContent(ndo.getTitle());
                Notifications.I.notify(ndo, it);

                //String cnt=ndo.getContent();
                //String a[]=cnt.split(" ");
                String cmd=ndo.getCmd();
                if(cmd.equals("open灯")){
                    openLight();
                }else if(cmd.equals("close灯")){
                    closeLight();
                }else{
                    Toast.makeText(context, "错误命令" + cmd, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (MPushService.ACTION_NOTIFICATION_OPENED.equals(intent.getAction())) {
            Notifications.I.clean(intent);
            String extras = intent.getStringExtra("my_extra");
            Toast.makeText(context, "通知被点击了， extras=" + extras, Toast.LENGTH_SHORT).show();
        } else if (MPushService.ACTION_KICK_USER.equals(intent.getAction())) {
            Toast.makeText(context, "用户被踢下线了", Toast.LENGTH_SHORT).show();
        } else if (MPushService.ACTION_BIND_USER.equals(intent.getAction())) {
            Toast.makeText(context, "绑定用户:"
                            + intent.getStringExtra(MPushService.EXTRA_USER_ID)
                            + (intent.getBooleanExtra(MPushService.EXTRA_BIND_RET, false) ? "成功" : "失败")
                    , Toast.LENGTH_SHORT).show();
        } else if (MPushService.ACTION_UNBIND_USER.equals(intent.getAction())) {
            Toast.makeText(context, "解绑用户:"
                            + (intent.getBooleanExtra(MPushService.EXTRA_BIND_RET, false)
                            ? "成功"
                            : "失败")
                    , Toast.LENGTH_SHORT).show();
        } else if (MPushService.ACTION_CONNECTIVITY_CHANGE.equals(intent.getAction())) {
            Toast.makeText(context, intent.getBooleanExtra(MPushService.EXTRA_CONNECT_STATE, false)
                            ? "连接建立成功"
                            : "连接断开"
                    , Toast.LENGTH_SHORT).show();
        } else if (MPushService.ACTION_HANDSHAKE_OK.equals(intent.getAction())) {
            Toast.makeText(context, "握手成功, 心跳:" + intent.getIntExtra(MPushService.EXTRA_HEARTBEAT, 0)
                    , Toast.LENGTH_SHORT).show();
        }
    }
    private void initPush(String allocServer, String userId,Context context) {
        //公钥有服务端提供和私钥对应
        String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCghPCWCobG8nTD24juwSVataW7iViRxcTkey/B792VZEhuHjQvA3cAJgx2Lv8GnX8NIoShZtoCg3Cx6ecs+VEPD2fBcg2L4JK7xldGpOJ3ONEAyVsLOttXZtNXvyDZRijiErQALMTorcgi79M5uVX9/jMv2Ggb2XAeZhlLD28fHwIDAQAB";
        ClientConfig cc = ClientConfig.build()
                .setPublicKey(publicKey)
                .setAllotServer(allocServer)
                .setDeviceId(getDeviceId(context))
                .setClientVersion(BuildConfig.VERSION_NAME)
                .setEnableHttpProxy(true)
                .setUserId(userId);
        MPush.I.checkInit(context).setClientConfig(cc);
    }
    private String getDeviceId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Activity.TELEPHONY_SERVICE);
        String deviceId = tm.getDeviceId();
        if (TextUtils.isEmpty(deviceId)) {
            String time = Long.toString((System.currentTimeMillis() / (1000 * 60 * 60)));
            deviceId = time + time;
        }
        return deviceId;
    }
    private NotificationDO fromJson(String message) {
        try {
            JSONObject messageDO = new JSONObject(message);
            if (messageDO != null) {
                JSONObject jo = new JSONObject(messageDO.optString("content"));
                NotificationDO ndo = new NotificationDO();
                //MPUSH开源推送，userid content
                String a[]=jo.optString("content").split("，");
                String b[]=a[1].split(" ");
                ndo.setContent("命令："+b[0]+"请求"+b[1]);
                ndo.setCmd(b[1]);
                ndo.setTitle("嘿嘿");
                ndo.setTicker(jo.optString("ticker"));
                ndo.setNid(jo.optInt("nid", 1));
                ndo.setExtras(jo.optJSONObject("extras"));
                return ndo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private void openLight(){
        Camera camera = Camera.open();
        if (!islight) {
            Camera.Parameters mParameters = camera.getParameters();
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(mParameters);
            camera.startPreview();
            islight = true;
        }
    }
    private void closeLight(){
        Camera camera = Camera.open();
        if (islight) {
            Camera.Parameters mParameters = camera.getParameters();
            mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(mParameters);
            camera.stopPreview();
            islight = false;
        }
    }
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

