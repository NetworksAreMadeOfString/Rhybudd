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
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import java.util.ArrayList;
import java.util.List;
import static android.nfc.NdefRecord.createMime;


public class ViewZenossDeviceActivity extends FragmentActivity implements NfcAdapter.CreateNdefMessageCallback
{
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link android.support.v4.view.ViewPager} that will host the section contents.
     */
    private static int UI_POPULATE = 0;
    ViewPager mViewPager;
    String currentDeviceID = "";
    String currentDeviceName = "";
    ArrayList<String> DeviceNames;
    ArrayList<String> DeviceIDs;
    int currentIndex = 0;
    ActionBar actionbar;
    Handler triggerUIHandler = null;
    /*Tag intentTag;
    NdefMessage[] msgs;
    Parcelable[] rawMsgs = null;*/
    NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_zenoss_event_activity);

        triggerUIHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                PopulatePager();
            }
        };

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

        try
        {
            // Check for available NFC Adapter
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (null != mNfcAdapter)
            {
                // Register callback
                mNfcAdapter.setNdefPushMessageCallback(this, this);
            }
        }
        catch(Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "NFC", e);
        }

        try
        {
            currentDeviceID = getIntent().getStringExtra(ViewZenossDeviceFragment.ARG_UID);
            //currentDeviceName = getIntent().getStringExtra(ViewZenossDeviceFragment.ARG_HOSTNAME);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "currentDeviceID", e);
        }

        try
        {
            DeviceNames = getIntent().getStringArrayListExtra(ViewZenossDeviceFragment.ARG_DEVICENAMES);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "DeviceNames", e);
        }

        try
        {
            DeviceIDs = getIntent().getStringArrayListExtra(ViewZenossDeviceFragment.ARG_DEVICEIDS);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "DeviceIDs", e);
        }
        //Log.e("ViewZenossDevice", "Checking...");

        if(null == DeviceNames || DeviceNames.size() == 0 || null == currentDeviceID || currentDeviceID.equals(""))
        {
            //Log.e("ViewZenossDevice", "No device names, fancy that!");
            try
            {
                /*Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                currentDeviceID = "/zport/dmd/Devices/" + msg.getRecords()[0].getPayload().toString();*/

                (new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            RhybuddDataSource datasource = new RhybuddDataSource(ViewZenossDeviceActivity.this);
                            datasource.open();
                            List<ZenossDevice> listOfDevices = datasource.GetRhybuddDevices();
                            datasource.close();

                            DeviceNames = new ArrayList<String>();
                            DeviceIDs = new ArrayList<String>();

                            for(ZenossDevice device : listOfDevices)
                            {
                                DeviceNames.add(device.getname());
                                DeviceIDs.add(device.getuid());
                            }

                            processIntent(getIntent());
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            try
            {
                triggerUIHandler.sendEmptyMessage(UI_POPULATE);
            }
            catch (Exception e)
            {
                BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "triggerUIHandler UI_POPULATE", e);
            }
        }
    }

    private void PopulatePager()
    {
        try
        {
            int i = 0;

            for(String str : DeviceIDs)
            {
                //Log.e("PopularPager", "Comparing " + str + " to " + currentDeviceID + " (" + Integer.toString(currentIndex) + ")");

                if(str.equals(currentDeviceID))
                {
                    //currentIndex = i;
                    break;
                }

                currentIndex++;
            }

            // Create the adapter that will return a fragment for each of the three
            // primary sections of the app.
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            // Set up the ViewPager with the sections adapter.
            mViewPager = (ViewPager) findViewById(R.id.pager);
            mViewPager.setAdapter(mSectionsPagerAdapter);

            //Log.e("PopularPager", "Setting current index to " + Integer.toString(currentIndex));
            mViewPager.setCurrentItem(currentIndex);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "PopulatePager", e);

            try
            {
                Toast.makeText(ViewZenossDeviceActivity.this,"There was an error populating the ViewPager",Toast.LENGTH_LONG).show();
            }
            catch (Exception e1)
            {
                BugSenseHandler.sendExceptionMessage("ViewZenossDevice", "PopulatePager Toast", e1);
            }
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //We handle the intents in the onCreate
        /*if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
        {
            processIntent(getIntent());
        }*/
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent)
    {
        try
        {
            //String UID = currentDeviceID.replace("/zport/dmd/Devices/","");

            int item = 0;
            if(mViewPager.getCurrentItem() > 0)
                item = mViewPager.getCurrentItem();

            String UID = DeviceIDs.get(item).replace("/zport/dmd/Devices/","");

            //Log.e("createNdefMessage",UID);

            NdefMessage msg = new NdefMessage(
                    new NdefRecord[] { createMime(
                            "application/vnd.net.networksaremadeofstring.rhybudd.devicepage", UID.getBytes())
                            //,NdefRecord.createApplicationRecord("com.example.android.beam")
                    });
            return msg;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceActivity","processIntent",e);
            return null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        processIntent(getIntent());
    }

    void processIntent(Intent intent)
    {
        try
        {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(null != rawMsgs && rawMsgs.length > 0)
            {
                NdefMessage msg = (NdefMessage) rawMsgs[0];

                /*for(NdefRecord nR : msg.getRecords())
                {
                    String tmpString = new String(nR.getPayload());
                    Log.e("Found: ", tmpString);
                }*/

                currentDeviceID = "/zport/dmd/Devices/" + new String(msg.getRecords()[0].getPayload());

            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceActivity","processIntent",e);
            try
            {
                Toast.makeText(this, "There was a problem trying to find the device ID that was beamed", Toast.LENGTH_SHORT).show();
            }
            catch (Exception e1)
            {
                BugSenseHandler.sendExceptionMessage("ViewZenossDeviceActivity","processIntent Toast",e1);
            }
        }
        finally
        {
            try
            {
                triggerUIHandler.sendEmptyMessage(UI_POPULATE);
            }
            catch (Exception e)
            {
                BugSenseHandler.sendExceptionMessage("ViewZenossDeviceActivity","processIntent",e);
            }
        }
    }

    /**
     *
     */
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
            currentDeviceID = DeviceIDs.get(position);
            currentDeviceName = DeviceNames.get(position);
            args.putString(ViewZenossDeviceFragment.ARG_UID, DeviceIDs.get(position));
            args.putString(ViewZenossDeviceFragment.ARG_HOSTNAME, DeviceNames.get(position));
            fragment.setArguments(args);
            fragment.setHasOptionsMenu(true);
            return fragment;
        }

        @Override
        public int getCount()
        {
            if(null != DeviceIDs)
            {
                return DeviceIDs.size();
            }
            else
            {
                return 0;
            }
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            if(null != DeviceNames)
            {
                return DeviceNames.get(position);
            }
            else
            {
                return "";
            }
        }
    }

}
