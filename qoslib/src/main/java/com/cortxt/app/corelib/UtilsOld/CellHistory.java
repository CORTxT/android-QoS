package com.cortxt.app.corelib.UtilsOld;

import java.util.ArrayList;
import java.util.List;

import com.cortxt.app.utillib.DataObjects.CellLocationEx;
import com.cortxt.app.utillib.DataObjects.PhoneState;
import com.cortxt.app.utillib.DataObjects.SignalEx;
import com.cortxt.app.utillib.Utils.LoggerUtil;
import com.cortxt.com.mmcextension.rilreader.MMCLogger;

import android.annotation.TargetApi;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import org.json.JSONArray;
import org.json.JSONObject;

public class CellHistory {
	
	TelephonyManager telephonyManager = null;
	private List<CellidSample> cell_history = new ArrayList<CellidSample>();
	private List<CellidSample> neighbor_history = new ArrayList<CellidSample>();
	private long tmLastNeighborUpdate = 0, tmLastCellUpdate = 0, tmLastLTEUpdate = 0, tmLastCDMAUpdate = 0, tmLastGSMUpdate = 0;
	private String lastCellString = "";
	public CellidSample lastLTECell = null, lastCDMACell = null, lastGSMCell = null;
	public static String lastLTEIdentity = null;
	public static final String TAG = CellHistory.class.getSimpleName();
	
	public CellHistory (TelephonyManager _telephonyManager)
	{
		telephonyManager = _telephonyManager;
	}
	
	public long getLastCellSeen (CellLocation cellLoc) // MMCCellLocationOld cellInfo)
	{
		if (cellLoc == null)
			return -1;
		if (tmLastCellUpdate + 60000 > System.currentTimeMillis() && cellLoc.toString().equals(lastCellString))
			return - 1;
		long timelastSeen = 0;
		tmLastCellUpdate = System.currentTimeMillis();
		
		// Is it reporting an unknown cell id? ignore those
//        int cellId = 0;//cellInfo.getBSLow(); //low
//        if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM && cellLoc instanceof GsmCellLocation)
//			cellId = ((GsmCellLocation)cellLoc).getCid();
//		else if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA && cellLoc instanceof CdmaCellLocation)
//			cellId = ((CdmaCellLocation)cellLoc).getBaseStationId();
//		if (cellId <= 0)
//			return -1;
		
		CellidSample smp = new CellidSample(cellLoc);
		cell_history.add (smp);
		
		// How long has it been since we last saw this basestation
        //int bs_high = cellInfo.getBSHigh(), bs_mid = cellInfo.getBSMid(), bs_low = cellInfo.getBSLow();
		int bs_high = 0, bs_mid = 0, bs_low = 0;
        if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM && cellLoc instanceof GsmCellLocation)
        {
			GsmCellLocation gsmCellLocation = (GsmCellLocation)cellLoc;
			bs_high = gsmCellLocation.getLac();
			bs_mid = gsmCellLocation.getCid() >> 16;
			bs_low = gsmCellLocation.getCid() & 0xffff;
        }
		else if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA && cellLoc instanceof CdmaCellLocation)
		{
			CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLoc;
			bs_high = cdmaCellLocation.getSystemId();
			bs_mid = cdmaCellLocation.getNetworkId();
			bs_low = cdmaCellLocation.getBaseStationId();
		}
		if (bs_low <= 0)
			return -1;
        int j;
        int histlen = cell_history.size();
		// How long has it been since we last saw this basestation
		long timestamp = System.currentTimeMillis();
		for (j=histlen-2; j>=0; j--)
		{
			if (cell_history.get(j).val2 == bs_low) //  && cell_history.get(j).val1 == bs_high)
			{
				// time last seen is the first timestamp after the cell was last seen
				// (the time this cell handed off to another cell)
				// (if the last known cell was this same cell, then the time last seen is now)
				timelastSeen = timestamp;  
				break;
			}
			else
				timestamp = cell_history.get(j).timestamp;
			
		}
		
		return timelastSeen;
	}
	
	// Called when a neighbor list is detected in the RadioLog
	// It augments that neighbor list with the neighbor from the API
	public String updateNeighborHistory (int[] _list, int[] _list_rssi, int[] _list_chan)
	{
		int i;
		int[] _list_qual = null;
		//if (tmLastNeighborUpdate + 2000 > System.currentTimeMillis())
		//	return "";
		
		//if (!_isTravelling)
		{
			String txt = "";
			String stringNeighboring = "";
			String[] _type = null;
			try
			{
				int netType = telephonyManager.getNetworkType();
				int gen = PhoneState.getNetworkGeneration(netType);
				int n = 0, len =0;
				boolean bValid = true;
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){	 
				try
				{
					List<CellInfo> cells = null;
					cells = telephonyManager.getAllCellInfo();

//					if (cells != null && cells.size() > 0 && cells.get(0) instanceof CellInfoLte) // && telephonyManager.getNetworkType() == PhoneState.NETWORK_NEWTYPE_LTE)
//					{
//						return updateLteNeighborHistory(cells);
//					}
					//if (cells == null)
					//	LoggerUtil.logToFile(LoggerUtil.Level.ERROR, TAG, "updateNeighborHistory", "cells = null");

					if (cells != null && cells.size() > 1)//1)
					{
						lastLTEIdentity = null;
						len = cells.size();
						if (len > 8)
							len = 8;
						_list = new int[len];
						_list_rssi = new int[len];
						_list_chan = new int[len];
						_list_qual = new int[len];
						_type = new String[len];
						for ( i =0; i<len; i++)
						{ 
							bValid = true;
							CellInfo neighbor = cells.get(i);
							String msg =  "cells[" + i + "]=" + neighbor.toString();
							String classname = neighbor.getClass().toString();
							//LoggerUtil.logToFile(LoggerUtil.Level.ERROR, TAG, "updateNeighborHistory", "cells[" + i + "]=" + neighbor.toString());
							//Log.d(TAG, "cells[" + c + "]=" + cells.get(c).toString());
//							if (gen < 3)
//								_type[i] = "2G";
//							else if (gen < 5)
//								_type[i] = "3G";
//							else if (gen == 5)
//								_type[i] = "LTE";
//							if (neighbor.isRegistered())
//								_type[i] += "*";

							if (neighbor instanceof CellInfoCdma)
							{
								CellIdentityCdma cdmacell =((CellInfoCdma) neighbor).getCellIdentity();
								CellSignalStrengthCdma cdmasig = ((CellInfoCdma) neighbor).getCellSignalStrength();
								if (cdmacell.getBasestationId() > 0)  // Don't include CDMA in the neighbor lists, it doesnt work
								{
									if (neighbor.isRegistered()) {
										updateCdmaNeighborHistory (cdmacell.getSystemId(), cdmacell.getNetworkId(), cdmacell.getBasestationId(), 0);
									}
									else {
										_list[n] = cdmacell.getBasestationId();
										_list_rssi[n] = cdmasig.getEvdoDbm();
										_list_chan[n] = 0;
										_list_qual[n] = cdmasig.getEvdoEcio();
										_type[n] = "C";
										n++;
									}
								//else if (neighbor.getSystemId() > 0)
								//	_list[n] = (neighbor.getLac()<<16) + neighbor.getCid();
								}
								else
									bValid = false;
							}
							else if (neighbor instanceof CellInfoGsm && gen < 5)
							{
								CellIdentityGsm gsmcell =((CellInfoGsm) neighbor).getCellIdentity();
								CellSignalStrengthGsm gsmsig = ((CellInfoGsm) neighbor).getCellSignalStrength();
								_type[n] = "2G";
								if (neighbor.isRegistered() && Build.VERSION.SDK_INT >= 24) {
									updateGSMNeighborHistory (gsmcell.getLac(), gsmcell.getCid(), gsmcell.getPsc(), gsmcell.getArfcn());
								}
								else {

									if (Build.VERSION.SDK_INT >= 24 && gsmcell.getArfcn() > 0 && gsmcell.getArfcn() < 1000000)
										_list_chan[n] = gsmcell.getArfcn();
									if (gsmcell.getPsc() > 0 && gsmcell.getPsc() < 1000) {
										_list[n] = gsmcell.getPsc();
										_list_rssi[n] = gsmsig.getDbm();
										n++;
									} else if (gsmcell.getCid() > 0 && gsmcell.getCid() < 10000000) {
										_list[n] = (gsmcell.getLac() << 16) + gsmcell.getCid();
										_list_rssi[n] = gsmsig.getDbm();
									} else
										bValid = false;
								}
							}
							else if (neighbor instanceof CellInfoLte)
							{
								if (neighbor.isRegistered())
									updateLteNeighborHistory(neighbor);
								else {
									CellIdentityLte ltecell = ((CellInfoLte) neighbor).getCellIdentity();

									CellSignalStrengthLte ltesig = ((CellInfoLte) neighbor).getCellSignalStrength();
									if (Build.VERSION.SDK_INT >= 24 && ltecell.getEarfcn() > 0 && ltecell.getEarfcn() < 1000000)
										_list_chan[n] = ltecell.getEarfcn();

									int rsrq = SignalEx.getPrivate("mRsrq", ltesig);
									if (rsrq < -1)
										_list_qual[n] = rsrq;
									if (ltecell.getPci() > 0 && ltecell.getPci() < 1000) {
										_list[n] = ltecell.getPci();
										_list_rssi[n] = ltesig.getDbm();
										_type[n] = "4G";
										n++;
									} else
										bValid = false;
								}
							}
							else if (Build.VERSION.SDK_INT >= 18 && neighbor instanceof CellInfoWcdma && gen < 5)
							{
								try {
									CellIdentityWcdma wcdmacell = ((CellInfoWcdma) neighbor).getCellIdentity();
									CellSignalStrengthWcdma wcdmasig = ((CellInfoWcdma) neighbor).getCellSignalStrength();
									Integer psc = wcdmacell.getPsc();
									Integer dbm = wcdmasig.getDbm();

									if (neighbor.isRegistered() && Build.VERSION.SDK_INT >= 24) {
										updateGSMNeighborHistory (wcdmacell.getLac(), wcdmacell.getCid(), wcdmacell.getPsc(), wcdmacell.getUarfcn());
										//_type[n] += "*";
									}
									else {
										if (Build.VERSION.SDK_INT >= 24 && wcdmacell.getUarfcn() > 0 && wcdmacell.getUarfcn() < 1000000)
											_list_chan[n] = wcdmacell.getUarfcn();
										if (psc > 0 && psc < 1000) {
											_type[n] = "3G";
											_list[n] = psc;
											//if (neighbor.isRegistered())
											//	_list[n] += 10000;
											_list_rssi[n] = dbm;
											n++;
										}
										else
											bValid = false;
									}
//									else if (cid > 0 && cid < 65536)
//									{	_list[n] = cid;
//										//if (neighbor.isRegistered())
//										//	_list[n] += 10000;
//										_list_rssi[n] = dbm;
//										n++;
//									}

								} catch (Exception e) {
									//Log.d(TAG, "Field does not exist - " + fieldname);
								}
								
	//							CellIdentityWcdma gsmcell =((CellInfoWcdma) neighbor).getCellIdentity();
	//							CellSignalStrengthWcdma gsmsig = ((CellInfoWcdma) neighbor).getCellSignalStrength();
	//							if (gsmcell.getPsc() > 0)
	//							{	_list[n] = gsmcell.getPsc();
	//								if (neighbor.isRegistered())
	//									_list[n] += 0;
	//								_list_rssi[n] = gsmsig.getDbm();
	//								n++;
	//							}
	//							else if (gsmcell.getCid() > 0)
	//							{
	//								_list[n] = (gsmcell.getLac()<<16) + gsmcell.getCid();
	//							}
	//							else
	//								bValid = false;
							}
						}
					
					}
					
				}
				catch (Exception e)
				{
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "updateNeighborHistory", "inner exception", e);
				}}
				if (_list == null)
				{
			        // Try to obtain the Neighbor list from the API
					// If the API neighbor list has more active entries than what the log provided, use it instead
			        List<NeighboringCellInfo> neighboringList = telephonyManager.getNeighboringCellInfo();
			        len = neighboringList.size();
					if (neighboringList != null && len > 0)
					{
						int activeN = 0;  // how many of the API neighbors are active?
						
						for (i=0; i<neighboringList.size(); i++)
						{
							NeighboringCellInfo neighbor = neighboringList.get(i);
							if (neighbor.getPsc() > 0 && neighbor.getRssi() > -120 && neighbor.getRssi() < -10)
								activeN ++;
							else if (neighbor.getCid() > 0 && neighbor.getRssi() > 0 && neighbor.getRssi() < 32)
								activeN ++;
						}
						_list = new int[len];
						_list_rssi = new int[len];
						_type = new String[len];
						
						for (i=0; i<len; i++)
						{
							bValid = true;
							NeighboringCellInfo neighbor = neighboringList.get(i);		
							if (gen < 3)
								_type[n] = "2G";
							else
								_type[n] = "3G";
							if (neighbor.getPsc() > 0)
								_list[n] = neighbor.getPsc();
							else if (neighbor.getLac() > 0)
								_list[n] = (neighbor.getLac()<<16) + neighbor.getCid();
							else
								bValid = false;
							
							if (bValid)
							{
								_list_rssi[n] = neighbor.getRssi();
								if (_list_rssi[n] >= 0)
									_list_rssi[n] = getDbmValue(_list_rssi[n]);
								n++;
							}						
						}					  
					}
				}
				
				if (_list == null)
					return null;
				tmLastNeighborUpdate = System.currentTimeMillis();
				// create and add a neighbor cell item for each neighbor
				for (i=0; i<_list.length; i++)
				{
					if (_list[i] != 0 && !_type[i].contains("*"))
					{
						String sig = String.valueOf(_list_rssi[i]);
						CellidSample smp = new CellidSample(_type[i], _list[i], _list_rssi[i]);
						if (_list_chan != null && _list_chan[i] != 0) {
							smp.val4 = _list_chan[i];
						}
						if (_list_qual != null && _list_qual[i] < -1) {
							smp.val3 = _list_qual[i];
							sig += "/" + _list_qual[i];
						} else
							sig += "/0";


						// Update Neighbor list history a maximum of once per 2 seconds
						neighbor_history.add (smp);
						stringNeighboring = stringNeighboring
						         + String.valueOf(_list[i]&0xFFFF) +"@"
						         + sig + ",";
						
					}
				}
				// Report the API neighbor list in the Log, build a string of neighbors
				if (lastCellString == stringNeighboring)
					return null;
				lastCellString = stringNeighboring;
				//if (neighboringList.size() > 0)
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "updateNeighborHistory", stringNeighboring);
			     
				return stringNeighboring;
			}
			catch (Exception e)
			{
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "updateNeighborHistory", "exception", e);
				return "";
			}
			finally 
			{
				//if (txt.length() > 0 && (txt.indexOf("might") > 0 || txt.indexOf("detect") > 0))
				//	MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "detectTravellingFromNeighbors", "\n"+txt.substring(0,txt.length()));
			}
		}
	}

	// Called when a neighbor list is detected in the RadioLog
	// It augments that neighbor list with the neighbor from the API
	public String updateNeighborHistory (JSONArray neighbors)
	{
		int i;
		String stringNeighboring = "";
		//if (tmLastNeighborUpdate + 2000 > System.currentTimeMillis())
		//	return "";

		try {
			int len = neighbors.length();
			int[] list = new int[len];
			int[] list_rssi = new int[len];
			String[] type = new String[len];
			int netType = telephonyManager.getNetworkType();
			int gen = PhoneState.getNetworkGeneration(netType);
			for (i = 0; i < len; i++) {
				JSONObject neigh = (JSONObject) neighbors.get(i);
				if (gen < 3)
					type[i] = "2G";
				else
					type[i] = "3G";
				if (neigh.has("type") && neigh.getString("type").equals("ASET"))
					type[i] += "*";

				if (neigh.has("psc")) {
					int psc = Integer.valueOf(neigh.getString("psc"));
					list[i] = psc;
				}
				if (neigh.has("rscp")) {
					int rscp = Integer.valueOf(neigh.getString("rscp"));
					if (rscp > 0)
						rscp = -rscp;
					list_rssi[i] = rscp;
				}
				else if (neigh.has("rxlev")) {
					int rx = Integer.valueOf(neigh.getString("rxlev"));
					int rssi = -121;
					if (rx > 0)
					{
						if (rx == 1)
							rssi = -111;  // officially 1 = -111 dB
						else if (rx > 1 && rx <= 31)
							rssi = (rx - 2) * 2 + -109;
					}
					list_rssi[i] = rssi;
				}
				CellidSample smp = new CellidSample(type[i], list[i], list_rssi[i]);
				// Update Neighbor list history a maximum of once per 2 seconds
				neighbor_history.add (smp);
				stringNeighboring = stringNeighboring
						+ String.valueOf(list[i]&0xFFFF) +"@"
						+ list_rssi[i] +",";
			}
			if (list.length == 0)
				return null;
			tmLastNeighborUpdate = System.currentTimeMillis();
			// Report the API neighbor list in the Log, build a string of neighbors
			if (lastCellString == stringNeighboring)
				return null;
			lastCellString = stringNeighboring;
			//if (neighboringList.size() > 0)
			//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "updateNeighborHistory", stringNeighboring);

			return stringNeighboring;
		}
		catch (Exception e)
		{
			LoggerUtil.logToFile(LoggerUtil.Level.ERROR, TAG, "updateNeighborHistory", "Exception", e);
		}

		return "";
	}
	
	@TargetApi(17)
	public String updateLteNeighborHistory (CellInfo cellinfo)
	{
		CellIdentityLte cellIDLte = ((CellInfoLte)cellinfo).getCellIdentity();
		if (cellIDLte.getTac() > 0 && cellIDLte.getTac() < 100000 && cellinfo.isRegistered())
		{
			int tac = cellIDLte.getTac(), pci = cellIDLte.getPci(), ci = cellIDLte.getCi();
			int chan = 0;
			if (Build.VERSION.SDK_INT >= 24)
				chan = cellIDLte.getEarfcn();
			String lteString = updateLteNeighborHistory (tac, ci, pci, chan);
			return lteString;
		}
		return null;
	}

	@TargetApi(17)
//	public String updateLteNeighborHistory (List<CellInfo> cellinfos)
//	{
//		if (cellinfos == null || cellinfos.size() == 0)
//		{
//			if(lastLTECell != null)
//			{
//				CellidSample smp = new CellidSample("L", 0, 0, 0, 0);
//				neighbor_history.add (smp);
//				lastLTECell = null;
//			}
//			return null;
//		}
//		String lteString = null;
//		lastLTECell = null;
//		for(int i = 0; i < cellinfos.size(); i++) {
//			// All we're interested in here is getting new Lte cell identity with this new API. Otherwise ignore and use CellLocation updates
//			if (cellinfos.get(i) instanceof CellInfoLte)
//			{
//				CellIdentityLte cellIDLte = ((CellInfoLte)cellinfos.get(i)).getCellIdentity();
//				if (cellIDLte.getTac() > 0 && cellIDLte.getTac() < 100000 && cellinfos.get(i).isRegistered())
//				{
//					int tac = cellIDLte.getTac(), pci = cellIDLte.getPci(), ci = cellIDLte.getCi();
//					lteString =  updateLteNeighborHistory (tac, ci, pci, 0);
//				} else {
//					if (cellIDLte.getPci() > 0 && cellIDLte.getPci() < 1000)
//					{
//						CellidSample smp = new CellidSample("L", 0, 0, cellIDLte.getPci(), 0);
//						neighbor_history.add (smp);
//					}
//					//else
//					//	bValid = false;
//				}
//			}
//			else  // Lte CellIdentity contains bogus ids, treat like its null
//			{
//				CellidSample smp = new CellidSample("L", 0, 0, 0, 0);
//				neighbor_history.add (smp);
//				lastLTECell = null;
//
//			}
//		}
//		return lteString;
//	}

	public String updateLteNeighborHistory (int tac, int ci, int pci, int chan)
	{
		String stringLTE = "";
		
		if (pci > 1000000)
			pci = 0; // bogus
		if (ci > 2000000000)
			ci = 0; // bogus
		if (tac <= 0 || tac > 2000000000)
			return null;
		
		CellidSample smp = new CellidSample("L", tac, ci, pci, chan);
		//if (i==0)
		{
			//int ciHigh = ci >> 16;
			//int ciLow = ci & 0xffff;
			stringLTE = stringLTE + "LTE Tac:" + tac + " Ci:" + ci + " pCi:" + pci + " Earfcn:" + chan;
			if (ci > 0)
			{
				int eNB = (ci >> 8) & 0xFFFFF;
				int cellid = (ci & 0xFF);
				stringLTE = stringLTE + " eNB:" + eNB + "/" + cellid;
				lastLTEIdentity = stringLTE;
			}
		}
		if(lastLTECell == null || !lastLTECell.valuesEqual(smp) || tmLastLTEUpdate + 30000 < System.currentTimeMillis()) { 
			// Update repeated LTE a maximum of once per 30 seconds
			neighbor_history.add (smp);
			lastLTECell = smp;
			tmLastLTEUpdate = System.currentTimeMillis();
		}

		//if (neighbors != null && neighbors.length() > 2)

   		return stringLTE;
	}

	public void updateCdmaNeighborHistory (int sid, int nid, int bid, int chan)
	{
		if (sid > 1000000 || nid <= 0 || nid > 200000000 || bid > 2000000000)
			return;

		CellidSample smp = new CellidSample("CDMA", sid, nid, bid, chan);

		if(lastCDMACell == null || !lastCDMACell.valuesEqual(smp) || tmLastCDMAUpdate + 30000 < System.currentTimeMillis()) {
			// Update repeated LTE a maximum of once per 30 seconds
			neighbor_history.add (smp);
			lastCDMACell = smp;
			tmLastCDMAUpdate = System.currentTimeMillis();
		}
	}
	public void updateGSMNeighborHistory (int lac, int cellid, int psc, int arfcn)
	{
		if (lac > 1000000 || psc > 200000000 || cellid > 2000000000)
			return;

		CellidSample smp = new CellidSample("GSM", lac, cellid, psc, arfcn);

		if(lastGSMCell == null || !lastGSMCell.valuesEqual(smp) || tmLastGSMUpdate + 30000 < System.currentTimeMillis()) {
			// Update repeated LTE a maximum of once per 30 seconds
			neighbor_history.add (smp);
			lastGSMCell = smp;
			tmLastGSMUpdate = System.currentTimeMillis();
		}
	}
	// Repeat the last history item at the beginning of recording an event
//	public void snapshotHistory ()
//	{
//		CellidSample smp = null;
//		if (neighbor_history.size() > 0)
//			smp = neighbor_history.get(neighbor_history.size()-1);	
//		smp = new CellidSample (smp.type, smp.val1, smp.val2, smp.val3);
//		neighbor_history.add (smp);	
//	}
	
	private Integer getDbmValue(int rssi) 
	{
		rssi /= 2;
		// according to the 3GPP specs: http://m10.home.xs4all.nl/mac/downloads/3GPP-27007-630.pdf
		if (rssi == 0)
			return -120;  // officially 0 means -113dB or less, but since lowest possible signal on Blackberry = -120, call it -120 for consistency
		else if (rssi == 1)
			return -111;  // officially 1 = -111 dB
		else if (rssi > 1 && rssi <= 31)  
			return (rssi - 2) * 2 + -109;
		else if (rssi == 99)
			return rssi;
		else
			return rssi; // shouldn't be possible
			
	}
	// Called when an event is being reported to the server
	// It builds a neighbor list history that doesnt ovrlap the previously reported neighbor list history
	public String getNeighborHistory (long startTime, long eventTime)
	{
		int i;
		String txt = "";
//		String hdr = "3,sec,psc,rssi, ";
		String hdr = "6,sec,type,val1,val2,val3,val4";
		int size = neighbor_history.size();
		for (i=size-1; i>=0; i--)
		{
			CellidSample smp = neighbor_history.get(i);
			//if (smp.sent == 0)
			{
				txt += toString(smp, eventTime);
			}
			if (smp.timestamp < startTime)
				break;
		}
		if (txt.length() > 1)
			return hdr+txt;
		else
			return "";
	}
	
	public String toString(CellidSample sample, long eventTime) {
		
		String txt = "";
		//txt += "," + (sample.timestamp/1000);
		txt += "," + ((sample.timestamp - eventTime)/1000);		
		txt += "," + sample.type;		
		txt += "," + sample.val1;	
		if (sample.type.equals("L"))  // valid ranges different for lte vs 3G/2G
		{
			txt += "," + (sample.val2 <= 2000000000 ? sample.val2 : 0);
			txt += "," + (sample.val3 <= 2000000000 ? sample.val3 : 0);	
		}
		else
		{
			txt += "," + sample.val2; // (sample.val2 <= -121 ? sample.val2 : 0);
			txt += "," + (sample.val3 > 0 ? sample.val3 : 0);	
		}
		if (sample.val4 > 0)
			txt += "," + sample.val4;
		else
			txt += ",0";

		sample.sent = 1;
		
		return txt;
	}
	
	// ON each 3hr checkpoint, prune the cell history lists, removing items 3 hours old
	public void clearCellHistory ()
	{
		// clear 3 hour old items out of history lists
		int i;
		for (i=0; i<neighbor_history.size(); i++)
		{
			if (neighbor_history.get(i).timestamp + 180*60000 < System.currentTimeMillis())
			{
				neighbor_history.remove(i);
				i--;
			}
			else
				break;
			
		}
		for (i=0; i<cell_history.size(); i++)
		{
			if (cell_history.get(i).timestamp + 180*60000*3 < System.currentTimeMillis())
			{
				cell_history.remove(i);
				i--;
			}
			else
				break;
			
		}
		LoggerUtil.logToFile(LoggerUtil.Level.DEBUG, TAG, "clearCellHistory", "neighbors=" + neighbor_history.size() + " cells=" + cell_history.size());
	}
	// Simple representation of either a cell or a neighbor cell
	public class CellidSample
	{
		public int  val1=-1, val2 = -1, val3 = -1, val4 = -1, sent = 0;
		public String type = "";
		public long timestamp;
		public CellidSample (CellLocation cell)
		{
			CellLocationEx mmcCell = new CellLocationEx(cell);
		
			timestamp = System.currentTimeMillis();
			val1 = mmcCell.getBSHigh();
			val2 = mmcCell.getBSLow();
//			
//			if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA && mmcCell instanceof CdmaCellLocation)
//			{
//				lac = ((CdmaCellLocation)mmcCell).getSystemId();
//				cid = ((CdmaCellLocation)mmcCell).getBaseStationId();
//			}
//			else if (mmcCell instanceof GsmCellLocation)
//			{
//				lac = ((GsmCellLocation)mmcCell).getLac();
//				cid = ((GsmCellLocation)mmcCell).getCid();
//			}
		}
		public CellidSample (NeighboringCellInfo neighbor)
		{
			timestamp = System.currentTimeMillis();
			val1 = neighbor.getPsc();
		}
		public CellidSample (String _type, int _psc, int _rssi)  //add type?
		{
			timestamp = System.currentTimeMillis();
			type = _type;
			val1 = _psc;
			val2 = _rssi;
		}
		
		public CellidSample (String _type, int _tac, int _ci, int _pci, int _chan)
		{
			timestamp = System.currentTimeMillis();
			type = _type;
			val1 = _tac;
			val2 = _ci;
			val3 = _pci;
			val4 = _chan;
		}		
		
		
		public boolean valuesEqual(CellidSample smp) {
			if(val1 == smp.val1 && val2 == smp.val2 && val3 == smp.val3 && val4 == smp.val4)
				return true;
			return false;
		}		
		
		
	}
}
