package br.com.heiderlopes.homeautomation.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import br.com.heiderlopes.homeautomation.service.MQTTService;


public class BootReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        Log.i("MQTT", "INICIEI");
        Log.i("MQTT", "BOOT_COMPLETED");
        Intent serviceIntent = new Intent(context, MQTTService.class);
        context.startService(serviceIntent);
    }
}