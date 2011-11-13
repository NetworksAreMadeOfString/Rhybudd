/*
* Copyright (C) 2011 - Gareth Llewellyn
*
* This file is part of Rhybudd - http://blog.NetworksAreMadeOfString.co.uk/Rhybudd/
*
* This program is free software: you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>
*/
package net.networksaremadeofstring.rhybudd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
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
	ZenossDeviceAdaptor adapter = null;
	Cursor dbResults = null;
	SQLiteDatabase rhybuddCache = null;
	
	@Override
	public Object onRetainNonConfigurationInstance() 
	{
		if(dialog != null && dialog.isShowing())
		{
			dialog.dismiss();
		}
	
	    return listOfZenossDevices;
	}
	
	/** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.devicelist);
        ((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
        list = (ListView)findViewById(R.id.ZenossDeviceList);
        
        firstLoadHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(dialog != null)
    				dialog.dismiss();
    			
    			if(listOfZenossDevices.size() > 0)
    			{
    				UpdateErrorMessage("",false);
					adapter = new ZenossDeviceAdaptor(DeviceList.this, listOfZenossDevices);
	    	        list.setAdapter(adapter);
    			}
    			else
    			{
    				UpdateErrorMessage("There are no devices in the list",false);
    			}
    			
    		}
    	};
    	
    	
    	listOfZenossDevices = (List<ZenossDevice>) getLastNonConfigurationInstance();
    	
    	if(listOfZenossDevices == null || listOfZenossDevices.size() < 1)
    	{
    		listOfZenossDevices = new ArrayList<ZenossDevice>();
    		
    		
    		//Check the DB first
    		if(CheckDB())
    		{
    			Log.i("CheckDB","We have data!");
    			DBGetDevices();
    		}
    		else
    		{
    			GetDevices();
    		}
    	}
    	else
    	{
	    	UpdateErrorMessage("",false);
	    	adapter = new ZenossDeviceAdaptor(DeviceList.this, listOfZenossDevices);
	        list.setAdapter(adapter);
    	}
    	
    	
    	
    	ImageView refreshButton = (ImageView) findViewById(R.id.RefreshViewImage);
        refreshButton.setClickable(true);
        refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listOfZenossDevices.clear();
	        	list.setAdapter(null);
	        	GetDevices();
			}
        });
        
    }
    
    public void DBGetDevices()
    {
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			Log.i("DBGetDevices",Integer.toString(dbResults.getCount()));
    			
    			while(dbResults.moveToNext())
    			{
    				HashMap<String, Integer> events = new HashMap<String, Integer>();
					events.put("info", dbResults.getInt(5));
					events.put("debug", dbResults.getInt(6));
					events.put("warning", dbResults.getInt(7));
					events.put("error", dbResults.getInt(8));
					events.put("critical", dbResults.getInt(9));
					
    				listOfZenossDevices.add(new ZenossDevice(dbResults.getString(1),
    						dbResults.getInt(2), 
    						events,
    						dbResults.getString(3),
    						dbResults.getString(4)));
    			}
    			
    			rhybuddCache.close();
        		dbResults.close();
        		Log.i("DBGetThread",Integer.toString(listOfZenossDevices.size()));
        		firstLoadHandler.sendEmptyMessage(0);
    		}
    	};
    	dataPreload.start();
    }
    
    private Boolean CheckDB()
    {
    	rhybuddCache = this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
    	try
    	{
    		dbResults = rhybuddCache.query("devices",new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents"},null, null, null, null, null);
    	}
    	catch(Exception e)
    	{
    		return false;
    	}
    	
    	if(dbResults.getCount() != 0)
    	{
    		return true;
    	}
    	else
    	{
    		rhybuddCache.close();
    		dbResults.close();
    		return false;
    	}
    }
    
    public void GetDevices()
    {
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
    					
    					//Log.i("log", Integer.toString(DeviceObject.getJSONObject("result").getInt("totalCount")) + " - " + Integer.toString(DeviceObject.getJSONObject("result").getJSONArray("devices").length()));
    					
    					for(int i = 0; i < DeviceCount; i++)
    	    			{
    	    				JSONObject CurrentDevice = null;
    	    				try 
    	    				{
    	    					CurrentDevice = DeviceObject.getJSONObject("result").getJSONArray("devices").getJSONObject(i);
    		    				
    	    					//Log.i("Device", CurrentDevice.toString());
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
    		    				
    		    				//Log.i("ForLoop",CurrentDevice.getString("name"));
    	    				}
    	    				catch (JSONException e) 
    	    				{
    	    					//Log.e("API - Stage 2 - Inner", e.getMessage());
    	    				}
    	    			}
    					
    					firstLoadHandler.sendEmptyMessage(1);
    				} 
    				catch (JSONException e) 
    				{
    					//Log.e("API - Stage 2", e.getMessage());
    					firstLoadHandler.sendEmptyMessage(0);
    				}
				} 
    			catch (Exception e) 
    			{
    				//Log.e("API - Stage 1", e.getMessage());
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
    
    public void ViewDevice(String UID)
    {
    	Intent ViewDeviceIntent = new Intent(DeviceList.this, ViewZenossDevice.class);
    	ViewDeviceIntent.putExtra("UID", UID);
    	DeviceList.this.startActivity(ViewDeviceIntent);
    }
}
