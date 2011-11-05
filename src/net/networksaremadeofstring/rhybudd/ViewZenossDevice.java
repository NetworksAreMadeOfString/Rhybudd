package net.networksaremadeofstring.rhybudd;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ViewZenossDevice extends Activity
{
	ZenossAPIv2 API = null;
	JSONObject DeviceObject = null, EventsObject = null;
	JSONObject DeviceDetails = null;
	private SharedPreferences settings = null;
	Handler firstLoadHandler, eventsHandler;
	ProgressDialog dialog;
	Thread dataPreload, eventsLoad;
	private String EventID;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	ListView list;
	ZenossEventsAdaptor adapter;
	JSONArray Events = null;
	private int EventCount = 0;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
        setContentView(R.layout.view_zenoss_device);
        
        ImageView deviceListButton = (ImageView) findViewById(R.id.DeviceListImage);
        deviceListButton.setClickable(true);
        deviceListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
        });
        
        list = (ListView)findViewById(R.id.ZenossEventsList);
        
        eventsHandler = new Handler()
        {
        	public void handleMessage(Message msg) 
    		{
        		((ProgressBar) findViewById(R.id.eventsProgressBar)).setVisibility(4);
        		
				if(EventCount > 0 && msg.what == 1)
				{
    				adapter = new ZenossEventsAdaptor(ViewZenossDevice.this, listOfZenossEvents, false);
        	        list.setAdapter(adapter);
				}
				else
				{
					list = null;
					
				}
    		}
        };
        
        firstLoadHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			dialog.dismiss();
    			try
    			{
    				if(DeviceObject.getJSONObject("result").getBoolean("success") == true)
    				{
    					DeviceDetails = DeviceObject.getJSONObject("result").getJSONObject("data");
    					
    					try
    					{
    						((TextView) findViewById(R.id.Title)).setText(DeviceDetails.getString("snmpSysName") + " Details");
    					}
    					catch(Exception e)
    					{
    						((TextView) findViewById(R.id.Title)).setText("No Name - Details");
    					}
    					
    					try
    					{
    						((TextView) findViewById(R.id.lastCollected)).setText(DeviceDetails.getString("lastCollected"));
    					}
    					catch(Exception e)
    					{
    						((TextView) findViewById(R.id.lastCollected)).setText("-----");
    					}
    					
    					try
    					{
    						((TextView) findViewById(R.id.osModel)).setText(DeviceDetails.getJSONObject("osModel").getString("name"));
    					}
    					catch(Exception e)
    					{
    						((TextView) findViewById(R.id.osModel)).setText("Unknown OS");
    					}
    					
    					try
    					{
    						((TextView) findViewById(R.id.uptime)).setText(DeviceDetails.getString("uptime"));
    					}
    					catch(Exception e)
    					{
    						//Already got a placeholder
    					}
    					
    					try
    					{
    						((TextView) findViewById(R.id.productionState)).setText(DeviceDetails.getString("productionState"));
    					}
    					catch(Exception e)
    					{
    						//Already got a placeholder
    					}
    					
    					String Groups = "";
    					try
    					{
	    					for (int i = 0; i < DeviceDetails.getJSONArray("groups").length(); i++)
	    					{
	    						if(i > 0)
	    							Groups += ", ";
	    						try
	    						{
	    							Groups +=  DeviceDetails.getJSONArray("groups").getJSONObject(i).getString("name");
	    						}
	    						catch(Exception e)
	    						{
	    							e.printStackTrace();
	    						}
	    					}

	    					((TextView) findViewById(R.id.groups)).setText(Groups);
	    					
    					}
	    				catch(Exception e)
    					{
    						//Already got a placeholder
    					}
	    				
    					String Systems = "";
    					try
    					{
	    					for (int i = 0; i < DeviceDetails.getJSONArray("systems").length(); i++)
	    					{
	    						if(i > 0)
	    							Systems += ", ";
	    						
	    						Systems +=  DeviceDetails.getJSONArray("systems").getJSONObject(i).getString("name");
	    					}

	    					((TextView) findViewById(R.id.systems)).setText(Systems);
	    					
    					}
	    				catch(Exception e)
    					{
    						//Already got a placeholder
    					}
    					
    					//etc
    				}
    				else
    				{
    					Toast.makeText(ViewZenossDevice.this, "There was an error loading the Device details", Toast.LENGTH_LONG).show();
    					finish();
    				}
    			}
    			catch(Exception e)
    			{
    				Toast.makeText(ViewZenossDevice.this, "An error was encountered parsing the JSON.", Toast.LENGTH_LONG).show();
    			}
    		}
    	};
    	
    	dialog = new ProgressDialog(this);
    	dialog.setTitle("Contacting Zenoss");
   	 	dialog.setMessage("Please wait:\nLoading Device details....");
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
    				
					DeviceObject = API.GetDevice(getIntent().getStringExtra("UID"));
				} 
    			catch (Exception e) 
    			{
    				//Log.e("API - Stage 1", e.getMessage());
    				firstLoadHandler.sendEmptyMessage(0);
				}
    			
    			firstLoadHandler.sendEmptyMessage(1);
    		}
    	};
    	
    	dataPreload.start();
    	
    	eventsLoad = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
    				if(API == null)
    				{
    					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
    				}
    				
					EventsObject = API.GetDeviceEvents(getIntent().getStringExtra("UID"));
					Events = EventsObject.getJSONObject("result").getJSONArray("events");
					
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
				    				listOfZenossEvents.add(new ZenossEvent(CurrentEvent.getString("evid"),
												    						CurrentEvent.getJSONObject("device").getString("text"),
												    						CurrentEvent.getString("summary"), 
												    						CurrentEvent.getString("eventState"),
												    						CurrentEvent.getString("severity")));
				    				//Log.i("ForLoop",CurrentEvent.getString("summary"));
			    				}
			    				catch (JSONException e) 
			    				{
			    					//Log.e("API - Stage 2 - Inner", e.getMessage());
			    				}
			    			}
							
							eventsHandler.sendEmptyMessage(1);
						}
						else
						{
							eventsHandler.sendEmptyMessage(0);
						}
					} 
					catch (JSONException e) 
					{
						eventsHandler.sendEmptyMessage(0);
					}
				} 
    			catch (Exception e) 
    			{
    				//Log.e("API - Stage 1", e.getMessage());
    				eventsHandler.sendEmptyMessage(0);
				}
    		}
    	};
    	
    	eventsLoad.start();
    }
}