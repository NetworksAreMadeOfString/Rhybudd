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

import org.json.JSONObject;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

public class ViewZenossEvent extends Activity
{
	ZenossAPIv2 API = null;
	JSONObject EventObject = null;
	JSONObject EventDetails = null;
	private SharedPreferences settings = null;
	Handler firstLoadHandler;
	ProgressDialog dialog;
	Thread dataPreload;
	private String EventID;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
        setContentView(R.layout.view_zenoss_event);
        
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
    						Title.setText(EventDetails.getString("device_title") + " Event Details");
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
}
