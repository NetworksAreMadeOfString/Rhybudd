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
import android.support.v4.app.*;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;

import com.bugsense.trace.BugSenseHandler;
import java.util.ArrayList;

public class ViewZenossEventActivity extends FragmentActivity implements ViewZenossEventFragment.Callbacks
{
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    String currentEVID = "";
    ArrayList<String> EventNames;
    ArrayList<String> EVIDs;
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
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventActivity", "OnCreate", e);
        }

        currentEVID = getIntent().getStringExtra("EventID");

        EventNames = getIntent().getStringArrayListExtra("eventnames");
        EVIDs = getIntent().getStringArrayListExtra("evids");

        int i = 0;

        try
        {
            for(String str : EVIDs)
            {
                if(str.equals(currentEVID))
                    currentIndex = i;

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
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventActivity","OnCreate",e);
        }

    }

    @Override
    public void onItemAcknowledged(int position)
    {
        //TODO Set the intent on exit so we know to do a refresh
        //Might not be neccessary because the ACK should be in the DB anyway
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            Fragment fragment = new ViewZenossEventFragment();
            Bundle args = new Bundle();
            args.putString("EventID", EVIDs.get(position));
            fragment.setArguments(args);
            fragment.setHasOptionsMenu(true);
            return fragment;
        }

        @Override
        public int getCount()
        {
            if(null != EVIDs)
            {
                return EVIDs.size();
            }
            else
            {
                return 0;
            }
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            if(null != EventNames)
            {
                return EventNames.get(position);
            }
            else
            {
                return "";
            }
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
