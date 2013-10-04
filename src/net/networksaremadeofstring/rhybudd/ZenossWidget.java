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


import java.util.ArrayList;
import java.util.List;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.RemoteViews;

public class ZenossWidget extends AppWidgetProvider 
{
	private SharedPreferences settings = null;
	private int EventCount = 0;
	Thread dataPreload;
	volatile Handler handler = null;
	private int CritCount = 0, ErrCount = 0, WarnCount = 0;
	List<ZenossEvent> tempZenossEvents = new ArrayList<ZenossEvent>();
	private final static String CRITCOUNT = "CRITCOUNT";
    private final static String WARNCOUNT = "WARNCOUNT";
    private final static String ERRCOUNT = "ERRCOUNT";
    private final static int WHAT = 0;

    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) 
    {
        final int N = appWidgetIds.length;
        
        /*if(settings == null)
			settings = PreferenceManager.getDefaultSharedPreferences(context);*/
        
        handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
                //Log.e("Widget", "handleMessage");

				RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.zenoss_widget);
				Intent intent = new Intent(context, ViewZenossEventsListActivity.class);
				intent.putExtra("forceRefresh", true);
	            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
	            
				for (int i=0; i<N; i++) 
		        {
					int appWidgetId = appWidgetIds[i];
					views.setTextViewText(R.id.CriticalCount, Integer.toString(msg.getData().getInt(CRITCOUNT,0)));
					views.setTextViewText(R.id.WarningCount, Integer.toString(msg.getData().getInt(WARNCOUNT,0)));
					views.setTextViewText(R.id.ErrorCount, Integer.toString(msg.getData().getInt(ERRCOUNT,0)));
					
					views.setOnClickPendingIntent(R.id.linearLayout1, pendingIntent);
					views.setOnClickPendingIntent(R.id.CriticalCount, pendingIntent);
					views.setOnClickPendingIntent(R.id.WarningCount, pendingIntent);
					views.setOnClickPendingIntent(R.id.ErrorCount, pendingIntent);
					
					
					appWidgetManager.updateAppWidget(appWidgetId, views);
					//Log.i("handler","Told the widget to update");
		        }
    		}
    	};
    	
    	Refresh(context);
    }
    
    private void Refresh(final Context context)
    {
    	new Thread()
		{  
			public void run() 
			{
                CritCount = 0;
                ErrCount = 0;
                WarnCount = 0;

                RhybuddDataSource datasource = new RhybuddDataSource(context);
                datasource.open();
                tempZenossEvents = datasource.GetRhybuddEvents();
                datasource.close();
				
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
				}

                Bundle bundle = new Bundle();
                Message msg = new Message();
                bundle.putInt(CRITCOUNT,CritCount);
                bundle.putInt(ERRCOUNT,ErrCount);
                bundle.putInt(WARNCOUNT,WarnCount);
                msg.setData(bundle);
                msg.what = WHAT;

				//No matter what send an update
				handler.sendMessage(msg);
			}
		}.start();
    }
}