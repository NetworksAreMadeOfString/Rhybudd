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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import com.bugsense.trace.BugSenseHandler;

public class RhybuddOpenHelper extends SQLiteOpenHelper
{
    private static final int DATABASE_VERSION = 9;
    public static final String DATABASE_NAME = "rhybudd3Cache";


    RhybuddOpenHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        try
        {
            db.execSQL("CREATE TABLE \"events\" (\"evid\" TEXT PRIMARY KEY  NOT NULL, " +
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

            db.execSQL("CREATE TABLE \"devices\" (\"rhybuddDeviceID\" INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL," +
                    "\"productionState\" TEXT," +
                    "\"ipAddress\" INTEGER," +
                    "\"name\" TEXT," +
                    "\"uid\" TEXT, " +
                    "\"os\" TEXT, " +
                    "\"infoEvents\" INTEGER DEFAULT (0) ," +
                    "\"debugEvents\" INTEGER DEFAULT (0) ," +
                    "\"warningEvents\" INTEGER DEFAULT (0) ," +
                    "\"errorEvents\" INTEGER DEFAULT (0) ," +
                    "\"criticalEvents\" INTEGER DEFAULT (0) )");
        }
        catch(SQLiteException s)
        {
            //BugSenseHandler.log("Database-onCreate", s);
            s.printStackTrace();
        }
        catch(Exception e)
        {
            //BugSenseHandler.log("Database-onCreate", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //Log.v("onUpgrade", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        try
        {
            db.execSQL("DROP TABLE IF EXISTS events");
            db.execSQL("DROP TABLE IF EXISTS devices");
        }
        catch(Exception e)
        {
            BugSenseHandler.sendExceptionMessage("RhybudOpenHelper", "onUpgrade", e);
            e.printStackTrace();
        }

        onCreate(db);
    }
}
