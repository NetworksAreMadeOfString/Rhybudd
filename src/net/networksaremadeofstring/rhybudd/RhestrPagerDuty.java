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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
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
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RhestrPagerDuty extends Activity
{
	PagerDutyAPI API = null;
	private SharedPreferences settings = null;
	JSONObject IncidentsObject = null;
	JSONArray Incidents = null;
	List<PagerDutyIncident> listOfPagerDutyIncidents = new ArrayList<PagerDutyIncident>();
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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.eventlist);
        list = (ListView)findViewById(R.id.ZenossEventsList);
        
        ImageView refreshButton = (ImageView) findViewById(R.id.RefreshViewImage);
        refreshButton.setClickable(true);
        refreshButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listOfPagerDutyIncidents.clear();
	        	list.setAdapter(null);
	        	CreateThread();
	        	dataPreload.start();
			}
        });
        
        /*ImageView deviceListButton = (ImageView) findViewById(R.id.DeviceListImage);
        deviceListButton.setClickable(true);
        deviceListButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent DeviceListIntent = new Intent(RhestrPagerDuty.this, DeviceList.class);
				RhestrPagerDuty.this.startActivity(DeviceListIntent);
				finish();
			}
        });*/
        
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
	    				PagerDutyIncidentsAdaptor adapter = new PagerDutyIncidentsAdaptor(RhestrPagerDuty.this, listOfPagerDutyIncidents);
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
    
    
    public void CreateThread()
    {
    	dialog = new ProgressDialog(this);
    	dialog.setTitle("Contacting PagerDuty");
   	 	dialog.setMessage("Please wait: loading Incidents....");
   	 	dialog.show();
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
    				if(API == null)
    				{
    					API = new PagerDutyAPI(settings.getString("pagerDutyEmail", ""), settings.getString("pagerDutyPass", ""), settings.getString("pagerDutyURL", ""));
    				}
    				
					IncidentsObject = API.GetIncidents();
	    			Incidents = IncidentsObject.getJSONArray("incidents");
				} 
    			catch (Exception e) 
    			{
    				//Log.e("API - Stage 1", e.getMessage() + " " + e.getLocalizedMessage());
    				e.printStackTrace();
    				totalFailure = true;
    				handler.sendEmptyMessage(0);
				}
    			
    			
				try 
				{
					if(IncidentsObject != null)
					{
						EventCount = IncidentsObject.getInt("total");
						
						for(int i = 0; i < EventCount; i++)
		    			{
		    				JSONObject CurrentEvent = null;
		    				try 
		    				{
			    				CurrentEvent = Incidents.getJSONObject(i);
			    				listOfPagerDutyIncidents.add(new PagerDutyIncident(CurrentEvent.getString("last_status_change_on"),
											    						CurrentEvent.getJSONObject("trigger_summary_data").getString("description"),
											    						CurrentEvent.getString("status"),
											    						CurrentEvent.getString("incident_key")));
			    				//Log.i("ForLoop",CurrentEvent.getJSONObject("trigger_summary_data").getString("description"));
		    				}
		    				catch (JSONException e) 
		    				{
		    					//Log.e("API - Stage 2 - Inner", e.getMessage());
		    				}
		    			}
						
						handler.sendEmptyMessage(0);
					}
					else
					{
						totalFailure = true;
	    				handler.sendEmptyMessage(0);
					}
				} 
				catch (JSONException e) 
				{
					//Log.e("API - Stage 2", e.getMessage());
					totalFailure = true;
    				handler.sendEmptyMessage(0);
				}
    		}
    	};
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
    
    public void AcknowledgeIncident(final String incident_key, final int viewID)
    {
    	 AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
    	 alertbox.setMessage("Acknowledge Incident?");
    	 //Log.i("View",Integer.toString(viewID));
    	 dialog = new ProgressDialog(this);
    	 dialog.setTitle("Contacting Zenoss");
    	 dialog.setMessage("Please wait: Sending Incident Acknowledgement");
    	 
    	 AckEventHandler = new Handler() 
     	 {
     		public void handleMessage(Message msg) 
     		{
     			dialog.dismiss();
     			if(msg.what == 1)
     			{
	     			/*RelativeLayout ListItem = (RelativeLayout) list.findViewWithTag(incident_key);
					ImageView ACKImg = (ImageView) ListItem.findViewById(R.id.AckImage);
					ACKImg.setImageResource(R.drawable.ack);
					list.invalidate();*/
     				Toast.makeText(getApplicationContext(), "Pager Duty's API leaves a lot to be desired - cannot resolve events at the moment", Toast.LENGTH_SHORT).show();
     			}
     			else
     			{
     				Toast.makeText(getApplicationContext(), "There was an error trying to ACK that incident.", Toast.LENGTH_SHORT).show();
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
								API.AcknowledgeIncident(incident_key);
								AckEventHandler.sendEmptyMessage(1);
        	    			}
        	    			catch (Exception e)
        	    			{
        	    				//Log.e("ACK",e.getMessage());
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
}
