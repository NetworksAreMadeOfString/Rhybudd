package net.networksaremadeofstring.rhybudd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceList extends Activity
{
	ZenossAPIv2 API = null;
	JSONObject DeviceObject = null;
	JSONObject EventDetails = null;
	private SharedPreferences settings = null;
	Handler firstLoadHandler;
	ProgressDialog dialog;
	Thread dataPreload;
	List<ZenossDevice> listOfZenossDevices = new ArrayList<ZenossDevice>();
	int DeviceCount = 0;
	ListView list;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
        setContentView(R.layout.devicelist);
        list = (ListView)findViewById(R.id.ZenossDeviceList);
        
        firstLoadHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			dialog.dismiss();
    			if(listOfZenossDevices.size() > 0)
    			{
    				UpdateErrorMessage("",false);
					ZenossDeviceAdaptor adapter = new ZenossDeviceAdaptor(DeviceList.this, listOfZenossDevices);
	    	        list.setAdapter(adapter);
    			}
    			else
    			{
    				UpdateErrorMessage("There are no devices in the list",false);
    			}
    			
    		}
    	};
    	
    	dialog = new ProgressDialog(this);
    	dialog.setTitle("Contacting Zenoss");
   	 	dialog.setMessage("Please wait:\nLoading Infrastructure....");
   	 	dialog.show();
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
    				if(API == null)
    				{
    					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				}
    				
    				DeviceObject = API.GetDevices();
    				
    				try 
    				{
    					DeviceCount = DeviceObject.getJSONObject("result").getInt("totalCount");
    					
    					Log.i("log", Integer.toString(DeviceObject.getJSONObject("result").getInt("totalCount")) + " - " + Integer.toString(DeviceObject.getJSONObject("result").getJSONArray("devices").length()));
    					
    					for(int i = 0; i < DeviceCount; i++)
    	    			{
    	    				JSONObject CurrentDevice = null;
    	    				try 
    	    				{
    	    					CurrentDevice = DeviceObject.getJSONObject("result").getJSONArray("devices").getJSONObject(i);
    		    				
    	    					HashMap<String, Integer> events = new HashMap<String, Integer>();
    	    					events.put("info", CurrentDevice.getJSONObject("events").getInt("info"));
    	    					events.put("debug", CurrentDevice.getJSONObject("events").getInt("debug"));
    	    					events.put("critical", CurrentDevice.getJSONObject("events").getInt("critical"));
    	    					events.put("warning", CurrentDevice.getJSONObject("events").getInt("warning"));
    	    					events.put("error", CurrentDevice.getJSONObject("events").getInt("error"));
    	    					
    		    				listOfZenossDevices.add(new ZenossDevice(CurrentDevice.getString("productionState"),
    		    						CurrentDevice.getInt("ipAddress"), 
    		    						events,
    		    						CurrentDevice.getString("name"),
    		    						CurrentDevice.getString("uid")));
    		    				
    		    				Log.i("ForLoop",CurrentDevice.getString("name"));
    	    				}
    	    				catch (JSONException e) 
    	    				{
    	    					Log.e("API - Stage 2 - Inner", e.getMessage());
    	    				}
    	    			}
    					
    					firstLoadHandler.sendEmptyMessage(1);
    				} 
    				catch (JSONException e) 
    				{
    					Log.e("API - Stage 2", e.getMessage());
    					firstLoadHandler.sendEmptyMessage(0);
    				}
				} 
    			catch (Exception e) 
    			{
    				Log.e("API - Stage 1", e.getMessage());
    				firstLoadHandler.sendEmptyMessage(0);
				}
    			
    			firstLoadHandler.sendEmptyMessage(1);
    		}
    	};
    	
    	dataPreload.start();
        
    }
    
    public void UpdateErrorMessage(String MessageText,boolean Critical)
	{
		TextView ErrorMessage = (TextView) findViewById(R.id.MessageText);
		if(MessageText.length() == 0)
		{
			ErrorMessage.setHeight(0);
		}
		else
		{
			
			ErrorMessage.setHeight(24);
			ErrorMessage.setTextColor(-16711936);
			ErrorMessage.setText(MessageText);
			
			if(Critical)
				ErrorMessage.setTextColor(-65536);
		}
	}
}
