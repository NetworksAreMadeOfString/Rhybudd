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
import java.util.List;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class ZenossWidget extends AppWidgetProvider 
{

	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	/*JSONObject EventsObject = null;
	JSONArray Events = null;*/
	private int EventCount = 0;
	Thread dataPreload;
	volatile Handler handler = null;
	private int CritCount = 0, ErrCount = 0, WarnCount = 0;
	List<ZenossEvent> tempZenossEvents = new ArrayList<ZenossEvent>();
	RhybuddDatabase rhybuddCache;
	
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) 
    {
        final int N = appWidgetIds.length;
        
        if(settings == null)
			settings = PreferenceManager.getDefaultSharedPreferences(context);
        
        try
        {
        	rhybuddCache = new RhybuddDatabase(context);
        }
        catch(Exception e)
        {
        	//Bugsense
        }
        
        handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
				RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.zenoss_widget);
				Intent intent = new Intent(context, RhybuddHome.class);
				intent.putExtra("forceRefresh", true);
	            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	            
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
    	
    	Refresh();
    }
    
    private void Refresh()
    {
    	CritCount = 0;
    	ErrCount = 0;
    	WarnCount = 0;
    	
    	((Thread) new Thread() 
		{  
			public void run() 
			{
				try
				{
					tempZenossEvents = rhybuddCache.GetRhybuddEvents();
				}
				catch(Exception e)
				{
					e.printStackTrace();
					//tempZenossEvents.clear();
					tempZenossEvents = null;
				}

				if(tempZenossEvents!= null)
				{
					//Log.i("CountWidget","Found DB Data!");
					handler.sendEmptyMessage(1);
				}
				else
				{
					//Log.i("CountWidget","No DB data found, querying API directly");
					//handler.sendEmptyMessage(2);
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
						
						try 
						{
							if(API != null)
							{
								tempZenossEvents = API.GetRhybuddEvents(true,
										true,
										true,
										false,
										false,
										true);
							}
							else
							{
								tempZenossEvents = null;
								handler.sendEmptyMessage(999);
							}
						}
						catch(Exception e)
						{
							handler.sendEmptyMessage(999);
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
				
				if(tempZenossEvents != null)
				{
					EventCount = tempZenossEvents.size();
					
					for(int i = 0; i < EventCount; i++)
	    			{
	    				if(tempZenossEvents.get(i).getSeverity().equals("5"))
	    					CritCount++;
	    				
	    				if(tempZenossEvents.get(i).getSeverity().equals("4"))
	    					ErrCount++;
	    				
	    				if(tempZenossEvents.get(i).getSeverity().equals("3"))
	    					WarnCount++;
	    			}
					tempZenossEvents = null;
					API = null;
				}
				
				//No matter what send an update
				handler.sendEmptyMessage(0);
			}
		}).start();
    }
}