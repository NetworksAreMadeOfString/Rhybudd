package net.networksaremadeofstring.rhybudd;

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
import android.widget.TextView;
import android.widget.Toast;

public class ViewZenossDevice extends Activity
{
	ZenossAPIv2 API = null;
	JSONObject DeviceObject = null;
	JSONObject DeviceDetails = null;
	private SharedPreferences settings = null;
	Handler firstLoadHandler;
	ProgressDialog dialog;
	Thread dataPreload;
	private String EventID;
	
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
	    			//Events = EventsObject.getJSONObject("result").getJSONArray("events");
					DeviceObject.toString(2);
				} 
    			catch (Exception e) 
    			{
    				Log.e("API - Stage 1", e.getMessage());
    				firstLoadHandler.sendEmptyMessage(0);
				}
    			
    			firstLoadHandler.sendEmptyMessage(1);
    		}
    	};
    	
    	dataPreload.start();
    }
}