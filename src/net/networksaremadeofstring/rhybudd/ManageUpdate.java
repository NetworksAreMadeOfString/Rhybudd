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

import com.bugsense.trace.BugSenseHandler;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ManageUpdate extends BroadcastReceiver
{
	@Override
	public void onReceive(Context arg0, Intent intent) 
	{
		//BugSenseHandler.setup(arg0, "44a76a8c");
		try
		{
			if (intent.getDataString().contains("net.networksaremadeofstring.rhybudd"))
			{
				//Log.i("ManangeUpdateBroadcastReceiver","Received Notification of app updated");
				   
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(arg0);
				SharedPreferences oldSettings = arg0.getSharedPreferences("rhybudd", 0);
				
				//Log.i("ManangeUpdateBroadcastReceiver","Preparing to check settings");
				
				if(settings.getString("URL","").equals("") && !oldSettings.getString("URL","").equals(""))
				{
					//Log.i("ManangeUpdateBroadcastReceiver","Detected the lack of new OR old preferences");
					
					try
					{
						//Log.i("ManangeUpdateBroadcastReceiver","Deleting the old AlarmManager");
						PendingIntent mAlarmSender = PendingIntent.getService(arg0, 0, new Intent(arg0, ZenossPoller.class), 0);
				        AlarmManager am = (AlarmManager) arg0.getSystemService(Activity.ALARM_SERVICE);
				        am.cancel(mAlarmSender);
					}
					catch(Exception e)
					{
						//Log.e("ManangeUpdateBroadcastReceiver","Failed to kill the alarm manager");
						e.printStackTrace();
						//BugSenseHandler.log("UpdateReceiver-mAlarmSender", e);
					}
					
					
					
					SharedPreferences.Editor editor = settings.edit();
		            editor.putString("URL", oldSettings.getString("URL",""));
		            editor.putString("userName", oldSettings.getString("userName",""));
		            editor.putString("passWord", oldSettings.getString("passWord",""));
		            editor.putBoolean("AllowBackgroundService", oldSettings.getBoolean("AllowBackgroundService", true));
		            if(settings.getBoolean("AllowBackgroundService", false))
		            {
		            	editor.putInt("BackgroundServiceDelay", 30);
		            }
		            
		            editor.putBoolean("SeverityCritical", oldSettings.getBoolean("SeverityCritical", true));
		            editor.putBoolean("SeverityError", oldSettings.getBoolean("SeverityError", true));
		            editor.putBoolean("SeverityWarning", oldSettings.getBoolean("SeverityWarning", true));
		            editor.putBoolean("SeverityInfo", oldSettings.getBoolean("SeverityInfo", true));
		            editor.putBoolean("SeverityDebug", oldSettings.getBoolean("SeverityDebug", true));
		            editor.putBoolean("onlyProductionAlerts", oldSettings.getBoolean("onlyProductionAlerts", true));
		            editor.putBoolean("notificationSound", oldSettings.getBoolean("notificationSound", true));
		            editor.commit();
		            
		            //TODO In the next version we'll release this to clear out space
		            SharedPreferences.Editor oldSettingsFlush = oldSettings.edit();
		            oldSettingsFlush.clear();
		            oldSettingsFlush.commit();
					
				}
				else
				{
					//Log.i("ManangeUpdateBroadcastReceiver","We found some settings:\r\n" + settings.getString("URL",""));
				}
			}
			else
			{
				//Log.i("ManangeUpdateBroadcastReceiver","Received Notification of an app updated but it wasn't ours");
			}
		}
		catch(Exception e)
		{
			//BugSenseHandler.log("UpdateReceiver", e);
		}
	}
}
