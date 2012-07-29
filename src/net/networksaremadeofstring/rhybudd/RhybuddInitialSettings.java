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

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;

public class RhybuddInitialSettings extends SherlockActivity
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
    	dialog = new ProgressDialog(this);
		dialog.setMessage("Checking Details.....");
		dialog.setCancelable(false);
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        
        actionbar = getSupportActionBar();
		actionbar.setTitle("Basic Zenoss Settings");
		
		setContentView(R.layout.settings_initial);
		
		handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 0 && API != null && API.getLoggedInStatus() == true)
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
    					BugSenseHandler.log("InitialSettings", e);
    					Toast.makeText(RhybuddInitialSettings.this, "Logged in Successfully. Preparing Database!", Toast.LENGTH_SHORT).show();
    				}
        			
    				try
    				{
	        			rhybuddCache = new RhybuddDatabase(RhybuddInitialSettings.this);
	        			rhybuddCache.RefreshCache();
	        			handler.sendEmptyMessageDelayed(1, 1000);
    				}
    				catch(Exception e)
    				{
    					BugSenseHandler.log("InitialSettings", e);
    				}
    			}
    			else if(msg.what == 1)
    			{
    				if(rhybuddCache.hasCacheRefreshed())
    				{
    					try
    					{
    						rhybuddCache.Close();
    					}
    					catch(Exception e)
    					{
    						BugSenseHandler.log("InitialSettings", e);
    					}
    					
    					try
    					{
    						dialog.setMessage("Caching Complete!");
    					}
    					catch(Exception e)
        				{
        					BugSenseHandler.log("InitialSettings", e);
        					Toast.makeText(RhybuddInitialSettings.this, "Caching Complete!", Toast.LENGTH_SHORT).show();
        				}
    					
    					this.sendEmptyMessageDelayed(2,1000);
    				}
    				else
    				{
    					ellipsis += ".";
    					try
    					{
    						dialog.setMessage("Performing initial cache.\r\nPlease wait..." + ellipsis);
    						handler.sendEmptyMessageDelayed(1, 1000);
    					}
    					catch(Exception e)
        				{
        					BugSenseHandler.log("InitialSettings", e);
        					Toast.makeText(RhybuddInitialSettings.this, "Performing initial cache.\r\nPlease wait..." + ellipsis, Toast.LENGTH_SHORT).show();
        					handler.sendEmptyMessageDelayed(1, 2000);
        				}
    				}
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
    					BugSenseHandler.log("InitialSettings", e);
    				}
    				
    				Intent in = new Intent();
        	        setResult(1,in);
        	        finish();
    			}
    			else if(msg.what == 99)
    			{
    				try
    				{
    					dialog.dismiss();
    				}
    				catch(Exception e)
    				{
    					//Not much else we can do here :/
    					BugSenseHandler.log("InitialSettings", e);
    				}
    				
    				try
    				{
    					Toast.makeText(RhybuddInitialSettings.this, "An error was encountered;\r\n"+ msg.getData().getString("error"), Toast.LENGTH_SHORT).show();
    				}
    				catch(Exception e)
    				{
    					BugSenseHandler.log("InitialSettings", e);
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
    					BugSenseHandler.log("InitialSettings", e);
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
				
    			try 
    			{
					API = new ZenossAPIv2(settings.getString("userName", "--"), 
							settings.getString("passWord", "--"), 
							settings.getString("URL", "--") , 
							settings.getString("BAUser", "") , 
							settings.getString("BAPassword", ""));
					
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
	    					bundle.putString("error", "Attempting to login to the API failed.");
	    					Message.obtain();
	    					msg.what = 99;
	    					msg.setData(bundle);
	    					handler.sendMessage(msg);
	    				}
	    			}
	    			catch(Exception e)
	    			{
	    				/*BugSenseHandler.log("InitialSettings-peformLogin", e);
	    				//e.printStackTrace();
	    				bundle.putString("error", "Attempting to login to the API failed;\r\n"+e.getMessage());
	    				Message.obtain();
						msg.what = 99;
						msg.setData(bundle);
						handler.sendMessage(msg);*/
	    				HandleException(e,"Attempting to login to the API failed;\r\n"+e.getMessage());
	    			}
				} 
    			catch(java.lang.IllegalStateException ise)
    			{
    				HandleException(ise, "Neither the Hostname, Username or Password can be null");
    			}
    			catch(java.net.UnknownHostException uhe)
    			{
    				HandleException(uhe, "That DNS name could not be resolved. Please double check.");
    			}
    			catch(org.apache.http.conn.ConnectTimeoutException cte)
    			{
    				Log.e("Timeout","Hit here");
    				/*BugSenseHandler.log("InitialSettings-peformLogin", cte);
    				bundle.putString("error", "Timed out connecting to your Zenoss instance (20 seconds)");
    				Message.obtain();
					msg.what = 99;
					msg.setData(bundle);
					handler.sendMessage(msg);*/
    				HandleException(cte, "Timed out connecting to your Zenoss instance (20 seconds)");
    			}
    			catch (Exception e) 
    			{
    				Log.e("Exception","Hit here");
					e.printStackTrace();
					/*BugSenseHandler.log("InitialSettings-peformLogin", e);
					
					bundle.putString("error", e.getMessage());
					Message.obtain();
					msg.what = 99;
					msg.setData(bundle);
					handler.sendMessage(msg);*/
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
	        CheckBox test;
	        
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
    		/*Message msg = new Message();
			Bundle bundle = new Bundle();
    		BugSenseHandler.log("InitialSettings-doSave", e);
    		Message.obtain();
			//e.printStackTrace();
			bundle.putString("error", "Attempting to save your credentials to local storage failed;\r\n"+e.getMessage());
			msg.setData(bundle);
			msg.what = 99;
			handler.sendMessage(msg);*/
    		HandleException(e,"Attempting to save your credentials to local storage failed;\r\n"+e.getMessage());
    	}
	}
	
	public void HandleException(final Exception e, String OverrideMessage)
	{
		Bundle bundle = new Bundle();
		Message msg = new Message();
		e.printStackTrace();
		((Thread) new Thread(){
			public void run() 
    		{
				BugSenseHandler.log("InitialSettings", e);
    		}
		}).start();
		
		if(e.getMessage() != null && OverrideMessage.equals(""))
		{
			bundle.putString("error", e.getMessage());
		}
		else
		{
			if(OverrideMessage.equals(""))
			{
				bundle.putString("error", "Unknown Error");
			}
			else
			{
				bundle.putString("error", OverrideMessage);
			}
		}
		
		Message.obtain();
		msg.what = 99;
		msg.setData(bundle);
		handler.sendMessage(msg);
	}
	
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getSupportMenuInflater();
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
