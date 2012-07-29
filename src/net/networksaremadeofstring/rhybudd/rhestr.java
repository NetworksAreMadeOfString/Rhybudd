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


import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bugsense.trace.BugSenseHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class rhestr extends Activity 
{
	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	private boolean totalFailure = false;
	private int EventCount = 0;
	Thread dataPreload,AckEvent;
	volatile Handler handler, AckEventHandler;
	ProgressDialog dialog;
	ListView list;
	ZenossEventsAdaptor adapter;
	Cursor dbResults = null;
	SQLiteDatabase rhybuddCache = null;
	
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() 
	{
		if(dialog != null && dialog.isShowing())
		{
			dialog.dismiss();
		}
	
	    return listOfZenossEvents;
	}
	
    /** Called when the activity is first created. */
    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.eventlist);
        ((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
        list = (ListView)findViewById(R.id.ZenossEventsList);
        
        ImageView refreshButton = (ImageView) findViewById(R.id.RefreshViewImage);
        refreshButton.setClickable(true);
        refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listOfZenossEvents.clear();
	        	list.setAdapter(null);
	        	CreateThread();
	        	dataPreload.start();
			}
        });
        
    	ConfigureHandlers();
    	
    	listOfZenossEvents = (List<ZenossEvent>) getLastNonConfigurationInstance();
    	
    	if(listOfZenossEvents == null || listOfZenossEvents.size() < 1)
    	{
    		listOfZenossEvents = new ArrayList<ZenossEvent>();

    		//Log.i("Intent",Boolean.toString(getIntent().getBooleanExtra("forceRefresh", false)));
    		/*if(getIntent().getBooleanExtra("forceRefresh", false) == false && CheckDB())//
    		{
    			DBGetThread();
    		}
    		else
    		{*/
    			CreateThread();
    			dataPreload.start();
    		//}
    	}
    	else
    	{
	    	UpdateErrorMessage("",false);
	    	adapter = new ZenossEventsAdaptor(rhestr.this, listOfZenossEvents,true);
	        list.setAdapter(adapter);
    	}

    }
    
    private void ConfigureHandlers()
    {
    	handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(dialog != null)
    				dialog.dismiss();
    			
    			if(totalFailure == false)
    			{
    				if(EventCount > 0)
    				{
	    				UpdateErrorMessage("",false);
	    				adapter = new ZenossEventsAdaptor(rhestr.this, listOfZenossEvents,true);
	        	        list.setAdapter(adapter);
    				}
    				else
    				{
    					UpdateErrorMessage("There are no open events",false);
    				}
    			}
    			else
    			{
    				UpdateErrorMessage("There was a problem fetching the events list",true);
    				
    				if(msg.what == 1)
    					Toast.makeText(rhestr.this, "Timed out communicating with host. Please check protocol, hostname and port.", Toast.LENGTH_LONG).show();
    			}
    		}
    	};
    	
    	AckEventHandler = new Handler() 
    	 {
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 0)
    			{
    				adapter.notifyDataSetChanged();
    			}
    			else if(msg.what == 1)
    			{
    				adapter.notifyDataSetChanged();
    			}
    			else
    			{
    				Toast.makeText(getApplicationContext(), "There was an error trying to ACK that event.", Toast.LENGTH_SHORT).show();
    			}
    		}
    	 };
    }
    
    private Boolean CheckDB()
    {
    	try
    	{
			//rhybuddCache = this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
    		rhybuddCache = SQLiteDatabase.openDatabase("/data/data/net.networksaremadeofstring.rhybudd/databases/rhybuddCache", null, SQLiteDatabase.OPEN_READONLY);
			dbResults = rhybuddCache.query("events",new String[]{"EVID","Count","lastTime","device","summary","eventState","firstTime","severity"},null, null, null, null, null);
	    }
		catch(Exception e)
		{
			//BugSenseHandler.log("rhestr", e);
			if(rhybuddCache != null && rhybuddCache.isOpen())
    		{
    			rhybuddCache.close();
    		}
    		if(dbResults != null && dbResults.isClosed() == false)
    		{
    			dbResults.close();
    		}
    		
			return false;
		}
    	
    	if(dbResults.getCount() != 0)
    	{
    		EventCount = dbResults.getCount();
    		return true;
    	}
    	else
    	{
    		if(rhybuddCache != null && rhybuddCache.isOpen())
    		{
    			rhybuddCache.close();
    		}
    		if(dbResults != null &&  dbResults.isClosed() == false)
    		{
    			dbResults.close();
    		}
    		return false;
    	}
    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.rhestr_menu, menu);
	    return true;
	}
    
    public void DBGetThread()
    {
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			if(dbResults != null)
    			{
	    			while(dbResults.moveToNext())
	    			{
	    				/*listOfZenossEvents.add(new ZenossEvent(dbResults.getString(0),
															   dbResults.getString(3),
															   dbResults.getString(4), 
															   dbResults.getString(5),
															   dbResults.getString(7)));*/
	    			}
	    			
	    			rhybuddCache.close();
	        		dbResults.close();
    			}
    			handler.sendEmptyMessage(0);
    		}
    	};
    	dataPreload.start();
    }
    
    public void CreateThread()
    {
    	dialog = new ProgressDialog(this);
    	dialog.setTitle("Contacting Zenoss");
   	 	dialog.setMessage("Please wait: loading Events....");
   	 	dialog.show();
    	
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
    				if(API == null)
    				{
    					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				}

    				EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true),
							settings.getBoolean("SeverityError", true),
							settings.getBoolean("SeverityWarning", true),
							settings.getBoolean("SeverityInfo", false),
							settings.getBoolean("SeverityDebug", false));
    				
	    			Events = EventsObject.getJSONObject("result").getJSONArray("events");
				} 
    			catch (org.apache.http.conn.ConnectTimeoutException e)
    			{
    				totalFailure = true;
    				handler.sendEmptyMessage(1);
    			}
    			catch (Exception e) 
    			{
    				BugSenseHandler.log("rhestr", e);
    				totalFailure = true;
    				handler.sendEmptyMessage(0);
				}
				try 
				{
					
					SQLiteDatabase cacheDB = null;
					if(EventsObject != null)
					{
						EventCount = EventsObject.getJSONObject("result").getInt("totalCount");
						
						try
						{
							//cacheDB = SQLiteDatabase.openDatabase("/data/data/net.networksaremadeofstring.rhybudd/databases/rhybuddCache", null, SQLiteDatabase.OPEN_READONLY);
							//cacheDB.close();
							cacheDB = SQLiteDatabase.openDatabase("/data/data/net.networksaremadeofstring.rhybudd/databases/rhybuddCache", null, SQLiteDatabase.OPEN_READWRITE);
							cacheDB.delete("events", null, null);
							//Log.i("delete","Deleted Events");
						}
						catch(Exception e)
						{
							BugSenseHandler.log("rhestr", e);
							if(cacheDB != null && cacheDB.isOpen() && !cacheDB.isDbLockedByOtherThreads())
								cacheDB.close();
						}
						finally
						{
							//Do nothing
						}
						
						if(true)//hack
						{
							for(int i = 0; i < EventCount; i++)
			    			{
			    				JSONObject CurrentEvent = null;
			    				ContentValues values = new ContentValues(2);
			    				try 
			    				{
				    				CurrentEvent = Events.getJSONObject(i);
				    				/*listOfZenossEvents.add(new ZenossEvent(CurrentEvent.getString("evid"),
												    						CurrentEvent.getJSONObject("device").getString("text"),
												    						CurrentEvent.getString("summary"), 
												    						CurrentEvent.getString("eventState"),
												    						CurrentEvent.getString("severity")));
				    				*/
									try
									{
										if(cacheDB != null && cacheDB.isDbLockedByOtherThreads() == false && cacheDB.isOpen() == true && cacheDB.isReadOnly() == false)
										{
											values.put("EVID", CurrentEvent.getString("evid"));
											values.put("device", CurrentEvent.getJSONObject("device").getString("text"));
											values.put("summary", CurrentEvent.getString("summary"));
											values.put("eventState", CurrentEvent.getString("eventState"));
											values.put("severity", CurrentEvent.getString("severity"));
											cacheDB.insert("events", null, values);
										}
									}
									catch(Exception e)
									{
										BugSenseHandler.log("rhestr", e);
									}
			    				}
			    				catch (JSONException e) 
			    				{
			    					//TODO Handle this better - we dont' need to the the user as it's in the loop but we do lose an entire device because of it
			    					BugSenseHandler.log("rhestr", e);
			    				}
			    			}
							if(cacheDB != null && cacheDB.isOpen())
								cacheDB.close();
							
							handler.sendEmptyMessage(0);
						}
						else
						{
							//Log.e("rhestr","We don't have DB lock");
							totalFailure = true;
		    				handler.sendEmptyMessage(0);
						}
					}
					else
					{
						totalFailure = true;
	    				handler.sendEmptyMessage(0);
					}
				
					if(cacheDB != null && cacheDB.isOpen())
						cacheDB.close();
					
				} 
				catch (JSONException e) 
				{
					BugSenseHandler.log("rhestr", e);
					totalFailure = true;
    				handler.sendEmptyMessage(0);
				}
    		}
    	};
    }
	
    public void ViewEvent(String EventID)
    {
    	Intent ViewEventIntent = new Intent(rhestr.this, ViewZenossEvent.class);
    	ViewEventIntent.putExtra("EventID", EventID);
    	rhestr.this.startActivity(ViewEventIntent);
    }
    
    public void AcknowledgeEvent(final String EventID, final int Position, final int viewID)
    {
    	 AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
    	 alertbox.setMessage("What would you like to do?");

    	 alertbox.setPositiveButton("Ack Event", new DialogInterface.OnClickListener() 
    	 {
             public void onClick(DialogInterface arg0, int arg1) 
             {
            	 listOfZenossEvents.get(Position).setProgress(true);
            	 AckEventHandler.sendEmptyMessage(0);
            	 AckEvent = new Thread() 
        	    	{  
        	    		public void run() 
        	    		{
        	    			try 
        	    			{
        	    				ZenossAPIv2 ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
        	    				ackEventAPI.AcknowledgeEvent(EventID);
								listOfZenossEvents.get(Position).setProgress(false);
								listOfZenossEvents.get(Position).setAcknowledged();
								AckEventHandler.sendEmptyMessage(1);
        	    			}
        	    			catch (Exception e)
        	    			{
        	    				AckEventHandler.sendEmptyMessage(99);
        	    			}
        	    		}
        	    	};
        	    	AckEvent.start();
             }
    	 });

    	 alertbox.setNeutralButton("View Event", new DialogInterface.OnClickListener() 
         {
             public void onClick(DialogInterface arg0, int arg1) 
             {
            	 ViewEvent(EventID);
             }
         });
         
         alertbox.setNegativeButton("Nothing", new DialogInterface.OnClickListener() 
         {
             public void onClick(DialogInterface arg0, int arg1) 
             {
                 //Toast.makeText(getApplicationContext(), "Event not ACK'd", Toast.LENGTH_SHORT).show();
             }
         });
         alertbox.show();
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        case R.id.settings:
	        {
	        	Intent SettingsIntent = new Intent(rhestr.this, RhybuddSettings.class);
	        	rhestr.this.startActivity(SettingsIntent);
	            return true;
	        }
	        
	        case R.id.infrastructure:
	        {
	        	Intent DeviceListIntent = new Intent(rhestr.this, DeviceList.class);
	        	rhestr.this.startActivity(DeviceListIntent);
	            return true;
	        }
	        
	        case R.id.Help:
	        {
	        	Intent i = new Intent(Intent.ACTION_VIEW);
	        	i.setData(Uri.parse("http://www.android-zenoss.info/help.php#Events"));
	        	startActivity(i);
	        	return true;
	        }
	        
	        case R.id.refresh:
	        {
	        	listOfZenossEvents.clear();
	        	list.setAdapter(null);
	        	CreateThread();
	        	dataPreload.start();
	            return true;
	        }
        }
        return false;
    }
    
    public void UpdateErrorMessage(String MessageText,boolean Critical)
	{
		TextView ErrorMessage = (TextView) findViewById(R.id.MessageText);
		if(MessageText.length() == 0)
		{
			ErrorMessage.setHeight(0);
		}
		else
		{
			ErrorMessage.setHeight(24);
			ErrorMessage.setTextColor(-16711936);
			ErrorMessage.setText(MessageText);
			
			if(Critical)
				ErrorMessage.setTextColor(-65536);
		}
	}
}