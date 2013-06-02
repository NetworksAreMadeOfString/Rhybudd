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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.bugsense.trace.BugSenseHandler;

/**
 * Created by Gareth on 31/05/13.
 */
public class ZenossCredentials
{
    String UserName = "";
    String Password = "";
    String URL = "";
    String BAUser = "";
    String BAPassword = "";
    Boolean Zaas = false;

    public ZenossCredentials(String username, String password, String url, String bauser, String bapassword)
    {
        this.UserName = username;
        this.Password = password;
        this.URL = url;
        this.BAUser = bauser;
        this.BAPassword = bapassword;
    }

    public ZenossCredentials(Context context)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        this.UserName = settings.getString("userName", "");
        this.Password = settings.getString("passWord", "");
        this.URL = settings.getString("URL", "");
        this.BAUser = settings.getString("BAUser", "");
        this.BAPassword = settings.getString("BAPassword", "");

        Log.e("ZenossCredentials", UserName);
        Log.e("ZenossCredentials", Password);
        Log.e("ZenossCredentials", URL);
        Log.e("ZenossCredentials", BAUser);
        Log.e("ZenossCredentials", BAPassword);
    }

    public ZenossCredentials(String username, String password, String url)
    {
        this.UserName = username;
        this.Password = password;
        this.URL = url;
    }

    public void saveCredentials(Context context)
    {
        try
        {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(ZenossAPI.PREFERENCE_USERNAME, this.UserName);
            editor.putString(ZenossAPI.PREFERENCE_PASSWORD, this.Password);
            editor.putString(ZenossAPI.PREFERENCE_URL, this.URL);
            editor.putString(ZenossAPI.PREFERENCE_BASIC_AUTH_USER, this.BAUser);
            editor.putString(ZenossAPI.PREFERENCE_BASIC_AUTH_PASSWORD, this.BAPassword);
            editor.putBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,this.Zaas);
            editor.commit();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ZenossCredentials", "saveCredentials", e);
        }
    }
}
