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
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

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
		//actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setTitle("Basic Zenoss Settings");
		//actionbar.setHomeButtonEnabled(true);
		
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
    				
        			dialog.setMessage("Logged in successfully. Preparing database...");
        			
        			rhybuddCache = new RhybuddDatabase(RhybuddInitialSettings.this);
        			rhybuddCache.RefreshCache();
        			handler.sendEmptyMessageDelayed(1, 1000);
    			}
    			else if(msg.what == 1)
    			{
    				if(rhybuddCache.hasCacheRefreshed())
    				{
    					Log.i("Handler","Cache hasn't updated yet");
    					rhybuddCache.Close();
    					dialog.setMessage("Caching Complete!");
    					this.sendEmptyMessageDelayed(2,1000);
    				}
    				else
    				{
    					ellipsis += ".";
    					dialog.setMessage("Performing initial cache.\r\nPlease wait..." + ellipsis);
    					handler.sendEmptyMessageDelayed(1, 1000);
    				}
    			}
    			else if (msg.what == 2)
    			{
    				dialog.dismiss();
    				Intent in = new Intent();
        	        setResult(1,in);
        	        finish();
    			}
    			else
    			{
    				dialog.dismiss();
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
				} 
    			catch (Exception e) 
    			{
					// TODO Auto-generated catch block
					e.printStackTrace();
					
					bundle.putString("error", e.getMessage());
					msg.what = 99;
					handler.sendMessage(msg);
				}
    			
    			try
    			{
    				Boolean Success = API.CheckLoggedIn();
    				handler.sendEmptyMessage(0);
    			}
    			catch(Exception e)
    			{
    				e.printStackTrace();
    				bundle.putString("error", "Attempting to login to the API failed;\r\n"+e.getMessage());
					msg.what = 99;
					handler.sendMessage(msg);
    			}
    			
            	return;
    		}
    	};
    	
    	EditText urlET = (EditText) findViewById(R.id.ZenossURL);
        EditText nameET = (EditText) findViewById(R.id.ZenossUserName);
        EditText passwordET = (EditText) findViewById(R.id.ZenossPassword);
        
        EditText BAUser = (EditText) findViewById(R.id.basicAuthUser);
        EditText BAPassword = (EditText) findViewById(R.id.basicAuthPassword);
        
    	SharedPreferences.Editor editor = settings.edit();
        editor.putString("URL", urlET.getText().toString());
        editor.putString("userName", nameET.getText().toString());
        editor.putString("passWord", passwordET.getText().toString());
        
        editor.putString("BAUser", BAUser.getText().toString());
        editor.putString("BAPassword", BAPassword.getText().toString());
       
       
        editor.commit();
        
        //Start the service (if it isn't already) and tell it that we're starting it with a settings change in mind
        peformLogin.start();
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
}
