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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.backup.BackupManager;
import android.content.*;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import com.google.android.gcm.GCMRegistrar;

import java.lang.reflect.GenericArrayType;
import java.util.ArrayList;
import java.util.List;


public class ViewZenossEventsListActivity extends FragmentActivity implements ViewZenossEventsListFragment.Callbacks, ViewZenossEventFragment.Callbacks
{
    private boolean mTwoPane;
    final static int LAUNCHDETAILACTIVITY = 2;
    final static int LAUNCHSETTINGS = 99;

    private static final int INFRASTRUCTURE = 0;
    private static final int MANAGEDATABASE = 1;
    private static final int GROUPS = 2;
    private static final int SETTINGS = 3;
    private static final int CONFIGURERHYBUDDPUSH = 4;
    private static final int HELP = 5;
    private static final int FEEDBACK = 6;
    private static final int SEARCH = 7;
    private static final int DIAGNOSTIC = 8;

    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "net.networksaremadeofstring.rhybudd.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "zenoss.com";
    // The account name
    public static final String ACCOUNT = "Zenoss Sync";
    // Instance fields
    Account mAccount;
    ContentResolver mResolver;
    SharedPreferences settings = null;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String[] mDrawerTitles;
    String regId = "";
    //String SenderID = "";
    boolean firstRun = false;
    AlertDialog alertDialog;
    int requestCode = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BugSenseHandler.initAndStartSession(ViewZenossEventsListActivity.this, "44a76a8c");

        setContentView(R.layout.view_zenoss_events_list);

        try
        {
            settings = PreferenceManager.getDefaultSharedPreferences(this);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onCreate",e);
        }

        try
        {
            getActionBar().setTitle("Zenoss Events List");
            getActionBar().setSubtitle(settings.getString("URL", ""));
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onCreate",e);
        }

        //SenderID = settings.getString("SenderID",ZenossAPI.SENDER_ID);

        if (findViewById(R.id.event_detail_container) != null)
        {
            try
            {
                mTwoPane = true;

                ((ViewZenossEventsListFragment) getSupportFragmentManager().findFragmentById(R.id.events_list)).setActivateOnItemClick(true);

                // In two-pane mode, list items should be given the
                // 'activated' state when touched.

                if(savedInstanceState == null)
                {
                    EventsListWelcomeFragment fragment = new EventsListWelcomeFragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.event_detail_container, fragment).commit();
                }
            }
            catch (Exception e)
            {
                BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onCreate twopane",e);
            }
        }

        try
        {
            ((ViewZenossEventsListFragment) getSupportFragmentManager().findFragmentById(R.id.events_list)).setHasOptionsMenu(true);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","fragment has options",e);
        }

        if((settings.getString("URL", "").equals("") || settings.getString("userName", "").equals("") || settings.getString("passWord", "").equals("")))
        {
            firstRun = true;
        }

        try
        {
            mDrawerTitles = getResources().getStringArray(R.array.drawer_array);
            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            mDrawerList = (ListView) findViewById(R.id.left_drawer);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerTitles));
            mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

            mDrawerToggle = new ActionBarDrawerToggle(
                    this,                  /* host Activity */
                    mDrawerLayout,         /* DrawerLayout object */
                    R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                    R.string.drawer_open,  /* "open drawer" description for accessibility */
                    R.string.drawer_close  /* "close drawer" description for accessibility */
            )
            {
                public void onDrawerClosed(View view)
                {
                    getActionBar().setTitle("Zenoss Events List");
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                public void onDrawerOpened(View drawerView)
                {
                    getActionBar().setTitle(getString(R.string.DrawerTitle));
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onCreate drawerlayout",e);
        }

        // Create the dummy account
        try
        {
            mAccount = CreateSyncAccount(this);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onCreate CreateSyncAccount",e);
        }
    }

    public static Account CreateSyncAccount(Context context)
    {
        try
        {
            // Create the account type and default account
            Account newAccount = new Account(ACCOUNT, ACCOUNT_TYPE);
            // Get an instance of the Android account manager
            AccountManager accountManager =(AccountManager) context.getSystemService(ACCOUNT_SERVICE);
            /*
             * Add the account and account type, no password or user data
             * If successful, return the Account object, otherwise report an error.
             */
            if (accountManager.addAccountExplicitly(newAccount, null, null))
            {
                /*
                 * If you don't set android:syncable="true" in
                 * in your <provider> element in the manifest,
                 * then call context.setIsSyncable(account, AUTHORITY, 1)
                 * here.
                 */
                //Log.e("CreateSyncAccount","Success!");
            }
            else
            {
                /*
                 * The account exists or some other error occurred. Log this, report it,
                 * or handle it internally.
                 */
                //Log.e("CreateSyncAccount","Fail!");
            }

            return newAccount;
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","CreateSyncAccount",e);
            return null;
        }
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id)
        {
            ProcessDrawerClick(position);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (mDrawerToggle.onOptionsItemSelected(item))
        {
            return true;
        }

        switch (item.getItemId())
        {
            case R.id.settings:
            {
                try
                {
                    Intent SettingsIntent = new Intent(ViewZenossEventsListActivity.this, SettingsFragment.class);
                    this.startActivityForResult(SettingsIntent, LAUNCHSETTINGS);
                    return true;
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onOptionsItemSelected settings",e);
                    return false;
                }
            }

            case R.id.search:
            {
                try
                {
                    onSearchRequested();
                    return true;
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onOptionsItemSelected search",e);
                }
            }
        }

        return false;
    }

    private void ProcessDrawerClick(int position)
    {
        switch(position)
        {
            case SETTINGS:
            {
                try
                {
                    Intent SettingsIntent = new Intent(ViewZenossEventsListActivity.this, SettingsFragment.class);
                    this.startActivityForResult(SettingsIntent, 99);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick settings",e);
                }

            }
            break;

            case CONFIGURERHYBUDDPUSH:
            {
                try
                {
                    Intent PushSettingsIntent = new Intent(ViewZenossEventsListActivity.this, PushConfigActivity.class);
                    this.startActivityForResult(PushSettingsIntent, ZenossAPI.ACTIVITYRESULT_PUSHCONFIG);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick CONFIGURERHYBUDDPUSH",e);
                }
            }
            break;

            case HELP:
            {
                try
                {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("http://wiki.zenoss.org/Android"));
                    startActivity(i);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick HELP",e);
                }

            }
            break;

            case INFRASTRUCTURE:
            {
                try
                {
                    Intent DeviceList = new Intent(ViewZenossEventsListActivity.this, ViewZenossDeviceListActivity.class);
                    ViewZenossEventsListActivity.this.startActivity(DeviceList);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick INFRASTRUCTURE",e);
                }
            }
            break;

            case GROUPS:
            {
                try
                {
                    Intent GroupsIntent = new Intent(ViewZenossEventsListActivity.this, ViewZenossGroupsActivity.class);
                    ViewZenossEventsListActivity.this.startActivity(GroupsIntent);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick GROUPS",e);
                }
            }
            break;

            case MANAGEDATABASE:
            {
                try
                {
                    Intent MangeDBIntent = new Intent(ViewZenossEventsListActivity.this, ManageDatabase.class);
                    ViewZenossEventsListActivity.this.startActivity(MangeDBIntent);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick MANAGEDATABASE",e);
                }
            }
            break;

            case FEEDBACK:
            {
                try
                {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","Gareth@DataSift.com", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback suggestion for Rhybudd");
                    startActivity(Intent.createChooser(emailIntent, "Send feedback as email"));
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick FEEDBACK",e);

                    try
                    {
                        Toast.makeText(ViewZenossEventsListActivity.this, "There was a problem launching your email client.\n\nPlease email Gareth@DataSift.com with your feedback.", Toast.LENGTH_LONG).show();
                    }
                    catch (Exception e1)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick FEEDBACK Toast",e1);
                    }

                }
            }
            break;

            case SEARCH:
            {
                try
                {
                    onSearchRequested();
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick SEARCH",e);
                }
            }
            break;

            case DIAGNOSTIC:
            {
                try
                {
                    Intent DiagIntent = new Intent(ViewZenossEventsListActivity.this, DiagnosticActivity.class);
                    ViewZenossEventsListActivity.this.startActivity(DiagIntent);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick DIAGNOSTIC",e);
                }
            }
            break;
        }

        // update selected item and title, then close the drawer
        try
        {
            mDrawerList.setItemChecked(position, false);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick setitemchecked false",e);
        }

        try
        {
            mDrawerLayout.closeDrawer(mDrawerList);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","ProcessDrawerClick closeDrawer",e);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        try
        {
            //Can't hurt to try and start the service just in case
            startService(new Intent(this, ZenossPoller.class));
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onResume StartService",e);
        }

        try
        {
            //User is about to see the list of events - no need for them to hang around
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_POLLED_ALERTS);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_GCM_GENERIC);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onResume Clear Notifications",e);
        }

        try
        {
            //Might as well update GCM whilst we're here
            if(settings.contains(ZenossAPI.PREFERENCE_PUSH_ENABLED) && settings.getBoolean(ZenossAPI.PREFERENCE_PUSH_ENABLED,false))
            {
                //Log.e("onResume","Doing a GCM Registration");
                doGCMRegistration();
            }
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onResume doGCMRegistration",e);
        }

        //Refreshes are done in the fragment - Forced login done in the activity
        if(firstRun)
        {
            //We need to set this now as no doubt on resume gets called before onActivityResult
            firstRun = false;

            Intent SettingsIntent = new Intent(ViewZenossEventsListActivity.this, FirstRunSettings.class);
            SettingsIntent.putExtra("firstRun", true);
            ViewZenossEventsListActivity.this.startActivityForResult(SettingsIntent, requestCode);
        }
    }

    private void finishStart()
    {
        try
        {
            ViewZenossEventsListFragment listFrag = (ViewZenossEventsListFragment) getSupportFragmentManager().findFragmentById(R.id.events_list);
            listFrag.DBGetThread();
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","finishStart",e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode,resultCode,data);

        //Forces our onResume() function to do a DB call rather than a full HTTP request just cos we returned
        //from one of our subscreens
        //resumeOnResultPollAPI = false;
        BackupManager bm = null;
        try
        {
            bm = new BackupManager(this);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult BackupManager",e);
        }

        switch(requestCode)
        {
            case LAUNCHSETTINGS:
            {
                settings = PreferenceManager.getDefaultSharedPreferences(this);

                try
                {
                    Intent intent = new Intent(this, ZenossPoller.class);
                    intent.putExtra("settingsUpdate", true);
                    startService(intent);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult startService",e);
                }

                try
                {
                    bm.dataChanged();
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult bm.dataChanged()",e);
                }

                //SyncAdapter stuff
                if(null != mAccount)
                {
                    try
                    {
                        mResolver = getContentResolver();
                        Bundle bndle = new Bundle();
                        ContentResolver.addPeriodicSync(
                                mAccount,
                                AUTHORITY,
                                bndle,
                                86400);

                        ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, settings.getBoolean("refreshCache", true));

                        /*bndle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        bndle.putBoolean(
                                ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

                        Log.e("addPeriodicSync","Requesting a full sync!");
                        ContentResolver.requestSync(mAccount, AUTHORITY, bndle);*/
                    }
                    catch (Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult LAUNCHSETTINGS",e);
                    }
                }
                else
                {
                    //Log.e("addPeriodicSync","mAccount was null");
                }
            }
            break;

            case ZenossAPI.ACTIVITYRESULT_PUSHCONFIG:
            {
                try
                {
                    GCMRegistrar.unregister(this);
                    doGCMRegistration();
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult ACTIVITYRESULT_PUSHCONFIG",e);
                }
            }
            break;

            default:
            {
                if(resultCode == 1)
                {
                    try
                    {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("FirstRun", false);
                        editor.commit();
                    }
                    catch (Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult default",e);
                    }

                    //Also update our onResume helper bool although it should already be set
                    firstRun = false;
                    AlertDialog.Builder builder = null;
                    AlertDialog welcomeDialog = null;
                    try
                    {
                        builder = new AlertDialog.Builder(this);
                        builder.setMessage("Additional settings and functionality can be found by pressing the Action bar overflow.\r\n" +
                                "\r\nIf you experience issues please email;\r\nGareth@DataSift.com before leaving negative reviews.")
                                .setTitle("Welcome to Rhybudd!")
                                .setCancelable(true)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id)
                                    {
                                        finishStart();
                                    }
                                });
                        welcomeDialog = builder.create();
                    }
                    catch (Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult default",e);
                    }


                    try
                    {
                        welcomeDialog.show();
                    }
                    catch(Exception e)
                    {
                        finishStart();
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","OnActivityResult welcomedialog",e);
                    }

                    try
                    {
                        bm.dataChanged();
                    }
                    catch (Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult default",e);
                    }
                }
                else if(resultCode == 2)
                {
                    try
                    {
                        Toast.makeText(ViewZenossEventsListActivity.this, getResources().getString(R.string.FirstRunNeedSettings), Toast.LENGTH_LONG).show();
                    }
                    catch (Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onActivityResult resultcode 2",e);
                    }
                    finish();
                }
                //Who knows what happened here - quit
                else
                {
                    //Toast.makeText(ViewZenossEventsListActivity.this, getResources().getString(R.string.FirstRunNeedSettings), Toast.LENGTH_LONG).show();
                    //finish();
                }
            }
            break;
        }
    }

    @Override
    public void fetchError()
    {
        AlertDialog.Builder builder = null;

        try
        {
            builder = new AlertDialog.Builder(this);
            builder.setMessage("Unable to get any events from the DB or from the API.\nCheck settings?")
                    .setCancelable(false)
                    .setPositiveButton("Edit Settings", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            Intent SettingsIntent = new Intent(ViewZenossEventsListActivity.this, SettingsFragment.class);
                            startActivityForResult(SettingsIntent, 99);
                            alertDialog.cancel();
                        }
                    })
                    .setNeutralButton("Run Diagnostics", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            Intent DiagIntent = new Intent(ViewZenossEventsListActivity.this, DiagnosticActivity.class);
                            startActivity(DiagIntent);
                            alertDialog.cancel();
                        }
                    })
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            alertDialog.cancel();
                        }
                    });
            alertDialog = builder.create();
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","fetchError",e);
        }

        if(!isFinishing())
        {
            try
            {
                alertDialog.show();
            }
            catch(Exception e)
            {
                //BugSenseHandler.log("alertDialog", e);
                BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","fetcherror alertdialog",e);
            }
        }
    }

    @Override
    public void onItemAcknowledged(int position)
    {
        try
        {
            ViewZenossEventsListFragment listFrag = (ViewZenossEventsListFragment) getSupportFragmentManager().findFragmentById(R.id.events_list);
            listFrag.onItemAcknowledged(position);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onItemAcknowledged",e);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        try
        {
            if(null != alertDialog)
                alertDialog.dismiss();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onpause",e);
        }

    }

    @Override
    public void onItemSelected(final ZenossEvent event, final int position)
    {
        try
        {
            if (mTwoPane)
            {
                // In two-pane mode, show the detail view in this activity by
                // adding or replacing the detail fragment using a
                // fragment transaction.
                Bundle arguments = new Bundle();
                arguments.putString("EventID", event.getEVID());
                arguments.putString("EventTitle", event.getDevice());
                arguments.putInt("EventCount", event.getCount());
                arguments.putBoolean(ViewZenossDeviceFragment.ARG_2PANE, true);
                arguments.putInt("position",position);
                ViewZenossEventFragment fragment = new ViewZenossEventFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction().replace(R.id.event_detail_container, fragment).commit();
            }
            else
            {
                AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
                alertbox.setTitle("Event Management");
                alertbox.setMessage("What would you like to do?");

                alertbox.setPositiveButton("Acknowledge", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        //AcknowledgeSingleEvent(position);
                        ViewZenossEventsListFragment listFrag = (ViewZenossEventsListFragment) getSupportFragmentManager().findFragmentById(R.id.events_list);
                        //Log.e("Activity","ack with position " + Integer.toString(position));
                        listFrag.acknowledgeSingleEvent(position);
                    }
                });

                alertbox.setNeutralButton("View Details", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                        Intent ViewEventIntent = new Intent(ViewZenossEventsListActivity.this, ViewZenossEventActivity.class);
                        ViewEventIntent.putExtra("EventID", event.getEVID());
                        ViewEventIntent.putExtra("position",position);
                        ViewEventIntent.putExtra("EventTitle", event.getDevice());
                        ViewEventIntent.putExtra("EventCount", event.getCount());
                        ArrayList<String> EventNames = new ArrayList<String>();
                        ArrayList<String> EVIDs = new ArrayList<String>();

                        ViewZenossEventsListFragment listFrag = (ViewZenossEventsListFragment) getSupportFragmentManager().findFragmentById(R.id.events_list);
                        List<ZenossEvent> listOfZenossEvents = listFrag.getListOfEvents();

                        for(ZenossEvent evt : listOfZenossEvents)
                        {
                            EventNames.add(evt.getDevice());
                            EVIDs.add(evt.getEVID());
                        }

                        ViewEventIntent.putStringArrayListExtra("eventnames",EventNames);
                        ViewEventIntent.putStringArrayListExtra("evids",EVIDs);
                        ViewZenossEventsListActivity.this.startActivity(ViewEventIntent);
                    }
                });

                alertbox.setNegativeButton("Nothing", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface arg0, int arg1)
                    {
                    }
                });
                alertbox.show();
            }
        }
        catch (Exception e)
        {
            //Damn I'm lazy
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","onItemSelected",e);
        }
    }

    private void doGCMRegistration()
    {
        try
        {
            GCMRegistrar.checkDevice(this);
            GCMRegistrar.checkManifest(this);
            regId = GCMRegistrar.getRegistrationId(this);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","doGCMRegistration()",e);
        }

        try
        {
            if (regId.equals(""))
            {
                //Log.e("GCM", "Registering");
                GCMRegistrar.register(this, settings.getString(ZenossAPI.PREFERENCE_PUSH_SENDERID,ZenossAPI.SENDER_ID));
            }
        }
        catch (Exception e)
        {
            //TODO we should probably do something about this
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","doGCMRegistration()",e);
        }


        try
        {
            (new Thread()
            {
                public void run()
                {
                    /*if(!ZenossAPI.registerPushKey(PushKey,regId,ZenossAPI.md5(Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID))))
                    {
                        try
                        {
                            runOnUiThread(new Runnable()
                            {
                                public void run() {
                                    Toast.makeText(ViewZenossEventsListActivity.this, getResources().getString(R.string.ErrorRegisterGCM), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                        catch (Exception e)
                        {
                            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","doGCMRegistration runnable",e);
                        }
                    }*/
                    try
                    {
                        ZenossCredentials credentials = new ZenossCredentials(ViewZenossEventsListActivity.this);
                        ZenossAPI API;

                        if (PreferenceManager.getDefaultSharedPreferences(ViewZenossEventsListActivity.this).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                        {
                            API = new ZenossAPIZaas();
                        }
                        else
                        {
                            API = new ZenossAPICore();
                        }

                        if(API.Login(credentials))
                        {
                            if(API.RegisterWithZenPack(ZenossAPI.md5(Settings.Secure.getString(ViewZenossEventsListActivity.this.getContentResolver(),Settings.Secure.ANDROID_ID)),regId))
                            {
                                //Nothing we are happy
                            }
                            else
                            {
                                try
                                {
                                    runOnUiThread(new Runnable()
                                    {
                                        public void run() {
                                            Toast.makeText(ViewZenossEventsListActivity.this, getResources().getString(R.string.ErrorRegisterGCM), Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }
                                catch (Exception e)
                                {
                                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","doGCMRegistration runnable",e);
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","doGCMRegistration registerzenpack",e);
                    }
                }
            }).start();
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListActivity","doGCMRegistration",e);
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }
}
