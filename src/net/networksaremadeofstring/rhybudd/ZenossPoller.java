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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.bugsense.trace.BugSenseHandler;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

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
	private Boolean onlyAlertOnProd = true;
	RhybuddDatabase rhybuddCache = null;
	Handler handler;
	Handler eventsHandler;
	int Delay;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	List<String> EventDetails = new ArrayList<String>();
	boolean EventsRefreshInProgress = false;
	Notification stickyNotification;
	
	@Override
	public void onLowMemory()
	{
		//These could be quite large
		EventsObject = null;
		Events = null;
		CriticalList = "";
		
		if(listOfZenossEvents != null)
			listOfZenossEvents.clear();
		
		//Maybe
		//API = null;
	}
	
	@Override
	public void onCreate() 
	{
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		Log.i("ServiceThread","Service Starting");
		
		BugSenseHandler.setup(this, "44a76a8c");
		
		String ns = Context.NOTIFICATION_SERVICE;
		mNM = (NotificationManager) getSystemService(ns);
		
		//TODO Remove?
		/*if(rhybuddCache == null)
		{
			rhybuddCache = new RhybuddDatabase(this);
		}*/

		eventsHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 1)
    			{
    				onlyAlertOnProd = settings.getBoolean("onlyProductionAlerts", true);
    				EventDetails.clear();
    				
    				Boolean alertsEnabled = settings.getBoolean("AllowBackgroundService", true);
    				if(listOfZenossEvents != null && listOfZenossEvents.size() > 0)
    				{
    					EventCount = 0;
    					for(ZenossEvent event : listOfZenossEvents)
    					{
    						if(alertsEnabled && event.isNew() && CheckIfNotify(event.getProdState(), event.getDevice()))
    	    				{
    							EventDetails.add(event.getDevice() + ": " + event.getSummary());
    	    					EventCount++;
    	    				}
    					}
    					
    					if(EventCount > 0)
    					{
    						if(Build.VERSION.SDK_INT >= 16)
    						{
    							SendInboxStyleNotification(EventCount);
    						}
    						else
    						{
    							SendCombinedNotification(EventCount,CriticalList);
    						}
    					}
    				}
    				else
        			{
    					//TODO Possibly warn if null (something went wrong) ignore if 0 (all is good (lucky oncall person!))
        			}
    				
    				if(alertsEnabled)
        				SendStickyNotification();
    			}
    			else if(msg.what == 999)
    			{
    				//TODO All manner of bad happened
    			}
    			EventsRefreshInProgress = false;
    		}
    	};
	}

	@Override
	public void onDestroy() 
	{
		if(mNM != null)
		{
			try
			{
				mNM.cancel(20);
			}
			catch(Exception e)
			{
				BugSenseHandler.log("ZenossPoller", e);
			}
		}
		/*if(rhybuddCache != null)
		{
			try
			{
				rhybuddCache.Close();
			}
			catch(Exception e)
			{
				BugSenseHandler.log("ZenossPoller", e);
			}
		}*/
	}

	@Override
	public void onStart(Intent intent, int startid) 
	{
		Log.d("Service", "onStart");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		//Just in case
		SendStickyNotification();
		
		if(intent != null && intent.getBooleanExtra("events", false))
		{
			Log.i("onStartCommand","Received an intent to check for events");
			CheckForEvents();
		}
		else if(intent != null && intent.getBooleanExtra("refreshCache", false))
		{
			Log.i("onStartCommand","Received an intent to refresh the cache");
			
			RefreshCache();
		}
		else if(intent != null && intent.getBooleanExtra("settingsUpdate", false))
		{
			//Refresh our reference to the settings just in case something changed
			settings = PreferenceManager.getDefaultSharedPreferences(this);
			
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
			try
			{
				am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, (long) 0,  Long.parseLong(settings.getString("BackgroundServiceDelay", "60")) * 1000, Monitoring);
			}
			catch(Exception e)
			{
				BugSenseHandler.log("ZenossPoller", e);
				am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, (long) 0,  60000, Monitoring);
			}
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
	public IBinder onBind(Intent intent) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	private void SendStickyNotification()
	{
		if(stickyNotification == null)
			stickyNotification = new Notification(R.drawable.ic_stat_polling, "Rhybudd is polling for events", System.currentTimeMillis());
		
		stickyNotification.flags |= Notification.FLAG_ONGOING_EVENT;
		stickyNotification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		
		if(Build.VERSION.SDK_INT > 16)
			stickyNotification.flags |= Notification.PRIORITY_LOW;
		
		Context context = getApplicationContext();
		Intent notificationIntent = new Intent(this, RhybuddHome.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("forceRefresh", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Calendar date = Calendar.getInstance();
		String strDate = "";
		if(date.get(Calendar.MINUTE) < 10)
		{
			strDate = Integer.toString(date.get(Calendar.HOUR_OF_DAY)) + ":0" + Integer.toString(date.get(Calendar.MINUTE));
		}
		else
		{
			strDate = date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE);
		}
		stickyNotification.setLatestEventInfo(context, "Rhybudd is actively polling", "Last query time: " + strDate, contentIntent);
		mNM.notify(20, stickyNotification);
	}
	
	private void SendCombinedNotification(int EventCount, String Summary)
	{
		Notification notification = new Notification(R.drawable.ic_stat_alert, Integer.toString(EventCount) + " new Zenoss Events!", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		
		if(settings.getBoolean("notificationSound", true))
		{
			if(settings.getBoolean("notificationSoundInsistent", false))
				notification.flags |= Notification.FLAG_INSISTENT;
			
			if(settings.getString("notificationSoundChoice", "").equals(""))
			{
				notification.defaults |= Notification.DEFAULT_SOUND;
			}
			else
			{
				try
				{
					notification.sound = Uri.parse(settings.getString("notificationSoundChoice", ""));
				}
				catch(Exception e)
				{
					notification.defaults |= Notification.DEFAULT_SOUND;
				}
			}
		}

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
	
	@TargetApi(16)
	private void SendInboxStyleNotification(int EventCount)
	{
		String Event1 = "--", Event2 = "---";
		int remainingCount = 0;
		
		/*Intent notificationIntent = new Intent(this, RhybuddHome.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("forceRefresh", true);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);*/

		if(EventDetails.size() > 1)
		{
			Event1 = EventDetails.get(0);
			Event2 = EventDetails.get(1);
			remainingCount = EventCount - 2;
		}
		else
		{
			Event1 = EventDetails.get(0);
			remainingCount = EventCount - 1;
		}

		Notification notification = new Notification.InboxStyle(
			      new Notification.Builder(this)
			         .setContentTitle(Integer.toString(EventCount) + " New Zenoss Alerts")
			         .setContentText("Click to start Rhybudd")
			         .setSmallIcon(R.drawable.ic_stat_alert)
			         .setVibrate(new long[] {0,100,200,300})
			         .setAutoCancel(true)
					 .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, RhybuddHome.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("forceRefresh", true), 0)))
			      .addLine(Event1)
			      .addLine(Event2)
			      .setBigContentTitle(Integer.toString(EventCount) + " New Zenoss Alerts")
			      .setSummaryText("+"+Integer.toString(remainingCount)+" more")
			      .build();

		if(settings.getBoolean("notificationSound", true))
		{
			if(settings.getBoolean("notificationSoundInsistent", false))
				notification.flags |= Notification.FLAG_INSISTENT;
			
			if(settings.getString("notificationSoundChoice", "").equals(""))
			{
				notification.defaults |= Notification.DEFAULT_SOUND;
			}
			else
			{
				try
				{
					notification.sound = Uri.parse(settings.getString("notificationSoundChoice", ""));
				}
				catch(Exception e)
				{
					notification.defaults |= Notification.DEFAULT_SOUND;
				}
			}
		}
		
		mNM.notify(43523, notification);
			 
	}
	
	/*private void SendNotification(String EventSummary,int Severity)
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
	}*/

	private void RefreshCache()
	{
		((Thread) new Thread(){
			public void run()
			{
				try 
				{
					if(API == null)
					{
						if(settings.getBoolean("httpBasicAuth", false))
						{
							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
						}
						else
						{
							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
						}
					}
					
					List<ZenossDevice> listOfZenossDevices = API.GetRhybuddDevices();
					
					if(listOfZenossDevices != null && listOfZenossDevices.size() > 0)
					{
						rhybuddCache = new RhybuddDatabase(ZenossPoller.this);
						rhybuddCache.UpdateRhybuddDevices(listOfZenossDevices);
					}
					else
					{
						//TODO Send Warning
						/*Message msg = new Message();
    					Bundle bundle = new Bundle();
    					bundle.putString("exception","A query to both the local DB and Zenoss API returned no devices");
    					msg.setData(bundle);
    					msg.what = 0;
    					handler.sendMessage(msg);*/
					}
					
				} 
				catch (Exception e) 
				{
					//TODO Send Warning
					BugSenseHandler.log("DeviceList", e);
					/*Message msg = new Message();
					Bundle bundle = new Bundle();
					bundle.putString("exception",e.getMessage());
					msg.setData(bundle);
					msg.what = 0;
					handler.sendMessage(msg);*/
				}
			}
		}).start();
	}
	
	private void CheckForEvents()
	{
		if(EventsRefreshInProgress)
		{
			Log.i("CheckForEvents","Lock flag in place, skipping this iteration");
			return;
		}
		//Flag must have been false let's set it!
		EventsRefreshInProgress = true;
		
		EventCount = 0;
		((Thread) new Thread(){
			public void run()
			{
				if(listOfZenossEvents != null)
					listOfZenossEvents.clear();
				
				try
				{
    				if(API == null)
    				{
    					if(settings.getBoolean("httpBasicAuth", false))
						{
							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
						}
						else
						{
							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
						}
    				}
				}
				catch(Exception e)
				{
					API = null;
					e.printStackTrace();
				}
    				
				try 
				{
					if(API != null)
					{
						listOfZenossEvents = API.GetRhybuddEvents(settings.getBoolean("SeverityCritical", true),
								settings.getBoolean("SeverityError", true),
								settings.getBoolean("SeverityWarning", true),
								settings.getBoolean("SeverityInfo", false),
								settings.getBoolean("SeverityDebug", false),
								settings.getBoolean("onlyProductionEvents", true));
						
						if(listOfZenossEvents!= null && listOfZenossEvents.size() > 0)
						{
							eventsHandler.sendEmptyMessage(1);
							
							if(rhybuddCache == null)
							{
								//XXX Main thread safe?
								rhybuddCache = new RhybuddDatabase(ZenossPoller.this);
							}
							rhybuddCache.UpdateRhybuddEvents(listOfZenossEvents);
							//rhybuddCache.Close();
						}
					}
					else
					{
						//TODO API Problems
					}
				} 
				catch (ClientProtocolException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				catch (JSONException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch(Exception e)
				{
					
				}
				finally
				{
					EventsRefreshInProgress = false;
				}
			}
		}).start();
	}

	private Boolean CheckIfNotify(String prodState, String UID)
	{
		//We always return true if the device is production as specified by a 4.1 event JSON prodState
		if(prodState != null && prodState.toLowerCase().equals("production"))
		{
			return true;
		}
		
		ZenossDevice thisDevice = rhybuddCache.getDevice(UID);
		if(thisDevice != null)
		{
			if(thisDevice.getproductionState().equals("Production"))
			{
				return true;
			}
			else
			{
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
