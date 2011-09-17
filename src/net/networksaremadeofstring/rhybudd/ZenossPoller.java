package net.networksaremadeofstring.rhybudd;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

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
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ZenossPoller extends Service
{
	private NotificationManager mNM;
	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	Thread dataPreload;
	private int EventCount = 0;
	private Handler handler = new Handler();
	private Runnable runnable;
	
	@Override
	public void onCreate() 
	{
		settings = getSharedPreferences("rhybudd", 0);
		try 
        {
			API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
		} 
        catch (Exception e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Toast.makeText(this, "My Service Created", Toast.LENGTH_LONG).show();
		Log.d("Service", "onCreate");
		
		String ns = Context.NOTIFICATION_SERVICE;
		mNM = (NotificationManager) getSystemService(ns);
		
		runnable = new Runnable() { public void run() { CreateThread(); handler.postDelayed(this, 30000); } }; 
		runnable.run();
	}

	@Override
	public void onDestroy() 
	{
		Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show();
		Log.d("Service", "onDestroy");
	}
	
	@Override
	public void onStart(Intent intent, int startid) 
	{
		//Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
		Log.d("Service", "onStart");
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void SendNotification(String EventSummary,int Severity)
	{
		Notification notification = new Notification(R.drawable.stat_sys_warning, "New Zenoss Events!", System.currentTimeMillis());
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
		mNM.notify(1, notification);
	}
	
	private void CreateThread()
    {
		Log.i("Service","Create Thread Called");
		
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
					EventsObject = API.GetEvents();
	    			Events = EventsObject.getJSONObject("result").getJSONArray("events");
				} 
    			catch (Exception e) 
    			{
    				Log.e("API - Stage 1", e.getMessage());
				}
    			
    			
				try 
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
	    					Log.e("API - Stage 2 - Inner", e.getMessage());
	    				}
	    			}
				} 
				catch (JSONException e) 
				{
					Log.e("API - Stage 2", e.getMessage());
				}
    		}
    	};
    	
    	dataPreload.start();
    }

}
