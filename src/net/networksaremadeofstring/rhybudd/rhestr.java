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
	Thread dataPreload;
	Handler handler;
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
        try 
        {
			API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
		} 
        catch (Exception e) 
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	    dialog = ProgressDialog.show(this, "Contacting Zenoss", "Please wait: loading Events....", true);
    	handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			dialog.hide();
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
	
    
    
    public boolean AcknowledgeEvent(final String EventID, final int viewID)
    {
    	 AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
    	 alertbox.setMessage("Acknowledge Event?");
    	 Log.i("View",Integer.toString(viewID));
    	 dialog.setTitle("Contacting Zenoss");
    	 dialog.setMessage("Please wait: Sending Events Acknowledgement");
    	 dialog.setProgressStyle(0);
    	 
    	 handler = new Handler() 
     	 {
     		public void handleMessage(Message msg) 
     		{
     			dialog.hide();
     			if(msg.what == 1)
     			{
	     			RelativeLayout ListItem = (RelativeLayout) list.findViewById(viewID);
					ImageView ACKImg = (ImageView) ListItem.findViewById(R.id.AckImage);
					ACKImg.setImageResource(R.drawable.ack);
					ACKImg.invalidate();
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
        		 dataPreload = new Thread() 
        	    	{  
        	    		public void run() 
        	    		{
        	    			try 
        	    			{
								API.AcknowledgeEvent(EventID);
								handler.sendEmptyMessage(1);
        	    			}
        	    			catch (Exception e)
        	    			{
        	    				Log.e("ACK",e.getMessage());
        	    				handler.sendEmptyMessage(0);
        	    			}
        	    		}
        	    	};
        	    	dataPreload.start();
             }
    	 });

         alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() 
         {
             public void onClick(DialogInterface arg0, int arg1) {
                 Toast.makeText(getApplicationContext(), "Event not ACK'd", Toast.LENGTH_SHORT).show();
             }
         });

         // display box
         alertbox.show();
         return true;
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
	        	Intent SettingsIntent = new Intent(rhestr.this, Settings.class);
	        	rhestr.this.startActivity(SettingsIntent);
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
	        	dialog.show();
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