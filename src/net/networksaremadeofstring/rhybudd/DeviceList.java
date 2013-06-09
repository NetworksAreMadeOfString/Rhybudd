/*
 * Copyright (C) 2013 - Gareth Llewellyn
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

import android.app.ActionBar;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;
import com.bugsense.trace.BugSenseHandler;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DeviceList extends Activity
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
	//Cursor dbResults = null;

	//new
	Handler handler;
	ActionBar actionbar;
	Thread dataLoad;
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		try
		{
		    if(dialog != null && dialog.isShowing())
			    dialog.dismiss();
		}
		catch(Exception e)
		{
			//BugSenseHandler.log("DeviceList-OnDestroy", e);
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
			//BugSenseHandler.log("DeviceList-onRetainNonConfigurationInstance", e);
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
        BugSenseHandler.initAndStartSession(DeviceList.this, "44a76a8c");

        actionbar = getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setHomeButtonEnabled(true);
		actionbar.setTitle("Infrastructure");

        list = (ListView)findViewById(R.id.ZenossDeviceList);

        BugSenseHandler.initAndStartSession(DeviceList.this, "44a76a8c");
        
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
			//BugSenseHandler.log("DeviceList", e);
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
    				//listOfZenossDevices = rhybuddCache.GetRhybuddDevices();
                    RhybuddDataSource datasource = new RhybuddDataSource(DeviceList.this);
                    datasource.open();
                    listOfZenossDevices = datasource.GetRhybuddDevices();
                    datasource.close();
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
    	if(dialog != null)
    	{
    		dialog.setTitle("Contacting Zenoss...");
       	 	dialog.setMessage("Please wait:\nLoading Infrastructure....");
       	 	dialog.setCancelable(false);
       	 	
    		if(!dialog.isShowing())
    		{
    			dialog.show();
    		}
    	}
    	else
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
						Message.obtain();
						handler.sendEmptyMessage(1);

                        RhybuddDataSource datasource = new RhybuddDataSource(DeviceList.this);
                        datasource.open();
                        datasource.UpdateRhybuddDevices(listOfZenossDevices);
                        datasource.close();
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
					//BugSenseHandler.log("DeviceList", e);
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
		MenuInflater inflater = getMenuInflater();
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
