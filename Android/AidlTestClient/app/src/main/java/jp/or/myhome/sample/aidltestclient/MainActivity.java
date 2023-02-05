package jp.or.myhome.sample.aidltestclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import jp.or.myhome.sample.awsiotservice.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback {
    public static final String TAG = "LogTag";
    public static final String REMOTE_ACTION_NAME = "AwsIotService";
    IAwsIotService binder;
    UIHandler handler;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new UIHandler(this);

        Button btn;
        btn = (Button)findViewById(R.id.btn_bind);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_unbind);
        btn.setOnClickListener(this);
        btn = (Button)findViewById(R.id.btn_call);
        btn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.btn_bind:{
                try {
                    Intent intent = new Intent(REMOTE_ACTION_NAME);
                    intent.setPackage(IAwsIotService.class.getPackage().getName());
                    bindService(intent, connection, Context.BIND_AUTO_CREATE);
                }catch(Exception ex){
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
                break;
            }
            case R.id.btn_unbind:{
                if( mBound ) {
                    try {
                        unbindService(connection);
                        mBound = false;
                    }catch(Exception ex){
                        Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
            case R.id.btn_call:{
                if( mBound ){
                    try {
                        EditText edit;
                        edit = (EditText)findViewById(R.id.edit_publish_message);
                        String message = edit.getText().toString();
                        binder.publishMessage(null, message);
                        Toast.makeText(this, "Published", Toast.LENGTH_SHORT).show();
                    }catch(Exception ex){
                        Toast.makeText(this, ex.toString(), Toast.LENGTH_LONG).show();
                    }
                }
                break;
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
//            CustomReceiverService.LocalBinder binder = (CustomReceiverService.LocalBinder) service;
                binder = IAwsIotService.Stub.asInterface(service);
                Log.d(TAG, "isSubscribed:" + binder.isSubscribed());
                binder.addListener(new IAwsIotServiceListener.Stub() {
                    @Override
                    public void onReceiveMessage(String topicName, String message) throws RemoteException {
                        Log.d(TAG, "onReceiveMessage: " + message);
                        try {
                            handler.sendTextMessage(message);
                        }catch(Exception ex){
                            Log.d(TAG, ex.getMessage());
                        }
                    }
                });
                mBound = true;
            }catch(Exception ex){
                Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected(" + className.getClassName() + ")");
            unbindService(connection);
            mBound = false;
        }
    };

    @Override
    public boolean handleMessage(@NonNull Message message) {
        if( message.what == UIHandler.MSG_ID_TEXT ) {
            Toast.makeText(this, (String) message.obj, Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }
}