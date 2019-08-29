package com.cortxt.app.utillib.DataObjects;

import java.util.HashMap;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellLocation;
import android.telephony.cdma.CdmaCellLocation;

/**
 * This class contains getters for device properties of CDMA phones.
 * @author nasrullah
 *
 */
public class CDMADevice extends DeviceInfo {

	public static final String KEY_MEID = "meid";
	public static final String KEY_ESN = "esn";
	public static final String KEY_SID = "sid";
	private Context mContext = null;

	public CDMADevice(Context context) {
		super(context);
		mContext = context;
	}

	/**
	 * @return The phone's esn, or an empty string if it is unknown
	 */
	public String getESN() {
		// TODO : determine if getDeviceID() actually returned ESN before returning it
		return "";//mTelephonyManager.getDeviceId();
	}

	/**
	 * @return The phone's meid, or an empty string if it is unknown
	 */
	public String getMEID() {
		// TODO : determine if getDeviceID() actually returned MEID before returning it
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || mContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
			try {
				String temp = mTelephonyManager.getDeviceId();
				return temp;
			} catch (Exception e){
				return "";
			}
		}

		return "";//mTelephonyManager.getDeviceId();
	}

	/**
	 * @return The System id, or -1 if it is unknown
	 */
	public int getSid() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (mContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && mContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
			CellLocation cellLoc = mTelephonyManager.getCellLocation();
			if(cellLoc != null && cellLoc instanceof CdmaCellLocation) {
				return ((CdmaCellLocation) cellLoc).getSystemId();
			}
			else {
				return -1;
			}
		}
		return -1;
	}
	
	@Override
	public HashMap<String, String> getProperties() {
		HashMap<String, String> properties = super.getProperties();
		
		if(getESN().length() > 0) {
			properties.put(KEY_ESN, getESN());
		}
		else if(getMEID().length() > 0) {
			properties.put(KEY_MEID, getMEID());
		}
		
		return properties;
	}
	
	@Override
	public HashMap<String, String> getCarrierProperties() {
		HashMap<String, String> carrierProperties = super.getCarrierProperties();
		
		if(getSid() != -1) {
			carrierProperties.put(KEY_SID, Integer.toString(getSid()));
		}
		
		return carrierProperties;
	}

	
}
