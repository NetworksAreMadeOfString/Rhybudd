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


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class ZenossPoller extends Service
{
	private NotificationManager mNM;
	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	//List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	Thread dataPreload;
	private int EventCount = 0;
	private Handler handler = new Handler();
	private Runnable runnable;
	private int NotificationID = 0;
	private int failureCount = 0;
	
	@Override
	public void onCreate() 
	{
		settings = getSharedPreferences("rhybudd", 0);

		//Log.d("Service", "onCreate");
		
		String ns = Context.NOTIFICATION_SERVICE;
		mNM = (NotificationManager) getSystemService(ns);
		
		runnable = new Runnable() 
		{ 
			public void run() 
			{ 
				final int Delay = settings.getInt("BackgroundServiceDelay", 30); 
				//Log.i("Delay", Integer.toString(Delay)); 
				CreateThread(); 
				//handler.postDelayed(this, Delay * 1000); 
			} 
		}; 
		runnable.run();
	}

	@Override
	public void onDestroy() 
	{
		//Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
		//Log.d("Service", "onDestroy");
		handler.removeCallbacks(runnable);
	}
	
	@Override
	public void onStart(Intent intent, int startid) 
	{
		//Log.d("Service", "onStart");
		//SendNotification("Zenoss Poller Background task started.",5);
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void SendNotification(String EventSummary,int Severity)
	{
		Notification notification = new Notification(R.drawable.stat_sys_warning, "New Zenoss Events!", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.defaults |= Notification.DEFAULT_VIBRATE;
		//notification.defaults |= Notification.DEFAULT_LIGHTS;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
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
		Intent notificationIntent = new Intent(this, launcher.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, "Rhybudd Notification", EventSummary, contentIntent);
		mNM.notify(NotificationID++, notification);
	}
	
	private void CreateThread()
    {
		//Log.i("Service","Create Thread Called");
		
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
    				if(API == null)
    					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				
					EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true),
							settings.getBoolean("SeverityError", true),
							settings.getBoolean("SeverityWarning", true),
							settings.getBoolean("SeverityInfo", false),
							settings.getBoolean("SeverityDebug", false));
					
	    			Events = EventsObject.getJSONObject("result").getJSONArray("events");
	    			failureCount = 0;
				} 
    			catch (Exception e) 
    			{
    				failureCount++;
    				//Log.e("Service", "Failure Count: " + Integer.toString(failureCount));
    				
    				if(failureCount > 10)
    				{
    					SendNotification("Background poller couldn't connect. Stopping. \r\nLaunch Rhybudd to restart poller.",5);
    					stopSelf();
    				}
				}
    			
    			
				try 
				{
					if(EventsObject != null)
					{
						EventCount = EventsObject.getJSONObject("result").getInt("totalCount");
						for(int i = 0; i < EventCount; i++)
		    			{
		    				JSONObject CurrentEvent = null;
		    				try 
		    				{
		    					CurrentEvent = Events.getJSONObject(i);
		    					
		    					if(CurrentEvent.getString("eventState").equals("New"))
		    						SendNotification(CurrentEvent.getString("summary"),Integer.parseInt(CurrentEvent.getString("severity")));
		    				}
		    				catch (JSONException e) 
		    				{
		    					//Log.e("API - Stage 2 - Inner", e.getMessage());
		    					//SendNotification("Background service failed",5);
		    					//stopSelf();
		    					//failureCount++;
		    				}
		    			}
					}
					else
					{
						//Might consider this a full failure?
						//failureCount++;
					}
				} 
				catch (JSONException e) 
				{
					//SendNotification("Background service failed",5);
					//stopSelf();
					failureCount++;
				}
				
				//At this point it might be a good idea to set stuff we don't need
				//anymore to null so GC can collect it
				
				ZenossPoller.this.stopSelf();
    		}
    	};
    	
    	dataPreload.start();
    }

}
