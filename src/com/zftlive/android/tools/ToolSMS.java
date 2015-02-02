package com.zftlive.android.tools;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.widget.Toast;
import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;

import com.zftlive.android.MApplication;


/**
 * 发送短信验证码工具类
 * @author 曾繁添
 * @version 1.0
 *
 */
public class ToolSMS {
	
	public static String APPKEY = "25c13dc2e1c4";
	public static String APPSECRET = "14340f710d155024867d4870786d4c10";
	public static String CHINA = "86";
	private static IValidateSMSCode mIValidateSMSCode;
	private static Handler mSMSHandle = new MySMSHandler();
	private static Context context = MApplication.gainContext();
	
	/**
	 * 初始化ShareSDK发送短信验证码实例
	 * @param appkey
	 * @param appSecrect
	 */
	public static void initSDK(String appkey, String appSecrect){
		// 初始化短信SDK
		SMSSDK.initSDK(context, appkey, appSecrect);
		//注册回调监听接口
		SMSSDK.registerEventHandler(new EventHandler() {
			public void afterEvent(int event, int result, Object data) {
				Message msg = new Message();
				msg.arg1 = event;
				msg.arg2 = result;
				msg.obj = data;
				mSMSHandle.sendMessage(msg);
			}
		});
	}
	
	/**
	 * 直接调用短信API发送信息(设置监听发送和接收状态)
	 * @param strPhone 手机号码
	 * @param strMsgContext 短信内容
	 */
	public static void sendMessage(final String strPhone,final String strMsgContext){
		
		//处理返回的发送状态 
		String SENT_SMS_ACTION = "SENT_SMS_ACTION";
		Intent sentIntent = new Intent(SENT_SMS_ACTION);
		PendingIntent sendIntent= PendingIntent.getBroadcast(context, 0, sentIntent,0);
		// register the Broadcast Receivers
		context.registerReceiver(new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context _context, Intent _intent) {
		        switch (getResultCode()) {
		        case Activity.RESULT_OK:
		            Toast.makeText(context,"短信发送成功", Toast.LENGTH_SHORT).show();
		            break;
		        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
		        	break;
		        case SmsManager.RESULT_ERROR_RADIO_OFF:
		        	break;
		        case SmsManager.RESULT_ERROR_NULL_PDU:
		        	break;
		        }
		    }
		}, new IntentFilter(SENT_SMS_ACTION));
		
		//处理返回的接收状态 
		String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
		// create the deilverIntent parameter
		Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
		PendingIntent backIntent= PendingIntent.getBroadcast(context, 0,deliverIntent, 0);
		context.registerReceiver(new BroadcastReceiver() {
				   @Override
				   public void onReceive(Context _context, Intent _intent) {
				       Toast.makeText(context,strPhone+"已经成功接收", Toast.LENGTH_SHORT).show();
				   }
				}, 
				new IntentFilter(DELIVERED_SMS_ACTION)
		);
		
		//拆分短信内容（手机短信长度限制）
		SmsManager smsManager = SmsManager.getDefault();
		ArrayList<String> msgList = smsManager.divideMessage(strMsgContext);
		for (String text : msgList) {
			smsManager.sendTextMessage(strPhone, null, text, sendIntent, backIntent);
		}
	}
	
	/**
	 * 跳转至发送短信界面(自动设置接收方的号码)
	 * @param mActivity Activity
	 * @param strPhone 手机号码
	 * @param strMsgContext 短信内容
	 */
	public static void sendMessage(Activity mActivity,String strPhone,String strMsgContext){
		if(PhoneNumberUtils.isGlobalPhoneNumber(strPhone)){
            Uri uri = Uri.parse("smsto:" + strPhone);
            Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);
            sendIntent.putExtra("sms_body", strMsgContext);
            mActivity.startActivity(sendIntent);
        }
	}
	
	/**
	 * 请求获取短信验证码
	 * @param phone 手机号
	 */
	public static void getVerificationCode(String phone){
		SMSSDK.getVerificationCode(CHINA, phone);
	}
	
	/**
	 * 提交短信验证码，校验是否正确
	 * @param phone 手机号
	 * @param validateCode 手机短信验证码
	 */
	public static void submitVerificationCode(String phone, String validateCode,IValidateSMSCode callback){
		mIValidateSMSCode = callback;
		SMSSDK.submitVerificationCode(CHINA, phone, validateCode);
	}
	
	/**
	 * 释放资源
	 */
	public static void release(){
		// 销毁回调监听接口
		SMSSDK.unregisterAllEventHandler();
	}
	
    /**
     * 消息处理Handle
     */
	private static class MySMSHandler extends Handler{
    	@Override
		public void handleMessage(Message msg) {
    		super.handleMessage(msg);
    		
    		int event = msg.arg1;
    		int result = msg.arg2;
    		Object data = msg.obj;
			
			if (result == SMSSDK.RESULT_COMPLETE) {
				//提交验证码成功
				if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {
					//验证成功回调
					if(null != mIValidateSMSCode){
						mIValidateSMSCode.onSucced();
					}
				} 
			} else {
				Throwable exption = ((Throwable) data);
				//验证成功回调
				if(null != mIValidateSMSCode){
					mIValidateSMSCode.onFailed(exption);
				}
			}
		}
    }
	
    /**
     * 提交短信验证码回调接口
     */
    public interface IValidateSMSCode{
    	void onSucced();
    	void onFailed(Throwable e);
    }
}
