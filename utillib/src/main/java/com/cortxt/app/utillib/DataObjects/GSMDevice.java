package com.cortxt.app.utillib.DataObjects;

import java.util.HashMap;

import android.content.Context;

import com.cortxt.app.utillib.Reporters.ReportManager;

/**
 * This class contains getters for device properties of GSM phones.
 * @author nasrullah
 *
 */
public class GSMDevice extends DeviceInfo {
	
	public static final String KEY_IMEI = "imei";
	private Context mContext = null;

	public GSMDevice(Context context) {
		super(context);
		mContext = context;
	}

	/**
	 * @return The phone's imei, or an empty string if it is unknown
	 */
//	public String getIMEI() {
//		ReportManager manager = ReportManager.getInstance(mContext);
//		String imei = manager.getDevice().getIMEI();
//		return imei;
//		//return super.mTelephonyManager.getDeviceId() != null ? super.mTelephonyManager.getDeviceId() : "";
//	}
	
	
	@Override
	public HashMap<String, String> getProperties() {
		HashMap<String, String> properties = super.getProperties();
		
		if(getIMEI().length() > 0)
			properties.put(KEY_IMEI, getIMEI());
		
		return properties;
	}

	
}
