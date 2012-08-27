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
import java.util.List;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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
    @SuppressWarnings({ "unchecked", "deprecation" })
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
		actionbar.setTitle("Infrastructure");
        
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
					dialog.setMessage("Refresh Complete!");
					this.sendEmptyMessageDelayed(2,1000);
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
    	dialog.setTitle("Querying DB");
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
    				listOfZenossDevices = rhybuddCache.GetRhybuddDevices();
    			}
    			catch(Exception e)
    			{
    				e.printStackTrace();
    				listOfZenossDevices.clear();
    			}
    			
    			if(listOfZenossDevices!= null && listOfZenossDevices.size() > 0)
    			{
    				DeviceCount = listOfZenossDevices.size();
    				//Log.i("DeviceList","Found DB Data!");
    				handler.sendEmptyMessage(2);
    			}
    			else
    			{
    				//Log.i("DeviceList","No DB data found, querying API directly");
    				Refresh();
    			}
    		}
    	};
    	dataLoad.start();
    }
  
    public void Refresh()
    {
    	if(!dialog.isShowing())
    	{
    		dialog = new ProgressDialog(this);
        	dialog.setTitle("Contacting Zenoss...");
       	 	dialog.setMessage("Please wait:\nLoading Infrastructure....");
       	 	dialog.setCancelable(false);
       	 	dialog.show();
    	}
    	
    	if(listOfZenossDevices != null)
			listOfZenossDevices.clear();
    	
		((Thread) new Thread()
		{
			public void run()
			{
				String MessageExtra = "";
				try 
				{
					Message msg = new Message();
					Bundle bundle = new Bundle();
					
					ZenossAPIv2 API = null;
					try
					{
						API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
					}
					catch(ConnectTimeoutException cte)
					{
						if(cte.getMessage() != null)
						{
							
							bundle.putString("exception","The connection timed out;\r\n" + cte.getMessage().toString());
							msg.setData(bundle);
							msg.what = 0;
							handler.sendMessage(msg);
							//Toast.makeText(DeviceList.this, "The connection timed out;\r\n" + cte.getMessage().toString(), Toast.LENGTH_LONG).show();
						}
						else
						{
							bundle.putString("exception","A time out error was encountered but the exception thrown contains no further information.");
							msg.setData(bundle);
							msg.what = 0;
							handler.sendMessage(msg);
							//Toast.makeText(DeviceList.this, "A time out error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
						}
					}
					catch(Exception e)
					{
						if(e.getMessage() != null)
						{
							bundle.putString("exception","An error was encountered;\r\n" + e.getMessage().toString());
							msg.setData(bundle);
							msg.what = 0;
							handler.sendMessage(msg);
							//Toast.makeText(DeviceList.this, "An error was encountered;\r\n" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
						}
						else
						{
							bundle.putString("exception","An error was encountered but the exception thrown contains no further information.");
							msg.setData(bundle);
							msg.what = 0;
							handler.sendMessage(msg);
							//Toast.makeText(DeviceList.this, "An error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
						}
					}
					
					try
					{
						if(API != null)
						{
							listOfZenossDevices = API.GetRhybuddDevices();
						}
						else
						{
							listOfZenossDevices = null;
						}
					}
					catch(Exception e)
					{
						if(e.getMessage() != null)
							MessageExtra = e.getMessage();
						
						listOfZenossDevices = null;
					}
					
					if(listOfZenossDevices != null && listOfZenossDevices.size() > 0)
					{
						DeviceCount = listOfZenossDevices.size();
						handler.sendEmptyMessage(1);
						rhybuddCache.UpdateRhybuddDevices(listOfZenossDevices);
					}
					else
					{
						//Message msg = new Message();
						//Bundle bundle = new Bundle();
						bundle.putString("exception","A query to both the local DB and Zenoss API returned no devices. " + MessageExtra );
						msg.setData(bundle);
						msg.what = 0;
						handler.sendMessage(msg);
					}
					
				} 
				catch (Exception e) 
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
		}).start();
    }
    public void ViewDevice(String UID)
    {
    	Intent ViewDeviceIntent = new Intent(DeviceList.this, ViewZenossDevice.class);
    	ViewDeviceIntent.putExtra("UID", UID);
    	DeviceList.this.startActivity(ViewDeviceIntent);
    }
    
    public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.devices, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
			case android.R.id.home:
	        {
	        	finish();
	            return true;
	        }
	        
			case R.id.search:
			{
				onSearchRequested();
				return true;
			}
			
			case R.id.refresh:
			{
				Refresh();
				return true;
			}

		}
		return false;
	}
}
