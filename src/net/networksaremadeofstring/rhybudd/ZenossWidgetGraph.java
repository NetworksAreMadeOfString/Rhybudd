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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class ZenossWidgetGraph extends AppWidgetProvider
{
	int CritCount = 0, ErrCount = 0, WarnCount = 0;
	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	private int EventCount = 0;
	Thread dataPreload;
	volatile Handler handler = null;
	int HighestCount = 0;
	List<ZenossEvent> tempZenossEvents = new ArrayList<ZenossEvent>();
	RhybuddDatabase rhybuddCache;
	
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) 
    {
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
        
        Refresh();
        
        handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 1)
    			{
    				Bundle Values = msg.getData();
    				
	    			final int N = appWidgetIds.length;
			        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.zenoss_widget_graph);
					//Intent intent = new Intent(context, rhestr.class);
			        //PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			        
					//Log.i("GraphWidget","Drawing Graph!");
					for (int i=0; i<N; i++) 
			        {
						int appWidgetId = appWidgetIds[i];
						views.setImageViewBitmap(R.id.graphCanvas, RenderBarGraph(Values.getInt("CritCount"),Values.getInt("ErrCount"),Values.getInt("WarnCount")));
						appWidgetManager.updateAppWidget(appWidgetId, views);
			        }
    			}
    		}
    	};
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
				//handler.sendEmptyMessage(0);
				Message Msg = new Message();
				Bundle data = new Bundle();
				data.putInt("CritCount", CritCount);
				data.putInt("ErrCount", ErrCount);
				data.putInt("WarnCount", WarnCount);
				Msg.setData(data);
				Msg.what = 1;
				handler.sendMessage(Msg);
			}
		}).start();
    }
	
	/*private void ProcessEvents()
	{
		dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			//Log.i("Widget","Started!");
    			CritCount = 0;
    			WarnCount = 0;
    			ErrCount = 0;
    			ZenossAPIv2 API;
    			
    			try 
    			{
    				API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				
    				EventsObject = API.GetEventsHistory();
    				
	    			Events = EventsObject.getJSONObject("result").getJSONArray("events");
				} 
    			catch (Exception e) 
    			{
    				Log.e("Thread","Fuck up");
    				handler.sendEmptyMessage(0);
    				e.printStackTrace();
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
					}
					else
					{
	    				//handler.sendEmptyMessage(0);
					}
				} 
				catch (JSONException e) 
				{
    				handler.sendEmptyMessage(0);
    				//e.printStackTrace();
				}

				Message Msg = new Message();
				Bundle data = new Bundle();
				data.putInt("CritCount", CritCount);
				data.putInt("ErrCount", ErrCount);
				data.putInt("WarnCount", WarnCount);
				Msg.setData(data);
				Msg.what = 1;
		    	//handler.sendEmptyMessage(0);
				handler.sendMessage(Msg);
		    	
				//Help out the garbage collector
				EventsObject = null;
				Events = null;
				API = null;
				dataPreload = null;
				//Log.i("Widget-Thread", Integer.toString(CritCount) + " / " + Integer.toString(ErrCount) + " / " +  Integer.toString(WarnCount));
    		}
    	};
    	
    	
    	dataPreload.start();
	}*/
	
	private Bitmap RenderBarGraph(int CritCount, int ErrCount, int WarnCount)
	{
		
		//Log.i("Counts", Integer.toString(CritCount) + " / " + Integer.toString(ErrCount) + " / " + Integer.toString(WarnCount));
		Bitmap emptyBmap = Bitmap.createBitmap(290,150, Config.ARGB_8888); 
        
		int width =  emptyBmap.getWidth();
		int height = emptyBmap.getHeight();
		Bitmap charty = Bitmap.createBitmap(width , height , Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(charty);
		//final int color = 0xff0B0B61; 
		final Paint paint = new Paint();

		paint.setStyle(Paint.Style.FILL); 
		paint.setColor(Color.WHITE ); 
		
		//y
		canvas.drawLine(25,0,25,289, paint);
		//x
		canvas.drawLine(25,149,289,149, paint);

		paint.setAntiAlias(true);
		int Max = 0;
		
		if ( CritCount > ErrCount && CritCount > WarnCount )
	         Max = CritCount;
	    else if ( ErrCount > CritCount && ErrCount > WarnCount )
	    	  Max = ErrCount;
	    else if ( WarnCount > CritCount && WarnCount > ErrCount )
	    	  Max = WarnCount;
	    else   
	    	  Max = CritCount;
		
		if(Max > 0)
			canvas.drawText(Integer.toString(Max),  0, 10 , paint );
		
		if(Max > 1)
			canvas.drawText(Integer.toString(Max / 2),  0, 75 , paint );
		canvas.drawText("0",  0, 148 , paint);
		
		double divisor = 148 / (double)Max;

		paint.setAlpha(128);
		
		Rect rect = new Rect(32, (int)(148 - (divisor * CritCount)), 64, 148);  
		paint.setColor(Color.argb(200, 208, 0, 0)); //red
		
		if(CritCount > 0)
			canvas.drawRect(new RectF(rect), paint);
		
		rect = new Rect(128, (int)(148 - (divisor * ErrCount)), 160, 148);  
		paint.setColor(Color.argb(200,255, 102, 0));//orange
		
		if(ErrCount > 0)
			canvas.drawRect(new RectF(rect), paint);
		
		rect = new Rect(224, (int)(148 - (divisor * WarnCount)), 256, 148);  
		paint.setColor(Color.argb(200, 255, 224, 57) ); //yellow
		if(WarnCount > 0)
			canvas.drawRect(new RectF(rect), paint);
		

		//Return
		ByteArrayOutputStream out =  new ByteArrayOutputStream();
		charty.compress(CompressFormat.PNG, 50, out);
		
		return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
	}
	
	@SuppressWarnings("unused")
	private Bitmap RenderLineGraph()
	{
		Bitmap emptyBmap = Bitmap.createBitmap(290,150, Config.ARGB_8888); 
        
		int width =  emptyBmap.getWidth();
		int height = emptyBmap.getHeight();
		Bitmap charty = Bitmap.createBitmap(width , height , Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(charty);
		final int color = 0xff0B0B61; 
		final Paint paint = new Paint();

		paint.setStyle(Paint.Style.FILL); 
		paint.setColor(Color.WHITE ); 
		
		//if(warningEvents > )
		canvas.drawText("100",  0, 10 , paint );
		
		//y
		canvas.drawLine(25,0,25,289, paint);
		//x
		canvas.drawLine(25,149,289,149, paint);
		
		int CritArray[] = { 5, 4, 6, 10, 10, 6, 4, 4};
		int curX = 25;
		
		int divisor = 148 / 10;
		paint.setColor(Color.RED);
		int curY = 148 - (CritArray[0] * divisor);
		
		for (int a : CritArray) 
		{
			canvas.drawLine(curX,curY,curX + 32, (148 - (a * divisor)), paint);
			curX += 32;
			curY = 148 - (a * divisor);
        }
		
		int ErrArray[] = { 1, 2, 2, 2, 4, 2, 1, 0};
		curX = 25;
		
		paint.setColor(Color.rgb(255, 102, 0));
		curY = 148 - (ErrArray[0] * divisor);
		
		for (int a : ErrArray) 
		{
			canvas.drawLine(curX,curY,curX + 32, (148 - (a * divisor)), paint);
			curX += 32;
			curY = 148 - (a * divisor);
        }
		
		int WarnArray[] = { 0, 2, 4, 8, 10, 4, 2, 2};
		curX = 25;
		
		paint.setColor(Color.YELLOW);
		curY = 148 - (WarnArray[0] * divisor);
		
		Path myPath = new Path();
	    
	    
	    
		for (int a : WarnArray) 
		{
			canvas.drawLine(curX,curY,curX + 32, (148 - (a * divisor)), paint);
			curX += 32;
			curY = 148 - (a * divisor);
        }

		ByteArrayOutputStream out =  new ByteArrayOutputStream();
		charty.compress(CompressFormat.PNG, 50, out);
		
		return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
	}
}