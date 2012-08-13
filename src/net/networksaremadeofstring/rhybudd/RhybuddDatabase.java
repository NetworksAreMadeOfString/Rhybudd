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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.bugsense.trace.BugSenseHandler;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class RhybuddDatabase 
{
	/*private static final String TAG = "RhybuddDatabase";
	public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;
	public static final String KEY_DEFINITION = SearchManager.SUGGEST_COLUMN_TEXT_2;*/

	private static final String DATABASE_NAME = "rhybudd3Cache";
	private static final int DATABASE_VERSION = 8;

	private RhybuddOpenHelper mDatabaseOpenHelper;
	Context context;

	/**
	 * Constructor
	 * @param context The Context within which to work, used to create the DB
	 */
	public RhybuddDatabase(Context _context) 
	{
		//Log.i("RhybuddDatabase","constructor");
		mDatabaseOpenHelper = new RhybuddOpenHelper(_context);
		//mDatabaseOpenHelper.getWritableDatabase();
		this.context = _context;
	}

	public void FlushDB()
	{
		mDatabaseOpenHelper.FlushDB();
	}
	
	public ZenossDevice getDevice(String UID)
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("devices");
		Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"rhybuddDeviceID","productionState","uid","name"},"uid = '"+UID+"'", null, null, null, null);
		if (cursor == null) 
		{
			mDatabaseOpenHelper.close();
			//cursor.close();
			return null;
		}
		else
		{
			if(cursor.moveToFirst())
			{
				HashMap<String, Integer> events = new HashMap<String, Integer>();
				
				//TODO This could do with being a bit more granular
				try
				{
					events.put("info", cursor.getInt(5));
					events.put("debug", cursor.getInt(6));
					events.put("warning", cursor.getInt(7));
					events.put("error", cursor.getInt(8));
					events.put("critical", cursor.getInt(9));
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
					if(cursor != null)
						cursor.close();
					
					
					ZenossDevice returnDevice = new ZenossDevice(cursor.getString(1),cursor.getInt(2), events, cursor.getString(3),cursor.getString(4));
					cursor.close();
					mDatabaseOpenHelper.close();
					return returnDevice;
				}
				catch(Exception e)
				{
					BugSenseHandler.log("DB-GetRhybuddDevices", e);
					if(cursor != null)
						cursor.close();
					
					mDatabaseOpenHelper.close();
					
					return null;
				}
			}
			else
			{
				cursor.close();
				mDatabaseOpenHelper.close();
				return null;
			}
		}
	}
	public List<ZenossEvent> GetRhybuddEvents()
	{
		List<ZenossEvent> ZenossEvents = new ArrayList<ZenossEvent>();
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		Cursor dbResults = null;
		try
		{
			
			builder.setTables("events");
			dbResults = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"evid",
																									"count",
																									"prodState",
																									"firstTime",
																									"severity",
																									"component_text",
																									"component_uid",
																									"summary",
																									"eventState",
																									"device",
																									"eventClass",
																									"lastTime",
																									"ownerid"}, 
																									null, null, null, null, null);
		}
		catch(Exception e)
		{
			BugSenseHandler.log("DB-GetRhybuddEvents", e);
			//e.printStackTrace();
			dbResults.close();
			dbResults = null;
		}
		
		if(dbResults != null && !dbResults.isClosed() && dbResults.getCount() > 0 )
		{
			while(dbResults.moveToNext())
			{
				try
				{
					ZenossEvents.add(new ZenossEvent(dbResults.getString(0),
							dbResults.getInt(1),
							dbResults.getString(2), 
							dbResults.getString(3),
							dbResults.getString(4),
							dbResults.getString(5),
							dbResults.getString(6),
							dbResults.getString(7),
							dbResults.getString(8),
							dbResults.getString(9),
							dbResults.getString(10),
							dbResults.getString(11),
							dbResults.getString(12)));
				}
				catch(Exception e)
				{
					BugSenseHandler.log("GetRhybuddEvents", e);
				}	
			}
			dbResults.close();
			mDatabaseOpenHelper.close();
			return ZenossEvents;
		}
		else
		{
			if(dbResults != null)
				dbResults.close();
			
			mDatabaseOpenHelper.close();
			return null;
		}
	}
		
		
	public List<ZenossDevice> GetRhybuddDevices()
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("devices");
		Cursor dbResults = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents"},null, null, null, null, null);
		
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
			
			if(dbResults != null)
				dbResults.close();
			
			mDatabaseOpenHelper.close();
			return ZenossDevices;
		}
		else
		{
			if(dbResults != null)
				dbResults.close();
			
			mDatabaseOpenHelper.close();
			return null;
		}
	}
	
	
	public List<ZenossDevice> SearchRhybuddDevices(String Query)
	{
		String Filter = "name like \"%"+Query.replaceAll(" ", "%")+"%\"";
		
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("devices");
		Cursor dbResults = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents"},Filter, null, null, null, null);
		
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
			
			if(dbResults != null)
				dbResults.close();
			
			mDatabaseOpenHelper.close();
			return ZenossDevices;
		}
		else
		{
			if(dbResults != null)
				dbResults.close();
			
			mDatabaseOpenHelper.close();
			return null;
		}
	}
	
	public void UpdateRhybuddDevices(final List<ZenossDevice> ZenossDevices)
	{
		//Log.i("UpdateRhybuddDevices","Recieved a request to update the Devices table");
		((Thread) new Thread()
		{
			public void run()
			{
				try
				{
					mDatabaseOpenHelper.getWritableDatabase();
					mDatabaseOpenHelper.UpdateRhybuddDevices(ZenossDevices);
					Log.i("UpdateRhybuddDevices","Finished updating the Devices table");
				}
				catch(Exception e)
				{
					BugSenseHandler.log("UpdateRhybuddDevices", e);
				}
				finally
				{
					mDatabaseOpenHelper.close();
				}
			}
		}).start();

	}
	
	public void UpdateRhybuddEvents(final List<ZenossEvent> ZenossEvents)
	{
		//Log.i("UpdateRhybudddEvents","Recieved a request to update the Events table");
		((Thread) new Thread()
		{
			public void run()
			{
				try
				{
					mDatabaseOpenHelper.getWritableDatabase();
					mDatabaseOpenHelper.UpdateRhybuddEvents(ZenossEvents);
					//Log.i("UpdateRhybuddEvents","Finished updating the Events table");
				}
				catch(Exception e)
				{
					e.printStackTrace();
					BugSenseHandler.log("UpdateRhybudddEvents", e);
				}
				finally
				{
					//Closing the DB
					mDatabaseOpenHelper.close();
					//Log.i("UpdateRhybuddEvents","Update events table finally");
				}
			}
		}).start();

	}

	public void Close()
	{
		mDatabaseOpenHelper.close();
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
			//Log.i("RhybuddOpenHelper","onOpen");
			mDatabase = db;
		}

		@Override
		public void onCreate(SQLiteDatabase db) 
		{
			//Log.i("RhybuddOpenHelper","onCreate");
			mDatabase = db;
			try
			{
				//Log.i("DB onCreate","Creating Tables");

				mDatabase.execSQL("CREATE TABLE \"events\" (\"evid\" TEXT PRIMARY KEY  NOT NULL, " +
						"\"count\" INTEGER, " +
						"\"prodState\" TEXT, " +
						"\"firstTime\" TEXT, " +
						"\"severity\" TEXT, " +
						"\"component_text\" TEXT, " +
						"\"component_uid\" TEXT, " +
						"\"summary\" TEXT, " +
						"\"eventState\" TEXT, " +
						"\"device\" TEXT, " +
						"\"eventClass\" TEXT, " +
						"\"lastTime\" TEXT, " +
						"\"ownerid\" TEXT)");
				
				mDatabase.execSQL("CREATE TABLE \"devices\" (\"rhybuddDeviceID\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL," +
						"\"productionState\" TEXT," +
						"\"ipAddress\" INTEGER," +
						"\"name\" TEXT," +
						"\"uid\" TEXT, " +
						"\"infoEvents\" INTEGER DEFAULT (0) ," +
						"\"debugEvents\" INTEGER DEFAULT (0) ," +
						"\"warningEvents\" INTEGER DEFAULT (0) ," +
						"\"errorEvents\" INTEGER DEFAULT (0) ," +
						"\"criticalEvents\" INTEGER DEFAULT (0) )");
			}
			catch(SQLiteException s)
			{
				BugSenseHandler.log("Database-onCreate", s);
				//s.printStackTrace();
			}
			catch(Exception e)
			{
				BugSenseHandler.log("Database-onCreate", e);
				//e.printStackTrace();
			}
		}

		private void FlushDB()
		{
			try
			{
				mDatabase.beginTransaction();
				mDatabase.delete("events", null, null);
				mDatabase.delete("devices", null, null);
				mDatabase.setTransactionSuccessful();
			}
			catch(Exception e)
			{
				BugSenseHandler.log("Database-Flush", e);
			}
			finally
			{
				mDatabase.endTransaction();
			}
		}
		
		private void UpdateRhybuddDevices(List<ZenossDevice> ZenossDevices)
		{
			int DeviceCount = 0;
			
			if(ZenossDevices != null)
				DeviceCount = ZenossDevices.size();
			
			try
			{
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
						BugSenseHandler.log("Database-UpdateRhybuddDevices", e);
					}
				}
				mDatabase.setTransactionSuccessful();
			}
			catch(Exception e)
			{
				//This could get a little excessive
				BugSenseHandler.log("Database-UpdateRhybuddDevices", e);
			}
			finally
			{
				mDatabase.endTransaction();
			}
		}
		
		private void UpdateRhybuddEvents(List<ZenossEvent> ZenossEvents)
		{
			int EventCount = 0;
			
			if(ZenossEvents != null)
				EventCount = ZenossEvents.size();
			
			try
			{
				mDatabase.beginTransaction();
				mDatabase.delete("events", null, null);
	
				for(int i = 0; i < EventCount; i++)
				{
					ZenossEvent CurrentEvent = null;
					ContentValues values = new ContentValues(2);
	
					try 
					{
						CurrentEvent = ZenossEvents.get(i);
						values.put("evid",CurrentEvent.getEVID());
						values.put("count", CurrentEvent.getCount());
						values.put("prodState", CurrentEvent.getProdState());
						values.put("firstTime", CurrentEvent.getfirstTime());
						values.put("severity", CurrentEvent.getSeverity());
						values.put("component_text", CurrentEvent.getComponentText());
						values.put("component_uid", CurrentEvent.getComponentUID());
						values.put("summary", CurrentEvent.getSummary());
						values.put("eventState", CurrentEvent.getEventState());
						values.put("device", CurrentEvent.getDevice());
						values.put("eventClass", CurrentEvent.geteventClass());
						values.put("lastTime", CurrentEvent.getlastTime());
						values.put("ownerid", CurrentEvent.getownerID());
					
						//Log.i("DB","Writing " + CurrentEvent.getEVID() + " to the DB");

						mDatabase.insert("events", null, values);
						//Log.i("DB","Done writing " + CurrentEvent.getEVID() + " to the DB");
					}
					catch (Exception e) 
					{
						e.printStackTrace();
						//This could get a little excessive
						BugSenseHandler.log("Database-refreshEvents", e);
					}
				}
				mDatabase.setTransactionSuccessful();
			}
			catch(Exception e)
			{
				//This could get a little excessive
				BugSenseHandler.log("Database-refreshDevices", e);
			}
			finally
			{
				mDatabase.endTransaction();
			}
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
		{
			//Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			try
			{
				db.execSQL("DROP TABLE IF EXISTS events");
				db.execSQL("DROP TABLE IF EXISTS devices");
			}
			catch(Exception e)
			{
				BugSenseHandler.log("Database-onUpgrade", e);
				e.printStackTrace();
			}
			
			onCreate(db);
		}
	}
}
