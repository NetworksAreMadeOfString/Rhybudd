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
import android.util.Log;

public class ManageUpdate extends BroadcastReceiver
{
	@Override
	public void onReceive(Context arg0, Intent intent) 
	{
		
		BugSenseHandler.setup(arg0, "44a76a8c");
		try
		{
			if (intent.getDataString().contains("net.networksaremadeofstring.rhybudd"))
			{
				Log.i("ManangeUpdateBroadcastReceiver","Received Notification of app updated");
				
				PendingIntent mAlarmSender = PendingIntent.getService(arg0, 0, new Intent(arg0, ZenossPoller.class), 0);
		        AlarmManager am = (AlarmManager) arg0.getSystemService(Activity.ALARM_SERVICE);
		        am.cancel(mAlarmSender);
		            
		            
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(arg0);
				SharedPreferences oldSettings = arg0.getSharedPreferences("rhybudd", 0);
				if(settings.getString("URL","").equals("") && !settings.getString("URL","").equals(""))
				{
					Log.i("Preferences","Detected the lack of new preferences");
					
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
			}
			else
			{
				Log.i("ManangeUpdateBroadcastReceiver","Received Notification of an app updated but it wasn't ours");
			}
		}
		catch(Exception e)
		{
			BugSenseHandler.log("UpdateReceiver", e);
		}
	}
}
