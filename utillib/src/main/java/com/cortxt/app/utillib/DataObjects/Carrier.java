package com.cortxt.app.utillib.DataObjects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.telephony.TelephonyManager;

import com.cortxt.app.utillib.Utils.Global;
import com.cortxt.app.utillib.Utils.LoggerUtil;

public class Carrier implements Serializable, Comparable<Carrier> {

	public String Name = "";
    public String Country = "", CCode = "";
    public int MCC = 0;
    public int MNC = 0;
    public int ID = 0;
    public boolean Main = false;
    public Bitmap Logo = null; 
    public String Facebook = "";
    public String Twitter = "";
    public String Email = "";
    public String Path = "";
    public int Samples = 0;
    public String OperatorId = "";
    public String Tech = "";

    public Carrier(String name, String country, String ccode, int mcc, int mnc, int id, boolean selected,
    		String facebook, String twitter, String email, String path, String tech)
    {
        Name = name;
        Country = country;
		CCode = ccode;
        MCC = mcc;
        MNC = mnc;
        ID = id;
        Main = selected;
        Facebook = facebook;
        Twitter = twitter;
        Email = email;
        Path = path;
        Tech = tech;
    }
    
    public Carrier (JSONObject jobj)
    {
        try
        {
        	if (jobj.has("samples"))
        	{
        		Name = jobj.getString ("name");
        		Country = jobj.getString ("cntry");
				CCode = jobj.getString ("ccode");
	            MCC = jobj.getInt ("mcc");
	            MNC = jobj.getInt ("mnc");
	            ID = jobj.getInt ("carrierid");
	            Main = false;
	            Facebook = jobj.getString ("facebook");
	            Twitter = jobj.getString ("twitter");
	            Email = jobj.getString ("email");
	            Path = jobj.getString ("path");
	            Samples = jobj.getInt ("samples");
	            OperatorId = jobj.getString ("opid");
                if (jobj.has("tech")) {
                    JSONArray aTech = jobj.getJSONArray("tech");
                    if (aTech.length() > 0)
                        Tech = aTech.getString(0);
                }
        	}
        	else if (jobj.has("uuid"))
        	{
        		Name = jobj.getString ("name");
        		Country = jobj.getString ("cntry");
				CCode = jobj.getString ("ccode");
	            //MCC = jobj.getInt ("mcc");
	            //MNC = jobj.getInt ("mnc");
	            ID = jobj.getInt ("id");
	            Facebook = jobj.getString ("fcbk");
	            Twitter = jobj.getString ("twit");
	            Email = jobj.getString ("email");
	            Path = jobj.getString ("logo");
	            OperatorId = jobj.getString ("uuid");
        	}
        	else
        	{
	            Name = jobj.getString ("Name");
	            Country = jobj.getString ("Country");
	            MCC = jobj.getInt ("MCC");
	            MNC = jobj.getInt ("MNC");
	            ID = jobj.getInt ("ID");
	            Main = jobj.getBoolean ("Main");
	            Facebook = jobj.getString ("Facebook");
	            Twitter = jobj.getString ("Twitter");
	            Email = jobj.getString ("Email");
	            Path = jobj.getString ("Path");
	            OperatorId = jobj.getString ("Operator");
        	}
			if ("null".equals(Email))
				Email = null;
			if ("null".equals(Facebook))
				Facebook = null;
			if ("null".equals(Twitter))
				Twitter = null;
			if ("null".equals(Path))
				Path = null;
        }
        catch (Exception e)
        {
        }
    }
    
    public String getShortName (int limit)
    {
    	if (Name.length() > limit)
    		return Name.subSequence(0, limit-1) + "..";
    	
    	return Name;
    }
    
    
    public void loadLogo (Context context)
    {
    	if (Path != null && Path.length() > 1)
		{
    		//String localPath = Environment.getExternalStorageDirectory().toString();
    		
    		String localPath = context.getApplicationContext().getFilesDir() + "/images/logos";
    		File file = new File(localPath);
    		file.mkdirs();
    		localPath = context.getApplicationContext().getFilesDir() + Path;
    		//file.mkdirs();
    		try
    		{
    			file = new File(localPath);
    			if (file.exists())
    				return;
    			
    		}
    		catch (Exception e)
    		{
    			String str = e.getMessage();
    		}
			InputStream stream = null;
			FileOutputStream fos = null;
			try {

				String imageUrl = Global.getApiUrl(null);//.getString("MMC_URL_LIN"); // MMC_URL_CLOUD
				String imagePath = Uri.encode(Path, "/");
				URL request = new URL (imageUrl + imagePath);
				HttpURLConnection connection = (HttpURLConnection) request.openConnection();
				connection.connect ();
				stream = connection.getInputStream();
				byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
				int n;

				fos = new FileOutputStream(localPath);
				while ( (n = stream.read(byteChunk)) > 0 ) {
					fos.write(byteChunk, 0, n);
				}

				fos.flush();
				fos.close();
				
			} catch (Exception e) {
				LoggerUtil.logToFile(LoggerUtil.Level.ERROR, "Carrier", "loadLogo", "Exception loading logo " + Path);
			}
			finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (Exception e) {}
				}
			}
		}
    }

	@Override
	public int compareTo(Carrier another) {
		// TODO Auto-generated method stub
		return this.Name.compareTo(another.Name);
	}

	public static boolean isEU (Context context) {
		TelephonyManager telephonyManager;
		try {
			telephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
			String ccode = telephonyManager.getNetworkCountryIso();
			String euCcodes = "AT,BE,BG,HR,CY,CZ,DK,EE,FI,FR,DE,GR,HU,IE,IT,LV,LT,LU,MT,NL,PL,PT,RO,SK,SI,ES,SE,GB";
			if (ccode != null && euCcodes.indexOf(ccode.toUpperCase()) >= 0)
				return true;
		} catch (Exception e) {
		}
		return false;
	}
    
}
