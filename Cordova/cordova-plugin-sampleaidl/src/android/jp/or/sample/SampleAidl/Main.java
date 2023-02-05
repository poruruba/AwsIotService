package jp.or.sample.SampleAidl;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import jp.or.myhome.sample.awsiotservice.IAwsIotService;
import jp.or.myhome.sample.awsiotservice.IAwsIotServiceListener;

public class Main extends CordovaPlugin {
	public static String TAG = "SampleAidl.Main";
	private Activity activity;
	private CallbackContext callback;
	String keyStorePath;
	public static final String REMOTE_ACTION_NAME = "AwsIotService";
	IAwsIotService serviceBinder;

	private ServiceConnection connection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
				Log.d(TAG, "onServiceConnected(" + className.getClassName() + ")");
				try {
						IAwsIotService binder = IAwsIotService.Stub.asInterface(service);
						binder.addListener(new IAwsIotServiceListener.Stub() {
								@Override
								public void onReceiveMessage(String topicName, String message) throws RemoteException {
										Log.d(TAG, "onReceiveMessage: " + message);
										try{
											JSONObject result = new JSONObject();
											result.put("message", message);
											result.put("topicName", topicName);
											sendMessageToJs(result, callback);
										}catch(Exception ex){
											Log.d(TAG, ex.getMessage());
										}
								}
						});
						serviceBinder = binder;
				}catch(Exception ex){
					Log.d(TAG, ex.getMessage());
				}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
				Log.d(TAG, "onServiceDisconnected(" + className.getClassName() + ")");
				activity.unbindService(connection);
				serviceBinder = null;
		}
	};

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView)
	{
		Log.d(TAG, "[Plugin] initialize called");
		super.initialize(cordova, webView);

		activity = cordova.getActivity();

		keyStorePath = activity.getFilesDir().getAbsolutePath();
	}

	@Override
	public void onResume(boolean multitasking)
	{
		Log.d(TAG, "[Plugin] onResume called");
		super.onResume(multitasking);
	}

	@Override
	public void onPause(boolean multitasking)
	{
		Log.d(TAG, "[Plugin] onPause called");
		super.onPause(multitasking);
	}

	@Override
	public void onNewIntent(Intent intent)
	{
		Log.d(TAG, "[Plugin] onNewIntent called");
		super.onNewIntent(intent);
	}

	private boolean sendMessageToJs(JSONObject message, CallbackContext callback) {
		if( callback != null ){
			final PluginResult result = new PluginResult(PluginResult.Status.OK, message);
			result.setKeepCallback(true);
			callback.sendPluginResult(result);
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException
	{
		Log.d(TAG, "[Plugin] execute called");
		if( action.equals("bind") ){
			Intent intent = new Intent(REMOTE_ACTION_NAME);
			intent.setPackage(IAwsIotService.class.getPackage().getName());
			activity.bindService(intent, connection, Context.BIND_AUTO_CREATE);
  		callbackContext.success("OK");
		}else
		if( action.equals("unbind") ){
			activity.unbindService(connection);
			serviceBinder = null;
			callbackContext.success("OK");
		}else
		if( action.equals("isBound") ){
			if( serviceBinder == null ){
				callbackContext.success(-1);
			}else{
				callbackContext.success(0);
			}
		}else
		if( action.equals("isSubscribed") ){
			if( serviceBinder == null ){
				callbackContext.success(-1);
			}else{
				try{
					boolean result = serviceBinder.isSubscribed();
					callbackContext.success(result ? 0 : -1);
				}catch(Exception ex){
					callbackContext.error(ex.getMessage());
				}
			}
		}else
		if( action.equals("publishMessage") ){
			try{
				if( serviceBinder != null ) {
					String topicName = args.isNull(0) ? null : args.getString(0);
					String message = args.getString(1);
					serviceBinder.publishMessage(topicName, message);
				}
				callbackContext.success("OK");
			}catch(Exception ex){
				callbackContext.error(ex.getMessage());
			}
		}else
		if( action.equals("addListener") ){
			boolean arg0 = args.getBoolean(0);
			if( arg0 ){
				callback = callbackContext;
			}else{
				callback = null;
				callbackContext.success("OK");
			}
		}else {
			String message = "Unknown action : (" + action + ") " + args.getString(0);
			Log.d(TAG, message);
			callbackContext.error(message);
			return false;
		}

		return true;
	}
}

