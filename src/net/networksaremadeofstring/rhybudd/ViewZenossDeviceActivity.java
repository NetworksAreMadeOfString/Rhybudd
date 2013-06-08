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

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;

import java.util.ArrayList;

/**
 * Created by Gareth on 02/06/13.
 */
public class ViewZenossDeviceActivity extends FragmentActivity
{
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    String currentDeviceID = "";
    String currentDeviceName = "";
    ArrayList<String> DeviceNames;
    ArrayList<String> DeviceIDs;
    int currentIndex = 0;
    ActionBar actionbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_zenoss_event_activity);

        try
        {
            actionbar = getActionBar();
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeButtonEnabled(true);
            actionbar.setSubtitle(getResources().getString(R.string.ViewDeviceActivitySubtitle));
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "OnCreate", e);
        }

        currentDeviceID = getIntent().getStringExtra(ViewZenossDeviceFragment.ARG_UID);
        currentDeviceName = getIntent().getStringExtra(ViewZenossDeviceFragment.ARG_HOSTNAME);

        DeviceNames = getIntent().getStringArrayListExtra(ViewZenossDeviceFragment.ARG_DEVICENAMES);
        DeviceIDs = getIntent().getStringArrayListExtra(ViewZenossDeviceFragment.ARG_DEVICEIDS);

        int i = 0;

        for(String str : DeviceIDs)
        {
            Log.e("test","Comparing " + str + " to " + currentDeviceID);
            if(str.equals(currentDeviceID))
            {
                Log.e("test", "----------------FOUND---------------");
                currentIndex = i;
                break;
            }
            i++;
        }

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(currentIndex);

    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            Fragment fragment = new ViewZenossDeviceFragment();
            Bundle args = new Bundle();
            args.putString(ViewZenossDeviceFragment.ARG_UID, DeviceIDs.get(position));
            args.putString(ViewZenossDeviceFragment.ARG_HOSTNAME, DeviceNames.get(position));
            fragment.setArguments(args);
            fragment.setHasOptionsMenu(true);
            return fragment;
        }

        @Override
        public int getCount()
        {
            return DeviceIDs.size();
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            /*Locale l = Locale.getDefault();
            switch (position)
            {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;*/
            return DeviceNames.get(position);
        }
    }

    /*@Override
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
    }*/
}
