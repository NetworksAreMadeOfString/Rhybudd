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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.bugsense.trace.BugSenseHandler;

public class SettingsFragment extends PreferenceActivity 
{
	SharedPreferences prefs;

	@Override
    public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        /*if(getIntent().getBooleanExtra(ZenossAPI.SETTINGS_PUSH,false))
        {*/
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            addPreferencesFromResource(R.xml.preferences);
       /* }
        else
        {
            //PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            //prefs = PreferenceManager.getDefaultSharedPreferences(this);
            addPreferencesFromResource(R.xml.preferences);
        }*/

        try
        {
            getActionBar().setTitle(getString(R.string.SettingsTitle));
            getActionBar().setSubtitle(getString(R.string.SettingsSubTitle));
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("SettingsFragment", "onCreate", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                finish();
                return true;
            }

            default:
            {
                return false;
            }
        }
    }
}
