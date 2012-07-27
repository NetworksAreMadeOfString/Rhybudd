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
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;

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
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class RhybuddHome extends SherlockFragmentActivity 
{
	SharedPreferences settings = null;
	Handler HomeHandler = null, runnablesHandler = null;
	Runnable updateEvents = null, updateDevices = null, updateDeviceDetails = null;
	Boolean OneOff = true;
	
	//New
	ZenossAPIv2 API = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	List<Integer> selectedEvents = new ArrayList<Integer>();

	private int EventCount = 0;
	Thread dataPreload,AckEvent,dataReload;
	volatile Handler handler, AckEventHandler;
	ProgressDialog dialog;
	ListView list;
	ZenossEventsAdaptor adapter;
	Cursor dbResults = null;
	ActionBar actionbar;
	int requestCode; //Used for evaluating what the settings Activity returned (Should always be 1)
	RhybuddDatabase rhybuddCache;
	ActionMode mActionMode;
	
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		rhybuddCache.Close();
	}
	
	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}

	
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences("rhybudd", 0);
		setContentView(R.layout.rhybudd_home);
		
		actionbar = getSupportActionBar();
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionbar.setTitle("Events List");
		actionbar.setSubtitle(settings.getString("URL", ""));
		//actionbar.setListNavigationCallbacks(adapter, callback)
		
		BugSenseHandler.setup(this, "44a76a8c");		
	     
		 if((settings.getString("URL", "").equals("") && settings.getString("userName", "").equals("") && settings.getString("passWord", "").equals("")) || settings.getBoolean("credentialsSuccess", false) == false)
	     {
			Intent SettingsIntent = new Intent(RhybuddHome.this, RhybuddInitialSettings.class);
			SettingsIntent.putExtra("firstRun", true);
			RhybuddHome.this.startActivityForResult(SettingsIntent, requestCode);
	     }
	     else
	     {
	    	 if(getIntent().hasCategory(Intent.CATEGORY_DESK_DOCK) || getIntent().hasCategory("android.intent.category.HE_DESK_DOCK") || getIntent().hasCategory("android.intent.category.LE_DESK_DOCK"))
	    	 {
	    		 Intent RhybuddDockIntent = new Intent(RhybuddHome.this, RhybuddDock.class);
	    		 RhybuddHome.this.startActivity(RhybuddDockIntent);
	    		 finish();
	    	 } 
	    	 else
	    	 {
	    		 finishStart(false);
	    	 }
	     }
	}
	
	public void DBGetThread()
    {
    	dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			dbResults = rhybuddCache.getEvents();
    			
    			if(dbResults != null)
    			{
	    			while(dbResults.moveToNext())
	    			{
	    				listOfZenossEvents.add(new ZenossEvent(dbResults.getString(0),
															   dbResults.getString(3),
															   dbResults.getString(4), 
															   dbResults.getString(5),
															   dbResults.getString(7)));
	    			}
    			}
    			handler.sendEmptyMessage(0);
    		}
    	};
    	dataPreload.start();
    }
	
	public void Refresh()
    {
		dialog = new ProgressDialog(this);
		dialog.setMessage("Refreshing Events...");
		dialog.show();
		rhybuddCache.RefreshEvents();
		handler.sendEmptyMessageDelayed(1, 1000);
    }
	
	private void finishStart(Boolean firstRun)
	{
		Intent intent = new Intent(this, ZenossPoller.class);
		if(firstRun)
		{
            intent.putExtra("settingsUpdate", true);
		}
		
		startService(intent);
		
		list = (ListView)findViewById(R.id.ZenossEventsList);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		rhybuddCache = new RhybuddDatabase(this);
		ConfigureHandlers();
		DBGetThread();
	}
	
	private void ConfigureHandlers()
    {
    	handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 0)
    			{
    				((ProgressBar) findViewById(R.id.backgroundWorkingProgressBar)).setVisibility(4);
    				
    				OnClickListener listener = new OnClickListener()
    				{
						public void onClick(View v) 
						{
							//AcknowledgeEvent(listOfZenossEvents.get((Integer)v.getTag()).getEVID().toString(),(Integer) v.getTag(R.integer.EventPositionInList),v.getId());
							AcknowledgeEvent(v.getTag(R.integer.EventID).toString(),(Integer) v.getTag(R.integer.EventPositionInList), v.getId());
						}
					};
					
					OnLongClickListener listenerLong = new OnLongClickListener()
    				{
						public boolean onLongClick(View v) 
						{
							selectForCAB((Integer)v.getTag(R.integer.EventPositionInList));
							//list.setItemChecked(R.integer.EventPositionInList, true);
							return true;
						}
					};
					
					OnClickListener addCAB = new OnClickListener()
    				{
						public void onClick(View v) 
						{
							addToCAB((Integer)v.getTag(R.integer.EventPositionInList));
						}
					};
    				
	    			adapter = new ZenossEventsAdaptor(RhybuddHome.this, listOfZenossEvents,listener,listenerLong,addCAB);
	        	    list.setAdapter(adapter);
    			}
    			else if(msg.what == 1)
    			{
    				if(rhybuddCache.hasCacheRefreshed())
    				{
    					dialog.setMessage("Refresh Complete!");
    					this.sendEmptyMessageDelayed(2,1000);
    				}
    				else
    				{
    					dialog.setMessage("Processing...");
        				handler.sendEmptyMessageDelayed(1, 1000);
    				}
    			}
    			else if(msg.what == 2)
    			{
    				dialog.dismiss();
    				dbResults = rhybuddCache.getEvents();
        			
        			if(dbResults != null)
        			{
        				listOfZenossEvents.clear();
    	    			while(dbResults.moveToNext())
    	    			{
    	    				listOfZenossEvents.add(new ZenossEvent(dbResults.getString(0),
    															   dbResults.getString(3),
    															   dbResults.getString(4), 
    															   dbResults.getString(5),
    															   dbResults.getString(7)));
    	    			}
        			}
        			handler.sendEmptyMessage(0);
    			}
    			else
    			{
    				Toast.makeText(RhybuddHome.this, "Timed out communicating with host. Please check protocol, hostname and port.", Toast.LENGTH_LONG).show();
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
	
	public void selectForCAB(int id)
	{
		if(listOfZenossEvents.get(id).isSelected())
		{
			selectedEvents.remove(id);
			listOfZenossEvents.get(id).SetSelected(false);
		}
		else
		{
	    	selectedEvents.add(id);
	    	listOfZenossEvents.get(id).SetSelected(true);
		}
		adapter.notifyDataSetChanged();
		mActionMode = startActionMode(mActionModeCallback);
	}
	
	public void addToCAB(int id)
	{
		if(mActionMode != null)
		{
			selectedEvents.add(id);
			listOfZenossEvents.get(id).SetSelected(true);
			adapter.notifyDataSetChanged();
			mActionMode.setTitle("Manage "+ selectedEvents.size()+" Events");
		}
		else
		{
			selectedEvents.add(id);
	    	listOfZenossEvents.get(id).SetSelected(true);
	    	adapter.notifyDataSetChanged();
			mActionMode = startActionMode(mActionModeCallback);
		}
	}
	
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() 
	{

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) 
        {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.events_cab, menu);
            mode.setTitle("Manage "+ selectedEvents.size()+" Events");
            mode.setSubtitle("Select multiple events for mass acknowledgement");
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) 
        {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) 
        {
            switch (item.getItemId()) 
            {
            
	            case R.id.Acknowledge:
	        	{
	        		for (final Integer i : selectedEvents)
	            	{
	        			listOfZenossEvents.get(i).setProgress(true);
	               	 AckEventHandler.sendEmptyMessage(0);
	               	 AckEvent = new Thread() 
	           	    	{  
	           	    		public void run() 
	           	    		{
	           	    			try 
	           	    			{
	           	    				ZenossAPIv2 ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
	           	    				ackEventAPI.AcknowledgeEvent(listOfZenossEvents.get(i).getEVID());
	   								listOfZenossEvents.get(i).setProgress(false);
	   								listOfZenossEvents.get(i).setAcknowledged();
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
	        		return true;
	        	}
	        	
	            default:
	            {
	            	for (Integer i : selectedEvents)
	            	{
	            		listOfZenossEvents.get(i).SetSelected(false);
	            		//list.setItemChecked(i, false);
	            	}
	            	selectedEvents.clear();
		        	adapter.notifyDataSetChanged();
		            return false;
	            }
			}
        }
			
			// Called when the user exits the action mode
			@Override
			public void onDestroyActionMode(ActionMode mode) 
			{
				for (Integer i : selectedEvents)
            	{
            		listOfZenossEvents.get(i).SetSelected(false);
            		//list.setItemChecked(i, false);
            	}
            	selectedEvents.clear();
	        	adapter.notifyDataSetChanged();
	        	mActionMode = null;
			}
		};
	
    public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.home_menu, menu);
	    return true;
    }
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch (item.getItemId()) 
        {
	        case R.id.settings:
	        {
	        	Intent SettingsIntent = new Intent(RhybuddHome.this, SettingsFragment.class);
	        	RhybuddHome.this.startActivity(SettingsIntent);
	            return true;
	        }
	        
	        case R.id.Help:
	        {
	        	Intent i = new Intent(Intent.ACTION_VIEW);
	        	i.setData(Uri.parse("http://www.android-zenoss.info/help.php"));
	        	startActivity(i);
	        	return true;
	        }
	        
	        case R.id.cache:
	        {
	        	Intent MangeDBIntent = new Intent(RhybuddHome.this, ManageDatabase.class);
	        	RhybuddHome.this.startActivity(MangeDBIntent);
	            return true;
	        }
	        
	        case R.id.refresh:
	        {
	        	Refresh();
	        	return true;
	        }
        }
        return false;
    }
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	//Check what the result was from the Settings Activity
    	//In theory the Settings activity should perform validation and only finish() if the settings pass validation
    	if(resultCode == 1)
    	{
    		SharedPreferences.Editor editor = settings.edit();
        	editor.putBoolean("FirstRun", false);
        	editor.commit();
        	Toast.makeText(RhybuddHome.this, "Welcome to Rhybudd!\r\nPress the menu button to configure additional settings.", Toast.LENGTH_LONG).show();
        	finishStart(true);
    	}
    	else if(resultCode == 2)
    	{
    		Toast.makeText(RhybuddHome.this, "Rhybudd cannot start without configured settings.\n\nExiting....", Toast.LENGTH_LONG).show();
    		finish();
    	}
    	else //There is the potential for an infinite loop of unhappiness here but I doubt it'll happen
    	{
    		Toast.makeText(RhybuddHome.this, "Settings did not validate, returning to the settings screen.", Toast.LENGTH_LONG).show();
    		Intent SettingsIntent = new Intent(RhybuddHome.this, RhybuddSettings.class);
			SettingsIntent.putExtra("firstRun", true);
			RhybuddHome.this.startActivityForResult(SettingsIntent, requestCode);
    	}
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
            	 //ViewEvent(EventID);
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
}
