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


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bugsense.trace.BugSenseHandler;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ZenossPoller extends Service
{
	private NotificationManager mNM;
	ZenossAPIv2 API = null;
	SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	Thread dataPreload;
	int EventCount = 0;
	String CriticalList = "";
	private int NotificationID = 0;
	private int failureCount = 0;
	private Boolean onlyAlertOnProd = true;
	RhybuddDatabase rhybuddCache = null;
	Handler handler;
	Cursor dbResults;
	int Delay;
	@Override
	public void onCreate() 
	{
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		Log.i("ServiceThread","Service Starting");
		BugSenseHandler.setup(this, "44a76a8c");

		onlyAlertOnProd = settings.getBoolean("onlyProductionAlerts", true);
		String ns = Context.NOTIFICATION_SERVICE;
		mNM = (NotificationManager) getSystemService(ns);
		if(rhybuddCache == null)
		{
			rhybuddCache = new RhybuddDatabase(this);
		}

		handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 1)
    			{
    				if(rhybuddCache.hasCacheRefreshed())
    				{
    					//Log.i("handler","Cache has refreshed, sending delayed message");
    					this.sendEmptyMessageDelayed(2,1000);
    				}
    				else
    				{
    					//Log.i("handler","Cache hasn't refreshed, sending delayed message");
        				handler.sendEmptyMessageDelayed(1, 1000);
    				}
    			}
    			else if(msg.what == 2)
    			{
    				//Log.i("handler","2 - Preparing to get events");
    				
    				dbResults = rhybuddCache.getEvents();
        			Boolean alertsEnabled = settings.getBoolean("AllowBackgroundService", true);
        			if(dbResults != null)
        			{
    	    			while(dbResults.moveToNext())
    	    			{
    	    				ZenossEvent CurrentEvent = new ZenossEvent(dbResults.getString(0),
									   dbResults.getString(3),
									   dbResults.getString(4), 
									   dbResults.getString(5),
									   dbResults.getString(7),
									   dbResults.getString(8));
    	    				
    	    				if(alertsEnabled && CurrentEvent.isNew() && CheckIfNotify(CurrentEvent.getProdState(), CurrentEvent.getDevice()))
    	    				{
    	    					EventCount++;
								//SendNotification(CurrentEvent.getSummary(),Integer.parseInt(CurrentEvent.getSeverity()));
    	    				}
    	    			}
    	    			
    	    			if(EventCount > 0)
    	    				SendCombinedNotification(EventCount,CriticalList);
        			}
        			if(alertsEnabled)
	    				SendStickyNotification();
    			}
    			else
    			{
    				//Toast.makeText(RhybuddHome.this, "Timed out communicating with host. Please check protocol, hostname and port.", Toast.LENGTH_LONG).show();
    			}
    		}
    	};
    	
		//CheckForEvents(); 
	}

	@Override
	public void onDestroy() 
	{
		rhybuddCache.Close();
	}

	@Override
	public void onStart(Intent intent, int startid) 
	{
		Log.d("Service", "onStart");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		if(intent != null && intent.getBooleanExtra("events", false))
		{
			Log.i("onStartCommand","Received an intent to check for events");
			CheckForEvents();
		}
		else if(intent != null && intent.getBooleanExtra("refreshCache", false))
		{
			Log.i("onStartCommand","Received an intent to refresh the cache");
			rhybuddCache.RefreshCache();
		}
		else if(intent != null && intent.getBooleanExtra("settingsUpdate", false))
		{
			Log.i("onStartCommand","Received an intent from the settings Activity");
			PollerCheck();
		}
		else
		{
			Log.i("onStartCommand","I got started for no particular reason. I should probably do a refresh");
			PollerCheck();
		}

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}


	private void PollerCheck()
	{
		AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
		Intent Poller = new Intent(this, ZenossPoller.class);

		if(settings.getBoolean("AllowBackgroundService", true))
		{
			Log.i("PollerCheck","Background scanning enabled!");
			SendStickyNotification();
			Poller.putExtra("events", true);
			PendingIntent Monitoring = PendingIntent.getService(this, 0, Poller, PendingIntent.FLAG_UPDATE_CURRENT);//PendingIntent.FLAG_UPDATE_CURRENT
			am.cancel(Monitoring);
			am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, (long) 0,  Long.parseLong(settings.getString("BackgroundServiceDelay", "60")) * 1000, Monitoring);
		}
		else
		{
			Log.i("PollerCheck","Background scanning disabled!");
			Poller.putExtra("events", true);
			PendingIntent Monitoring = PendingIntent.getService(this, 0, Poller, PendingIntent.FLAG_UPDATE_CURRENT);//PendingIntent.FLAG_UPDATE_CURRENT
			am.cancel(Monitoring);
			mNM.cancel(20);
		}

		
		if(settings.getBoolean("refreshCache", true))
		{
			Log.i("PollerCheck","Background cache refresh enabled!");
			
			Poller.putExtra("refreshCache", true);
			PendingIntent CacheRefresh = PendingIntent.getService(this, 1, Poller, PendingIntent.FLAG_UPDATE_CURRENT);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 10000, AlarmManager.INTERVAL_HOUR, CacheRefresh);
		}
		else
		{
			Log.i("PollerCheck","Background cache refresh disabled!");
			Poller.putExtra("refreshCache", true);
			PendingIntent CacheRefresh = PendingIntent.getService(this, 1, Poller, PendingIntent.FLAG_UPDATE_CURRENT);
			am.cancel(CacheRefresh);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private void SendStickyNotification()
	{
		Notification notification = new Notification(R.drawable.ic_stat_polling, "Rhybudd is polling for events", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, RhybuddHome.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("forceRefresh", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Calendar date = Calendar.getInstance();
		notification.setLatestEventInfo(context, "Rhybudd is actively polling", "Last query time was " + date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE), contentIntent);
		mNM.notify(20, notification);
	}
	
	private void SendCombinedNotification(int EventCount, String Summary)
	{
		Notification notification = new Notification(R.drawable.ic_stat_alert, Integer.toString(EventCount) + " new Zenoss Events!", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.flags |= Notification.FLAG_INSISTENT;
		
		if(settings.getBoolean("notificationSound", true))
			notification.defaults |= Notification.DEFAULT_SOUND;

		notification.ledARGB = 0xffff0000;

		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;

		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, RhybuddHome.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("forceRefresh", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, Integer.toString(EventCount) + " Zenoss Events", "Click to launch Rhybudd", contentIntent);
		mNM.notify(43523, notification);//NotificationID++ 
	}
	
	private void SendNotification(String EventSummary,int Severity)
	{
		Notification notification = new Notification(R.drawable.ic_stat_alert, "New Zenoss Events!", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.flags |= Notification.FLAG_INSISTENT;
		
		if(settings.getBoolean("notificationSound", true))
			notification.defaults |= Notification.DEFAULT_SOUND;

		if(Severity == 5)
			notification.ledARGB = 0xffff0000;
		if(Severity == 4)
			notification.ledARGB = 0xffFF9933;
		if(Severity == 3)
			notification.ledARGB = 0xffFFFF00;
		if(Severity > 3)
			notification.ledARGB = 0xff6699FF;

		notification.ledOnMS = 300;
		notification.ledOffMS = 1000;

		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, RhybuddHome.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("forceRefresh", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, "Rhybudd Notification", EventSummary, contentIntent);
		mNM.notify(43523, notification);//NotificationID++ 
	}

	private void CheckForEvents()
	{
		EventCount = 0;
		CriticalList = "";
		rhybuddCache.RefreshEvents();
		handler.sendEmptyMessageDelayed(1, 1000);
		/*dataPreload = new Thread() 
		{  
			public void run() 
			{
				Log.i("ServiceThread","Thread Started");
				onlyAlertOnProd = settings.getBoolean("onlyProductionAlerts", false);

				try 
				{
					if(API == null)
						API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));

					EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true),
							settings.getBoolean("SeverityError", true),
							settings.getBoolean("SeverityWarning", true),
							settings.getBoolean("SeverityInfo", false),
							settings.getBoolean("SeverityDebug", false),
							settings.getBoolean("onlyProductionAlerts", true));

					Events = EventsObject.getJSONObject("result").getJSONArray("events");
					EventCount = EventsObject.getJSONObject("result").getInt("totalCount");
					failureCount = 0;
					
					//Log.i("Events",Events.toString(3));
				} 
				catch (Exception e) 
				{
					Log.i("ServiceThread","Exception encountered; " + e.getMessage());
					failureCount++;

					if(failureCount > 10)
					{
						SendNotification("Background poller couldn't connect. Stopping. \r\nLaunch Rhybudd to restart poller.",5);
						stopSelf();
					}
				}
				Log.i("ServiceThread","API Call finsihed");
				
				if(Events != null)
				{
					for(int i = 0; i < EventCount; i++)
					{
						JSONObject CurrentEvent = null;
						String ProdState = null;
						ContentValues values = new ContentValues(2);
						try 
						{
							CurrentEvent = Events.getJSONObject(i);

							try
							{
								ProdState = CurrentEvent.getString("prodState");
							}
							catch(Exception e)
							{
								e.printStackTrace();
								ProdState = null;
							}

							if(CurrentEvent.getString("eventState").equals("New") && CheckIfNotify(ProdState, CurrentEvent.getJSONObject("device").getString("uid")))
								SendNotification(CurrentEvent.getString("summary"),Integer.parseInt(CurrentEvent.getString("severity")));

						}
						catch (JSONException j) 
						{
							Log.i("ServiceThread","Exception encountered; " + j.getMessage());
							BugSenseHandler.log("PollerEventsLoop", j);
						}
						catch (Exception e)
						{
							Log.i("ServiceThread","Exception encountered; " + e.getMessage());
							BugSenseHandler.log("PollerEventsLoop", e);
						}
					}
				}
				else
				{
					Log.e("Service", "EventsObject was null");
				}

				//At this point it might be a good idea to set stuff we don't need
				//anymore to null so GC can collect it
				Events = null;
				//ZenossPoller.this.stopSelf();

				Log.i("ServiceThread","stopping");
			}
		};
		dataPreload.start();*/
	}

	private Boolean CheckIfNotify(String prodState, String UID)
	{
		//We always return true if the device is production as specified by a 4.1 event JSON prodState
		if(prodState != null && prodState.equals("Production"))
		{
			return true;
		}
		
		Cursor dbResults = rhybuddCache.getDevice(UID);
		if(dbResults.moveToFirst())
		{
			String dbProdState = dbResults.getString(1);
			if(dbProdState.equals("Production"))
			{
				dbResults.close();
				return true;
			}
			else
			{
				dbResults.close();
				if(onlyAlertOnProd)
				{
					return false;
				}
				else
				{
					return true;
				}
			}
		}
		else
		{
			dbResults.close();
			if(onlyAlertOnProd)
			{
				return false;
			}
			else
			{
				return true;
			}
		}

	}
}
