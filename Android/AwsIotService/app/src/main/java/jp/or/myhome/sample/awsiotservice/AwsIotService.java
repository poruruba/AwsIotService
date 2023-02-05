package jp.or.myhome.sample.awsiotservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import java.security.KeyStore;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.regions.Regions;

public class AwsIotService extends Service {
    public static final String TAG = MainActivity.TAG;
    Context context;
    static final String CHANNEL_ID = "default_channel_id";
    static final String NOTIFICATION_CONTENT = "受信待ち受け中";
    static final String NOTIFICATION_TITLE = "AwsIotService";
    static final int NOTIFICATION_ID = 1;
    static final String AWSIOT_DEFAULT_PASSWORD = "default_password";
    private static final String AWSIOT_CERT_ID = "default_cert_id";
    private static final String AWSIOT_KEY_STORE_NAME = "default.jks";
    AWSIotMqttManager mqttManager;
    boolean isSubscribed = false;
    String mTopicName;
    RemoteCallbackList<IAwsIotServiceListener> listeners;

    public static void saveCertificate(String keyStorePath, String cert, String priv) {
        if (AWSIotKeystoreHelper.isKeystorePresent(keyStorePath, AWSIOT_KEY_STORE_NAME))
            AWSIotKeystoreHelper.deleteKeystoreAlias(AWSIOT_CERT_ID, keyStorePath, AWSIOT_KEY_STORE_NAME, AWSIOT_DEFAULT_PASSWORD);
        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(AWSIOT_CERT_ID, cert, priv, keyStorePath, AWSIOT_KEY_STORE_NAME, AWSIOT_DEFAULT_PASSWORD);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");

        context = getApplicationContext();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NOTIFICATION_TITLE, NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        listeners = new RemoteCallbackList<IAwsIotServiceListener>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");

        connectionClose();

        String endpointPrefix = intent.getStringExtra("endpointPrefix");
        String keyStorePath = intent.getStringExtra("keyStorePath");
        mTopicName = intent.getStringExtra("topicName");
        String clientId = intent.getStringExtra("clientId");
        if (endpointPrefix != null && keyStorePath != null && clientId != null && mTopicName != null ) {
            try {
                mqttManager = new AWSIotMqttManager(clientId, Region.getRegion(Regions.AP_NORTHEAST_1), endpointPrefix);
                KeyStore keyStore = AWSIotKeystoreHelper.getIotKeystore(AWSIOT_CERT_ID, keyStorePath, AWSIOT_KEY_STORE_NAME, AWSIOT_DEFAULT_PASSWORD);
                mqttManager.connect(keyStore, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                        Log.d(TAG, "AWSIotMqttClientStatus changed.(" + status.toString() + ")");
                        if (status.equals(AWSIotMqttClientStatus.Connected)) {
                            mqttManager.subscribeToTopic(mTopicName, AWSIotMqttQos.QOS1, new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(String topic, byte[] data) {
                                    Log.d(TAG, "onMessageArrived");
                                    String message = new String(data);
                                    fireReceiveMessage(topic, message);
                                }
                            });
                            isSubscribed = true;
                        }
                    }
                });
            } catch (Exception ex) {
                Log.d(TAG, ex.getMessage());
            }
        }

        Intent notifyIntent = new Intent(this, MainActivity.class);
//        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.btn_star)
//                .setContentTitle(NOTIFICATION_TITLE)
                .setContentText(NOTIFICATION_CONTENT)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .build();
        startForeground(NOTIFICATION_ID, notification);

        return START_NOT_STICKY;
    }

    private void connectionClose() {
        if (mqttManager != null) {
            try {
                mqttManager.disconnect();
            } catch (Exception ex) {
            }
            mqttManager = null;
            isSubscribed = false;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");

        connectionClose();
    }

    private void fireReceiveMessage(String topic, String message) {
        Log.d(TAG, "fireReceiveMessage");

        int num = listeners.beginBroadcast();
        Log.d(TAG, "num=" + num);
        try {
            for (int i = 0; i < num; i++) {
                try {
                    IAwsIotServiceListener listener = listeners.getBroadcastItem(i);
                    listener.onReceiveMessage(topic, message);
                } catch (RemoteException ex) {
                    Log.d(TAG, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, ex.getMessage());
        } finally {
            listeners.finishBroadcast();
        }
    }

    final IAwsIotService.Stub binder = new IAwsIotService.Stub() {
        @Override
        public void publishMessage(String topicName, String message) throws RemoteException {
            if( mqttManager == null )
                throw new IllegalStateException("is not subscribed");

            mqttManager.publishString(message, topicName == null ? mTopicName : topicName, AWSIotMqttQos.QOS1);
        }

        @Override
        public boolean isSubscribed() throws RemoteException {
            return isSubscribed;
        }

        @Override
        public void addListener(IAwsIotServiceListener listener) throws RemoteException {
            listeners.register(listener);
        }

        @Override
        public void removeListener(IAwsIotServiceListener listener) throws RemoteException {
            listeners.unregister(listener);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind called");
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind called");
    }
}