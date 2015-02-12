package com.zftlive.android.tools;

import java.util.ArrayList;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

/**
 * 手机相关操作API
 * @author 曾繁添
 * @version 1.0
 *
 */
public class ToolPhone {
	
	/**
	 * 直接呼叫指定的号码(需要<uses-permission android:name="android.permission.CALL_PHONE"/>权限)
	 * @param mContext 上下文Context
	 * @param phoneNumber 需要呼叫的手机号码
	 */
	public static void callPhone(Context mContext,String phoneNumber){
		Uri uri = Uri.parse("tel:" + phoneNumber);
		Intent call = new Intent(Intent.ACTION_CALL, uri);
		mContext.startActivity(call);
	}
	
	/**
	 * 跳转至拨号界面
	 * @param mContext 上下文Context
	 * @param phoneNumber 需要呼叫的手机号码
	 */
	public static void toCallPhoneActivity(Context mContext,String phoneNumber){
		Uri uri = Uri.parse("tel:" + phoneNumber);
		Intent call = new Intent(Intent.ACTION_DIAL, uri);
		mContext.startActivity(call);
	}
	
	/**
	 * 直接调用短信API发送信息(设置监听发送和接收状态)
	 * @param strPhone 手机号码
	 * @param strMsgContext 短信内容
	 */
	public static void sendMessage(final Context mContext,final String strPhone,final String strMsgContext){
		
		//处理返回的发送状态 
		String SENT_SMS_ACTION = "SENT_SMS_ACTION";
		Intent sentIntent = new Intent(SENT_SMS_ACTION);
		PendingIntent sendIntent= PendingIntent.getBroadcast(mContext, 0, sentIntent,0);
		// register the Broadcast Receivers
		mContext.registerReceiver(new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context _context, Intent _intent) {
		        switch (getResultCode()) {
		        case Activity.RESULT_OK:
		            Toast.makeText(mContext,"短信发送成功", Toast.LENGTH_SHORT).show();
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
		PendingIntent backIntent= PendingIntent.getBroadcast(mContext, 0,deliverIntent, 0);
		mContext.registerReceiver(new BroadcastReceiver() {
				   @Override
				   public void onReceive(Context _context, Intent _intent) {
				       Toast.makeText(mContext,strPhone+"已经成功接收", Toast.LENGTH_SHORT).show();
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
	public static void toSendMessageActivity(Context mContext,String strPhone,String strMsgContext){
		if(PhoneNumberUtils.isGlobalPhoneNumber(strPhone)){
            Uri uri = Uri.parse("smsto:" + strPhone);
            Intent sendIntent = new Intent(Intent.ACTION_VIEW, uri);
            sendIntent.putExtra("sms_body", strMsgContext);
            mContext.startActivity(sendIntent);
        }
	}
	
	/**
	 * 跳转至联系人选择界面
	 * @param mContext 上下文
	 * @param requestCode 请求返回区分代码
	 */
	public static void toChooseContactsList(Activity mContext,int requestCode){
		Intent intent = new Intent(Intent.ACTION_PICK,ContactsContract.Contacts.CONTENT_URI);
		mContext.startActivityForResult(intent, requestCode);
	}
	
	/**
	 * 获取选择的联系人的手机号码
	 * @param mContext 上下文
	 * @param resultCode 请求返回Result状态区分代码
	 * @param data onActivityResult返回的Intent
	 * @return
	 */
	public static String getChoosedPhoneNumber(Activity mContext,int resultCode,Intent data) {
		//返回结果
		String phoneResult = "";
		if (Activity.RESULT_OK == resultCode) 
		{
			Uri uri = data.getData();
			Cursor mCursor = mContext.managedQuery(uri, null, null, null, null);
			mCursor.moveToFirst();  
		
			int phoneColumn = mCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
			int phoneNum = mCursor.getInt(phoneColumn);
			if (phoneNum > 0) {
				// 获得联系人的ID号
				int idColumn = mCursor.getColumnIndex(ContactsContract.Contacts._ID);
				String contactId = mCursor.getString(idColumn);
				// 获得联系人的电话号码的cursor;
				Cursor phones = mContext.getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						null,
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
								+ contactId, null, null);
				if (phones.moveToFirst()) {
					// 遍历所有的电话号码
					for (; !phones.isAfterLast(); phones.moveToNext()) {
						int index = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
						int typeindex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
						int phone_type = phones.getInt(typeindex);
						String phoneNumber = phones.getString(index);
						switch (phone_type) {
						case 2:
							phoneResult = phoneNumber;
							break;
						}
					}
					if (!phones.isClosed()) {
						phones.close();
					}
				}
			}
			//关闭游标
			mCursor.close();
		}
		
		return phoneResult;
	}
	
	/**
	 * 跳转至拍照程序界面
	 * @param mContext 上下文
	 * @param requestCode 请求返回Result区分代码
	 */
	public static void toCameraActivity(Activity mContext,int requestCode){
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		mContext.startActivityForResult(intent, requestCode);
	}
	
	/**
	 * 跳转至相册选择界面
	 * @param mContext 上下文
	 * @param requestCode 
	 */
	public static void toImagePickerActivity(Activity mContext,int requestCode){
		Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
        mContext.startActivityForResult(intent, requestCode);
	}
	
	/**
	 * 获得选中相册的图片
	 * @param mContext 上下文
	 * @param data onActivityResult返回的Intent
	 * @return
	 */
	public static Bitmap getChoosedImage(Activity mContext,Intent data){
		if (data == null) {
            return null;
        }
		
		Bitmap bm = null;

        // 外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口
        ContentResolver resolver = mContext.getContentResolver();

        // 此处的用于判断接收的Activity是不是你想要的那个
        try {
            Uri originalUri = data.getData(); // 获得图片的uri
            bm = MediaStore.Images.Media.getBitmap(resolver, originalUri); // 显得到bitmap图片
            // 这里开始的第二部分，获取图片的路径：
            String[] proj = {
                    MediaStore.Images.Media.DATA
            };
            // 好像是android多媒体数据库的封装接口，具体的看Android文档
            Cursor cursor = mContext.managedQuery(originalUri, proj, null, null, null);
            // 按我个人理解 这个是获得用户选择的图片的索引值
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            // 将光标移至开头 ，这个很重要，不小心很容易引起越界
            cursor.moveToFirst();
            // 最后根据索引值获取图片路径
            String path = cursor.getString(column_index);
            //不用了关闭游标
            cursor.close();
        } catch (Exception e) {
        	Log.e("ToolPhone", e.getMessage());
        }
        
        return bm;
	}
	
	/**
	 * 调用本地浏览器打开一个网页
	 * @param mContext 上下文
	 * @param strSiteUrl 网页地址
	 */
	public static void openWebSite(Context mContext,String strSiteUrl){
		Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(strSiteUrl));  
		mContext.startActivity(webIntent);
	}
	
	/**
	 * 跳转至系统设置界面
	 * @param mContext 上下文
	 */
	public static void toSettingActivity(Context mContext){
		Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);  
		mContext.startActivity(settingsIntent);  
	}
	
	/**
	 * 跳转至WIFI设置界面
	 * @param mContext 上下文
	 */
	public static void toWIFISettingActivity(Context mContext){
		Intent wifiSettingsIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);  
		mContext.startActivity(wifiSettingsIntent); 
	}
	
}