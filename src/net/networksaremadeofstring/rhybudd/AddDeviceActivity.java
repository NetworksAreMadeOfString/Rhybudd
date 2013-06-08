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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.bugsense.trace.BugSenseHandler;

public class AddDeviceActivity extends FragmentActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BugSenseHandler.initAndStartSession(AddDeviceActivity.this, "44a76a8c");

        setContentView(R.layout.add_device_activity);

        try
        {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
            getActionBar().setSubtitle(getString(R.string.AddDeviceTitle));
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("AddDeviceActivity", "OnCreate", e);
        }

        try
        {
            AddDeviceFragment fragment = new AddDeviceFragment();
            Bundle bndle = new Bundle();
            bndle.putBoolean(AddDeviceFragment.TWOPANEINDICATOR,false);

            fragment.setArguments(bndle);
            getSupportFragmentManager().beginTransaction().replace(R.id.device_detail_container, fragment).commit();
        }
        catch(Exception e)
        {
            BugSenseHandler.sendExceptionMessage("AddDeviceActivity","onCreate",e);
        }
    }
}
