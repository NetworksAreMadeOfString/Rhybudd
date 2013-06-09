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

import android.app.ActionBar;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.View.OnClickListener;
import com.bugsense.trace.BugSenseHandler;


public class ViewZenossEvent extends Activity
{
	JSONObject EventObject = null;
	JSONObject EventDetails = null;
	private SharedPreferences settings = null;
	Handler firstLoadHandler, addLogMessageHandler;
	ProgressDialog dialog,addMessageProgressDialog;
	Thread dataPreload, addLogMessageThread;
	Dialog addMessageDialog;
	String[] LogEntries;
	ActionBar actionbar;
    ZenossAPI API;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.view_event, menu);
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
	
			case R.id.AddLog:
			{
				addMessageDialog = new Dialog(ViewZenossEvent.this);
				addMessageDialog.setContentView(R.layout.add_message);
				addMessageDialog.setTitle("Add Message to Event Log");
				((Button) addMessageDialog.findViewById(R.id.SaveButton)).setOnClickListener(new OnClickListener() 
				{
					@Override
					public void onClick(View v) 
					{
						AddLogMessage(((EditText) addMessageDialog.findViewById(R.id.LogMessage)).getText().toString());
						addMessageDialog.dismiss();
					}
				});
	
				addMessageDialog.show();
				return true;
			}
	
			case R.id.escalate:
			{
				Intent intent=new Intent(android.content.Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
	
				// Add data to the intent, the receiving app will decide what to do with it.
				intent.putExtra(Intent.EXTRA_SUBJECT, "Escalation of Zenoss Event on " + getIntent().getStringExtra("Device"));
				String EventDetails = getIntent().getStringExtra("Summary") + "\r\r\n" +
										getIntent().getStringExtra("LastTime") + "\r\r\n" +
										"Count: " + getIntent().getIntExtra("Count",0);
	
				intent.putExtra(Intent.EXTRA_TEXT, EventDetails);
	
				startActivity(Intent.createChooser(intent, "How would you like to escalate this event?"));
			}
	
			default:
			{
				return false;
			}
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
        BugSenseHandler.initAndStartSession(ViewZenossEvent.this, "44a76a8c");

		settings = PreferenceManager.getDefaultSharedPreferences(this);

		setContentView(R.layout.view_zenoss_event);

		try
		{
			actionbar = getActionBar();
			actionbar.setDisplayHomeAsUpEnabled(true);
			actionbar.setHomeButtonEnabled(true);
		}
		catch(Exception e)
		{
            BugSenseHandler.sendExceptionMessage("ViewZenossEvent","OnCreate",e);
		}
		
		try
		{
			((TextView) findViewById(R.id.EventTitle)).setText(getIntent().getStringExtra("Device"));
			((TextView) findViewById(R.id.Summary)).setText(Html.fromHtml(getIntent().getStringExtra("Summary")));
			((TextView) findViewById(R.id.LastTime)).setText(getIntent().getStringExtra("LastTime"));
			((TextView) findViewById(R.id.EventCount)).setText("Count: " + Integer.toString(getIntent().getIntExtra("Count",0)));
		}
		catch(Exception e)
		{
			//We don't need to much more than report it because the direct API request will sort it out for us.
            BugSenseHandler.sendExceptionMessage("ViewZenossEvent","OnCreate",e);
		}

		firstLoadHandler = new Handler() 
		{
			public void handleMessage(Message msg) 
			{
				dialog.dismiss();
				try
				{
					if(EventObject.has("result") && EventObject.getJSONObject("result").getBoolean("success") == true)
					{
						//Log.i("Event",EventObject.toString(3));
						TextView Title = (TextView) findViewById(R.id.EventTitle);
						TextView Component = (TextView) findViewById(R.id.Componant);
						TextView EventClass = (TextView) findViewById(R.id.EventClass);
						TextView Summary = (TextView) findViewById(R.id.Summary);
						TextView FirstTime = (TextView) findViewById(R.id.FirstTime);
						TextView LastTime = (TextView) findViewById(R.id.LastTime);
                        LinearLayout logList;

						EventDetails = EventObject.getJSONObject("result").getJSONArray("event").getJSONObject(0);

                        try
                        {
                            if(EventDetails.getString("eventState").equals("Acknowledged"))
                            {
                                ((ImageView) findViewById(R.id.ackIcon)).setImageResource(R.drawable.ic_acknowledged);
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        //Log.e("EventDetails",EventDetails.toString(3));

						try
						{
							Title.setText(EventDetails.getString("device_title"));
						}
						catch(Exception e)
						{
							Title.setText("Unknown Device - Event Details");
						}

						try
						{
							Component.setText(EventDetails.getString("component"));
						}
						catch(Exception e)
						{
							Component.setText("Unknown Component");
						}

						try
						{
							EventClass.setText(EventDetails.getString("eventClassKey"));
						}
						catch(Exception e)
						{
							EventClass.setText("Unknown Event Class");
						}
						
						try
						{
							ImageView img = (ImageView) findViewById(R.id.summaryImage);
							
							URLImageParser p = new URLImageParser(img, ViewZenossEvent.this);
							Spanned htmlSpan = Html.fromHtml(EventDetails.getString("message"), p, null);
							
							Summary.setText(htmlSpan);
							//Summary.setText(Html.fromHtml(EventDetails.getString("message")));
							
							//((ImageView) findViewById(R.id.summaryImage)).setImageDrawable(p.drawable);
							//Log.i("Summary",EventDetails.getString("message"));
							
							//((TextView) findViewById(R.id.Summary)).setVisibility(View.GONE);
							//((WebView) findViewById(R.id.summaryWebView)).loadData(EventDetails.getString("message"), "text/html", null);
							//((WebView) findViewById(R.id.summaryWebView)).loadDataWithBaseURL(null, EventDetails.getString("message"), "text/html", "UTF-8", "about:blank");
							
							try
							{
								Summary.setMovementMethod(LinkMovementMethod.getInstance());
							}
							catch(Exception e)
							{
								//Worth a shot
							}
						}
						catch(Exception e)
						{
							Summary.setText("No Summary available");
						}

						try
						{
							FirstTime.setText(EventDetails.getString("firstTime"));
						}
						catch(Exception e)
						{
							FirstTime.setText("No Start Date Provided");
						}

						try
						{
							LastTime.setText(EventDetails.getString("stateChange"));
						}
						catch(Exception e)
						{
							LastTime.setText("No Recent Date Provided");
						}

						try
						{
							((TextView) findViewById(R.id.EventCount)).setText("Count: " + EventDetails.getString("count"));
						}
						catch(Exception e)
						{
							((TextView) findViewById(R.id.EventCount)).setText("Count: ??");
						}

						try
						{
							((TextView) findViewById(R.id.Agent)).setText(EventDetails.getString("agent"));
						}
						catch(Exception e)
						{
							((TextView) findViewById(R.id.Agent)).setText("unknown");
						}
						
						try
						{
							JSONArray Log = EventDetails.getJSONArray("log");

							int LogEntryCount = Log.length();

                            logList = (LinearLayout) findViewById(R.id.LogList);

							if(LogEntryCount == 0)
							{
								/*String[] LogEntries = {"No log entries could be found"};
								((ListView) findViewById(R.id.LogList)).setAdapter(new ArrayAdapter<String>(ViewZenossEvent.this, R.layout.search_simple,LogEntries));*/

                                TextView newLog = new TextView(ViewZenossEvent.this);
                                newLog.setText("No log entries could be found");

                                logList.addView(newLog);
							}
							else
							{
								LogEntries = new String[LogEntryCount];


								for (int i = 0; i < LogEntryCount; i++)
								{
									//LogEntries[i] = Log.getJSONArray(i).getString(0) + " set " + Log.getJSONArray(i).getString(2) +"\nAt: " + Log.getJSONArray(i).getString(1);

                                    TextView newLog = new TextView(ViewZenossEvent.this);
                                    newLog.setText(Html.fromHtml("<strong>" + Log.getJSONArray(i).getString(0) + "</strong> wrote " + Log.getJSONArray(i).getString(2) +"\n<br/><strong>At:</strong> " + Log.getJSONArray(i).getString(1)));
                                    newLog.setPadding(0,6,0,6);
                                    logList.addView(newLog);
								}
	
								/*try
								{
									((ListView) findViewById(R.id.LogList)).setAdapter(new ArrayAdapter<String>(ViewZenossEvent.this, R.layout.search_simple,LogEntries));
								}
								catch(Exception e)
								{
									Toast.makeText(getApplicationContext(), "There was an error trying process the log entries for this event.", Toast.LENGTH_SHORT).show();
								}*/
							}
						}
						catch(Exception e)
						{
                            TextView newLog = new TextView(ViewZenossEvent.this);
                            newLog.setText("No log entries could be found");
                            newLog.setPadding(0,6,0,6);
                            ((LinearLayout) findViewById(R.id.LogList)).addView(newLog);

							/*String[] LogEntries = {"No log entries could be found"};
							try
							{
								((ListView) findViewById(R.id.LogList)).setAdapter(new ArrayAdapter<String>(ViewZenossEvent.this, R.layout.search_simple,LogEntries));
							}
							catch(Exception e1)
							{
								//BugSenseHandler.log("ViewZenossEvent-LogEntries", e1);
							}*/
						}
					}
					else
					{
						//Log.e("ViewEvent",EventObject.toString(3));
						Toast.makeText(ViewZenossEvent.this, "There was an error loading the Event details", Toast.LENGTH_LONG).show();
						//finish();
					}
				}
				catch(Exception e)
				{
					Toast.makeText(ViewZenossEvent.this, "An error was encountered parsing the JSON. An error report has been sent.", Toast.LENGTH_LONG).show();
					//BugSenseHandler.log("ViewZenossEvent", e);
				}
			}
		};

		dialog = new ProgressDialog(this);
		dialog.setTitle("Contacting Zenoss");
		dialog.setMessage("Please wait:\nLoading Event details....");
		dialog.show();
		dataPreload = new Thread() 
		{  
			public void run() 
			{
				try 
				{
					/*if(API == null)
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

					EventObject = API.GetEvent(getIntent().getStringExtra("EventID"));*/


                    if(settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
                    {
                        API = new ZenossAPIZaas();
                    }
                    else
                    {
                        API = new ZenossAPICore();
                    }

                    ZenossCredentials credentials = new ZenossCredentials(ViewZenossEvent.this);
                    API.Login(credentials);
                    EventObject = API.GetEvent(getIntent().getStringExtra("EventID"));
				} 
				catch (Exception e) 
				{
					firstLoadHandler.sendEmptyMessage(0);
                    BugSenseHandler.sendExceptionMessage("ViewZenossEvent","DataPreloadThread",e);
				}
                finally
                {
                    firstLoadHandler.sendEmptyMessage(1);
                }
			}
		};

		dataPreload.start();
	}

	private void AddLogMessage(final String Message)
	{
		addMessageProgressDialog  = new ProgressDialog(this);
		addMessageProgressDialog.setTitle("Contacting Zenoss");
		addMessageProgressDialog.setMessage("Please wait:\nProcessing Event Log Updates");
		addMessageProgressDialog.show();

		addLogMessageHandler = new Handler() 
		{
			public void handleMessage(Message msg) 
			{
				addMessageProgressDialog.dismiss();

				if(msg.what == 1)
				{
					try 
					{
						/*String[] tmp = LogEntries.clone();
						final int NewArrlength = tmp.length + 1;
						LogEntries = new String[NewArrlength];

						LogEntries[0] = settings.getString("userName", "") + " wrote " + Message + "\nAt: Just now";*/

						/*for (int i = 1; i < NewArrlength; ++i) //
						{
							LogEntries[i] = tmp[(i -1)];
						}
						tmp = null;//help out the GC
                        ((ListView) findViewById(R.id.LogList)).setAdapter(new ArrayAdapter<String>(ViewZenossEvent.this, R.layout.search_simple,LogEntries));*/

                        TextView newLog = new TextView(ViewZenossEvent.this);
                        newLog.setText(Html.fromHtml("<strong>" + settings.getString("userName", "") + "</strong> wrote " + Message + "\n<br/><strong>At:</strong> Just now"));
                        newLog.setPadding(0,6,0,6);
                        ((LinearLayout) findViewById(R.id.LogList)).addView(newLog);
					} 
					catch (Exception e) 
					{
                        BugSenseHandler.sendExceptionMessage("ViewZenossEvent","AddMessageProgressHandler",e);
						Toast.makeText(ViewZenossEvent.this, "The log message was successfully sent to Zenoss but an error occured when updating the UI", Toast.LENGTH_LONG).show();
					}
				}
				else
				{
					Toast.makeText(ViewZenossEvent.this, "An error was encountered adding your message to the log", Toast.LENGTH_LONG).show();
				}

			}
		};

		addLogMessageThread = new Thread() 
		{  
			public void run() 
			{
				Boolean Success = false;

				try 
				{
					if(API == null)
					{
                        if(settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
                        {
                            API = new ZenossAPIZaas();
                        }
                        else
                        {
                            API = new ZenossAPICore();
                        }
                        ZenossCredentials credentials = new ZenossCredentials(ViewZenossEvent.this);
                        API.Login(credentials);
					}

                    Success = API.AddEventLog(getIntent().getStringExtra("EventID"),Message);
				} 
				catch (Exception e) 
				{
                    BugSenseHandler.sendExceptionMessage("ViewZenossEvent","AddLogMessageThread",e);
					addLogMessageHandler.sendEmptyMessage(0);
				}

				if(Success)
				{
					addLogMessageHandler.sendEmptyMessage(1);
				}
				else
				{
					addLogMessageHandler.sendEmptyMessage(0);
				}
			}
		};

		addLogMessageThread.start();
	}
}
