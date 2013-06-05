/*
 * Copyright (C) 2013 - Gareth Llewellyn
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

import android.app.ActionBar;
import android.content.*;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.*;
import com.google.android.gcm.GCMRegistrar;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import com.bugsense.trace.BugSenseHandler;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.backup.BackupManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class RhybuddHome extends FragmentActivity
{
	SharedPreferences settings = null;
	ZenossAPIv2 API = null;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	List<Integer> selectedEvents = new ArrayList<Integer>();
	Thread dataPreload,AckEvent;
	volatile Handler handler, AckEventHandler;
	ProgressDialog dialog;
	AlertDialog alertDialog;
	ListView list;
	ZenossEventsAdaptor adapter;
	ActionBar actionbar;
	int requestCode;
	ActionMode mActionMode;
	FragmentManager fragmentManager;
	FragmentTransaction fragmentTransaction;
	boolean FragmentVisible = false;
	int selectedFragmentEvent = 0;
    String regId = "";
    ZenossPoller mService;
    boolean mBound = false;
    int retryCount = 0;
    boolean firstRun = false;
    boolean resumeOnResultPollAPI = true;

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
		
		setContentView(R.layout.rhybudd_home);
		finishStart(false);
	}

	@Override
	public void onNewIntent(Intent newIntent)
	{
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_POLLED_ALERTS);
		if(newIntent.getBooleanExtra("forceRefresh", false))
		{
			DBGetThread();
		}
	}
	
	@Override
    protected void onResume() 
	{
        super.onResume();

        //Can't hurt to try and start the service just in case
        startService(new Intent(this, ZenossPoller.class));

        Log.e("-----------------------------------------","OnResume");
        //User is about to see the list of events - no need for them to hang around
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_POLLED_ALERTS);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_GCM_GENERIC);

        //Lets try and bind to our service (if it's alive)
        doBindService();

        //Might as well update GCM whilst we're here
        if(settings.contains(ZenossAPI.PREFERENCE_PUSHKEY) && !settings.getString(ZenossAPI.PREFERENCE_PUSHKEY,"").equals(""))
        {
            doGCMRegistration(settings.getString(ZenossAPI.PREFERENCE_PUSHKEY,""));
        }

        //OnCreate checks the settings for various bits. If they aren't there it sets first run to true
        //Checking a single bool is a hell of a lot more lightweight than 3 Preferences each onResume !
        if(firstRun)
        {
            //We need to set this now as no doubt on resume gets called before onActivityResult
            firstRun = false;

            Intent SettingsIntent = new Intent(RhybuddHome.this, FirstRunSettings.class);
            SettingsIntent.putExtra("firstRun", true);
            //Test
            RhybuddHome.this.startActivityForResult(SettingsIntent, requestCode);
        }
        else
        {
            //If we've resumed from one of our own activities we probably don't want to do a full refresh
            //finishStart will force a full refresh if backgroundPolling isn't enabled
            if(resumeOnResultPollAPI)
            {
                finishStart(false);
            }
            else
            {
                //Nice and fast for returning from within our own app
                DBGetThread();
                resumeOnResultPollAPI = true;
            }
        }
    }
	
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

        BugSenseHandler.initAndStartSession(RhybuddHome.this, "44a76a8c");

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.rhybudd_home);

        actionbar = getActionBar();
        actionbar.setTitle("Rhybudd Events List");
        actionbar.setSubtitle(settings.getString("URL", ""));

        if((settings.getString("URL", "").equals("") || settings.getString("userName", "").equals("") || settings.getString("passWord", "").equals("")))
        {
            firstRun = true;
        }
	}

	public void DBGetThread()
	{
        Log.e("DBGetThread","Doing a DB lookup");
		listOfZenossEvents.clear();
		dataPreload = new Thread() 
		{  
			public void run() 
			{
				List<ZenossEvent> tempZenossEvents = null;

				try
				{
                    RhybuddDataSource datasource = new RhybuddDataSource(RhybuddHome.this);
                    datasource.open();
                    tempZenossEvents = datasource.GetRhybuddEvents();
                    datasource.close();
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
						//Log.i("EventList","Found DB Data!");
						handler.sendEmptyMessage(1);
					}
					catch(Exception e)
					{
                        BugSenseHandler.sendExceptionMessage("RhybuddHome","DBGetThread",e);
					}
				}
				else
				{
					Log.i("EventList","No DB data found, querying API directly");
					try
					{
						handler.sendEmptyMessage(2);
						
						if(tempZenossEvents != null)
							tempZenossEvents.clear();

						//Can we get away with just calling refresh now?
                        //TODO This needs to be sent as a runnable or something
						Refresh();
					}
					catch(Exception e)
					{
                        BugSenseHandler.sendExceptionMessage("RhybuddHome","DBGetThread",e);
					}
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
			//dialog.setCancelable(false);

			if(!dialog.isShowing())
				dialog.show();
		}
		catch(Exception e)
		{
            e.printStackTrace();
			//TODO Handle this and tell the user
            BugSenseHandler.sendExceptionMessage("RhybuddHome","Refresh",e);
		}

		((Thread) new Thread(){
			public void run()
			{
				List<ZenossEvent> tempZenossEvents = null;

                //This is a bit dirty but hell it saves an extra API call
                if (null == mService && !mBound)
                {
                    Log.e("Refresh","Service was dead or something so sleeping");
                    try
                    {
                        sleep(500);
                    }
                    catch(Exception e)
                    {

                    }
                }

                if (null != mService && mBound)
                {
                    Log.e("Refresh","yay not dead");
                    try
                    {
                        if(null == mService.API)
                        {
                            mService.PrepAPI(true);
                        }

                        ZenossCredentials credentials = new ZenossCredentials(RhybuddHome.this);
                        mService.API.Login(credentials);

                        tempZenossEvents = mService.API.GetRhybuddEvents(RhybuddHome.this);

                        if(null == tempZenossEvents)
                        {
                            Log.e("Refresh","We got a null return from the service API, lets try ourselves");
                            ZenossAPI API;

                            if(settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
                            {
                                API = new ZenossAPIZaas();
                            }
                            else
                            {
                                API = new ZenossAPICore();
                            }

                            credentials = new ZenossCredentials(RhybuddHome.this);
                            API.Login(credentials);

                            tempZenossEvents = API.GetRhybuddEvents(RhybuddHome.this);
                        }

                        if(tempZenossEvents != null && tempZenossEvents.size() > 0)
                        {
                            retryCount = 0;
                            listOfZenossEvents = tempZenossEvents;
                            handler.sendEmptyMessage(1);
                            RhybuddDataSource datasource = new RhybuddDataSource(RhybuddHome.this);
                            datasource.open();
                            datasource.UpdateRhybuddEvents(listOfZenossEvents);
                            datasource.close();
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
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        BugSenseHandler.sendExceptionMessage("CoreSettingsFragment","General success path",e);
                        handler.sendEmptyMessage(999);
                    }
                }
                else
                {
                    Log.e("Refresh","The service wasn't running for some reason");
                    dialog.setMessage("The backend service wasn't running.\n\nStarting...");
                    Intent intent = new Intent(RhybuddHome.this, ZenossPoller.class);
                    startService(intent);
                    retryCount++;

                    doBindService();
                    if(retryCount > 5)
                    {
                        handler.sendEmptyMessage(999);
                    }
                    else
                    {
                        //handler.sendEmptyMessage(ZenossAPI.HANDLER_REDOREFRESH);
                        handler.sendEmptyMessageDelayed(ZenossAPI.HANDLER_REDOREFRESH,300);
                    }
                }
			}
		}).start();
	}

	private void finishStart(Boolean firstRun)
	{
		list = (ListView)findViewById(R.id.ZenossEventsList);
		list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		ConfigureHandlers();

        //In certain cases it's perfectly safe to rely that the DB is up to date.
        if(settings.getBoolean("AllowBackgroundService", false) ||
           getIntent().getBooleanExtra("forceRefresh", false) == false ||
           (!regId.equals("") && !settings.getString(ZenossAPI.PREFERENCE_PUSHKEY,"").equals("")))
        {
            Log.i("RhybuddHome","Background polling is enabled OR we are GCM enabled AND we haven't been asked to force a refresh so we're safe to query the DB");
            DBGetThread();
        }
        else
        {
            Log.i("RhybuddHome","Doing a direct call to the API");
            Refresh();
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
								//BugSenseHandler.log("EventListOnclick", e);
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
								//BugSenseHandler.log("RhybuddHome-onDestroy", e);
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
								//BugSenseHandler.log("RhybuddHome-onDestroy", e);
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
							//BugSenseHandler.log("alertDialog", e);
						}
					}
				}
                else if (msg.what == ZenossAPI.HANDLER_REDOREFRESH)
                {
                    Refresh();
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
					//BugSenseHandler.log("AckEventHandler", e);
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
				//BugSenseHandler.log("addToCAB", e);
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
				//BugSenseHandler.log("addToCAB", e);
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
							//BugSenseHandler.log("CABAcknowledge", e);
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
		MenuInflater inflater = getMenuInflater();
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

            case R.id.pushconfig:
            {
                Intent PushSettingsIntent = new Intent(RhybuddHome.this, PushConfigActivity.class);
                this.startActivityForResult(PushSettingsIntent, ZenossAPI.ACTIVITYRESULT_PUSHCONFIG);
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
                Intent DeviceList = new Intent(RhybuddHome.this, ViewZenossDeviceListActivity.class);
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

    private void doGCMRegistration(final String PushKey)
    {
        //TODO check for freshness so as to not waste too much data / bandwidth on every resume!
        if(!PushKey.equals(""))
        {
            GCMRegistrar.checkDevice(this);
            GCMRegistrar.checkManifest(this);
            regId = GCMRegistrar.getRegistrationId(this);

            if (regId.equals(""))
            {
                //Log.e("GCM", "Registering");
                GCMRegistrar.register(this, ZenossAPI.SENDER_ID);
            }
            else
            {
                //Log.e("GCM", "Already registered");
            }


            ((Thread) new Thread()
            {
                public void run()
                {
                    if(!ZenossAPI.registerPushKey(PushKey,regId,ZenossAPI.md5(Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID))))
                    {
                        runOnUiThread(new Runnable()
                        {
                            public void run() {
                                Toast.makeText(RhybuddHome.this, getResources().getString(R.string.ErrorRegisterGCM), Toast.LENGTH_LONG).show();
                            }
                        });

                    }
                }
            }).start();

        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
        super.onActivityResult(requestCode,resultCode,data);

        //Forces our onResume() function to do a DB call rather than a full HTTP request just cos we returned
        //from one of our subscreens
        resumeOnResultPollAPI = false;

		BackupManager bm = new BackupManager(this);

		//Check what the result was from the Settings Activity
		if(requestCode == 99)
		{
			//Refresh the settings
			settings = PreferenceManager.getDefaultSharedPreferences(this);

			Intent intent = new Intent(this, ZenossPoller.class);
			intent.putExtra("settingsUpdate", true);
			startService(intent);
			bm.dataChanged();
		}
        else if(requestCode == ZenossAPI.ACTIVITYRESULT_PUSHCONFIG)
        {
            if(data.hasExtra(ZenossAPI.PREFERENCE_PUSHKEY))
            {
                doGCMRegistration(data.getStringExtra(ZenossAPI.PREFERENCE_PUSHKEY));
            }
        }
		else
		{
			//In theory the Settings activity should perform validation and only finish() if the settings pass validation
			if(resultCode == 1)
			{
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean("FirstRun", false);
				editor.commit();

                //Also update our onResume helper bool although it should already be set
                firstRun = false;

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

				bm.dataChanged();
				
			}
			else if(resultCode == 2)
			{
				Toast.makeText(RhybuddHome.this, getResources().getString(R.string.FirstRunNeedSettings), Toast.LENGTH_LONG).show();

				finish();
			}
            //Who knows what happened here - quit
			else
			{
                Toast.makeText(RhybuddHome.this, getResources().getString(R.string.FirstRunNeedSettings), Toast.LENGTH_LONG).show();
                finish();
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
		
		Fragment fragment = new ViewEventFragment();
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
			}
		});

		alertbox.setNeutralButton("View Event", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface arg0, int arg1) 
			{
				/*Intent ViewEventIntent = new Intent(RhybuddHome.this, ViewZenossEvent.class);
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

				RhybuddHome.this.startActivity(ViewEventIntent);*/

                Intent ViewEventIntent = new Intent(RhybuddHome.this, ViewZenossEventActivity.class);
                ViewEventIntent.putExtra("EventID", EventID);
                ArrayList<String> EventNames = new ArrayList<String>();
                ArrayList<String> EVIDs = new ArrayList<String>();

                for(ZenossEvent evt : listOfZenossEvents)
                {
                    EventNames.add(evt.getDevice());
                    EVIDs.add(evt.getEVID());
                }

                ViewEventIntent.putStringArrayListExtra("eventnames",EventNames);
                ViewEventIntent.putStringArrayListExtra("evids",EVIDs);
                RhybuddHome.this.startActivity(ViewEventIntent);
			}
		});

		alertbox.setNegativeButton("Nothing", new DialogInterface.OnClickListener() 
		{
			public void onClick(DialogInterface arg0, int arg1) 
			{
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
                    //TODO This needs moving to the new API model
					ZenossAPIv2 ackEventAPI = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
					ackEventAPI.AcknowledgeEvent(listOfZenossEvents.get(Position).getEVID());//ackEventAPI
					listOfZenossEvents.get(Position).setProgress(false);
					listOfZenossEvents.get(Position).setAcknowledged();
					AckEventHandler.sendEmptyMessage(1);
				}
				catch (Exception e)
				{
					e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("RhybuddHome","AcknowledgeSingleEvent",e);
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


    //------------------------------------------------------------------//
    //                                                                  //
    //               Connection to the the ZenossPoller Service         //
    //                                                                  //
    //------------------------------------------------------------------//
    void doUnbindService()
    {
        if (mBound)
        {
            // Detach our existing connection.
            unbindService(mConnection);
            mBound = false;
        }
    }

    void doBindService()
    {
        bindService(new Intent(this, ZenossPoller.class), mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // Unbind from the service
        doUnbindService();
    }

    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ZenossPoller.LocalBinder binder = (ZenossPoller.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            //Toast.makeText(RhybuddHome.this, "Connected to Service", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            //Toast.makeText(RhybuddHome.this, "Disconnected to Service", Toast.LENGTH_SHORT).show();
            mBound = false;
        }
    };
}
