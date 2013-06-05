package net.networksaremadeofstring.rhybudd;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
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
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "OnCreate", e);
        }

        currentDeviceID = getIntent().getStringExtra("EventID");

        DeviceNames = getIntent().getStringArrayListExtra("eventnames");
        DeviceIDs = getIntent().getStringArrayListExtra("evids");

        int i = 0;

        for(String str : DeviceIDs)
        {
            if(str.equals(currentDeviceID))
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
            args.putString("DeviceID", DeviceIDs.get(position));
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
}
