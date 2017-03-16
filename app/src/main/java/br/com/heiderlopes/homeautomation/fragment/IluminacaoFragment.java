package br.com.heiderlopes.homeautomation.fragment;


import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;

import br.com.heiderlopes.homeautomation.ExampleActivity;
import br.com.heiderlopes.homeautomation.R;
import br.com.heiderlopes.homeautomation.service.MQTTService;
import br.com.heiderlopes.homeautomation.utils.MQTTConstantes;

public class IluminacaoFragment extends Fragment {

    private static final int REQ_CODE_SPEECH_INPUT = 100;

    private MqttAndroidClient client;
    private String TAG = "ILUMINACAO_FRAGMENT";

    public IluminacaoFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        connectMQTTClient();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnectMQTTClient();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_iluminacao, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.ac_comando_voz) {
            startVoiceInput();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Olá, como posso ajudá-lo?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == getActivity().RESULT_OK && null != data) {
                    List<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    //Toast.makeText(getContext(), result.get(0), Toast.LENGTH_LONG).show();

                    if(result.get(0).equals("ligar")) {
                        publish("1");
                    } else if(result.get(0).equals("desligar")) {
                        publish("0");
                    }
                }
                break;
            }

        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_iluminacao, container, false);
    }

    private void connectMQTTClient() {
        String clientId = MqttClient.generateClientId();

        client =
                new MqttAndroidClient(getContext(),
                        MQTTConstantes.MQTT_SERVER_URI,
                        clientId);

        try {
            IMqttToken token = client.connect();

            //token.waitForCompletion(3500);

            client.setCallback(new MqttEventCallback());

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess");
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onFailure");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe() {
        String topic = "/homeautomation/lampada";
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // The message was published
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnectMQTTClient() {
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // we are now successfully disconnected
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // something went wrong, but probably we are disconnected anyway
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String msg) {

        byte[] encodedPayload;
        try {
            encodedPayload = msg.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            message.setRetained(true);
            client.publish(MQTTConstantes.TOPICO_LAMPADA, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }

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
            Handler h = new Handler(getContext().getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    //createNotification(msg.toString());
                    Toast.makeText(getContext(), msg.toString(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
