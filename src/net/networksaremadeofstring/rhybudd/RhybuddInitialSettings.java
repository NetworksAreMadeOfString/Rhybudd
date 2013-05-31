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

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.view.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;

public class RhybuddInitialSettings extends Activity
{
	SharedPreferences settings = null;
	ProgressDialog dialog;
	Thread peformLogin;
	Handler handler;
	ZenossAPIv2 API = null;
	ActionBar actionbar;
	RhybuddDatabase rhybuddCache;
	String ellipsis = ".";
	
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
		super.onCreate(savedInstanceState);
		
    	dialog = new ProgressDialog(this);
		dialog.setMessage("Checking Details.....");
		dialog.setCancelable(false);
        
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        actionbar = getActionBar();
		actionbar.setTitle("Basic Zenoss Settings");
		
		setContentView(R.layout.settings_initial);
		
		handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 0 && API != null)// && API.getLoggedInStatus() == true
    			{
    				SharedPreferences.Editor editor = settings.edit();
    				editor.putBoolean("credentialsSuccess", true);
    				editor.commit();
    				
    				try
    				{
    					dialog.setMessage("Logged in successfully. Preparing database...");
    				}
    				catch(Exception e)
    				{
    					//BugSenseHandler.log("InitialSettings", e);
    					Toast.makeText(RhybuddInitialSettings.this, "Logged in Successfully. Preparing Database!", Toast.LENGTH_SHORT).show();
    				}
        			
    				try
    				{
	        			rhybuddCache = new RhybuddDatabase(RhybuddInitialSettings.this);
	        			((Thread) new Thread(){
	        				public void run()
	        				{
	        					//Events
	        					try
	        					{
		        					//ZenossAPIv2 API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
	        						if(settings.getBoolean("httpBasicAuth", false))
	        						{
	        							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
	        						}
	        						else
	        						{
	        							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
	        						}
	        						
		        					if(API != null)
		    						{
		        						List<ZenossEvent> listOfZenossEvents = API.GetRhybuddEvents(settings.getBoolean("SeverityCritical", true),
		    									settings.getBoolean("SeverityError", true),
		    									settings.getBoolean("SeverityWarning", true),
		    									settings.getBoolean("SeverityInfo", false),
		    									settings.getBoolean("SeverityDebug", false),
		    									settings.getBoolean("onlyProductionEvents", true),
		    									settings.getString("SummaryFilter", ""),
		    									settings.getString("DeviceFilter", ""));
	
		    							if(listOfZenossEvents!= null && listOfZenossEvents.size() > 0)
		    							{
		    								rhybuddCache.UpdateRhybuddEvents(listOfZenossEvents);
		    								handler.sendEmptyMessage(1);
		    							}
		    							else
		    							{
		    								Log.e("initialSettings","There was a problem processing the GetRhybuddEvents call");
		    								HandleException(null, "Initialising the API Failed. An error message has been logged.");
		    							}
		    						}
		        					else
		        					{
		        						HandleException(null, "Initialising the API Failed. An error message has been logged.");
		        					}
	        					}
	        					catch(Exception e)
	        					{
	        						HandleException(e, "Initialising the API Failed. An error message has been logged.");
	        					}
	        					
	        					//Devices
	        					try
	        					{
		        					List<ZenossDevice> listOfZenossDevices = API.GetRhybuddDevices();
		    						
		    						if(listOfZenossDevices != null)
		    						{
		    							handler.sendEmptyMessage(3);
		    							handler.sendEmptyMessageDelayed(2, 1500);
		    							rhybuddCache.UpdateRhybuddDevices(listOfZenossDevices);
		    						}
		    						else
		    						{
		    							//TODO Bundle an error
		    						}
	        					}
	        					catch(Exception e)
	        					{
	        						e.printStackTrace();
	        						HandleException(e, "Caching devices as failed. An error message has been logged.");
	        					}
	        					
	        				}
	        			}).start();
    				}
    				catch(Exception e)
    				{
    					////BugSenseHandler.log("InitialSettings", e);
    					HandleException(e, "Initialising the Database failed. An error message has been logged.");
    				}
    			}
    			else if(msg.what == 1)
    			{
    				dialog.setMessage("Events Cached! Now caching Devices.\r\nPlease wait...");
    			}
    			else if (msg.what == 2)
    			{
    				try
    				{
    					dialog.dismiss();
    				}
    				catch(Exception e)
    				{
    					//Not much else we can do here :/
    					//BugSenseHandler.log("InitialSettings", e);
    				}
    				
    				Intent in = new Intent();
        	        setResult(1,in);
        	        finish();
    			}
    			else if(msg.what == 3)
    			{
    				dialog.setMessage("Caching complete!\r\nVerifying.");
    			}
    			else if(msg.what == 99)
    			{
    				((TextView) findViewById(R.id.debugOutput)).setText(msg.getData().getString("exception") + "\n");
    				
    				try
    				{
    					dialog.dismiss();
    				}
    				catch(Exception e)
    				{
    					//Not much else we can do here :/
    					//BugSenseHandler.log("InitialSettings", e);
    				}
    				
    				try
    				{
    					Toast.makeText(RhybuddInitialSettings.this, "An error was encountered;\r\n"+ msg.getData().getString("error"), Toast.LENGTH_SHORT).show();
    				}
    				catch(Exception e)
    				{
    					//BugSenseHandler.log("InitialSettings", e);
    					Toast.makeText(RhybuddInitialSettings.this, "An unknown error occured. It has been reported.", Toast.LENGTH_SHORT).show();
    				}
    			}
    			else
    			{
    				try
    				{
    					dialog.dismiss();
    				}
    				catch(Exception e)
    				{
    					//Not much else we can do here :/
    					//BugSenseHandler.log("InitialSettings", e);
    				}
    				Toast.makeText(RhybuddInitialSettings.this, "Login Failed - Please check details.", Toast.LENGTH_SHORT).show();
    			}
    		}
    	};

    	Button LoginButton = (Button) findViewById(R.id.SaveButton);
        LoginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) 
            {
            	DoSave();
            }
        });
    }
	
	public void DoSave()
	{
		dialog.show();
    	
    	peformLogin = new Thread() 
    	{  
    		public void run() 
    		{
    			Message msg = new Message();
				Bundle bundle = new Bundle();
				String MessageExtra = "";
				
    			try 
    			{
    				//try
    				//{
						API = new ZenossAPIv2(settings.getString("userName", "--"), 
								settings.getString("passWord", "--"), 
								settings.getString("URL", "--") , 
								settings.getString("BAUser", "") , 
								settings.getString("BAPassword", ""));
    				/*}
    				catch(ConnectTimeoutException cte)
					{
						if(cte.getMessage() != null)
						{
							MessageExtra = "A time out exception encountered;\r\n" + cte.getMessage().toString();
						}
						else
						{
							MessageExtra = "An time out exception was encountered but the exception thrown contains no further information.";
						}
						API = null;
					}
    				catch(HttpHostConnectException hhce)
    				{
    					if(hhce.getMessage() != null)
						{
							MessageExtra = "A host connection exception encountered;\r\n" + hhce.getMessage().toString();
						}
						else
						{
							MessageExtra = "An host connection exception (most likely connection refused) was encountered but the exception thrown contains no further information.";
						}
    					API = null;
    				}
    				catch(Exception e)
    				{
    					if(e.getMessage() != null)
						{
							MessageExtra = "An exception was encountered;\r\n" + e.getMessage().toString();
						}
						else
						{
							MessageExtra = "An exception was encountered but the exception thrown contains no further information.";
						}
    					API = null;
    				}*/
					
					try
	    			{
	    				if(API != null && API.CheckLoggedIn())
	    				{
	    					//API.CheckLoggedIn();
	    					Message.obtain();
	    					handler.sendEmptyMessage(0);
	    				}
	    				else
	    				{
	    					bundle.putString("error", "Attempting to login to the API failed." + MessageExtra);
	    					Message.obtain();
	    					msg.what = 99;
	    					msg.setData(bundle);
	    					handler.sendMessage(msg);
	    				}
	    			}
	    			catch(Exception e)
	    			{
	    				HandleException(e,"Attempting to login to the API failed;\r\n"+e.getMessage());
	    			}
				} 
    			catch(java.lang.IllegalStateException ise)
    			{
    				HandleException(ise, "Neither the Schema, Hostname, Username or Password can be null");
    			}
    			catch(java.net.UnknownHostException uhe)
    			{
    				HandleException(uhe, "That DNS name could not be resolved. Please double check.");
    			}
    			catch(org.apache.http.conn.ConnectTimeoutException cte)
    			{
    				//Log.e("Timeout","Hit here");
    				HandleException(cte, "Timed out connecting to your Zenoss instance (20 seconds)");
    			}
    			catch (Exception e) 
    			{
    				//Log.e("Exception","Hit here");
					e.printStackTrace();
					API = null;
					HandleException(e, "");
				}
    			
            	return;
    		}
    	};
    	
    	try
    	{
	    	EditText urlET = (EditText) findViewById(R.id.ZenossURL);
	        EditText nameET = (EditText) findViewById(R.id.ZenossUserName);
	        EditText passwordET = (EditText) findViewById(R.id.ZenossPassword);
	        
	        EditText BAUser = (EditText) findViewById(R.id.basicAuthUser);
	        EditText BAPassword = (EditText) findViewById(R.id.basicAuthPassword);
	        
	    	SharedPreferences.Editor editor = settings.edit();
	        editor.putString("URL", urlET.getText().toString());
	        editor.putString("userName", nameET.getText().toString());
	        editor.putString("passWord", passwordET.getText().toString());
	        
	        editor.putBoolean("httpBasicAuth", ((CheckBox) findViewById(R.id.basicAuthCheckBox)).isChecked());
	        editor.putString("BAUser", BAUser.getText().toString());
	        editor.putString("BAPassword", BAPassword.getText().toString());
	        editor.commit();
	        
	        //Attempt to login
	        peformLogin.start();
    	}
    	catch(Exception e)
    	{
    		HandleException(e,"Attempting to save your credentials to local storage failed;\r\n"+e.getMessage());
    	}
	}
	
	public void HandleException(final Exception e, String OverrideMessage)
	{
		Bundle bundle = new Bundle();
		Message msg = new Message();
		
		if(e != null)
		{
			e.printStackTrace();
			
			((Thread) new Thread(){
				public void run() 
	    		{
					////BugSenseHandler.log("InitialSettings", e);
	    		}
			}).start();
		}
		
		if(e != null && e.getMessage() != null && OverrideMessage.equals(""))
		{
			bundle.putString("error", e.getMessage());
		}
		else
		{
			if(OverrideMessage.equals(""))
			{
				//bundle.putString("error", "Unknown Error");
				bundle.putString("exception", e.getMessage());
			}
			else
			{
				bundle.putString("error", OverrideMessage);
				bundle.putString("exception", e.getMessage());
			}
		}
		
		Message.obtain();
		msg.what = 99;
		msg.setData(bundle);
		handler.sendMessage(msg);
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.initial_settings, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) 
        {
	        case R.id.save_settings:
	        {
	        	DoSave();
	            return true;
	        }
	        
	        default:
	        {
	        	return false;
	        }
        }
    }
	
	@Override
    public void onBackPressed() 
    {
    	//Return back to the launcher
    	Intent in = new Intent();
        setResult(2,in);
        finish();
    }
}
