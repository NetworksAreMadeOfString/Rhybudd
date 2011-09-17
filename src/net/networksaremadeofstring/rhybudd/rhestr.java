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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

public class rhestr extends Activity 
{
	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	private boolean totalFailure = false;
	private int EventCount = 0;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
        setContentView(R.layout.eventlist);
        final ListView list = (ListView)findViewById(R.id.ZenossEventsList);
	    
	    final ProgressDialog dialog = ProgressDialog.show(this, "Contacting Zenoss", "Please wait: loading Events....", true);
    	final Handler handler = new Handler() 
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
    	
    	Thread dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
					API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
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
    	
    	dataPreload.start();

    }
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.rhestr_menu, menu);
	    return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        /*case R.id.change:
	        {
	        	//Toast.makeText(this, "Changes", Toast.LENGTH_SHORT).show();
	        	Intent EditServerIntent = new Intent(ViewServer.this, EditServer.class);
	        	
	        	EditServerIntent.putExtra("servercode", getIntent().getStringExtra("servercode"));
	        	EditServerIntent.putExtra("description", getIntent().getStringExtra("description"));
	        	EditServerIntent.putExtra("software", getIntent().getStringExtra("software"));
	        	EditServerIntent.putExtra("sessionid", getIntent().getStringExtra("sessionid"));
	        	ViewServer.this.startActivity(EditServerIntent);
	            return true;
	        }*/
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