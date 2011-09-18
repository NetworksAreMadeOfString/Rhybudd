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
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
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
	Handler handler, AckEventHandler;
	ProgressDialog dialog;
	ListView list;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
        setContentView(R.layout.eventlist);
        list = (ListView)findViewById(R.id.ZenossEventsList);
        
	    //dialog = ProgressDialog.show(this, "Contacting Zenoss", "Please wait: loading Events....", true);
    	handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			dialog.dismiss();
    			if(totalFailure == false)
    			{
    				if(EventCount > 0)
    				{
	    				UpdateErrorMessage("",false);
	    				ZenossEventsAdaptor adapter = new ZenossEventsAdaptor(rhestr.this, listOfZenossEvents);
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
    			}
    		}
    	};
    	
    	CreateThread();
    	
    	dataPreload.start();

    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.rhestr_menu, menu);
	    return true;
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
    				
					EventsObject = API.GetEvents();
	    			Events = EventsObject.getJSONObject("result").getJSONArray("events");
				} 
    			catch (Exception e) 
    			{
    				Log.e("API - Stage 1", e.getMessage());
    				totalFailure = true;
    				handler.sendEmptyMessage(0);
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
		    				listOfZenossEvents.add(new ZenossEvent(CurrentEvent.getString("evid"),
										    						CurrentEvent.getJSONObject("device").getString("text"),
										    						CurrentEvent.getString("summary"), 
										    						CurrentEvent.getString("eventState"),
										    						CurrentEvent.getString("severity")));
		    				Log.i("ForLoop",CurrentEvent.getString("summary"));
	    				}
	    				catch (JSONException e) 
	    				{
	    					Log.e("API - Stage 2 - Inner", e.getMessage());
	    				}
	    			}
					
					handler.sendEmptyMessage(0);
				} 
				catch (JSONException e) 
				{
					Log.e("API - Stage 2", e.getMessage());
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
    
    public void AcknowledgeEvent(final String EventID, final int viewID)
    {
    	 AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
    	 alertbox.setMessage("Acknowledge Event?");
    	 Log.i("View",Integer.toString(viewID));
    	 dialog = new ProgressDialog(this);
    	 dialog.setTitle("Contacting Zenoss");
    	 dialog.setMessage("Please wait: Sending Events Acknowledgement");
    	 
    	 AckEventHandler = new Handler() 
     	 {
     		public void handleMessage(Message msg) 
     		{
     			dialog.dismiss();
     			if(msg.what == 1)
     			{
	     			RelativeLayout ListItem = (RelativeLayout) list.findViewWithTag(EventID);
					ImageView ACKImg = (ImageView) ListItem.findViewById(R.id.AckImage);
					ACKImg.setImageResource(R.drawable.ack);
					list.invalidate();
     			}
     			else
     			{
     				Toast.makeText(getApplicationContext(), "There was an error trying to ACK that event.", Toast.LENGTH_SHORT).show();
     			}
     		}
     	 };
     	 
    	 alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
    	 {
             public void onClick(DialogInterface arg0, int arg1) 
             {
            	 dialog.show();
            	 AckEvent = new Thread() 
        	    	{  
        	    		public void run() 
        	    		{
        	    			try 
        	    			{
								API.AcknowledgeEvent(EventID);
								AckEventHandler.sendEmptyMessage(1);
        	    			}
        	    			catch (Exception e)
        	    			{
        	    				Log.e("ACK",e.getMessage());
        	    				AckEventHandler.sendEmptyMessage(0);
        	    			}
        	    		}
        	    	};
        	    	AckEvent.start();
             }
    	 });

         alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() 
         {
             public void onClick(DialogInterface arg0, int arg1) {
                 //Toast.makeText(getApplicationContext(), "Event not ACK'd", Toast.LENGTH_SHORT).show();
             }
         });

         // display box
         alertbox.show();
    }
    
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        case R.id.settings:
	        {
	        	Intent SettingsIntent = new Intent(rhestr.this, Settings.class);
	        	rhestr.this.startActivity(SettingsIntent);
	            return true;
	        }
	        
	        case R.id.infrastructure:
	        {
	        	Intent DeviceListIntent = new Intent(rhestr.this, DeviceList.class);
	        	rhestr.this.startActivity(DeviceListIntent);
	            return true;
	        }
	        
	        case R.id.pagerduty:
	        {
	        	Intent SettingsIntent = new Intent(rhestr.this, Settings.class);
	        	rhestr.this.startActivity(SettingsIntent);
	            return true;
	        }
	        
	        case R.id.refresh:
	        {
	        	//dialog = new ProgressDialog(this.getApplicationContext());
	        	//dialog.show();
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