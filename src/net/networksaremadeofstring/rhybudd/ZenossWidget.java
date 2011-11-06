package net.networksaremadeofstring.rhybudd;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ListView;
import android.widget.RemoteViews;

public class ZenossWidget extends AppWidgetProvider 
{

	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	private int EventCount = 0;
	Thread dataPreload;
	volatile Handler handler = null;
	private int CritCount = 0, ErrCount = 0, WarnCount = 0;
	
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) 
    {
        final int N = appWidgetIds.length;
        
        if(settings == null)
			settings = context.getSharedPreferences("rhybudd", 0);
        
        handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			//Log.i("Handler","Msg recieved");

				RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.zenoss_widget);
				Intent intent = new Intent(context, rhestr.class);
	            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
	            
	            
				for (int i=0; i<N; i++) 
		        {
					int appWidgetId = appWidgetIds[i];
					views.setTextViewText(R.id.CriticalCount, Integer.toString(CritCount));
					views.setTextViewText(R.id.WarningCount, Integer.toString(WarnCount));
					views.setTextViewText(R.id.ErrorCount, Integer.toString(ErrCount));
					
					views.setOnClickPendingIntent(R.id.linearLayout1, pendingIntent);
					views.setOnClickPendingIntent(R.id.CriticalCount, pendingIntent);
					views.setOnClickPendingIntent(R.id.WarningCount, pendingIntent);
					views.setOnClickPendingIntent(R.id.ErrorCount, pendingIntent);
					
					
					appWidgetManager.updateAppWidget(appWidgetId, views);
					//Log.i("handler","Told the widget to update");
		        }
    		}
    	};
    	
    	CreateThread();
    	dataPreload.start();
    }
    
    private void CreateThread()
    {
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			//Log.i("Widget","Started!");
    			CritCount = 0;
    			WarnCount = 0;
    			ErrCount = 0;
    			
    			try 
    			{
    				if(API == null)
    				{
    					//Log.i("Widget",settings.getString("userName", "No User Name"));
    					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				}
    				
    				EventsObject = API.GetEvents();
    				
	    			Events = EventsObject.getJSONObject("result").getJSONArray("events");
				} 
    			catch (Exception e) 
    			{
    				handler.sendEmptyMessage(0);
    				//e.printStackTrace();
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
			    				
			    				if(CurrentEvent.getString("severity").equals("5"))
			    					CritCount++;
			    				
			    				if(CurrentEvent.getString("severity").equals("4"))
			    					ErrCount++;
			    				
			    				if(CurrentEvent.getString("severity").equals("3"))
			    					WarnCount++;
		    				}
		    				catch (JSONException e) 
		    				{
		    					//e.printStackTrace();
		    				}
		    			}
						
						handler.sendEmptyMessage(0);
					}
					else
					{
	    				handler.sendEmptyMessage(0);
					}
				} 
				catch (JSONException e) 
				{
    				handler.sendEmptyMessage(0);
    				//e.printStackTrace();
				}
				
				//Help out the garbage collector
				EventsObject = null;
				Events = null;
				API = null;
				dataPreload = null;
				//Log.i("Widget-Thread", Integer.toString(CritCount) + " / " + Integer.toString(ErrCount) + " / " +  Integer.toString(WarnCount));
    		}
    	};
    }
}