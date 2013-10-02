/*
 * Copyright (C) 2013 - Gareth Llewellyn
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
import java.util.List;
import java.util.Random;
import android.os.*;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.bugsense.trace.BugSenseHandler;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ZenossPoller extends Service
{
	private NotificationManager mNM;
	public ZenossAPI API = null;
	SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	int EventCount = 0;
	String CriticalList = "";
	Handler handler;
	Handler eventsHandler;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	List<String> EventDetails = new ArrayList<String>();
	boolean EventsRefreshInProgress = false;
    boolean loginSuccessful = false;
    public boolean loginInProgress = false;

    private final IBinder mBinder = new LocalBinder();
    // Random number generator
    private final Random mGenerator = new Random();


    public class LocalBinder extends Binder
    {
        ZenossPoller getService()
        {
            return ZenossPoller.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    public int getRandomNumber()
    {
        return mGenerator.nextInt(100);
    }



	@Override
	public void onLowMemory()
	{
		//These could be quite large
		EventsObject = null;
		Events = null;
		CriticalList = "";
		
		if(listOfZenossEvents != null)
			listOfZenossEvents.clear();
	}

    public void PrepAPI(Boolean Login, Boolean inThread)
    {
            //There are some minor differences here
            if(settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
            {
                //Log.e("PrepAPI","Am ZAAS");
                API = new ZenossAPIZaas();
            }
            else
            {
                //Log.e("PrepAPI","Am not ZAAS");
                API = new ZenossAPICore();
            }


        if(Login)
        {
            if(inThread)
            {
                ZenossCredentials credentials = new ZenossCredentials(ZenossPoller.this);
                try
                {
                    loginSuccessful = API.Login(credentials);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("ZenossPoller","PrepAPI",e);
                    loginSuccessful = false;
                }
            }
            else
            {
                (new Thread()
                {
                    public void run()
                    {
                        ZenossCredentials credentials = new ZenossCredentials(ZenossPoller.this);

                        if(!credentials.UserName.equals("") && !credentials.URL.equals(""))
                        {
                            try
                            {
                                loginSuccessful = API.Login(credentials);
                            }
                            catch(Exception e)
                            {
                                BugSenseHandler.sendExceptionMessage("ZenossPoller","PrepAPI",e);
                                e.printStackTrace();
                                loginSuccessful = false;
                            }
                        }
                        else
                        {
                            Log.e("ZenossPoller", "No credentials yet");
                        }
                    }
                }).start();
            }
        }
    }

	@Override
	public void onCreate() 
	{
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		//Log.i("ServiceThread","Service Starting");
	
        BugSenseHandler.initAndStartSession(ZenossPoller.this, "44a76a8c");

		String ns = Context.NOTIFICATION_SERVICE;
		mNM = (NotificationManager) getSystemService(ns);

        PrepAPI(true,false);

		eventsHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 1)
    			{
    				//onlyAlertOnProd = settings.getBoolean("onlyProductionAlerts", true);
    				EventDetails.clear();
    				
    				Boolean alertsEnabled = settings.getBoolean("AllowBackgroundService", true);
    				if(  null != listOfZenossEvents && listOfZenossEvents.size() > 0)
    				{
                        try
                        {
                            EventCount = 0;
                            for(ZenossEvent event : listOfZenossEvents)
                            {
                                if(alertsEnabled && event.isNew() && ZenossAPI.CheckIfNotify(event.getProdState(), event.getDevice(),getApplicationContext(),settings.getBoolean("onlyProductionAlerts", true)))
                                {
                                    EventDetails.add(event.getDevice() + ": " + event.getSummary());
                                    EventCount++;
                                }
                            }

                            if(EventCount > 0)
                            {
                                Notifications.SendPollNotification(EventCount,EventDetails,getApplicationContext());
                            }
                        }
                        catch(Exception e)
                        {
                            BugSenseHandler.sendExceptionMessage("ZenossPoller","eventsHandler",e);
                            e.printStackTrace();
                        }
    				}
    				else
        			{
    					//TODO Possibly warn if null (something went wrong) ignore if 0 (all is good (lucky oncall person!))
        			}
    				
    				if(alertsEnabled)
                    {
                        Notifications.SendStickyNotification(getApplicationContext());
                    }
                    else
                    {
                        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_POLLED_STICKY);
                    }
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
        try
        {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_POLLED_STICKY);
        }
        catch(Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ZenossPoller","onDestroy",e);
        }

        //Log.e("ZenossPoller","I GOT DESTROYED WTF?!?!?!?!??!?!?!");
	}

	/*@Override
	public void onStart(Intent intent, int startid) 
	{
		Log.d("Service", "onStart");
	}*/

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		if(intent != null && intent.getBooleanExtra("events", false))
		{
			//Log.i("onStartCommand","Received an intent to check for events");
			CheckForEvents();
		}
		/*else if(intent != null && intent.getBooleanExtra("refreshCache", false))
		{
			//Log.i("onStartCommand","Received an intent to refresh the cache");
			RefreshCache();
		}*/
		else if(intent != null && intent.getBooleanExtra("settingsUpdate", false))
		{
			//Refresh our reference to the settings just in case something changed
			settings = PreferenceManager.getDefaultSharedPreferences(this);
			//Log.i("onStartCommand","Received an intent from the settings Activity");
			PollerCheck();
		}
		else
		{
			//Log.i("onStartCommand","I got started for no particular reason. I should probably do a refresh (but I'm not going too");
			//PollerCheck();
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
			//Log.i("PollerCheck","Background scanning enabled!");
			Notifications.SendStickyNotification(this);

			Poller.putExtra("events", true);
			PendingIntent Monitoring = PendingIntent.getService(this, 0, Poller, PendingIntent.FLAG_UPDATE_CURRENT);//PendingIntent.FLAG_UPDATE_CURRENT
			am.cancel(Monitoring);
			try
			{
				am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, (long) 0,  Long.parseLong(settings.getString("BackgroundServiceDelay", "60")) * 1000, Monitoring);
			}
			catch(Exception e)
			{
				am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, (long) 0,  60000, Monitoring);
                BugSenseHandler.sendExceptionMessage("ZenossPoller","PollerCheck",e);
			}
		}
		else
		{
			//Log.i("PollerCheck","Background scanning disabled!");
			Poller.putExtra("events", true);
			PendingIntent Monitoring = PendingIntent.getService(this, 0, Poller, PendingIntent.FLAG_UPDATE_CURRENT);//PendingIntent.FLAG_UPDATE_CURRENT
			am.cancel(Monitoring);
			mNM.cancel(Notifications.NOTIFICATION_POLLED_STICKY);
		}

		
		/*if(settings.getBoolean("refreshCache", true))
		{
			//Log.i("PollerCheck","Background cache refresh enabled!");
			
			Poller.putExtra("refreshCache", true);
			PendingIntent CacheRefresh = PendingIntent.getService(this, 1, Poller, PendingIntent.FLAG_UPDATE_CURRENT);
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 10000, AlarmManager.INTERVAL_HOUR, CacheRefresh);
		}
		else
		{
			//Log.i("PollerCheck","Background cache refresh disabled!");
			Poller.putExtra("refreshCache", true);
			PendingIntent CacheRefresh = PendingIntent.getService(this, 1, Poller, PendingIntent.FLAG_UPDATE_CURRENT);
			am.cancel(CacheRefresh);
		}*/

        //We use a SyncAdapter now like good citizens
        try
        {
            if(settings.getBoolean("refreshCache", true))
            {
                Poller.putExtra("refreshCache", true);
                PendingIntent CacheRefresh = PendingIntent.getService(this, 1, Poller, PendingIntent.FLAG_UPDATE_CURRENT);
                am.cancel(CacheRefresh);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("refreshCache", false);
                editor.commit();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ZenossPoller","cancel refresh cache poller",e);
        }
	}

	/*private void RefreshCache()
	{
		(new Thread(){
			public void run()
			{
				try 
				{

                    ZenossCredentials credentials = new ZenossCredentials(ZenossPoller.this);
                    loginSuccessful = API.Login(credentials);

					List<ZenossDevice> listOfZenossDevices = API.GetRhybuddDevices();
					
					if(listOfZenossDevices != null && listOfZenossDevices.size() > 0)
					{
                        RhybuddDataSource datasource = new RhybuddDataSource(ZenossPoller.this);
                        datasource.open();
                        datasource.UpdateRhybuddDevices(listOfZenossDevices);
                        datasource.close();

                        ZenossAPI.updateLastChecked(ZenossPoller.this);
					}
					else
					{
					}
					
				} 
				catch (Exception e) 
				{
                    BugSenseHandler.sendExceptionMessage("ZenossPoller","onDestroy",e);
				}
			}
		}).start();
	}*/
	
	private void CheckForEvents()
	{
		if(EventsRefreshInProgress)
		{
			//Log.i("CheckForEvents","Lock flag in place, skipping this iteration");
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
                        PrepAPI(true,true);
                    }

                    ZenossCredentials credentials = new ZenossCredentials(ZenossPoller.this);
                    loginSuccessful = API.Login(credentials);

                    listOfZenossEvents = API.GetRhybuddEvents(ZenossPoller.this);

                    if(listOfZenossEvents!= null && listOfZenossEvents.size() > 0)
                    {
                        eventsHandler.sendEmptyMessage(1);

                        RhybuddDataSource datasource = new RhybuddDataSource(ZenossPoller.this);
                        datasource.open();
                        datasource.UpdateRhybuddEvents(listOfZenossEvents);
                        datasource.close();
                    }
				} 
				catch (ClientProtocolException e) 
				{
					e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("ZenossPoller","CheckForEvents",e);
				} 
				catch (JSONException e) 
				{
					e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("ZenossPoller","CheckForEvents",e);
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("ZenossPoller","CheckForEvents",e);
				}
				catch(Exception e)
				{
					e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("ZenossPoller","CheckForEvents",e);
				}
				finally
				{
					EventsRefreshInProgress = false;
				}
			}
		}).start();
	}
}
