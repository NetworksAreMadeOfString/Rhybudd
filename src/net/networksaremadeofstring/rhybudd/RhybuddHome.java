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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RhybuddHome extends SherlockFragmentActivity 
{
	SharedPreferences settings = null;
	Handler HomeHandler = null, runnablesHandler = null;
	Runnable updateEvents = null, updateDevices = null, updateDeviceDetails = null;
	Boolean OneOff = true;
	
	//New
	ZenossAPIv2 API = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	private boolean totalFailure = false;
	private int EventCount = 0;
	Thread dataPreload,AckEvent;
	volatile Handler handler, AckEventHandler;
	ProgressDialog dialog;
	ListView list;
	ZenossEventsAdaptor adapter;
	Cursor dbResults = null;
	ActionBar actionbar;
	int requestCode; //Used for evaluating what the settings Activity returned (Should always be 1)
	RhybuddDatabase rhybuddCache;
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		rhybuddCache.Close();
	}
	
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences("rhybudd", 0);
		setContentView(R.layout.rhybudd_home);
		
		actionbar = getSupportActionBar();
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		BugSenseHandler.setup(this, "44a76a8c");		
	     
		 if((settings.getString("URL", "").equals("") && settings.getString("userName", "").equals("") && settings.getString("passWord", "").equals("")) || settings.getBoolean("credentialsSuccess", false) == false)
	     {
			Intent SettingsIntent = new Intent(RhybuddHome.this, RhybuddInitialSettings.class);
			SettingsIntent.putExtra("firstRun", true);
			RhybuddHome.this.startActivityForResult(SettingsIntent, requestCode);
	     }
	     else
	     {
	    	 if(getIntent().hasCategory(Intent.CATEGORY_DESK_DOCK) || getIntent().hasCategory("android.intent.category.HE_DESK_DOCK") || getIntent().hasCategory("android.intent.category.LE_DESK_DOCK"))
	    	 {
	    		 Intent RhybuddDockIntent = new Intent(RhybuddHome.this, RhybuddDock.class);
	    		 RhybuddHome.this.startActivity(RhybuddDockIntent);
	    		 finish();
	    	 } 
	    	 else
	    	 {
	    		 finishStart(false);
	    	 }
	     }
	}
	
	public void DBGetThread()
    {
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			dbResults = rhybuddCache.getEvents();
    			
    			if(dbResults != null)
    			{
	    			while(dbResults.moveToNext())
	    			{
	    				listOfZenossEvents.add(new ZenossEvent(dbResults.getString(0),
															   dbResults.getString(3),
															   dbResults.getString(4), 
															   dbResults.getString(5),
															   dbResults.getString(7)));
	    			}
    			}
    			handler.sendEmptyMessage(0);
    		}
    	};
    	dataPreload.start();
    }
	
	private void finishStart(Boolean firstRun)
	{
		Intent intent = new Intent(this, ZenossPoller.class);
		if(firstRun)
		{
            intent.putExtra("settingsUpdate", true);
		}
		
		startService(intent);
		
		list = (ListView)findViewById(R.id.ZenossEventsList);
		rhybuddCache = new RhybuddDatabase(this);
		ConfigureHandlers();
		DBGetThread();
	}
	
	private void ConfigureHandlers()
    {
    	handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 0)
    			{
	    			//UpdateErrorMessage("",false);
    				((ProgressBar) findViewById(R.id.backgroundWorkingProgressBar)).setVisibility(4);
	    			adapter = new ZenossEventsAdaptor(RhybuddHome.this, listOfZenossEvents);
	        	    list.setAdapter(adapter);
    			}
    			else
    			{
    				Toast.makeText(RhybuddHome.this, "Timed out communicating with host. Please check protocol, hostname and port.", Toast.LENGTH_LONG).show();
    			}
    		}
    	};
    	
    	AckEventHandler = new Handler() 
    	 {
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 0)
    			{
    				adapter.notifyDataSetChanged();
    			}
    			else if(msg.what == 1)
    			{
    				adapter.notifyDataSetChanged();
    			}
    			else
    			{
    				Toast.makeText(getApplicationContext(), "There was an error trying to ACK that event.", Toast.LENGTH_SHORT).show();
    			}
    		}
    	 };
    }
	
    public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.home_menu, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        case R.id.pagerduty:
	        {
	        	Intent PagerDutyIntent = new Intent(RhybuddHome.this, RhestrPagerDuty.class);
	        	//Intent PagerDutyIntent = new Intent(RhybuddHome.this, RhybuddDock.class);
	        	RhybuddHome.this.startActivity(PagerDutyIntent);
	            return true;
	        }
	        
	        case R.id.Help:
	        {
	        	Intent i = new Intent(Intent.ACTION_VIEW);
	        	i.setData(Uri.parse("http://www.android-zenoss.info/help.php"));
	        	startActivity(i);
	        	return true;
	        }
	        
	        case R.id.cache:
	        {
	        	Intent MangeDBIntent = new Intent(RhybuddHome.this, ManageDatabase.class);
	        	RhybuddHome.this.startActivity(MangeDBIntent);
	            return true;
	        }
        }
        return false;
    }
	
	private void ConfigureRunnable() 
	{
		/*updateDeviceDetails = new Runnable() 
		{
			public void run() 
			{
				
				HomeHandler.sendEmptyMessage(100);
				HomeHandler.sendEmptyMessage(1);
				Thread devicesDetailsRefreshThread = new Thread() 
		    	{  
		    		public void run() 
		    		{
		    			try 
		    			{
		    				ZenossAPIv2 API = null;
							JSONObject DeviceObject = null;
							SQLiteDatabase cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
							Cursor dbResults = null;
							
					    	try
					    	{
					    		dbResults = cacheDB.query("devices",new String[]{"rhybuddDeviceID","uid"},null, null, null, null, null);
					    	}
					    	catch(Exception e)
					    	{
					    		throw e;
					    	}
					    	
					    	API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
					    	
					    	while(dbResults.moveToNext())
			    			{
					    		DeviceObject = API.GetDevice(dbResults.getString(1));
			    			}
						} 
		    			catch (Exception e) 
		    			{
		    				//TODO We should probably do something about this
						}
		    			
		    			// Hide progress
						HomeHandler.sendEmptyMessage(99);

						// Try again later if the app is still live
						runnablesHandler.postDelayed(this, 3604000);// 1 hour
		    		}
		    	};
		    	
				devicesDetailsRefreshThread.start();
			}
		};*/
		
		updateDevices = new Runnable() 
		{
			public void run() 
			{
				// Update the GUI
				HomeHandler.sendEmptyMessage(100);
				HomeHandler.sendEmptyMessage(1);

				Thread devicesRefreshThread = new Thread() 
				{
					public void run() 
					{
						ZenossAPIv2 API = null;
						JSONObject DeviceObject = null;
						SQLiteDatabase cacheDB = null;
						try 
						{
							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							
							if(API != null)
							{
								DeviceObject = API.GetDevices();
								int DeviceCount = DeviceObject.getJSONObject("result").getInt("totalCount");
								cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
								cacheDB.delete("devices", null, null);
								
								for(int i = 0; i < DeviceCount; i++)
								{
									JSONObject CurrentDevice = null;
									ContentValues values = new ContentValues(2);
									
									try 
									{
										CurrentDevice = DeviceObject.getJSONObject("result").getJSONArray("devices").getJSONObject(i);
	    		    				
		    		    				values.put("productionState",CurrentDevice.getString("productionState"));
		    		    				try
		    		    				{
		    		    					values.put("ipAddress", CurrentDevice.getInt("ipAddress"));
		    		    				}
		    		    				catch(Exception e)
		    		    				{
		    		    					values.put("ipAddress", "0.0.0.0");
		    		    				}
										values.put("name", CurrentDevice.getString("name"));
										values.put("uid", CurrentDevice.getString("uid"));
										values.put("infoEvents", CurrentDevice.getJSONObject("events").getInt("info"));
		    	    					values.put("debugEvents", CurrentDevice.getJSONObject("events").getInt("debug"));
		    	    					values.put("warningEvents", CurrentDevice.getJSONObject("events").getInt("warning"));
		    	    					values.put("errorEvents", CurrentDevice.getJSONObject("events").getInt("error"));
		    	    					values.put("criticalEvents", CurrentDevice.getJSONObject("events").getInt("critical"));
		    	    					
										cacheDB.insert("devices", null, values);
		    	    				}
		    	    				catch (JSONException e) 
		    	    				{
		    	    					e.printStackTrace();
		    	    				}
								}
								cacheDB.close();
							}
						}
						catch (ClientProtocolException e1) 
						{
							BugSenseHandler.log("updateDevices", e1);
						} 
						catch (JSONException e1) 
						{
							BugSenseHandler.log("updateDevices", e1);
						} 
						catch (IOException e1) 
						{
							BugSenseHandler.log("updateDevices", e1);
						}
						catch (Exception e1) 
						{
							BugSenseHandler.log("updateDevices", e1);
						}
						finally
						{
							if(cacheDB != null && cacheDB.isOpen())
								cacheDB.close();
						}

						// Hide progress
						HomeHandler.sendEmptyMessage(99);

						// Try again later if the app is still live
						//runnablesHandler.postDelayed(this, 3604000);// 1 hour
						try
						{
							if(cacheDB != null && cacheDB.isOpen())
								cacheDB.close();
						}
						catch(Exception e)
						{
							BugSenseHandler.log("updateDevices", e);
						}
					}
				};
				devicesRefreshThread.start();
			}
		};

		updateEvents = new Runnable() 
		{
			public void run() 
			{
				// Update the GUI
				HomeHandler.sendEmptyMessage(100);
				HomeHandler.sendEmptyMessage(0);

				// Thread
				Thread eventsRefreshThread = new Thread() 
				{
					public void run() 
					{
						HomeHandler.sendEmptyMessage(100);
						HomeHandler.sendEmptyMessage(0);
						
						JSONObject EventsObject = null;
						JSONArray Events = null;

						try 
						{
							ZenossAPIv2 API = new ZenossAPIv2(settings.getString("userName", ""),settings.getString("passWord", ""),settings.getString("URL", ""));

							EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true), settings.getBoolean("SeverityError", true),settings.getBoolean("SeverityWarning", true),settings.getBoolean("SeverityInfo",false), settings.getBoolean("SeverityDebug",false));
							Events = EventsObject.getJSONObject("result").getJSONArray("events");
						} 
						catch (Exception e) 
						{
							HomeHandler.sendEmptyMessage(0);
							//e.printStackTrace();
						}

						try 
						{
							if (EventsObject != null) 
							{
								int EventCount = EventsObject.getJSONObject("result").getInt("totalCount");

								SQLiteDatabase cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache",MODE_PRIVATE, null);
								cacheDB.delete("events", null, null);

								for (int i = 0; i < EventCount; i++) 
								{
									JSONObject CurrentEvent = null;
									ContentValues values = new ContentValues(2);
									try 
									{
										
										CurrentEvent = Events.getJSONObject(i);
										
										//Log.i("evi",CurrentEvent.getString("evid"));
										
										values.put("EVID",CurrentEvent.getString("evid"));
										values.put("device", CurrentEvent.getJSONObject("device").getString("text"));
										values.put("summary", CurrentEvent.getString("summary"));
										values.put("eventState", CurrentEvent.getString("eventState"));
										values.put("severity", CurrentEvent.getString("severity"));

										cacheDB.insert("events", null, values);
									} 
									catch (JSONException e) 
									{
										// Log.e("API - Stage 2 - Inner",
										// e.getMessage());
										//e.printStackTrace();
										//TODO We should tell the user about this or recover from it as they could miss an alert
									}
								}
								cacheDB.close();
							}
						} 
						catch (Exception e) 
						{
							// Log.e("API - Stage 2 - Inner", e.getMessage());
							//e.printStackTrace();
							//TODO Total failure is pretty bad - we should tell the user about it
						}
						
						// Hide progress
						HomeHandler.sendEmptyMessageDelayed(99, 2000);

						runnablesHandler.postDelayed(this, 300000);// 5 mins 300000

						if (OneOff) 
						{
							// Kick off the infrastructure refresh now we're
							// done with the other bit
							HomeHandler.sendEmptyMessage(98);
							OneOff = false;
						}
					}
				};
				eventsRefreshThread.start();
			}
		};
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	//Check what the result was from the Settings Activity
    	//In theory the Settings activity should perform validation and only finish() if the settings pass validation
    	if(resultCode == 1)
    	{
    		SharedPreferences.Editor editor = settings.edit();
        	editor.putBoolean("FirstRun", false);
        	editor.commit();
        	Toast.makeText(RhybuddHome.this, "Welcome to Rhybudd!\r\nPress the menu button to configure additional settings.", Toast.LENGTH_LONG).show();
        	finishStart(true);
    	}
    	else if(resultCode == 2)
    	{
    		Toast.makeText(RhybuddHome.this, "Rhybudd cannot start without configured settings.\n\nExiting....", Toast.LENGTH_LONG).show();
    		finish();
    	}
    	else //There is the potential for an infinite loop of unhappiness here but I doubt it'll happen
    	{
    		Toast.makeText(RhybuddHome.this, "Settings did not validate, returning to the settings screen.", Toast.LENGTH_LONG).show();
    		Intent SettingsIntent = new Intent(RhybuddHome.this, RhybuddSettings.class);
			SettingsIntent.putExtra("firstRun", true);
			RhybuddHome.this.startActivityForResult(SettingsIntent, requestCode);
    	}
    }
}
