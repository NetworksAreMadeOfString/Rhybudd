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

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

public class ViewZenossEvent extends Activity
{
	ZenossAPIv2 API = null;
	JSONObject EventObject = null;
	JSONObject EventDetails = null;
	private SharedPreferences settings = null;
	Handler firstLoadHandler, addLogMessageHandler;
	ProgressDialog dialog,addMessageProgressDialog;
	Thread dataPreload, addLogMessageThread;
	//private String EventID;
	Dialog addMessageDialog;
	String[] LogEntries;
	
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        setContentView(R.layout.view_zenoss_event);
        ((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
        
        ImageView AddToLogButton = (ImageView) findViewById(R.id.AddToLogImageView);
        AddToLogButton.setClickable(true);
        AddToLogButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
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
					}
				);
				
				addMessageDialog.show();
			}
        });

        firstLoadHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			dialog.dismiss();
    			try
    			{
    				if(EventObject.getJSONObject("result").getBoolean("success") == true)
    				{
    					
    					TextView Title = (TextView) findViewById(R.id.EventTitle);
    					TextView Component = (TextView) findViewById(R.id.Componant);
    					TextView EventClass = (TextView) findViewById(R.id.EventClass);
    					TextView Summary = (TextView) findViewById(R.id.Summary);
    					TextView FirstTime = (TextView) findViewById(R.id.FirstTime);
    					TextView LastTime = (TextView) findViewById(R.id.LastTime);
    					
    					EventDetails = EventObject.getJSONObject("result").getJSONArray("event").getJSONObject(0);
    					
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
    						Summary.setText(EventDetails.getString("message"));
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
    						((TextView) findViewById(R.id.EventCount)).setText(EventDetails.getString("count"));
    					}
    					catch(Exception e)
    					{
    						((TextView) findViewById(R.id.EventCount)).setText("Unknown");
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
    						
    						LogEntries = new String[LogEntryCount];
    						
    						for (int i = 0; i < LogEntryCount; i++)
	    					{
    							LogEntries[i] = Log.getJSONArray(i).getString(0) + " set " + Log.getJSONArray(i).getString(2) +"\nAt: " + Log.getJSONArray(i).getString(1);
	    					}
    						
    						((ListView) findViewById(R.id.LogList)).setAdapter(new ArrayAdapter<String>(ViewZenossEvent.this, R.layout.search_simple,LogEntries));
    					}
    					catch(Exception e)
    					{
    						//e.printStackTrace();
    						String[] LogEntries = {"No log entries could be found"};
    						((ListView) findViewById(R.id.LogList)).setAdapter(new ArrayAdapter<String>(ViewZenossEvent.this, R.layout.search_simple,LogEntries));
    					}
    				}
    				else
    				{
    					Toast.makeText(ViewZenossEvent.this, "There was an error loading the Event details", Toast.LENGTH_LONG).show();
    					finish();
    				}
    			}
    			catch(Exception e)
    			{
    				Toast.makeText(ViewZenossEvent.this, "An error was encountered parsing the JSON.", Toast.LENGTH_LONG).show();
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
    				if(API == null)
    				{
    					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				}
    				
					EventObject = API.GetEvent(getIntent().getStringExtra("EventID"));
	    			//Events = EventsObject.getJSONObject("result").getJSONArray("events");
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
		    			String[] tmp = LogEntries.clone();
		    			final int NewArrlength = tmp.length + 1;
		    			LogEntries = new String[NewArrlength];
		    			
		    			LogEntries[0] = settings.getString("userName", "") + " set " + Message + "\nAt: Just now";
		    			
		    			for (int i = 1; i < NewArrlength; ++i) //
		    			{
		    				LogEntries[i] = tmp[(i -1)];
		    	        }

		    			tmp = null;//help out the GC
	    			
					
						((ListView) findViewById(R.id.LogList)).setAdapter(new ArrayAdapter<String>(ViewZenossEvent.this, R.layout.search_simple,LogEntries));
					} 
					catch (Exception e) 
					{
						//e.printStackTrace();
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
    					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				}
    				
    				Success = API.AddEventLog(getIntent().getStringExtra("EventID"),Message);
				} 
    			catch (Exception e) 
    			{
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
