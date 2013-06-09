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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RhybuddDataSource
{
    private SQLiteDatabase database;
    private RhybuddOpenHelper dbHelper;

    private String[] EventColumns = {"evid",
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
            "ownerid"};


    public RhybuddDataSource(Context context)
    {
        dbHelper = new RhybuddOpenHelper(context);
    }

    public void open() throws SQLException
    {
        database = dbHelper.getWritableDatabase();
    }

    public void close()
    {
        dbHelper.close();
    }

    public boolean ackEvent(String EVID)
    {
        ContentValues values = new ContentValues();
        values.put("eventState","Acknowledged");

        int rows = database.update ("events", values,"evid = ?",new String[] {EVID});

        if(rows > 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean ackAllEvents(List<String> EventIDs)
    {
        ContentValues values = new ContentValues();
        values.put("eventState","Acknowledged");
        String EventIDsAsString = "";
        for(String evt : EventIDs)
        {
            EventIDsAsString += "\""+evt+"\"" +",";
        }

        EventIDsAsString = EventIDsAsString.substring(0,EventIDsAsString.length() -1);
        Log.e("EventIDsAsString", EventIDsAsString);
        int rows = database.update ("events", values,"evid in ("+EventIDsAsString+")",null);
        Log.e("Rows",Integer.toString(rows));
        if(rows > 0)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean addEvent(ZenossEvent event)
    {
        ContentValues values = new ContentValues();

        values.put("evid",event.getEVID());
        values.put("count",event.getCount());
        values.put("prodState",event.getProdState());
        values.put("firstTime",event.getfirstTime());
        values.put("severity",event.getSeverity());
        values.put("component_text",event.getComponentText());
        values.put("component_uid",event.getComponentUID());
        values.put("summary",event.getSummary());
        values.put("eventState",event.getEventState());
        values.put("device",event.getDevice());
        values.put("eventClass",event.geteventClass());
        values.put("lastTime",event.getlastTime());
        values.put("ownerid",event.getownerID());

        //long insertId = database.insert("events", null, values);
        long insertId = database.insertWithOnConflict ("events", null, values,SQLiteDatabase.CONFLICT_REPLACE);

        if(insertId > -1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public List<ZenossDevice> SearchRhybuddDevices(String Query)
    {
        String Filter = "name like \"%"+Query.replaceAll(" ", "%")+"%\"";

        Cursor dbResults = database.query("devices", new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents","os"},  Filter, 	null, 					null, 			null, 			null);

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
                            dbResults.getString(4),
                            dbResults.getString(10)));
                }
                catch(Exception e)
                {
                    //BugSenseHandler.log("DB-GetRhybuddDevices", e);
                }
            }

            if(dbResults != null)
                dbResults.close();

            return ZenossDevices;
        }
        else
        {
            if(dbResults != null)
                dbResults.close();
            return null;
        }
    }

    public List<ZenossEvent> GetRhybuddEvents()
    {
        List<ZenossEvent> ZenossEvents = new ArrayList<ZenossEvent>();


        Cursor cursor = database.query("events", EventColumns,  null, 	null, 					null, 			null, 			"severity DESC");

        cursor.moveToFirst();

        while (!cursor.isAfterLast())
        {
            try
            {
                ZenossEvents.add(new ZenossEvent(cursor.getString(0),
                    cursor.getInt(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                    cursor.getString(7),
                    cursor.getString(8),
                    cursor.getString(9),
                    cursor.getString(10),
                    cursor.getString(11),
                    cursor.getString(12))
                );
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            cursor.moveToNext();
        }

        return ZenossEvents;
    }


    public List<ZenossDevice> GetRhybuddDevices()
    {
        Cursor dbResults = database.query("devices", new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents","os"},  null, 	null, 					null, 			null, 			null);

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
                            dbResults.getString(4),
                            dbResults.getString(10)));
                }
                catch(Exception e)
                {
                    //BugSenseHandler.log("DB-GetRhybuddDevices", e);
                }
            }

            if(dbResults != null)
                dbResults.close();

            return ZenossDevices;
        }
        else
        {
            if(dbResults != null)
                dbResults.close();
            return null;
        }
    }


    public void UpdateRhybuddEvents(final List<ZenossEvent> ZenossEvents)
    {
        int EventCount = 0;

        if(ZenossEvents != null)
            EventCount = ZenossEvents.size();

        try
        {
            database.beginTransaction();
            database.delete("events", null, null);

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

                    //Log.i("DB", "Writing " + CurrentEvent.getEVID() + " to the DB");

                    database.insert("events", null, values);
                    //Log.i("DB","Done writing " + CurrentEvent.getEVID() + " to the DB");
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    //This could get a little excessive
                    //BugSenseHandler.log("Database-refreshEvents", e);
                }
            }
            database.setTransactionSuccessful();
        }
        catch(Exception e)
        {
            //This could get a little excessive
            //BugSenseHandler.log("Database-refreshDevices", e);
        }
        finally
        {
            database.endTransaction();
        }
    }

    public void UpdateRhybuddDevices(List<ZenossDevice> ZenossDevices)
    {
        int DeviceCount = 0;

        if(ZenossDevices != null)
            DeviceCount = ZenossDevices.size();

        try
        {
            database.beginTransaction();
            database.delete("devices", null, null);

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

                    database.insert("devices", null, values);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            database.setTransactionSuccessful();
        }
        catch(Exception e)
        {
            //This could get a little excessive
            //BugSenseHandler.log("Database-UpdateRhybuddDevices", e);
        }
        finally
        {
            database.endTransaction();
        }
    }

    public void FlushDB()
    {
        try
        {
            database.beginTransaction();
            database.delete("events", null, null);
            database.delete("devices", null, null);
            database.setTransactionSuccessful();
        }
        catch(Exception e)
        {
            //BugSenseHandler.log("Database-Flush", e);
            e.printStackTrace();
        }
        finally
        {
            if(database != null)
                database.endTransaction();
        }
    }

    public ZenossDevice getDevice(String UID)
    {
        //						 query(String table, 	String[] columns, 	                                             String selection, 	String[] selectionArgs, String groupBy, String having, String orderBy)
        Cursor cursor = database.query("devices",       new String[]{"rhybuddDeviceID","productionState","uid","name","os"},  "uid = '"+UID+"'", 	            null, 					null, 			null, 			null);

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


                ZenossDevice returnDevice = new ZenossDevice(cursor.getString(1),cursor.getInt(2), events, cursor.getString(3),cursor.getString(4),cursor.getString(5));
                cursor.close();
                return returnDevice;
            }
            catch(Exception e)
            {
                if(cursor != null)
                    cursor.close();

                return null;
            }
        }
        else
        {
            cursor.close();
            return null;
        }
    }
}
