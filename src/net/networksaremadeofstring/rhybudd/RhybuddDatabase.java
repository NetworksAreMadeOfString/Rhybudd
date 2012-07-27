package net.networksaremadeofstring.rhybudd;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bugsense.trace.BugSenseHandler;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class RhybuddDatabase 
{
	private static final String TAG = "RhybuddDatabase";

	//The columns we'll include in the dictionary table
	public static final String KEY_WORD = SearchManager.SUGGEST_COLUMN_TEXT_1;
	public static final String KEY_DEFINITION = SearchManager.SUGGEST_COLUMN_TEXT_2;

	private static final String DATABASE_NAME = "rhybuddCache";
	private static final String FTS_VIRTUAL_TABLE = "FTSdictionary";
	private static final int DATABASE_VERSION = 1;

	private final RhybuddOpenHelper mDatabaseOpenHelper;
	private static Boolean finishedRefresh = false;

	//private static final HashMap<String,String> mColumnMap = buildColumnMap();

	/**
	 * Constructor
	 * @param context The Context within which to work, used to create the DB
	 */
	public RhybuddDatabase(Context context) 
	{
		Log.i("RhybuddDatabase","constructor");
		mDatabaseOpenHelper = new RhybuddOpenHelper(context);
		mDatabaseOpenHelper.getWritableDatabase();

	}

	public Boolean hasCacheRefreshed()
	{
		return this.finishedRefresh;
	}

	public void GetDBLock()
	{
		mDatabaseOpenHelper.getWritableDatabase();
	}

	public Cursor getEvents() 
	{
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("events");
		Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),new String[]{"EVID","Count","lastTime","device","summary","eventState","firstTime","severity"}, null, null, null, null, null);
		if (cursor == null) 
		{
			return null;
		} 
		else if (!cursor.moveToFirst()) 
		{
			cursor.close();
			return null;
		}
		return cursor;
	}

	public void RefreshEvents()
	{
		mDatabaseOpenHelper.refreshEvents();
	}
	
	public void RefreshCache()
	{
		mDatabaseOpenHelper.refreshCache();
	}

	/**
	 * Performs a database query.
	 * @param selection The selection clause
	 * @param selectionArgs Selection arguments for "?" components in the selection
	 * @param columns The columns to return
	 * @return A Cursor over all rows matching the query
	 */
	private Cursor query(String selection, String[] selectionArgs, String[] columns) {
		/* The SQLiteBuilder provides a map for all possible columns requested to
		 * actual columns in the database, creating a simple column alias mechanism
		 * by which the ContentProvider does not need to know the real column names
		 */
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables(FTS_VIRTUAL_TABLE);
		//builder.setProjectionMap(mColumnMap);

		Cursor cursor = builder.query(mDatabaseOpenHelper.getReadableDatabase(),
				columns, selection, selectionArgs, null, null, null);

		if (cursor == null) {
			return null;
		} else if (!cursor.moveToFirst()) {
			cursor.close();
			return null;
		}
		return cursor;
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
				mDatabase.execSQL("CREATE TABLE \"events\" (\"EVID\" TEXT PRIMARY KEY  NOT NULL  UNIQUE , \"Count\" INTEGER, \"lastTime\" TEXT, \"device\" TEXT, \"summary\" TEXT, \"eventState\" TEXT, \"firstTime\" TEXT, \"severity\" TEXT)");
				mDatabase.execSQL("CREATE TABLE \"devices\" (\"rhybuddDeviceID\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,\"productionState\" TEXT,\"ipAddress\" INTEGER,\"name\" TEXT,\"uid\" TEXT, \"infoEvents\" INTEGER DEFAULT (0) ,\"debugEvents\" INTEGER DEFAULT (0) ,\"warningEvents\" INTEGER DEFAULT (0) ,\"errorEvents\" INTEGER DEFAULT (0) ,\"criticalEvents\" INTEGER DEFAULT (0) )");
				//refreshCache();
			}
			catch(SQLiteException s)
			{
				s.printStackTrace();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}


		private void refreshEvents()
		{
			finishedRefresh = false;

			new Thread(new Runnable() 
			{
				public void run() 
				{
					Thread RefreshThread = new Thread() 
					{
						public void run() 
						{
							ZenossAPIv2 API = null;
							JSONObject EventsObject = null;
							JSONArray Events = null;
							SharedPreferences settings = mHelperContext.getSharedPreferences("rhybudd", 0);

							try 
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							} 
							catch (Exception e1) 
							{
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							if(API != null)
							{
								try 
								{
									EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true), settings.getBoolean("SeverityError", true),settings.getBoolean("SeverityWarning", true),settings.getBoolean("SeverityInfo",false), settings.getBoolean("SeverityDebug",false));

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
										EventCount = EventsObject.getJSONObject("result").getInt("totalCount");

										mDatabase.delete("events", null, null);
									}
									catch(Exception e)
									{
										BugSenseHandler.log("updateEvents", e);
										e.printStackTrace();
									}

									for (int i = 0; i < EventCount; i++) 
									{
										JSONObject CurrentEvent = null;
										ContentValues values = new ContentValues(2);
										try 
										{

											CurrentEvent = Events.getJSONObject(i);

											//Log.i("Event",CurrentEvent.toString(3));

											values.put("EVID",CurrentEvent.getString("evid"));
											values.put("device", CurrentEvent.getJSONObject("device").getString("text"));
											values.put("summary", CurrentEvent.getString("summary"));
											values.put("eventState", CurrentEvent.getString("eventState"));
											values.put("severity", CurrentEvent.getString("severity"));

											mDatabase.insert("events", null, values);
										} 
										catch (JSONException e) 
										{
											BugSenseHandler.log("updateEvents-DBLoop", e);
											//TODO We should tell the user about this or recover from it as they could miss an alert
										}
									}
								}
							}

							finishedRefresh = true;
						}
					};
					RefreshThread.start();
				}
			}).start();
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
					Thread RefreshThread = new Thread() 
					{
						public void run() 
						{
							ZenossAPIv2 API = null;
							JSONObject DeviceObject = null;
							SharedPreferences settings = mHelperContext.getSharedPreferences("rhybudd", 0);
							try 
							{
								API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));

								if(API != null)
								{
									DeviceObject = API.GetDevices();
									int DeviceCount = DeviceObject.getJSONObject("result").getInt("totalCount");
									//cacheDB = RhybuddHome.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
									mDatabase.delete("devices", null, null);

									for(int i = 0; i < DeviceCount; i++)
									{
										JSONObject CurrentDevice = null;
										ContentValues values = new ContentValues(2);

										try 
										{
											CurrentDevice = DeviceObject.getJSONObject("result").getJSONArray("devices").getJSONObject(i);

											values.put("productionState",CurrentDevice.getString("productionState"));
											try
											{
												values.put("ipAddress", CurrentDevice.getInt("ipAddress"));
											}
											catch(Exception e)
											{
												values.put("ipAddress", "0.0.0.0");
											}
											values.put("name", CurrentDevice.getString("name"));
											values.put("uid", CurrentDevice.getString("uid"));
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
										}
									}
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


							//Events -----------------------------------------------------------------
							JSONObject EventsObject = null;
							JSONArray Events = null;

							try 
							{
								EventsObject = API.GetEvents(settings.getBoolean("SeverityCritical", true), settings.getBoolean("SeverityError", true),settings.getBoolean("SeverityWarning", true),settings.getBoolean("SeverityInfo",false), settings.getBoolean("SeverityDebug",false));

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
									EventCount = EventsObject.getJSONObject("result").getInt("totalCount");

									mDatabase.delete("events", null, null);
								}
								catch(Exception e)
								{
									BugSenseHandler.log("updateEvents", e);
									e.printStackTrace();
								}

								for (int i = 0; i < EventCount; i++) 
								{
									JSONObject CurrentEvent = null;
									ContentValues values = new ContentValues(2);
									try 
									{

										CurrentEvent = Events.getJSONObject(i);

										//Log.i("Event",CurrentEvent.toString(3));

										values.put("EVID",CurrentEvent.getString("evid"));
										values.put("device", CurrentEvent.getJSONObject("device").getString("text"));
										values.put("summary", CurrentEvent.getString("summary"));
										values.put("eventState", CurrentEvent.getString("eventState"));
										values.put("severity", CurrentEvent.getString("severity"));

										mDatabase.insert("events", null, values);
									} 
									catch (JSONException e) 
									{
										BugSenseHandler.log("updateEvents-DBLoop", e);
										//TODO We should tell the user about this or recover from it as they could miss an alert
									}
								}
							}

							finishedRefresh = true;
						}
					};
					RefreshThread.start();
				}
			}).start();
		}

		/**
		 * Add a word to the dictionary.
		 * @return rowId or -1 if failed
		 */
		/*public long addWord(String word, String definition) {
			 ContentValues initialValues = new ContentValues();
			 initialValues.put(KEY_WORD, word);
			 initialValues.put(KEY_DEFINITION, definition);

			 return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
		 }*/

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
				//Oh well
			}

			db.execSQL("CREATE TABLE \"events\" (\"EVID\" TEXT PRIMARY KEY  NOT NULL  UNIQUE , \"Count\" INTEGER, \"lastTime\" TEXT, \"device\" TEXT, \"summary\" TEXT, \"eventState\" TEXT, \"firstTime\" TEXT, \"severity\" TEXT)");
			db.execSQL("CREATE TABLE \"devices\" (\"rhybuddDeviceID\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,\"productionState\" TEXT,\"ipAddress\" INTEGER,\"name\" TEXT,\"uid\" TEXT, \"infoEvents\" INTEGER DEFAULT (0) ,\"debugEvents\" INTEGER DEFAULT (0) ,\"warningEvents\" INTEGER DEFAULT (0) ,\"errorEvents\" INTEGER DEFAULT (0) ,\"criticalEvents\" INTEGER DEFAULT (0) )");
			onCreate(db);
		}
	}
}
