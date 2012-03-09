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
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RhybuddHome extends Activity 
{
	private SharedPreferences settings = null;
	private Handler HomeHandler = null, runnablesHandler = null;
	@SuppressWarnings("unused")
	private Runnable updateEvents = null, updateDevices = null, updateDeviceDetails = null;
	private Boolean OneOff = true;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences("rhybudd", 0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.home);
		
		((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
		
		ConfigureHandler();
		ConfigureRunnable();
		updateEvents.run();
		
		ImageView EventsButton = (ImageView) findViewById(R.id.EventsImageView);
		EventsButton.setClickable(true);
		EventsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent EventsIntent = new Intent(RhybuddHome.this, rhestr.class);
				RhybuddHome.this.startActivity(EventsIntent);
			}
        });
		
		ImageView DevicesButton = (ImageView) findViewById(R.id.DevicesImageView);
		DevicesButton.setClickable(true);
		DevicesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent DeviceListIntent = new Intent(RhybuddHome.this, DeviceList.class);
				RhybuddHome.this.startActivity(DeviceListIntent);
			}
        });
		
		ImageView ReportsButton = (ImageView) findViewById(R.id.ReportsImageView);
		ReportsButton.setClickable(true);
		ReportsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*Intent ReportsIntent = new Intent(RhybuddHome.this, rhestr.class);
				RhybuddHome.this.startActivity(ReportsIntent);*/
				
				Toast.makeText(RhybuddHome.this, "Survey is complete. Reports will be in the next feature release!", Toast.LENGTH_LONG).show();
				/*AlertDialog.Builder alertbox = new AlertDialog.Builder(RhybuddHome.this);
		    	 alertbox.setMessage("Reports are not available yet.\n\nWould you like to help shape the reporting feature in Rhybudd by answering a short 3 question survey?");

		    	 alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
		    	 {
		             public void onClick(DialogInterface arg0, int arg1) 
		             {
		            	 Toast.makeText(RhybuddHome.this, "Thank you!", Toast.LENGTH_SHORT).show();
		            	 Intent i = new Intent(Intent.ACTION_VIEW);
		 	        	i.setData(Uri.parse("http://www.surveymonkey.com/s/QM7CWHC"));
		 	        	startActivity(i);
		             }
		    	 });

		         alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() 
		         {
		             public void onClick(DialogInterface arg0, int arg1) 
		             {
		             }
		         });
		         alertbox.show();*/
				
			}
        });
		
		ImageView SettingsButton = (ImageView) findViewById(R.id.SettingsImageView);
		SettingsButton.setClickable(true);
		SettingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				Intent SettingsIntent = new Intent(RhybuddHome.this, RhybuddSettings.class);
				startActivity(SettingsIntent);
			}
        });
		
		ImageView SearchButton = (ImageView) findViewById(R.id.SearchImageView);
		SearchButton.setClickable(true);
		SearchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent SearchIntent = new Intent(RhybuddHome.this, Search.class);
				startActivity(SearchIntent);
			}
        });
		
		ImageView VoiceSearchButton = (ImageView) findViewById(R.id.VoiceSearchImageView);
		VoiceSearchButton.setClickable(true);
		VoiceSearchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
        });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
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
	        
	        case R.id.EmptyDB:
	        {
	        	Intent MangeDBIntent = new Intent(RhybuddHome.this, ManageDatabase.class);
	        	RhybuddHome.this.startActivity(MangeDBIntent);
	            return true;
	        }
        }
        return false;
    }
	
	private void ConfigureHandler() {
		runnablesHandler = new Handler();

		HomeHandler = new Handler() {
			public void handleMessage(Message msg) 
			{
				if (msg.what == 0) 
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Refreshing Events...");
				} 
				else if (msg.what == 1) 
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Refreshing Infrastructure..");
				} 
				else if (msg.what == 98)// One off case
				{
					updateDevices.run();
				} 
				else if (msg.what == 99)// Hide
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Last Update:\n" + (new Date()).toString());
					((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(4);
				} 
				else if (msg.what == 100)// Show
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Update in Progress..");
					((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(0);
				}
			}
		};
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
										values.put("ipAddress", CurrentDevice.getInt("ipAddress"));
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
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} 
						catch (JSONException e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} 
						catch (IOException e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						catch (Exception e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						finally
						{
							if(cacheDB != null && cacheDB.isOpen())
								cacheDB.close();
						}

						// Hide progress
						HomeHandler.sendEmptyMessage(99);

						// Try again later if the app is still live
						runnablesHandler.postDelayed(this, 3604000);// 1 hour
						
						if(cacheDB != null && cacheDB.isOpen())
							cacheDB.close();
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
}
