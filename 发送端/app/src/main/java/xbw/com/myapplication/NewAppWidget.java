package xbw.com.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.app.AlertDialog;
import com.mpush.api.Constants;
import com.mpush.api.http.HttpCallback;
import com.mpush.api.http.HttpMethod;
import com.mpush.api.http.HttpRequest;
import com.mpush.api.http.HttpResponse;
import com.mpush.client.ClientConfig;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import xbw.com.android.MPush;
import xbw.com.android.MPushService;
import xbw.com.android.Notifications;

/**
 * Implementation of App Widget functionality.
 */
public class NewAppWidget extends AppWidgetProvider {
    public static final String CLICK_ACTION = "android.appwidget.action.APPWIDGET_UPDATE";
    public static final String CLICK_ACTION2 = "android.appwidget.action.APPWIDGET_UPDATE2";
    public String alloc="http://182.254.146.68:9999";
    public String userid="shit";
    public String userto="xubowen";
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.new_app_widget);
        Intent intent = new Intent(CLICK_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,R.id.button, intent,PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.button, pendingIntent);
        Intent intent2 = new Intent(CLICK_ACTION2);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context,R.id.button2, intent2,PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.button2, pendingIntent2);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (CLICK_ACTION.equals(intent.getAction())) {
            MPush.I.bindAccount(userid, "mpush:" + (int) (Math.random() * 10));
            initPush(alloc, userid,context);
            MPush.I.checkInit(context).startPush();
            try{
                sendPush("open灯",context);
                Toast.makeText(context, "开灯!", Toast.LENGTH_SHORT).show();
            }catch (Exception e){
                Toast.makeText(context,"Ooops",Toast.LENGTH_LONG).show();
            }
            //Toast.makeText(context, "hello dog!", Toast.LENGTH_SHORT).show();
        }else if(CLICK_ACTION2.equals(intent.getAction())){
            try{
                sendPush("close灯",context);
                Toast.makeText(context, "关灯!", Toast.LENGTH_SHORT).show();
            }catch (Exception e){
                Toast.makeText(context,"Ooops",Toast.LENGTH_LONG).show();
            }
        }
        if (MPushService.ACTION_MESSAGE_RECEIVED.equals(intent.getAction())) {
            byte[] bytes = intent.getByteArrayExtra(MPushService.EXTRA_PUSH_MESSAGE);
            int messageId = intent.getIntExtra(MPushService.EXTRA_PUSH_MESSAGE_ID, 0);
            String message = new String(bytes, Constants.UTF_8);

            Toast.makeText(context, "收到新的通知：" + message, Toast.LENGTH_SHORT).show();

            if (messageId > 0) MPush.I.ack(messageId);

            if (TextUtils.isEmpty(message)) return;

            NotificationDO ndo = fromJson(message);

            if (ndo != null) {
                Intent it = new Intent(context, NewAppWidget.class);
                it.setAction(MPushService.ACTION_NOTIFICATION_OPENED);
                if (ndo.getExtras() != null) it.putExtra("my_extra", ndo.getExtras().toString());
                if (TextUtils.isEmpty(ndo.getTitle())) ndo.setTitle("MPush");
                if (TextUtils.isEmpty(ndo.getTicker())) ndo.setTicker(ndo.getTitle());
                if (TextUtils.isEmpty(ndo.getContent())) ndo.setContent(ndo.getTitle());
                Notifications.I.notify(ndo, it);
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
                            ? "MPUSH连接建立成功"
                            : "MPUSH连接断开"
                    , Toast.LENGTH_SHORT).show();
        } else if (MPushService.ACTION_HANDSHAKE_OK.equals(intent.getAction())) {
            Toast.makeText(context, "MPUSH握手成功, 心跳:" + intent.getIntExtra(MPushService.EXTRA_HEARTBEAT, 0)
                    , Toast.LENGTH_SHORT).show();
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
                ndo.setContent(jo.optString("content"));
                ndo.setTitle(jo.optString("title"));
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
    public void sendPush(String s,final Context context) throws Exception {
        String to = userto;
        String from = userid;
        String hello = s;
        JSONObject params = new JSONObject();
        params.put("userId", to);
        params.put("hello", from  +" "+ hello);

        HttpRequest request = new HttpRequest(HttpMethod.POST, alloc + "/push");
        byte[] body = params.toString().getBytes(Constants.UTF_8);
        request.setBody(body, "application/json; charset=utf-8");
        request.setTimeout((int) TimeUnit.SECONDS.toMillis(10));
        MPush.I.sendHttpProxy(request);
    }

}

