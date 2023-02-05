package jp.or.myhome.sample.awsiotservice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.io.InputStreamReader;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback {
    public static final String TAG = "LogTag";
    public static final int REQEST_CODE_FILEOPEN_CRT = 1001;
    public static final int REQEST_CODE_FILEOPEN_KEY = 1002;
    private static final String AWSITO_DEFAULT_TOPIC_NAME = "awsiot/default";
    String crt_string;
    String key_string;
    String keyStorePath;
    SharedPreferences pref;
    IAwsIotService serviceBinder;
    UIHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new UIHandler(this);
        pref = getSharedPreferences("Private", Context.MODE_PRIVATE);
        keyStorePath = getFilesDir().getAbsolutePath();

        Button btn;
        btn = (Button)findViewById(R.id.btn_start);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_open_certificate);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_stop);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_bind);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_unbind);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_publish_message);
        btn.setOnClickListener(this);

        String endpointPrefix = pref.getString("endpointPrefix", ""); // sampleendpoint-ats
        String clientId = pref.getString("clientId", Build.MODEL);
        String topicName = pref.getString("topicName", AWSITO_DEFAULT_TOPIC_NAME);

        EditText edit;
        edit = (EditText)findViewById(R.id.edit_endpoint_prefix);
        edit.setText(endpointPrefix);
        edit = (EditText)findViewById(R.id.edit_client_id);
        edit.setText(clientId);
        edit = (EditText)findViewById(R.id.edit_topic_name);
        edit.setText(topicName);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btn_open_certificate:{
                Toast.makeText(this, "証明書ファイルを選択してください。", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/x-x509-ca-cert");
                startActivityForResult(intent, REQEST_CODE_FILEOPEN_CRT);
                break;
            }
            case R.id.btn_start:{
                try {
                    SharedPreferences.Editor editor = pref.edit();
                    Intent intent = new Intent(this, AwsIotService.class);
                    EditText edit;
                    edit = (EditText) findViewById(R.id.edit_endpoint_prefix);
                    String endpointPrefix = edit.getText().toString();
                    edit = (EditText) findViewById(R.id.edit_client_id);
                    String clientId = edit.getText().toString();
                    edit = (EditText) findViewById(R.id.edit_topic_name);
                    String topicName = edit.getText().toString();
                    intent.putExtra("keyStorePath", keyStorePath);
                    intent.putExtra("endpointPrefix", endpointPrefix);
                    intent.putExtra("clientId", clientId);
                    intent.putExtra("topicName", topicName);
                    editor.putString("endpointPrefix", endpointPrefix);
                    editor.putString("clientId", clientId);
                    editor.putString("topicName", topicName);

                    startForegroundService(intent);
                    Toast.makeText(this, "待ち受けを開始しました。", Toast.LENGTH_LONG).show();
                    editor.apply();
                }catch(Exception ex){
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }

                break;
            }
            case R.id.btn_stop:{
                Intent intent = new Intent(this, AwsIotService.class);
                stopService(intent);
                Toast.makeText(this, "待ち受けを停止しました。", Toast.LENGTH_LONG).show();
                break;
            }
            case R.id.btn_bind:{
                try {
                    Intent intent = new Intent(this, AwsIotService.class);
                    bindService(intent, connection, Context.BIND_AUTO_CREATE);
                }catch(Exception ex){
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            }
            case R.id.btn_unbind:{
                try {
                    unbindService(connection);
                }catch(Exception ex){
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            }
            case R.id.btn_publish_message:{
                EditText edit;
                edit = (EditText)findViewById(R.id.edit_publish_message);
                String message = edit.getText().toString();
                edit = (EditText)findViewById(R.id.edit_topic_name);

                try {
                    serviceBinder.publishMessage(null, message);
                }catch (Exception ex){
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if( resultCode == RESULT_OK && requestCode == REQEST_CODE_FILEOPEN_CRT ){
            try {
                Uri fileUri = resultData.getData();
                InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(fileUri));
                char[] buffer = new char[1024];
                StringWriter writer = new StringWriter();
                int size;
                while((size = reader.read(buffer)) != -1 ){
                    writer.write(buffer, 0, size);
                }
                crt_string = writer.toString();

                Toast.makeText(this, "秘密鍵ファイルを選択してください。", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/pgp-keys");
                startActivityForResult(intent, REQEST_CODE_FILEOPEN_KEY);
            }catch(Exception ex){
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }else if( resultCode == RESULT_OK && requestCode == REQEST_CODE_FILEOPEN_KEY ){
            try {
                Uri fileUri = resultData.getData();
                InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(fileUri));
                char[] buffer = new char[1024];
                StringWriter writer = new StringWriter();
                int size;
                while((size = reader.read(buffer)) != -1 ){
                    writer.write(buffer, 0, size);
                }
                key_string = writer.toString();
                EditText edit;
                AwsIotService.saveCertificate(keyStorePath, crt_string, key_string);
                Toast.makeText(this, "証明書が設定されました。", Toast.LENGTH_LONG).show();
            }catch(Exception ex){
                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onBindingDied(ComponentName className){
            Log.d(TAG, "onBindingDied(" + className.getClassName() + ")");
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "onServiceConnected(" + className.getClassName() + ")");
            try {
                IAwsIotService binder = IAwsIotService.Stub.asInterface(service);
                binder.addListener(new IAwsIotServiceListener.Stub() {
                    @Override
                    public void onReceiveMessage(String topicName, String message) throws RemoteException {
                        Log.d(TAG, "onReceiveMessage: " + message);

                        handler.sendTextMessage(message);
                    }
                });
                serviceBinder = binder;
            }catch(Exception ex){
                Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected(" + className.getClassName() + ")");
            unbindService(connection);
            serviceBinder = null;
        }
    };

    @Override
    public boolean handleMessage(@NonNull Message message) {
        if( message.what == UIHandler.MSG_ID_TEXT ){
            Toast.makeText(this, (String)message.obj, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }
}