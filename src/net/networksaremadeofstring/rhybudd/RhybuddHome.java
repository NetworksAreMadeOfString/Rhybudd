package net.networksaremadeofstring.rhybudd;

import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RhybuddHome extends Activity {
	private SharedPreferences settings = null;
	private Handler HomeHandler = null, runnablesHandler = null;
	private Runnable updateEvents = null, updateDevices = null;
	private Boolean OneOff = true;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences("rhybudd", 0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.home);
		
		((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
		
		ConfigureHandler();
		ConfigureRunnable();
		updateEvents.run();
		
		ImageView EventsButton = (ImageView) findViewById(R.id.EventsImageView);
		EventsButton.setClickable(true);
		EventsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent EventsIntent = new Intent(RhybuddHome.this, rhestr.class);
				RhybuddHome.this.startActivity(EventsIntent);
			}
        });
		
		ImageView DevicesButton = (ImageView) findViewById(R.id.DevicesImageView);
		DevicesButton.setClickable(true);
		DevicesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent DeviceListIntent = new Intent(RhybuddHome.this, DeviceList.class);
				RhybuddHome.this.startActivity(DeviceListIntent);
			}
        });
		
		ImageView ReportsButton = (ImageView) findViewById(R.id.ReportsImageView);
		ReportsButton.setClickable(true);
		ReportsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent ReportsIntent = new Intent(RhybuddHome.this, rhestr.class);
				RhybuddHome.this.startActivity(ReportsIntent);
			}
        });
		
		ImageView SettingsButton = (ImageView) findViewById(R.id.SettingsImageView);
		SettingsButton.setClickable(true);
		SettingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent SettingsIntent = new Intent(RhybuddHome.this, Settings.class);
				RhybuddHome.this.startActivity(SettingsIntent);
			}
        });
		
		ImageView SearchButton = (ImageView) findViewById(R.id.SearchImageView);
		SearchButton.setClickable(true);
		SearchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent SearchIntent = new Intent(RhybuddHome.this, Search.class);
				RhybuddHome.this.startActivity(SearchIntent);
			}
        });
		
		ImageView VoiceSearchButton = (ImageView) findViewById(R.id.VoiceSearchImageView);
		VoiceSearchButton.setClickable(true);
		VoiceSearchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onSearchRequested();
			}
        });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.home_menu, menu);
	    return true;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        case R.id.infrastructure:
	        {
	        	Intent DeviceListIntent = new Intent(RhybuddHome.this, DeviceList.class);
	        	RhybuddHome.this.startActivity(DeviceListIntent);
	            return true;
	        }
	        
	        case R.id.Help:
	        {
	        	Intent i = new Intent(Intent.ACTION_VIEW);
	        	i.setData(Uri.parse("http://www.android-zenoss.info/help.php"));
	        	startActivity(i);
	        	return true;
	        }
	        
	        case R.id.EmptyDB:
	        {
	        	Toast.makeText(RhybuddHome.this, "Cache Clearing isn't available yet\n\nPlease use the Clear Data Function in Application Settings", Toast.LENGTH_LONG).show();
	        	startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS),0);
	        	return true;
	        }
        }
        return false;
    }
	
	private void ConfigureHandler() {
		runnablesHandler = new Handler();

		HomeHandler = new Handler() {
			public void handleMessage(Message msg) 
			{
				if (msg.what == 0) 
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Refreshing Events...");
				} 
				else if (msg.what == 1) 
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Refreshing Infrastructure..");
				} 
				else if (msg.what == 98)// One off case
				{
					updateDevices.run();
				} 
				else if (msg.what == 99)// Hide
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(8);
					((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(8);
				} 
				else if (msg.what == 100)// Show
				{
					((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(0);
					((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(0);
				}
			}
		};
	}

	private void ConfigureRunnable() 
	{
		updateDevices = new Runnable() 
		{
			public void run() 
			{
				// Update the GUI
				HomeHandler.sendEmptyMessage(100);
				HomeHandler.sendEmptyMessage(1);

				// Thread
				Thread devicesRefreshThread = new Thread() 
				{
					public void run() 
					{
						ZenossAPIv2 API = null;
						try 
						{
							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
						} 
						catch (Exception e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
    				
						JSONObject DeviceObject = null;
						try 
						{
							DeviceObject = API.GetDevices();
						} 
						catch (ClientProtocolException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
    				
						try 
						{
							int DeviceCount = DeviceObject.getJSONObject("result").getInt("totalCount");
							SQLiteDatabase cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
							cacheDB.delete("devices", null, null);
							
							for(int i = 0; i < DeviceCount; i++)
							{
								JSONObject CurrentDevice = null;
								ContentValues values = new ContentValues(2);
								
								try 
								{
									CurrentDevice = DeviceObject.getJSONObject("result").getJSONArray("devices").getJSONObject(i);
    		    				
	    		    				values.put("productionState",CurrentDevice.getString("productionState"));
									values.put("ipAddress", CurrentDevice.getInt("ipAddress"));
									values.put("name", CurrentDevice.getString("name"));
									values.put("uid", CurrentDevice.getString("uid"));
									values.put("infoEvents", CurrentDevice.getJSONObject("events").getInt("info"));
	    	    					values.put("debugEvents", CurrentDevice.getJSONObject("events").getInt("debug"));
	    	    					values.put("warningEvents", CurrentDevice.getJSONObject("events").getInt("warning"));
	    	    					values.put("errorEvents", CurrentDevice.getJSONObject("events").getInt("error"));
	    	    					values.put("criticalEvents", CurrentDevice.getJSONObject("events").getInt("critical"));
	    	    					
									cacheDB.insert("devices", null, values);
	    	    				}
	    	    				catch (JSONException e) 
	    	    				{
	    	    					e.printStackTrace();
	    	    				}
							}
							cacheDB.close();
						}
						catch(Exception e)
						{
							
						}

						// Hide progress
						HomeHandler.sendEmptyMessage(99);

						// Try again later if the app is still live
						runnablesHandler.postDelayed(this, 3604000);// 1 hour
					}
				};
				devicesRefreshThread.start();
			}
		};

		updateEvents = new Runnable() 
		{
			public void run() 
			{
				// Update the GUI
				HomeHandler.sendEmptyMessage(100);
				HomeHandler.sendEmptyMessage(0);

				// Thread
				Thread eventsRefreshThread = new Thread() 
				{
					public void run() 
					{
						HomeHandler.sendEmptyMessage(100);
						HomeHandler.sendEmptyMessage(0);
						
						JSONObject EventsObject = null;
						JSONArray Events = null;

						try 
						{
							ZenossAPIv2 API = new ZenossAPIv2(
									settings.getString("userName", ""),
									settings.getString("passWord", ""),
									settings.getString("URL", ""));

							// EventsObject = API.GetEvents();
							EventsObject = API
									.GetEvents(settings.getBoolean(
											"SeverityCritical", true), settings
											.getBoolean("SeverityError", true),
											settings.getBoolean(
													"SeverityWarning", true),
											settings.getBoolean("SeverityInfo",
													false), settings
													.getBoolean(
															"SeverityDebug",
															false));
							Events = EventsObject.getJSONObject("result").getJSONArray("events");
						} 
						catch (Exception e) 
						{
							HomeHandler.sendEmptyMessage(0);
							e.printStackTrace();
						}

						try 
						{
							if (EventsObject != null) 
							{
								int EventCount = EventsObject.getJSONObject("result").getInt("totalCount");

								SQLiteDatabase cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache",MODE_PRIVATE, null);
								cacheDB.delete("events", null, null);

								for (int i = 0; i < EventCount; i++) 
								{
									JSONObject CurrentEvent = null;
									ContentValues values = new ContentValues(2);
									try 
									{
										CurrentEvent = Events.getJSONObject(i);

										values.put("EVID",CurrentEvent.getString("evid"));
										values.put("device", CurrentEvent.getJSONObject("device").getString("text"));
										values.put("summary", CurrentEvent.getString("summary"));
										values.put("eventState", CurrentEvent.getString("eventState"));
										values.put("severity", CurrentEvent.getString("severity"));

										cacheDB.insert("events", null, values);
									} 
									catch (JSONException e) 
									{
										// Log.e("API - Stage 2 - Inner",
										// e.getMessage());
										e.printStackTrace();
									}
								}
								cacheDB.close();
							}
						} 
						catch (Exception e) 
						{
							// Log.e("API - Stage 2 - Inner", e.getMessage());
							e.printStackTrace();
						}
						
						// Hide progress
						HomeHandler.sendEmptyMessageDelayed(99, 2000);

						runnablesHandler.postDelayed(this, 300000);// 5 mins 300000

						if (OneOff) 
						{
							// Kick off the infrastructure refresh now we're
							// done with the other bit
							HomeHandler.sendEmptyMessage(98);
							OneOff = false;
						}
					}
				};
				eventsRefreshThread.start();

			}
		};
	}
}
