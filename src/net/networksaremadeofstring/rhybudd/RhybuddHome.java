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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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
	Thread dataPreload,AckEvent,dataReload;
	volatile Handler handler, AckEventHandler;
	ProgressDialog dialog;
	AlertDialog alertDialog;
	ListView list;
	ZenossEventsAdaptor adapter;
	Cursor dbResults = null;
	ActionBar actionbar;
	int requestCode; //Used for evaluating what the settings Activity returned (Should always be 1)
	RhybuddDatabase rhybuddCache;
	ActionMode mActionMode;
	FragmentManager fragmentManager;
	FragmentTransaction fragmentTransaction;
	boolean FragmentVisible = false;
	int selectedFragmentEvent = 0;
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		try
		{
			if(dbResults != null)
			{
				dbResults.close();
			}

			if(rhybuddCache != null)
			{
				rhybuddCache.Close();
			}
		}
		catch(Exception e)
		{
			BugSenseHandler.log("RhybuddHome-onDestroy", e);
		}
	}

	@Override
	public void onAttachedToWindow() 
	{
		super.onAttachedToWindow();

		try
		{
			Window window = getWindow();
			window.setFormat(PixelFormat.RGBA_8888);
		}
		catch(Exception e)
		{
			//TODO Do something although I doubt this will ever happen
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) 
	{
		super.onConfigurationChanged(newConfig);
		Log.i("onConfigurationChanged","Now sending handler");
		
		setContentView(R.layout.rhybudd_home);
		finishStart(false);
	}

	@Override
	public void onNewIntent(Intent newIntent)
	{
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(43523);
		Log.e("onNewIntent",Boolean.toString(newIntent.getBooleanExtra("forceRefresh", false)));
		if(newIntent.getBooleanExtra("forceRefresh", false))
		{
			DBGetThread();
		}
		/*else
		{
			Log.e("onNewIntent","Didn't get told to refresh");
		}*/
	}
	
	@Override
    protected void onResume() 
	{
        super.onResume();
        DBGetThread();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(43523);
    }
	
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		
		actionbar = getSupportActionBar();
		actionbar.setTitle("Rhybudd Events List");
		actionbar.setSubtitle(settings.getString("URL", ""));

		setContentView(R.layout.rhybudd_home);

		Log.e("onCreate",Boolean.toString(getIntent().getBooleanExtra("forceRefresh", false)));
		if(getIntent().getBooleanExtra("forceRefresh", false))
		{
			DBGetThread();
		}
		
		//Clear any notifications event notifications 
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(43523);

		BugSenseHandler.setup(this, "44a76a8c");		

		if((settings.getString("URL", "").equals("") || settings.getString("userName", "").equals("") || settings.getString("passWord", "").equals("")))
		{
			Intent SettingsIntent = new Intent(RhybuddHome.this, RhybuddInitialSettings.class);
			SettingsIntent.putExtra("firstRun", true);
			RhybuddHome.this.startActivityForResult(SettingsIntent, requestCode);
		}
		else
		{
			finishStart(false);
		}
	}

	public void DBGetThread()
	{
		listOfZenossEvents.clear();
		dataPreload = new Thread() 
		{  
			public void run() 
			{
				List<ZenossEvent> tempZenossEvents = null;

				try
				{
					tempZenossEvents = rhybuddCache.GetRhybuddEvents();
				}
				catch(Exception e)
				{
					e.printStackTrace();
					if(tempZenossEvents != null)
						tempZenossEvents.clear();
				}

				if(tempZenossEvents!= null && tempZenossEvents.size() > 0)
				{
					try
					{
						listOfZenossEvents = tempZenossEvents;
	
						Log.i("DeviceList","Found DB Data!");
						handler.sendEmptyMessage(1);
					}
					catch(Exception e)
					{
						//TODO Report to Bugsense
					}
				}
				else
				{
					Log.i("DeviceList","No DB data found, querying API directly");
					try
					{
						handler.sendEmptyMessage(2);
						
						if(tempZenossEvents != null)
							tempZenossEvents.clear();

						//Can we get away with just calling refresh now?
						Refresh();
					}
					catch(Exception e)
					{
						//TODO Report to Bugsense
					}

					
					/*try
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

					}
					catch(Exception e)
					{
						API = null;
						e.printStackTrace();
					}

					try 
					{
						if(API != null)
						{
							tempZenossEvents = API.GetRhybuddEvents(settings.getBoolean("SeverityCritical", true),
									settings.getBoolean("SeverityError", true),
									settings.getBoolean("SeverityWarning", true),
									settings.getBoolean("SeverityInfo", false),
									settings.getBoolean("SeverityDebug", false),
									settings.getBoolean("onlyProductionEvents", true));

							if(tempZenossEvents!= null && tempZenossEvents.size() > 0)
							{
								listOfZenossEvents = tempZenossEvents;
								handler.sendEmptyMessage(1);
							}
							else
							{
								handler.sendEmptyMessage(999);
							}
						}
						else
						{
							handler.sendEmptyMessage(999);
						}
					} 
					catch (ClientProtocolException e) 
					{
						// TODO Send a proper message
						e.printStackTrace();
						handler.sendEmptyMessage(999);
					} 
					catch (JSONException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
						handler.sendEmptyMessage(999);
					} 
					catch (IOException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
						handler.sendEmptyMessage(999);
					}
					catch(Exception e)
					{
						handler.sendEmptyMessage(999);
					}*/
				}
			}
		};
		dataPreload.start();
	}

	public void Refresh()
	{
		Log.i("RhybuddHOMe","Performing a Direct API Refresh");
		try
		{
			if(dialog == null || !dialog.isShowing())
			{
				dialog = new ProgressDialog(this);
			}

			dialog.setTitle("Querying Zenoss Directly");
			dialog.setMessage("Refreshing Events...");
			dialog.setCancelable(false);

			if(!dialog.isShowing())
				dialog.show();
		}
		catch(Exception e)
		{
			//TODO Handle this and tell the user
			Log.i("RhybuddHOMe","Error launching Dialog window");
		}

		((Thread) new Thread(){
			public void run()
			{
				List<ZenossEvent> tempZenossEvents = null;

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
				}
				catch(Exception e)
				{
					API = null;
					e.printStackTrace();
				}

				try 
				{
					if(API != null)
					{
						tempZenossEvents = API.GetRhybuddEvents(settings.getBoolean("SeverityCritical", true),
								settings.getBoolean("SeverityError", true),
								settings.getBoolean("SeverityWarning", true),
								settings.getBoolean("SeverityInfo", false),
								settings.getBoolean("SeverityDebug", false),
								settings.getBoolean("onlyProductionEvents", true));

						if(tempZenossEvents!= null && tempZenossEvents.size() > 0)
						{
							listOfZenossEvents = tempZenossEvents;
							handler.sendEmptyMessage(1);
						}
						else if(tempZenossEvents!= null && tempZenossEvents.size() == 0)
						{
							handler.sendEmptyMessage(50);
						}
						else
						{
							// TODO Send a proper message
							handler.sendEmptyMessage(999);
						}
					}
					else
					{
						// TODO Send a proper message
						handler.sendEmptyMessage(999);
					}
				} 
				catch (ClientProtocolException e) 
				{
					// TODO Send a proper message
					e.printStackTrace();
					handler.sendEmptyMessage(999);
				} 
				catch (JSONException e) 
				{
					// TODO Send a proper message
					e.printStackTrace();
					handler.sendEmptyMessage(999);
				} 
				catch (IOException e) 
				{
					// TODO Send a proper message
					e.printStackTrace();
					handler.sendEmptyMessage(999);
				}
				catch(Exception e)
				{
					// TODO Send a proper message
					e.printStackTrace();
					handler.sendEmptyMessage(999);
				}
			}
		}).start();
	}

	private void finishStart(Boolean firstRun)
	{
		Intent intent = new Intent(this, ZenossPoller.class);
		list = (ListView)findViewById(R.id.ZenossEventsList);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		rhybuddCache = new RhybuddDatabase(this);
		ConfigureHandlers();

		if(firstRun)
		{
			//TODO Remove as the callback handles this
			intent.putExtra("settingsUpdate", true);
			startService(intent);
			//Refresh();
			DBGetThread();
		}
		else
		{
			if(settings.getBoolean("AllowBackgroundService", true))
			{
				Log.i("RhybuddHome","Background polling is enabled so querying the DB");
				DBGetThread();
			}
			else//TODO Remove as the DB query will always fall back to doing a direct API call
			{
				Log.i("RhybuddHome","Doing a direct call to the API");
				Refresh();
			}

			//TODO Check if we don't need this anymore
			startService(intent);
		}
	}

	private void ConfigureHandlers()
	{
		handler = new Handler() 
		{
			public void handleMessage(Message msg) 
			{
				if(msg.what == 0)
				{
					try
					{
						if(dialog != null && dialog.isShowing())
						{
							dialog.dismiss();
						}
					}
					catch(NullPointerException npe)
					{
						//Sigh
					}

					try
					{
						((ProgressBar) findViewById(R.id.backgroundWorkingProgressBar)).setVisibility(4);
					}
					catch(NullPointerException npe)
					{
						//Sigh
					}

					
					OnClickListener listener = new OnClickListener()
					{
						public void onClick(View v) 
						{
							try
							{
								//Check if we are in a big layout
								FrameLayout EventDetailsFragmentHolder = (FrameLayout) findViewById(R.id.EventDetailsFragment);
								if(EventDetailsFragmentHolder == null)
								{
									ManageEvent(v.getTag(R.integer.EventID).toString(),(Integer) v.getTag(R.integer.EventPositionInList), v.getId());
								}
								else
								{
									LoadEventDetailsFragment((Integer) v.getTag(R.integer.EventPositionInList));
								}
							}
							catch(Exception e)
							{
								Toast.makeText(getApplicationContext(), "There was an internal error. A report has been sent.", Toast.LENGTH_SHORT).show();
								BugSenseHandler.log("EventListOnclick", e);
							}
						}
					};

					OnLongClickListener listenerLong = new OnLongClickListener()
					{
						public boolean onLongClick(View v) 
						{
							try
							{
								selectForCAB((Integer)v.getTag(R.integer.EventPositionInList));
							}
							catch(Exception e)
							{
								Toast.makeText(getApplicationContext(), "There was an internal error. A report has been sent.", Toast.LENGTH_SHORT).show();
								BugSenseHandler.log("RhybuddHome-onDestroy", e);
							}
							return true;
						}
					};

					OnClickListener addCAB = new OnClickListener()
					{
						public void onClick(View v) 
						{
							try
							{
								addToCAB((Integer)v.getTag(R.integer.EventPositionInList));
							}
							catch(Exception e)
							{
								Toast.makeText(getApplicationContext(), "There was an internal error. A report has been sent.", Toast.LENGTH_SHORT).show();
								BugSenseHandler.log("RhybuddHome-onDestroy", e);
							}
						}
					};

					adapter = new ZenossEventsAdaptor(RhybuddHome.this, listOfZenossEvents,listener,listenerLong,addCAB);
					list.setAdapter(adapter);
				}
				else if(msg.what == 1)
				{
					try
					{
						if(dialog != null && dialog.isShowing())
							dialog.setMessage("Refresh Complete!");
					}
					catch(NullPointerException npe)
					{
						//Sigh
					}
					this.sendEmptyMessageDelayed(0,1000);
				}
				else if(msg.what == 2)
				{
					if(dialog != null && dialog.isShowing())
					{
						dialog.setMessage("DB Cache incomplete.\r\nQuerying Zenoss directly.\r\nPlease wait....");
					}
					else
					{
						dialog = new ProgressDialog(RhybuddHome.this);
						dialog.setMessage("DB Cache incomplete.\r\nQuerying Zenoss directly.\r\nPlease wait....");
						dialog.setCancelable(false);
						dialog.show();
					}
				}
				else if(msg.what == 50)
				{
					if(dialog != null && dialog.isShowing())
						dialog.dismiss();
					
					try
					{
					if(listOfZenossEvents != null)
						listOfZenossEvents.clear();
					
					if(adapter != null)
						adapter.notifyDataSetChanged();
					}
					catch(Exception e)
					{
						//TODO Bugsense
					}
					
					Toast.makeText(RhybuddHome.this, "There are no events to display", Toast.LENGTH_LONG).show();
				}
				else if(msg.what == 3 || msg.what == 999)
				{
					if(dialog != null && dialog.isShowing())
						dialog.dismiss();

					AlertDialog.Builder builder = new AlertDialog.Builder(RhybuddHome.this);
					builder.setMessage("An error was encountered. Please check your settings and try again.")
					.setCancelable(false)
					.setPositiveButton("Edit Settings", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) 
						{
							Intent SettingsIntent = new Intent(RhybuddHome.this, SettingsFragment.class);
							startActivityForResult(SettingsIntent, 99);
							alertDialog.cancel();
						}
					})
					.setNegativeButton("Close", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) 
						{
							alertDialog.cancel();
						}
					});
					alertDialog = builder.create();
					if(!isFinishing())
					{
						try
						{
							alertDialog.show();
						}
						catch(Exception e)
						{
							BugSenseHandler.log("alertDialog", e);
						}
					}
				}
				else
				{
					if(dialog != null && dialog.isShowing())
					{
						dialog.dismiss();
					}
					Toast.makeText(RhybuddHome.this, "Timed out communicating with host. Please check protocol, hostname and port.", Toast.LENGTH_LONG).show();
				}
			}
		};

		AckEventHandler = new Handler() 
		{
			public void handleMessage(Message msg) 
			{
				try
				{
					if(msg.what == 0)
					{
						if(adapter != null)
							adapter.notifyDataSetChanged();
					}
					else if(msg.what == 1)
					{
						for (ZenossEvent evt : listOfZenossEvents)
						{
							if(!evt.getEventState().equals("Acknowledged") && evt.getProgress())
							{
								evt.setProgress(false);
								evt.setAcknowledged();	
							}
						}
						
						if(adapter != null)
							adapter.notifyDataSetChanged();
					}
					else if(msg.what == 2)
					{
						for (Integer i : selectedEvents)
						{
							if(listOfZenossEvents.get(i).getProgress())
							{
								listOfZenossEvents.get(i).setProgress(false);
								listOfZenossEvents.get(i).setAcknowledged();
							}
						}
						
						if(adapter != null)
							adapter.notifyDataSetChanged();
					}
					else if(msg.what == 99)
					{
						for (ZenossEvent evt : listOfZenossEvents)
						{
							if(!evt.getEventState().equals("Acknowledged") && evt.getProgress())
							{
								evt.setProgress(false);
							}
						}
						
						if(adapter != null)
							adapter.notifyDataSetChanged();
						
						Toast.makeText(getApplicationContext(), "There was an error trying to ACK those events.", Toast.LENGTH_SHORT).show();
					}
					else
					{
	
						Toast.makeText(getApplicationContext(), "There was an error trying to ACK that event.", Toast.LENGTH_SHORT).show();
					}
				}
				catch(Exception e)
				{
					BugSenseHandler.log("AckEventHandler", e);
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

	/**
	 * Starts a Contextual Action Bar or adds to the array of items
	 * to be processed by the CAB
	 * @param id The position in the listOfZenossEvents / listview
	 */
	public void addToCAB(int id)
	{
		if(selectedEvents.contains(id))
		{
			try
			{
				selectedEvents.remove(id);
				listOfZenossEvents.get(id).SetSelected(false);
				adapter.notifyDataSetChanged();
				mActionMode.setTitle("Manage "+ selectedEvents.size()+" Events");
			}
			catch(Exception e)
			{
				BugSenseHandler.log("addToCAB", e);
			}
		}
		else
		{
			try
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
			catch(Exception e)
			{
				BugSenseHandler.log("addToCAB", e);
			}
		}
	}


	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() 
	{
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) 
		{
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
				final List<String> EventIDs = new ArrayList<String>();

				for (final Integer i : selectedEvents)
				{
					listOfZenossEvents.get(i).setProgress(true);
					EventIDs.add(listOfZenossEvents.get(i).getEVID());

					/*AckEvent = new Thread() 
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
								BugSenseHandler.log("Acknowledge", e);
							}
						}
					};
					AckEvent.start();*/
				}
				AckEventHandler.sendEmptyMessage(0);

				AckEvent = new Thread() 
				{  
					public void run() 
					{
						try 
						{
							ZenossAPIv2 ackEventAPI;
							if(settings.getBoolean("httpBasicAuth", false))
							{
								ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
							}
							else
							{
								ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							}
							ackEventAPI.AcknowledgeEvents(EventIDs);//ackEventAPI

							//TODO Check it actually succeeded
							AckEventHandler.sendEmptyMessage(2);
						}
						catch (Exception e)
						{
							BugSenseHandler.log("CABAcknowledge", e);
							e.printStackTrace();
							AckEventHandler.sendEmptyMessage(99);
						}
					}
				};
				AckEvent.start();
				return true;
			}

			case R.id.escalate:
			{
				Intent intent=new Intent(android.content.Intent.ACTION_SEND);
				intent.setType("text/plain");
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

				// Add data to the intent, the receiving app will decide what to do with it.
				intent.putExtra(Intent.EXTRA_SUBJECT, "Escalation of "+ selectedEvents.size() +" Zenoss Events");
				String Events = "Escalated events;\r\n\r\n";
				for (Integer i : selectedEvents)
				{
					Events += listOfZenossEvents.get(i).getDevice() + " - " + listOfZenossEvents.get(i).getSummary() + "\r\n\r\n";
					//list.setItemChecked(i, false);
				}
				intent.putExtra(Intent.EXTRA_TEXT, Events);

				startActivity(Intent.createChooser(intent, "How would you like to escalate these events?"));
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
			this.startActivityForResult(SettingsIntent, 99);
			return true;
		}

		case R.id.Help:
		{
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse("http://wiki.zenoss.org/index.php?title=Rhybudd#Getting_Started"));
			startActivity(i);
			return true;
		}

		case R.id.devices:
		{
			Intent DeviceList = new Intent(RhybuddHome.this, DeviceList.class);
			RhybuddHome.this.startActivity(DeviceList);
			return true;
		}

		case R.id.search:
		{
			onSearchRequested();
			return true;
		}

		case R.id.cache:
		{
			Intent MangeDBIntent = new Intent(RhybuddHome.this, ManageDatabase.class);
			RhybuddHome.this.startActivity(MangeDBIntent);
			return true;
		}

		case R.id.resolveall:
		{
			final List<String> EventIDs = new ArrayList<String>();


			for (ZenossEvent evt : listOfZenossEvents)
			{
				if(!evt.getEventState().equals("Acknowledged"))
				{
					evt.setProgress(true);
					AckEventHandler.sendEmptyMessage(0);
					EventIDs.add(evt.getEVID());	
				}
			}

			AckEventHandler.sendEmptyMessage(0);

			AckEvent = new Thread() 
			{  
				public void run() 
				{
					try 
					{
						ZenossAPIv2 ackEventAPI;
						if(settings.getBoolean("httpBasicAuth", false))
						{
							ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
						}
						else
						{
							ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
						}
						ackEventAPI.AcknowledgeEvents(EventIDs);//ackEventAPI

						//TODO Check it actually succeeded
						AckEventHandler.sendEmptyMessage(1);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						AckEventHandler.sendEmptyMessage(99);
					}
				}
			};
			AckEvent.start();

			return true;
		}
		case R.id.refresh:
		{
			Refresh();
			return true;
		}

		case R.id.escalate:
		{
			Intent intent=new Intent(android.content.Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

			// Add data to the intent, the receiving app will decide what to do with it.
			intent.putExtra(Intent.EXTRA_SUBJECT, "Escalation of Zenoss Events");
			String Events = "";
			for (ZenossEvent evt : listOfZenossEvents)
			{
				Events += evt.getDevice() + " - " + evt.getSummary() + "\r\n\r\n";
			}
			intent.putExtra(Intent.EXTRA_TEXT, Events);

			startActivity(Intent.createChooser(intent, "How would you like to escalate these events?"));
		}
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{

		//Check what the result was from the Settings Activity
		if(requestCode == 99)
		{
			//Refresh the settings
			settings = PreferenceManager.getDefaultSharedPreferences(this);

			Intent intent = new Intent(this, ZenossPoller.class);
			intent.putExtra("settingsUpdate", true);
			startService(intent);
		}
		else
		{
			//In theory the Settings activity should perform validation and only finish() if the settings pass validation
			if(resultCode == 1)
			{
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("FirstRun", false);
				editor.commit();
				//Toast.makeText(RhybuddHome.this, "Welcome to Rhybudd!\r\nPress the menu button to configure additional settings.", Toast.LENGTH_LONG).show();
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Additional settings and functionality can be found by pressing the action bar overflow (or pressing the menu button).\r\n" +
						"\r\nPlease note that this is app is still in Beta. If you experience issues please email;\r\nGareth@NetworksAreMadeOfString.co.uk")
						.setTitle("Welcome to Rhybudd!")
				       .setCancelable(false)
				       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) 
				           {
				        	   finishStart(true);
				           }
				       });
				AlertDialog welcomeDialog = builder.create();
				try
				{
					welcomeDialog.show();
				}
				catch(Exception e)
				{
					finishStart(true);
				}
				
			}
			else if(resultCode == 2)
			{
				Toast.makeText(RhybuddHome.this, "Rhybudd cannot start without configured settings.\n\nExiting....", Toast.LENGTH_LONG).show();

				SharedPreferences.Editor editor = settings.edit();
				editor.putString("URL", "");
				editor.commit();

				finish();
			}
			else //There is the potential for an infinite loop of unhappiness here but I doubt it'll happen
			{
				Toast.makeText(RhybuddHome.this, "Settings did not validate, returning to the settings screen.", Toast.LENGTH_LONG).show();
				Intent SettingsIntent = new Intent(RhybuddHome.this, RhybuddInitialSettings.class);
				SettingsIntent.putExtra("firstRun", true);
				RhybuddHome.this.startActivityForResult(SettingsIntent, requestCode);
			}
		}
	}

	public void LoadEventDetailsFragment(int Position)
	{
		listOfZenossEvents.get(selectedFragmentEvent).setFragmentDisplay(false);
		listOfZenossEvents.get(Position).setFragmentDisplay(true);
		selectedFragmentEvent = Position;
		adapter.notifyDataSetChanged();
		
		fragmentManager = getSupportFragmentManager();
		fragmentTransaction = fragmentManager.beginTransaction();
		
		SherlockFragment fragment = new ViewEventFragment();
		Bundle args = new Bundle();
		
		args.putInt("Position", Position);
	    args.putString("Device", listOfZenossEvents.get(Position).getDevice());
	    args.putString("Component", listOfZenossEvents.get(Position).getComponentText());
	    args.putString("EventClass", listOfZenossEvents.get(Position).geteventClass());
	    args.putString("Summary", listOfZenossEvents.get(Position).getSummary());
	    args.putString("FirstSeen", listOfZenossEvents.get(Position).getfirstTime());
	    args.putString("LastSeen", listOfZenossEvents.get(Position).getlastTime());
	    args.putInt("EventCount", listOfZenossEvents.get(Position).getCount());
	    
		fragment.setArguments(args);
		fragmentTransaction.replace(R.id.EventDetailsFragment, fragment);
		fragmentTransaction.commit();
		
		((FrameLayout) findViewById(R.id.EventDetailsFragment)).setVisibility(View.VISIBLE);
		//((View) findViewById(R.id.fragmentIndicator)).setVisibility(View.VISIBLE);
		FragmentVisible = true;
	}
	
	public void ManageEvent(final String EventID, final int Position, final int viewID)
	{
		AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
		alertbox.setMessage("What would you like to do?");

		alertbox.setPositiveButton("Ack Event", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface arg0, int arg1) 
			{
				AcknowledgeSingleEvent(Position);
				/*listOfZenossEvents.get(Position).setProgress(true);
				AckEventHandler.sendEmptyMessage(0);
				AckEvent = new Thread() 
				{  
					public void run() 
					{
						try 
						{
							ZenossAPIv2 ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							ackEventAPI.AcknowledgeEvent(EventID);//ackEventAPI
							listOfZenossEvents.get(Position).setProgress(false);
							listOfZenossEvents.get(Position).setAcknowledged();
							AckEventHandler.sendEmptyMessage(1);
						}
						catch (Exception e)
						{
							e.printStackTrace();
							BugSenseHandler.log("Acknowledge", e);
							AckEventHandler.sendEmptyMessage(99);
						}
					}
				};
				AckEvent.start();*/
			}
		});

		alertbox.setNeutralButton("View Event", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface arg0, int arg1) 
			{
				Intent ViewEventIntent = new Intent(RhybuddHome.this, ViewZenossEvent.class);
				//This shouldn't fail but no harm in being safe
				try
				{
					ViewEventIntent.putExtra("EventID", EventID);
					ViewEventIntent.putExtra("Count", listOfZenossEvents.get(Position).getCount());
					ViewEventIntent.putExtra("Device", listOfZenossEvents.get(Position).getDevice());
					ViewEventIntent.putExtra("EventState", listOfZenossEvents.get(Position).getEventState());
					ViewEventIntent.putExtra("FirstTime", listOfZenossEvents.get(Position).getfirstTime());
					ViewEventIntent.putExtra("LastTime", listOfZenossEvents.get(Position).getlastTime());
					ViewEventIntent.putExtra("Severity", listOfZenossEvents.get(Position).getSeverity());
					ViewEventIntent.putExtra("Summary", listOfZenossEvents.get(Position).getSummary());
				}
				catch(Exception e)
				{

				}

				RhybuddHome.this.startActivity(ViewEventIntent);
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
	
	public void AcknowledgeSingleEvent(final int Position)
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
					ackEventAPI.AcknowledgeEvent(listOfZenossEvents.get(Position).getEVID());//ackEventAPI
					listOfZenossEvents.get(Position).setProgress(false);
					listOfZenossEvents.get(Position).setAcknowledged();
					AckEventHandler.sendEmptyMessage(1);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					BugSenseHandler.log("Acknowledge", e);
					AckEventHandler.sendEmptyMessage(99);
				}
			}
		};
		AckEvent.start();
	}
	@Override
    public void onBackPressed() 
    {
    	if(FragmentVisible)
    	{
    		try
    		{
    			((FrameLayout) findViewById(R.id.EventDetailsFragment)).setVisibility(View.GONE);
    			//((View) findViewById(R.id.fragmentIndicator)).setVisibility(View.GONE);
    			listOfZenossEvents.get(selectedFragmentEvent).setFragmentDisplay(false);
    			selectedFragmentEvent = 0;
    			adapter.notifyDataSetChanged();
    		}
    		catch(Exception e)
    		{
    			super.onBackPressed();
    		}
    		FragmentVisible = false;
    	}
    	else
    	{
    		super.onBackPressed();
    	}
    }
}
