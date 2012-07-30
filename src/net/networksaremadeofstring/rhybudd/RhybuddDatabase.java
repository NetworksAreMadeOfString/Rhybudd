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
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bugsense.trace.BugSenseHandler;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.preference.PreferenceManager;
import android.util.Log;

public class RhybuddDatabase 
{
	private static final String TAG = "RhybuddDatabase";

	//The columns we'll include in the dictionary table
	public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;
	public static final String KEY_DEFINITION = SearchManager.SUGGEST_COLUMN_TEXT_2;

	private static final String DATABASE_NAME = "rhybudd3Cache";
	private static final int DATABASE_VERSION = 5;

	private final RhybuddOpenHelper mDatabaseOpenHelper;
	private static Boolean finishedRefresh = false;
	static Context context;

	/**
	 * Constructor
	 * @param context The Context within which to work, used to create the DB
	 */
	public RhybuddDatabase(Context _context) 
	{
		Log.i("RhybuddDatabase","constructor");
		mDatabaseOpenHelper = new RhybuddOpenHelper(_context);
		mDatabaseOpenHelper.getWritableDatabase();
		this.context = _context;
	}

	public Boolean hasCacheRefreshed()
	{
		return this.finishedRefresh;
	}

	public void FlushDB()
	{
		mDatabaseOpenHelper.FlushDB();
	}

	public void GetDBLock()
	{
		mDatabaseOpenHelper.getWritableDatabase();
	}

	public Cursor getDevice(String UID)
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("devices");
		Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"rhybuddDeviceID","productionState","uid","name"},"uid = '"+UID+"'", null, null, null, null);
		if (cursor == null) 
		{
			return null;
		} 
		return cursor;
	}

	public Cursor getDevices()
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("devices");
		Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents"},null, null, null, null, null);
		if (cursor == null) 
		{
			return null;
		} 
		return cursor;
	}

	public Cursor getEvents() 
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("events");
		Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"EVID","Count","lastTime","device","summary","eventState","firstTime","severity","prodState"}, null, null, null, null, null);
		if (cursor == null) 
		{
			return null;
		} 
		return cursor;
	}
	
	public List<ZenossDevice> GetRhybuddDevices()
	{
		Cursor dbResults = getDevices();
		List<ZenossDevice> ZenossDevices = new ArrayList<ZenossDevice>();
		if(dbResults.getCount() > 0)
		{
			while(dbResults.moveToNext())
			{
				HashMap<String, Integer> events = new HashMap<String, Integer>();
				try
				{
					events.put("info", dbResults.getInt(5));
					events.put("debug", dbResults.getInt(6));
					events.put("warning", dbResults.getInt(7));
					events.put("error", dbResults.getInt(8));
					events.put("critical", dbResults.getInt(9));
				}
				catch(Exception e)
				{
					events.put("info", 0);
					events.put("debug", 0);
					events.put("warning", 0);
					events.put("error", 0);
					events.put("critical", 0);
				}
				
				try
				{
					ZenossDevices.add(new ZenossDevice(dbResults.getString(1),
	    					 dbResults.getInt(2), 
	    					 events,
	    					 dbResults.getString(3),
	    					 dbResults.getString(4)));
				}
				catch(Exception e)
				{
					BugSenseHandler.log("DB-GetRhybuddDevices", e);
				}
				
			}
			dbResults.close();
			return ZenossDevices;
		}
		else
		{
			return null;
		}
	}
	
	public void UpdateRhybuddDevices(final List<ZenossDevice> ZenossDevices)
	{
		Log.i("UpdateRhybuddDevices","Recieved a request to update the Devices table");
		((Thread) new Thread()
		{
			public void run()
			{
				mDatabaseOpenHelper.UpdateRhybuddDevices(ZenossDevices);
				Log.i("UpdateRhybuddDevices","Finished updating the Devices table");
			}
		}).start();

	}

	//----------------------------------------------------------------------------------------------------------------------
	public boolean blockingRefreshEvents()
	{
		return mDatabaseOpenHelper.blockingRefreshEvents();
	}
	//----------------------------------------------------------------------------------------------------------------------
	
	public void RefreshEvents()
	{
		mDatabaseOpenHelper.refreshEvents();
	}

	public void RefreshCache()
	{
		mDatabaseOpenHelper.refreshCache();
	}

	public void Close()
	{
		mDatabaseOpenHelper.close();
	}

	private static void SendWarningNotification(String Summary)
	{
		NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.ic_stat_alert, "Rhybudd Database Issues", System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.flags |= Notification.FLAG_SHOW_LIGHTS;

		Intent notificationIntent = new Intent(context, RhybuddHome.class);
		notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("forceRefresh", true);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, "Database / Polling Errors",Summary, contentIntent);
		mNM.notify(999, notification);//NotificationID++ 
	}

	private static class RhybuddOpenHelper extends SQLiteOpenHelper 
	{
		private final Context mHelperContext;
		private SQLiteDatabase mDatabase;

		RhybuddOpenHelper(Context context) 
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mHelperContext = context;
			Log.i("RhybuddOpenHelper","constructor");
		}

		@Override
		public void onOpen(SQLiteDatabase db) 
		{
			Log.i("RhybuddOpenHelper","onOpen");

			mDatabase = db;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			Log.i("RhybuddOpenHelper","onCreate");
			mDatabase = db;
			try
			{
				Log.i("DB onCreate","Creating Tables");
				mDatabase.execSQL("CREATE TABLE \"events\" (\"EVID\" TEXT PRIMARY KEY  NOT NULL  UNIQUE , \"Count\" INTEGER, \"lastTime\" TEXT, \"device\" TEXT, \"summary\" TEXT, \"eventState\" TEXT, \"firstTime\" TEXT, \"severity\" TEXT, \"prodState\" TEXT, \"ownerid\" TEXT)");
				mDatabase.execSQL("CREATE TABLE \"devices\" (\"rhybuddDeviceID\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,\"productionState\" TEXT,\"ipAddress\" INTEGER,\"name\" TEXT,\"uid\" TEXT, \"infoEvents\" INTEGER DEFAULT (0) ,\"debugEvents\" INTEGER DEFAULT (0) ,\"warningEvents\" INTEGER DEFAULT (0) ,\"errorEvents\" INTEGER DEFAULT (0) ,\"criticalEvents\" INTEGER DEFAULT (0) )");
				//refreshCache();
			}
			catch(SQLiteException s)
			{
				BugSenseHandler.log("Database-onCreate", s);
				//s.printStackTrace();
				SendWarningNotification(s.getMessage());
			}
			catch(Exception e)
			{
				BugSenseHandler.log("Database-onCreate", e);
				//e.printStackTrace();
				SendWarningNotification(e.getMessage());
			}
		}

		private void FlushDB()
		{
			try
			{
				mDatabase.delete("events", null, null);
				mDatabase.delete("devices", null, null);
			}
			catch(Exception e)
			{
				BugSenseHandler.log("Database-Flush", e);
			}
		}

		private boolean blockingRefreshEvents()
		{
			ZenossAPIv2 API = null;
			JSONObject EventsObject = null;
			JSONArray Events = null;
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mHelperContext);

			try 
			{
				API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));

				if(API != null)
				{
					try 
					{
						EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true), settings.getBoolean("SeverityError", true),settings.getBoolean("SeverityWarning", true),settings.getBoolean("SeverityInfo",false), settings.getBoolean("SeverityDebug",false),settings.getBoolean("onlyProductionEvents",true));

						Events = EventsObject.getJSONObject("result").getJSONArray("events");
					} 
					catch (Exception e) 
					{
						BugSenseHandler.log("updateEvents", e);
					}

					if (EventsObject != null) 
					{
						
						int EventCount = 0;
						try
						{
							mDatabase.beginTransaction();
							
							EventCount = EventsObject.getJSONObject("result").getInt("totalCount");
							
							mDatabase.delete("events", null, null);
							
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

									values.put("count", CurrentEvent.getString("count"));
									values.put("firstTime", CurrentEvent.getString("firstTime"));
									values.put("lastTime", CurrentEvent.getString("lastTime"));
									values.put("ownerid", CurrentEvent.getString("ownerid"));
									values.put("prodState", CurrentEvent.getString("prodState"));

									mDatabase.insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_IGNORE);
								} 
								catch (JSONException e) 
								{
									BugSenseHandler.log("updateEvents-DBLoop", e);
									//TODO We should tell the user about this or recover from it as they could miss an alert
								}
							}
						}
						catch(Exception e)
						{
							mDatabase.endTransaction();
							BugSenseHandler.log("updateEvents", e);
							e.printStackTrace();
							return false;
						}
						
						mDatabase.setTransactionSuccessful();
						mDatabase.endTransaction();
					}
					else
					{
						return false;
					}
				}
			} 
			catch (Exception e1) 
			{
				BugSenseHandler.log("Database-refreshEvents", e1);
				SendWarningNotification(e1.getMessage());
				return false;
			}
			
			return true;
		}
		
		private synchronized void refreshEvents()
		{
			finishedRefresh = false;

			new Thread(new Runnable() 
			{
				public void run() 
				{
					/*ZenossAPIv2 API = null;
					JSONObject EventsObject = null;
					JSONArray Events = null;
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mHelperContext);

					try 
					{
						API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));

						if(API != null)
						{
							try 
							{
								EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true), settings.getBoolean("SeverityError", true),settings.getBoolean("SeverityWarning", true),settings.getBoolean("SeverityInfo",false), settings.getBoolean("SeverityDebug",false),settings.getBoolean("onlyProductionEvents",true));

								Events = EventsObject.getJSONObject("result").getJSONArray("events");
							} 
							catch (Exception e) 
							{
								BugSenseHandler.log("updateEvents", e);
							}

							if (EventsObject != null) 
							{
								
								int EventCount = 0;
								try
								{
									mDatabase.beginTransaction();
									
									EventCount = EventsObject.getJSONObject("result").getInt("totalCount");
									
									mDatabase.delete("events", null, null);
									
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

											values.put("count", CurrentEvent.getString("count"));
											values.put("firstTime", CurrentEvent.getString("firstTime"));
											values.put("lastTime", CurrentEvent.getString("lastTime"));
											values.put("ownerid", CurrentEvent.getString("ownerid"));
											values.put("prodState", CurrentEvent.getString("prodState"));

											mDatabase.insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_IGNORE);
										} 
										catch (JSONException e) 
										{
											BugSenseHandler.log("updateEvents-DBLoop", e);
											//TODO We should tell the user about this or recover from it as they could miss an alert
										}
									}
								}
								catch(Exception e)
								{
									mDatabase.endTransaction();
									BugSenseHandler.log("updateEvents", e);
									e.printStackTrace();
								}
								
								mDatabase.setTransactionSuccessful();
								mDatabase.endTransaction();
							}
						}
					} 
					catch (Exception e1) 
					{
						BugSenseHandler.log("Database-refreshEvents", e1);
						SendWarningNotification(e1.getMessage());
					}
					finishedRefresh = true;*/
					finishedRefresh = blockingRefreshEvents();
				}
			}).start();
		}

		
		private void UpdateRhybuddDevices(List<ZenossDevice> ZenossDevices)
		{
			int DeviceCount = ZenossDevices.size();
			//cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
			mDatabase.beginTransaction();
			mDatabase.delete("devices", null, null);

			for(int i = 0; i < DeviceCount; i++)
			{
				ZenossDevice CurrentDevice = null;
				ContentValues values = new ContentValues(2);

				try 
				{
					CurrentDevice = ZenossDevices.get(i);
					values.put("productionState",CurrentDevice.getproductionState());
					values.put("ipAddress", CurrentDevice.getipAddress());
					values.put("name", CurrentDevice.getname());
					values.put("uid", CurrentDevice.getuid());
					
					HashMap<String, Integer> events = CurrentDevice.getevents();
					
					values.put("infoEvents", events.get("info"));
					values.put("debugEvents", events.get("debug"));
					values.put("warningEvents", events.get("warning"));
					values.put("errorEvents", events.get("error"));
					values.put("criticalEvents", events.get("critical"));

					mDatabase.insert("devices", null, values);
				}
				catch (Exception e) 
				{
					e.printStackTrace();
					//This could get a little excessive
					//BugSenseHandler.log("Database-refreshDevices", e);
				}
			}
			mDatabase.setTransactionSuccessful();
			mDatabase.endTransaction();
		}
		
		/**
		 * Starts a thread to load the database table with words
		 */
		private void refreshCache() 
		{
			finishedRefresh = false;

			new Thread(new Runnable() 
			{
				public void run() 
				{
					ZenossAPIv2 API = null;
					JSONObject DeviceObject = null;
					SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mHelperContext);
					try 
					{
						API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));

						if(API != null)
						{
							DeviceObject = API.GetDevices();
							int DeviceCount = DeviceObject.getJSONObject("result").getInt("totalCount");
							//cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
							mDatabase.beginTransaction();
							mDatabase.delete("devices", null, null);

							for(int i = 0; i < DeviceCount; i++)
							{
								JSONObject CurrentDevice = null;
								ContentValues values = new ContentValues(2);

								try 
								{
									CurrentDevice = DeviceObject.getJSONObject("result").getJSONArray("devices").getJSONObject(i);

									//Log.e("CurrentDevice",CurrentDevice.toString(3)+"\r\n\r\n\r\n\r\n\r\n\r\n");
									values.put("productionState",CurrentDevice.getString("productionState"));
									try
									{
										values.put("ipAddress", CurrentDevice.getInt("ipAddress"));
									}
									catch(Exception e)
									{
										values.put("ipAddress", "0.0.0.0");
									}
									
									//TODO Lots more catching here
									values.put("name", CurrentDevice.getString("name"));
									try
									{
										values.put("uid", CurrentDevice.getString("uid"));
									}
									catch(Exception e)
									{
										//values.put("uid", CurrentDevice.getString("uid"));
									}
									
									values.put("infoEvents", CurrentDevice.getJSONObject("events").getInt("info"));
									values.put("debugEvents", CurrentDevice.getJSONObject("events").getInt("debug"));
									values.put("warningEvents", CurrentDevice.getJSONObject("events").getInt("warning"));
									values.put("errorEvents", CurrentDevice.getJSONObject("events").getInt("error"));
									values.put("criticalEvents", CurrentDevice.getJSONObject("events").getInt("critical"));

									mDatabase.insert("devices", null, values);
								}
								catch (JSONException e) 
								{
									e.printStackTrace();
									//This could get a little excessive
									//BugSenseHandler.log("Database-refreshDevices", e);
								}
							}
							mDatabase.setTransactionSuccessful();
							//mDatabase.close();
						}
					}
					catch (ClientProtocolException e1) 
					{
						BugSenseHandler.log("updateDevices", e1);
					} 
					catch (JSONException e1) 
					{
						BugSenseHandler.log("updateDevices", e1);
					} 
					catch (IOException e1) 
					{
						BugSenseHandler.log("updateDevices", e1);
					}
					catch (Exception e1) 
					{
						BugSenseHandler.log("updateDevices", e1);
					}
					
					mDatabase.endTransaction();
					
					if(settings.getBoolean("AllowBackgroundService", true))
					{
						//No need to do anything with events the alarm checker will do that
						finishedRefresh = true;
					}
					else
					{
						refreshEvents();
					}
					//finishedRefresh = true;
				}
			}).start();

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			//db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
			try
			{
				db.execSQL("DROP TABLE IF EXISTS events");
				db.execSQL("DROP TABLE IF EXISTS devices");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}

			db.execSQL("CREATE TABLE \"events\" (\"EVID\" TEXT PRIMARY KEY  NOT NULL  UNIQUE , \"Count\" INTEGER, \"lastTime\" TEXT, \"device\" TEXT, \"summary\" TEXT, \"eventState\" TEXT, \"firstTime\" TEXT, \"severity\" TEXT, \"prodState\" TEXT, \"ownerid\" TEXT)");
			db.execSQL("CREATE TABLE \"devices\" (\"rhybuddDeviceID\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,\"productionState\" TEXT,\"ipAddress\" INTEGER,\"name\" TEXT,\"uid\" TEXT, \"infoEvents\" INTEGER DEFAULT (0) ,\"debugEvents\" INTEGER DEFAULT (0) ,\"warningEvents\" INTEGER DEFAULT (0) ,\"errorEvents\" INTEGER DEFAULT (0) ,\"criticalEvents\" INTEGER DEFAULT (0) )");
			onCreate(db);
		}
	}
}
