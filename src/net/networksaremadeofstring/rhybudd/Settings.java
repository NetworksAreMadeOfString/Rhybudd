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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
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
	private PendingIntent mAlarmSender;
	private Boolean firstRun = false;
	
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	dialog = new ProgressDialog(this);
		dialog.setMessage("Checking Details.....");
		
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);

        if(getIntent().getBooleanExtra("firstRun", false))
        {
        	Thread ProcessDatabase = new Thread() 
    		{  
    			public void run() 
    			{
    				SQLiteDatabase cacheDB = Settings.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
    				cacheDB.execSQL("CREATE  TABLE \"events\" (\"EVID\" TEXT PRIMARY KEY  NOT NULL  UNIQUE , \"Count\" INTEGER, \"lastTime\" TEXT, \"device\" TEXT, \"summary\" TEXT, \"eventState\" TEXT, \"firstTime\" TEXT, \"severity\" TEXT)");
    				cacheDB.execSQL("CREATE  TABLE \"devices\" (\"productionState\" TEXT, \"ipAddress\" INTEGER, \"events\" TEXT, \"name\" TEXT, \"uid\" TEXT PRIMARY KEY  NOT NULL  UNIQUE )");
    				cacheDB.close();
    			}
    		};
    		
    		ProcessDatabase.start();
        }
        
        setContentView(R.layout.settings);
        
        EditText urlET = (EditText) findViewById(R.id.ZenossURL);
        EditText nameET = (EditText) findViewById(R.id.ZenossUserName);
        EditText passwordET = (EditText) findViewById(R.id.ZenossPassword);
        EditText pagerDutyURL = (EditText) findViewById(R.id.PagerDutyURL);
        EditText pagerDutyEmail = (EditText) findViewById(R.id.PagerDutyEmailAddress);
        EditText pagerDutyPass = (EditText) findViewById(R.id.PagerDutyPassword);
        
        SeekBar BackgroundServiceDelay = (SeekBar) findViewById(R.id.BackgroundServiceDelay);
        
        if(settings.getString("URL", "--").equals("--") == false)
        	urlET.setText(settings.getString("URL",""));
        
        if(settings.getString("userName", "--").equals("--") == false)
        	nameET.setText(settings.getString("userName",""));
        
        if(settings.getString("passWord", "--").equals("--") == false)
        	passwordET.setText(settings.getString("passWord",""));
        
        if(settings.getString("pagerDutyURL", "--").equals("--") == false)
        	pagerDutyURL.setText(settings.getString("pagerDutyURL",""));
        
        if(settings.getString("pagerDutyEmail", "--").equals("--") == false)
        	pagerDutyEmail.setText(settings.getString("pagerDutyEmail",""));
        
        if(settings.getString("pagerDutyPass", "--").equals("--") == false)
        	pagerDutyPass.setText(settings.getString("pagerDutyPass",""));
        
        if(settings.getBoolean("AllowBackgroundService", true) == true)
        	((CheckBox) findViewById(R.id.AllowBackgroundService)).setChecked(true);
        
        if(settings.getBoolean("SeverityCritical", true) == true)
        	((CheckBox) findViewById(R.id.criticalCheckBox)).setChecked(true);
        
        if(settings.getBoolean("SeverityError", true) == true)
        	((CheckBox) findViewById(R.id.errorCheckBox)).setChecked(true);
        
        if(settings.getBoolean("SeverityWarning", true) == true)
        	((CheckBox) findViewById(R.id.warningCheckBox)).setChecked(true);
        
        if(settings.getBoolean("SeverityInfo", false) == true)
        	((CheckBox) findViewById(R.id.infoCheckBox)).setChecked(true);
        
        if(settings.getBoolean("SeverityDebug", false) == true)
        	((CheckBox) findViewById(R.id.debugCheckBox)).setChecked(true);
        
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
    			
    			if(API != null && API.getLoggedInStatus() == true)
    			{
            		//Toast.makeText(Settings.this, "Login Successful", Toast.LENGTH_SHORT).show();
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
                
                EditText pagerDutyURL = (EditText) findViewById(R.id.PagerDutyURL);
                EditText pagerDutyEmail = (EditText) findViewById(R.id.PagerDutyEmailAddress);
                EditText pagerDutyPass = (EditText) findViewById(R.id.PagerDutyPassword);
                CheckBox BackgroundService = (CheckBox) findViewById(R.id.AllowBackgroundService);
                SeekBar BackgroundServiceDelay = (SeekBar) findViewById(R.id.BackgroundServiceDelay);
                
                CheckBox criticalCheckBox = (CheckBox) findViewById(R.id.criticalCheckBox);
                CheckBox errorCheckBox = (CheckBox) findViewById(R.id.errorCheckBox);
                CheckBox warningCheckBox = (CheckBox) findViewById(R.id.warningCheckBox);
                CheckBox infoCheckBox = (CheckBox) findViewById(R.id.infoCheckBox);
                CheckBox debugCheckBox = (CheckBox) findViewById(R.id.debugCheckBox);
                
            	SharedPreferences.Editor editor = settings.edit();
                editor.putString("URL", urlET.getText().toString());
                editor.putString("userName", nameET.getText().toString());
                editor.putString("passWord", passwordET.getText().toString());
                editor.putString("pagerDutyURL", pagerDutyURL.getText().toString());
                editor.putString("pagerDutyEmail", pagerDutyEmail.getText().toString());
                editor.putString("pagerDutyPass", pagerDutyPass.getText().toString());
                editor.putBoolean("AllowBackgroundService", BackgroundService.isChecked());
                editor.putInt("BackgroundServiceDelay", BackgroundServiceDelay.getProgress());
                
                editor.putBoolean("SeverityCritical", criticalCheckBox.isChecked());
                editor.putBoolean("SeverityError", errorCheckBox.isChecked());
                editor.putBoolean("SeverityWarning", warningCheckBox.isChecked());
                editor.putBoolean("SeverityInfo", infoCheckBox.isChecked());
                editor.putBoolean("SeverityDebug", debugCheckBox.isChecked());
                editor.commit();
                
                mAlarmSender = PendingIntent.getService(Settings.this, 0, new Intent(Settings.this, ZenossPoller.class), 0);
                AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
                
                if(BackgroundService.isChecked())
                {
                	//Stop it first and then start it otherwise it'll never get it's new time
                	am.cancel(mAlarmSender);
                	am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, (long) BackgroundServiceDelay.getProgress(), ((long) BackgroundServiceDelay.getProgress() * 1000), mAlarmSender);
                }
                else
                {
                    am.cancel(mAlarmSender);
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
					handler.sendEmptyMessage(0);
				}
    			
    			//Double check it all worked
    			Boolean Success = false;
    			try
    			{
    				Success = API.CheckLoggedIn();
    			}
    			catch(Exception e)
    			{
    				Success = false;
    			}
    			
            	if(Success)
            	{
            		handler.sendEmptyMessage(1);
            	}
            	else
            	{
            		handler.sendEmptyMessage(0);
            	}
            	return;
    		}
    	};
    	
    }
}
