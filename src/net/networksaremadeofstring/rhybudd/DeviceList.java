/*
* Copyright (C) 2012 - Gareth Llewellyn
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
import org.json.JSONObject;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.bugsense.trace.BugSenseHandler;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceList extends SherlockActivity
{
	ZenossAPIv2 API = null;
	JSONObject DeviceObject = null;
	JSONObject EventDetails = null;
	SharedPreferences settings = null;
	Handler firstLoadHandler;
	ProgressDialog dialog;
	Thread dataPreload;
	List<ZenossDevice> listOfZenossDevices = new ArrayList<ZenossDevice>();
	int DeviceCount = 0;
	ListView list;
	ZenossDeviceAdaptor adapter = null;
	Cursor dbResults = null;
	//SQLiteDatabase rhybuddCache = null;
	
	
	//new
	Handler handler;
	ActionBar actionbar;
	RhybuddDatabase rhybuddCache;
	Thread dataLoad;
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		rhybuddCache.Close();
		try
		{
		if(dialog != null && dialog.isShowing())
			dialog.dismiss();
		}
		catch(Exception e)
		{
			BugSenseHandler.log("DeviceList-OnDestroy", e);
		}
	}
	
	@Override
	public void onAttachedToWindow() 
	{
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() 
	{
		try
		{
			if(dialog != null && dialog.isShowing())
			{
				dialog.dismiss();
			}
		}
		catch(Exception e)
		{
			BugSenseHandler.log("DeviceList-onRetainNonConfigurationInstance", e);
		}
	
	    return listOfZenossDevices;
	}
	
	/** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        setContentView(R.layout.devicelist);
        BugSenseHandler.setup(this, "44a76a8c");	
        
        actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setHomeButtonEnabled(true);
        
        list = (ListView)findViewById(R.id.ZenossDeviceList);
        
        rhybuddCache = new RhybuddDatabase(this);
        
        BugSenseHandler.setup(this, "44a76a8c");
        
        handler = new Handler()
        {
        	public void handleMessage(Message msg)
        	{
        		if(msg.what == 0)
        		{
        			Toast.makeText(DeviceList.this, "An error was encountered;\r\n" + msg.getData().getString("exception"), Toast.LENGTH_LONG).show();
        		}
        		else if(msg.what == 1)
    			{
    				if(rhybuddCache.hasCacheRefreshed())
    				{
    					dialog.setMessage("Refresh Complete!");
    					this.sendEmptyMessageDelayed(2,1000);
    				}
    				else
    				{
    					dialog.setMessage("Processing...");
        				handler.sendEmptyMessageDelayed(1, 1000);
    				}
    			}
    			else if(msg.what == 2)
    			{
    				dialog.dismiss();
    				adapter = new ZenossDeviceAdaptor(DeviceList.this, listOfZenossDevices);
    		        list.setAdapter(adapter);
    		        ((TextView) findViewById(R.id.ServerCountLabel)).setText("Monitoring " + DeviceCount + " servers");
    			}
        	}
        };

        try
        {
        	listOfZenossDevices = (List<ZenossDevice>) getLastNonConfigurationInstance();
        }
        catch(Exception e)
		{
        	listOfZenossDevices = null;
			BugSenseHandler.log("DeviceList", e);
		}
    	
    	if(listOfZenossDevices == null || listOfZenossDevices.size() < 1)
    	{
    		listOfZenossDevices = new ArrayList<ZenossDevice>();
    		DBGetThread();
    	}
    	else
    	{
    		adapter = new ZenossDeviceAdaptor(DeviceList.this, listOfZenossDevices);
    		list.setAdapter(adapter);
    	}
    }
    
    
    public void DBGetThread()
    {
    	dialog = new ProgressDialog(this);
    	dialog.setTitle("Contacting Zenoss");
   	 	dialog.setMessage("Please wait:\nLoading Infrastructure....");
   	 	dialog.setCancelable(false);
   	 	dialog.show();
   	 	
    	listOfZenossDevices.clear();
    	dataLoad = new Thread() 
    	{  
    		public void run() 
    		{
    			try
    			{
    				dbResults = rhybuddCache.getDevices();
    			}
    			catch(Exception e)
    			{
    				BugSenseHandler.log("DeviceList-DBGetThread", e);
    				dbResults = null;
    				Message msg = new Message();
					Bundle bundle = new Bundle();
					bundle.putString("exception",e.getMessage());
					msg.setData(bundle);
					msg.what = 0;
					handler.sendMessage(msg);
    			}
    			
    			if(dbResults != null)
    			{
    				try
    				{
    					DeviceCount = dbResults.getCount();
	    				while(dbResults.moveToNext())
		    			{
	    					HashMap<String, Integer> events = new HashMap<String, Integer>();
	    					
	    					//TODO I could do this better in a loop over an array but that's for another time
	    					//because the columns don't match up properly
	    					/*
	    					String[] Array = {"info","debug", "warning","error","critical"};
	    					for(String s :Array)
	    					{
	    						events.put("info", dbResults.getInt(5));
	    					}*/
	    					try
	    					{
	    						events.put("info", dbResults.getInt(5));
	    						events.put("debug", dbResults.getInt(6));
		    					events.put("warning", dbResults.getInt(7));
		    					events.put("error", dbResults.getInt(8));
		    					events.put("critical", dbResults.getInt(9));
	    					}
	    					catch(Exception e)
	    					{
	    						events.put("info", 0);
	    						events.put("debug", 0);
		    					events.put("warning", 0);
		    					events.put("error", 0);
		    					events.put("critical", 0);
	    					}
	    					
	    					try
	    					{
		    					listOfZenossDevices.add(new ZenossDevice(dbResults.getString(1),
				    					 dbResults.getInt(2), 
				    					 events,
				    					 dbResults.getString(3),
				    					 dbResults.getString(4)));
	    					}
	    					catch(Exception e)
	    					{
	    						BugSenseHandler.log("DeviceList-CursorLoop", e);
	    					}
	    					
		    			}
	    				handler.sendEmptyMessageDelayed(1, 500);
    				}
    				catch(Exception e)
    				{
    					BugSenseHandler.log("DeviceList", e);
    					Message msg = new Message();
    					Bundle bundle = new Bundle();
    					bundle.putString("exception",e.getMessage());
    					msg.setData(bundle);
    					msg.what = 0;
    					handler.sendMessage(msg);
    				}
    			}
    			else
    			{
    				//Nothing the handler is sent in the try catch above
    			}
    		}
    	};
    	dataLoad.start();
    }
  
    public void ViewDevice(String UID)
    {
    	Intent ViewDeviceIntent = new Intent(DeviceList.this, ViewZenossDevice.class);
    	ViewDeviceIntent.putExtra("UID", UID);
    	DeviceList.this.startActivity(ViewDeviceIntent);
    }
}
