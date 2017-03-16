package br.com.heiderlopes.homeautomation.service;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import br.com.heiderlopes.homeautomation.ExampleActivity;
import br.com.heiderlopes.homeautomation.R;


public class MQTTService extends Service {

    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private ConnectivityManager mConnMan;
    private volatile IMqttAsyncClient mqttClient;


    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            IMqttToken token;

            boolean hasConnectivity;
            boolean hasChanged = false;

            NetworkInfo infos[] = mConnMan.getAllNetworkInfo();

            for(NetworkInfo info : infos) {

                if (info.getTypeName().equalsIgnoreCase("MOBILE")) {
                    if ((info.isConnected() != hasMmobile)) {
                        hasChanged = true;
                        hasMmobile = info.isConnected();
                    }
                } else if (info.getTypeName().equalsIgnoreCase("WIFI")) {
                    if ((info.isConnected() != hasWifi)) {
                        hasChanged = true;
                        hasWifi = info.isConnected();
                    }
                }
            }

            hasConnectivity = hasMmobile || hasWifi;

            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                doConnect();
            } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
                try {
                    token = mqttClient.disconnect();
                    token.waitForCompletion(1000);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*public class MQTTBinder extends Binder {
        public MQTTService getService(){
            return MQTTService.this;
        }
    }*/

    @Override
    public void onCreate() {
        IntentFilter intentf = new IntentFilter();
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new MQTTBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);

    }

    private void doConnect() {
        IMqttToken token;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        try {

            String clientId = MqttClient.generateClientId();

            mqttClient = new MqttAsyncClient("tcp://10.167.68.26:1883", clientId, new MemoryPersistence());
            token = mqttClient.connect();

            token.waitForCompletion(3500);

            mqttClient.setCallback(new MqttEventCallback());

            token = mqttClient.subscribe("/homeautomation/lampada", 0);

            token.waitForCompletion(5000);

        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            //e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "SUBIUUUUUU!", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    private class MqttEventCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable arg0) {


        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            Handler h = new Handler(getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    createNotification(msg.toString());
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotification(String mensagem) {
        Intent intent = new Intent(this, ExampleActivity.class);
        intent.putExtra("mensagem", mensagem);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT |
                Intent.FLAG_ACTIVITY_NO_ANIMATION);

        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        Notification noti = new Notification.Builder(this)
                .setContentTitle("Home Automation")
                .setContentText(mensagem)
                .setSmallIcon(R.drawable.smart_home)
                .setContentIntent(pIntent)
                //.addAction(R.mipmap.ic_launcher, "Call", pIntent)
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        noti.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(0, noti);
    }
}