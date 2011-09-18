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

import java.text.DecimalFormat;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends Activity
{
	private SharedPreferences settings = null;
	ProgressDialog dialog;
	Thread peformLogin;
	Handler handler;
	ZenossAPIv2 API = null;
	
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	dialog = new ProgressDialog(this);
		dialog.setMessage("Checking Details.....");
		
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);

        setContentView(R.layout.settings);
        
        
        EditText urlET = (EditText) findViewById(R.id.ZenossURL);
        EditText nameET = (EditText) findViewById(R.id.ZenossUserName);
        EditText passwordET = (EditText) findViewById(R.id.ZenossPassword);
        EditText pagerDutyET = (EditText) findViewById(R.id.PagerDutyAPIKey);
        CheckBox BackgroundService = (CheckBox) findViewById(R.id.AllowBackgroundService);
        SeekBar BackgroundServiceDelay = (SeekBar) findViewById(R.id.BackgroundServiceDelay);
        
        if(settings.getString("URL", "--").equals("--") == false)
        	urlET.setText(settings.getString("URL",""));
        
        if(settings.getString("userName", "--").equals("--") == false)
        	nameET.setText(settings.getString("userName",""));
        
        if(settings.getString("passWord", "--").equals("--") == false)
        	passwordET.setText(settings.getString("passWord",""));
        
        if(settings.getString("pagerDuty", "--").equals("--") == false)
        	pagerDutyET.setText(settings.getString("pagerDuty",""));
        
        if(settings.getBoolean("AllowBackgroundService", true) == true)
        	BackgroundService.setChecked(true);
        
        BackgroundServiceDelay.setProgress(settings.getInt("BackgroundServiceDelay", 30));
        TextView DelayLabel = (TextView) findViewById(R.id.DelayLabel);
        if(settings.getInt("BackgroundServiceDelay", 30) < 60)
		{
			DelayLabel.setText(Integer.toString(settings.getInt("BackgroundServiceDelay", 30)) + " secs");
		}
		else
		{
			double minutes = (0.016666667 * settings.getInt("BackgroundServiceDelay", 30));
			DecimalFormat df = new DecimalFormat("#.##");
			DelayLabel.setText(df.format(minutes) + " mins");
		}
        
        BackgroundServiceDelay.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
        {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) 
			{
				if(progress >= 30)
				{
					TextView DelayLabel = (TextView) findViewById(R.id.DelayLabel);
					if(progress < 60)
					{
						DelayLabel.setText(Integer.toString(progress) + " secs");
					}
					else
					{
						double minutes = (0.016666667 * progress);
						DecimalFormat df = new DecimalFormat("#.##");
						DelayLabel.setText(df.format(minutes) + " mins");
					}
				}
				else
				{
					seekBar.setProgress(30);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
		});
        
        handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			dialog.dismiss();
    			
    			if(API.getLoggedInStatus() == true)
    			{
            		Toast.makeText(Settings.this, "Login Successful", Toast.LENGTH_SHORT).show();
            		if(getIntent().getBooleanExtra("firstRun", false) == true)
            		{
            			Intent EventListIntent = new Intent(Settings.this, rhestr.class);
            			Settings.this.startActivity(EventListIntent);
           	    	 	finish();
            		}
            		else
            		{
            			finish();
            		}
    			}
    			else
    			{
    				Toast.makeText(Settings.this, "Login Failed", Toast.LENGTH_SHORT).show();
    			}
    		}
    	};
        
        
        
        Button LoginButton = (Button) findViewById(R.id.SaveButton);
        LoginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) 
            {
            	dialog.show();
            	EditText urlET = (EditText) findViewById(R.id.ZenossURL);
                EditText nameET = (EditText) findViewById(R.id.ZenossUserName);
                EditText passwordET = (EditText) findViewById(R.id.ZenossPassword);
                EditText pagerDutyET = (EditText) findViewById(R.id.PagerDutyAPIKey);
                CheckBox BackgroundService = (CheckBox) findViewById(R.id.AllowBackgroundService);
                SeekBar BackgroundServiceDelay = (SeekBar) findViewById(R.id.BackgroundServiceDelay);
                
            	SharedPreferences.Editor editor = settings.edit();
                editor.putString("URL", urlET.getText().toString());
                editor.putString("userName", nameET.getText().toString());
                editor.putString("passWord", passwordET.getText().toString());
                editor.putString("pagerDuty", pagerDutyET.getText().toString());
                editor.putBoolean("AllowBackgroundService", BackgroundService.isChecked());
                editor.putInt("BackgroundServiceDelay", BackgroundServiceDelay.getProgress());
                editor.commit();
                
                if(BackgroundService.isChecked())
                {
                	startService(new Intent(v.getContext(), ZenossPoller.class));
                }
                else
                {
                	stopService(new Intent(v.getContext(), ZenossPoller.class));
                }
                CreateThread();
                peformLogin.start();
                    
            }
        });
        
    }
    
    public void CreateThread()
    {
    	peformLogin = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
					API = new ZenossAPIv2(settings.getString("userName", "--"), settings.getString("passWord", "--"), settings.getString("URL", "--"));
				} 
    			catch (Exception e) 
    			{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    			//Double check it all worked
    			API.CheckLoggedIn();
            	
    			handler.sendEmptyMessage(0);
            	return;
    		}
    	};
    	
    }
}
