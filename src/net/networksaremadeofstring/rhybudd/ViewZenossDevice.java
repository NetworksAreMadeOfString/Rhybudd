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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;

public class ViewZenossDevice extends SherlockActivity
{
	ZenossAPIv2 API = null;
	JSONObject DeviceObject = null, EventsObject = null;
	JSONObject DeviceDetails = null;
	private SharedPreferences settings = null;
	Handler firstLoadHandler, eventsHandler, errorHandler,loadAverageHandler,CPUGraphHandler,MemoryGraphHandler;
	ProgressDialog dialog;
	Thread dataPreload, eventsLoad, loadAvgGraphLoad, CPUGraphLoad;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	ListView list;
	ZenossEventsAdaptor adapter;
	JSONArray Events = null;
	private int EventCount = 0;
	ActionBar actionbar; 
	Drawable loadAverageGraph;
	Drawable CPUGraph;
	Drawable MemoryGraph;
	
	@Override
	public void onAttachedToWindow() 
	{
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.view_zenoss_device3);

		actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setHomeButtonEnabled(true);
		
		list = (ListView)findViewById(R.id.ZenossEventsList);

		errorHandler = new Handler()
		{
			public void handleMessage(Message msg) 
			{
				try
				{
					Toast.makeText(ViewZenossDevice.this, msg.getData().getString("exception"), Toast.LENGTH_LONG).show();
				}
				catch(Exception e)
				{
					BugSenseHandler.log("ViewZenossDevice-ErrorHandler", e);
				}
			}
		};
		
		loadAverageHandler = new Handler()
		{
			public void handleMessage(Message msg) 
			{
				((ImageView) findViewById(R.id.loadAverageGraph)).setImageDrawable(loadAverageGraph);
			}
		};
		
		CPUGraphHandler = new Handler()
		{
			public void handleMessage(Message msg) 
			{
				((ImageView) findViewById(R.id.CPUGraph)).setImageDrawable(CPUGraph);
			}
		};
		
		MemoryGraphHandler = new Handler()
		{
			public void handleMessage(Message msg) 
			{
				((ImageView) findViewById(R.id.MemoryGraph)).setImageDrawable(MemoryGraph);
			}
		};
		
		eventsHandler = new Handler()
		{
			public void handleMessage(Message msg) 
			{
				try
				{
					//((ProgressBar) findViewById(R.id.eventsProgressBar)).setVisibility(4);
				}
				catch(Exception e)
				{
					BugSenseHandler.log("ViewZenossDevice", e);
				}
				
				if(EventCount > 0 && msg.what == 1)
				{
					try
					{
						adapter = new ZenossEventsAdaptor(ViewZenossDevice.this, listOfZenossEvents, false);
						list.setAdapter(adapter);
					}
					catch(Exception e)
					{
						BugSenseHandler.log("ViewZenossDevice", e);
					}
				}
				else
				{
					list = null;
				}
			}
		};

		firstLoadHandler = new Handler() 
		{
			public void handleMessage(Message msg) 
			{
				if(dialog != null)
					dialog.dismiss();
				
				try
				{
					if(DeviceObject != null && msg.what == 1 && DeviceObject.getJSONObject("result").getBoolean("success") == true)
					{
						//Log.i("DeviceDetails",DeviceObject.toString());
						DeviceDetails = DeviceObject.getJSONObject("result").getJSONObject("data");

						try
						{
							String Name = DeviceDetails.getString("snmpSysName").toUpperCase();
							if(Name.equals(""))
							{
								Name = DeviceDetails.getString("name").toUpperCase();
							}
							
							((TextView) findViewById(R.id.deviceID)).setText(Name);
							//actionbar.setTitle(DeviceDetails.getString("snmpSysName"));
							actionbar.setSubtitle(Name);
						}
						catch(Exception e)
						{
							((TextView) findViewById(R.id.deviceID)).setText("--");
						}

						try
						{
							((TextView) findViewById(R.id.modelTime)).setText(DeviceDetails.getString("lastCollected"));
						}
						catch(Exception e)
						{
							((TextView) findViewById(R.id.modelTime)).setText("Unknown");
						}
						
						try
						{
							((TextView) findViewById(R.id.firstSeen)).setText(DeviceDetails.getString("firstSeen"));
						}
						catch(Exception e)
						{
							((TextView) findViewById(R.id.firstSeen)).setText("");
						}
						

						try
						{
							((TextView) findViewById(R.id.location)).setText(DeviceDetails.getString("snmpLocation"));
						}
						catch(Exception e)
						{
							((TextView) findViewById(R.id.location)).setText("Unknown Location");
						}

						try
						{
							((TextView) findViewById(R.id.uptime)).setText(DeviceDetails.getString("uptime"));
						}
						catch(Exception e)
						{
							//Already got a placeholder
						}

						try
						{
							((TextView) findViewById(R.id.productionState)).setText(DeviceDetails.getString("productionState"));
						}
						catch(Exception e)
						{
							//Already got a placeholder
						}
						
						try
						{
							((TextView) findViewById(R.id.memorySwap)).setText(DeviceDetails.getJSONObject("memory").getString("ram") + " / " + DeviceDetails.getJSONObject("memory").getString("swap"));
						}
						catch(Exception e)
						{
							//Already got a placeholder
						}

						String Groups = "";
						try
						{
							for (int i = 0; i < DeviceDetails.getJSONArray("groups").length(); i++)
							{
								if(i > 0)
									Groups += ", ";
								try
								{
									Groups +=  DeviceDetails.getJSONArray("groups").getJSONObject(i).getString("name");
								}
								catch(Exception e)
								{
									//BugSenseHandler.log("ViewZenossDevice", e);
								}
							}

							((TextView) findViewById(R.id.groups)).setText(Groups);

						}
						catch(Exception e)
						{
							//BugSenseHandler.log("ViewZenossDevice", e);
						}

						String Systems = "";
						try
						{
							for (int i = 0; i < DeviceDetails.getJSONArray("systems").length(); i++)
							{
								if(i > 0)
									Systems += ", ";

								Systems +=  DeviceDetails.getJSONArray("systems").getJSONObject(i).getString("name");
							}

							((TextView) findViewById(R.id.systems)).setText(Systems);

						}
						catch(Exception e)
						{
							//Already got a placeholder
							BugSenseHandler.log("ViewZenossDevice", e);
						}

						//etc
					}
					else
					{
						Toast.makeText(ViewZenossDevice.this, "There was an error loading the Device details", Toast.LENGTH_LONG).show();
						//finish();
					}
				}
				catch(Exception e)
				{
					//e.printStackTrace();
					Toast.makeText(ViewZenossDevice.this, "An error was encountered parsing the JSON.", Toast.LENGTH_LONG).show();
					BugSenseHandler.log("ViewZenossDevice", e);
				}
			}
		};

		dialog = new ProgressDialog(this);
		dialog.setTitle("Contacting Zenoss");
		dialog.setMessage("Please wait:\nLoading Device details....");
		dialog.show();
		dataPreload = new Thread() 
		{  
			public void run() 
			{
				try 
				{
					Message msg = new Message();
					Bundle bundle = new Bundle();
					
					if(API == null)
					{
						try
						{
							if(settings.getBoolean("httpBasicAuth", false))
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
							}
							else
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							}
						}
						catch(ConnectTimeoutException cte)
						{
							if(cte.getMessage() != null)
							{
								//Toast.makeText(ViewZenossDevice.this, "An error was encountered;\r\n" + cte.getMessage().toString(), Toast.LENGTH_LONG).show();
								bundle.putString("exception","The connection timed out;\r\n" + cte.getMessage().toString());
								msg.setData(bundle);
								msg.what = 0;
								errorHandler.sendMessage(msg);
							}
							else
							{
								bundle.putString("exception","A time out error was encountered but the exception thrown contains no further information.");
								msg.setData(bundle);
								msg.what = 0;
								errorHandler.sendMessage(msg);
								//Toast.makeText(ViewZenossDevice.this, "An error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
							}
						}
						catch(Exception e)
						{
							if(e.getMessage() != null)
							{
								bundle.putString("exception","An error was encountered;\r\n" + e.getMessage().toString());
								msg.setData(bundle);
								msg.what = 0;
								errorHandler.sendMessage(msg);
								//Toast.makeText(ViewZenossDevice.this, "An error was encountered;\r\n" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
							}
							else
							{
								bundle.putString("exception","An error was encountered but the exception thrown contains no further information.");
								msg.setData(bundle);
								msg.what = 0;
								errorHandler.sendMessage(msg);
								//Toast.makeText(ViewZenossDevice.this, "An error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
							}
						}
						
						DeviceObject = API.GetDevice(getIntent().getStringExtra("UID"));
					}
				} 
				catch (Exception e) 
				{
					//e.printStackTrace();
					BugSenseHandler.log("updateDevices-dataPreload", e);
					firstLoadHandler.sendEmptyMessage(0);
				}

				firstLoadHandler.sendEmptyMessage(1);
			}
		};

		dataPreload.start();

		loadAvgGraphLoad = new Thread() 
		{  
			public void run() 
			{
				try 
				{
					if(API == null)
					{
						Message msg = new Message();
						Bundle bundle = new Bundle();
						
						try
						{
							if(settings.getBoolean("httpBasicAuth", false))
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
							}
							else
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					
					JSONObject graphURLs = API.GetDeviceGraphs(getIntent().getStringExtra("UID"));
					
					//Log.e("graphURLs",graphURLs.toString(3));
					int urlCount = graphURLs.getJSONObject("result").getJSONArray("data").length();
					
					for(int i = 0; i < urlCount; i++)
					{
						JSONObject currentGraph = null;
						try 
						{
							currentGraph = graphURLs.getJSONObject("result").getJSONArray("data").getJSONObject(i);
							
							if(currentGraph.getString("title").equals("Load Average"))
							{
								loadAverageGraph = API.GetGraph(currentGraph.getString("url"));
								loadAverageHandler.sendEmptyMessage(1);
							}
							else if(currentGraph.getString("title").equals("CPU Utilization"))
							{
								CPUGraph = API.GetGraph(currentGraph.getString("url"));
								CPUGraphHandler.sendEmptyMessage(1);
							}
							else if(currentGraph.getString("title").equals("Memory Utilization"))
							{
								MemoryGraph = API.GetGraph(currentGraph.getString("url"));
								MemoryGraphHandler.sendEmptyMessage(1);
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					
					
					
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		loadAvgGraphLoad.start();
		
		eventsLoad = new Thread() 
		{  
			public void run() 
			{
				try 
				{
					if(API == null)
					{
						Message msg = new Message();
						Bundle bundle = new Bundle();
						
						try
						{
							if(settings.getBoolean("httpBasicAuth", false))
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
							}
							else
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							}
						}
						catch(ConnectTimeoutException cte)
						{
							if(cte.getMessage() != null)
							{
								//Toast.makeText(ViewZenossDevice.this, , Toast.LENGTH_LONG).show();
								bundle.putString("exception","An error was encountered;\r\n" + cte.getMessage().toString());
							}
							else
							{
								//Toast.makeText(ViewZenossDevice.this, "An error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
								bundle.putString("exception","An error was encountered but the exception thrown contains no further information.");
							}
							msg.setData(bundle);
							msg.what = 0;
							errorHandler.sendMessage(msg);
						}
						catch(Exception e)
						{
							if(e.getMessage() != null)
							{
								//Toast.makeText(ViewZenossDevice.this, "An error was encountered;\r\n" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
								bundle.putString("exception","An error was encountered;\r\n" + e.getMessage().toString());
							}
							else
							{
								//Toast.makeText(ViewZenossDevice.this, "An error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
								bundle.putString("exception","An error was encountered but the exception thrown contains no further information.");
							}
							
							msg.setData(bundle);
							msg.what = 0;
							errorHandler.sendMessage(msg);
						}
					}

					EventsObject = API.GetDeviceEvents(getIntent().getStringExtra("UID"),false);
					
					try
					{
						if((EventsObject.has("type") && EventsObject.get("type").equals("exception")) || !EventsObject.getJSONObject("result").has("totalCount") )
						{
							EventsObject = API.GetDeviceEvents(getIntent().getStringExtra("UID"),true);
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					
					if(EventsObject.has("result") && EventsObject.getJSONObject("result").getInt("totalCount") > 0)
					{
						Events = EventsObject.getJSONObject("result").getJSONArray("events");
	
						try 
						{
							if(EventsObject != null)
							{
								EventCount = EventsObject.getJSONObject("result").getInt("totalCount");
	
								for(int i = 0; i < EventCount; i++)
								{
									//JSONObject CurrentEvent = null;
									try 
									{
										//CurrentEvent = Events.getJSONObject(i);
										listOfZenossEvents.add(new ZenossEvent(Events.getJSONObject(i)));
										//Log.i("ForLoop",CurrentEvent.getString("summary"));
									}
									catch (JSONException e) 
									{
										//Log.e("API - Stage 2 - Inner", e.getMessage());
									}
									catch(Exception e)
									{
										BugSenseHandler.log("ViewZenossDevice-EventsLoop", e);
									}
								}
	
								eventsHandler.sendEmptyMessage(1);
							}
							else
							{
								//Log.i("eventsLoad","Had a problem; EventsObject was null");
								//eventsHandler.sendEmptyMessage(0);
							}
						} 
						catch (JSONException e) 
						{
							e.printStackTrace();
							BugSenseHandler.log("ViewZenossDevice-Events", e);
							eventsHandler.sendEmptyMessage(0);
						}
					}
					else
					{
						eventsHandler.sendEmptyMessage(0);
					}
				} 
				catch (Exception e) 
				{
					//Log.e("API - Stage 1", e.getMessage());
					BugSenseHandler.log("ViewZenossDevice-Events", e);
					eventsHandler.sendEmptyMessage(0);
				}
			}
		};
		eventsLoad.start();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        {
	        	//No need for crazy intents
	        	finish();
	            
	            return true;
	        }
	        
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}