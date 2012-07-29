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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsFragment extends PreferenceActivity 
{
	SharedPreferences prefs;
   /* @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        // Add a button to the header list.
        if (hasHeaders()) 
        {
            Button button = new Button(this);
            button.setText("Some action");
            setListFooter(button);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) 
    {
        loadHeadersFromResource(R.xml.preferences, target);
    }
    */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //boolean autoStart = prefs.getBoolean("pref_boot_startup", true);
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

    }
	
	/*@Override
	public void onContentChanged ()
	{
		if(prefs.getBoolean(key, defValue))
	}*/
}
