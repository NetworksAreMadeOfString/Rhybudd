package net.networksaremadeofstring.rhybudd;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gareth on 04/06/13.
 */
public class ViewZenossDeviceListActivity extends FragmentActivity implements ViewZenossDeviceListFragment.Callbacks
{
    private boolean mTwoPane;
    String selectedDevice = "";
    String selectedUID = "";
    final static int LAUNCHDETAILACTIVITY = 2;
    ActionBar ab;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        BugSenseHandler.initAndStartSession(ViewZenossDeviceListActivity.this, "44a76a8c");

        setContentView(R.layout.view_zenoss_device_list);


        ab = getActionBar();
        ab.setTitle("Rhybudd");
        ab.setSubtitle("Zenoss Infrastructure Details");
        ab.setDisplayHomeAsUpEnabled(true);

        if (findViewById(R.id.device_detail_container) != null)
        {
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ViewZenossDeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.device_list)).setActivateOnItemClick(true);

            /*if(savedInstanceState == null)
            {
                WelcomeFragment fragment = new WelcomeFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.trap_detail_container, fragment).commit();
            }*/
        }
     }

    /*@Override
    public void onItemSelected(ZenossDevice ZenossDevice)*/
    @Override
    public void onItemSelected(ZenossDevice device, ArrayList<String> DeviceNames, ArrayList<String> DeviceIDs)
    {
        selectedUID = device.getuid();
        selectedDevice = device.getname();

        if (mTwoPane)
        {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString(ViewZenossDeviceFragment.ARG_HOSTNAME, selectedDevice);
            arguments.putString(ViewZenossDeviceFragment.ARG_UID, selectedUID);
            arguments.putBoolean(ViewZenossDeviceFragment.ARG_2PANE, true);
            arguments.putStringArrayList(ViewZenossDeviceFragment.ARG_DEVICENAMES,DeviceNames);
            arguments.putStringArrayList(ViewZenossDeviceFragment.ARG_DEVICEIDS,DeviceIDs);


            ViewZenossDeviceFragment fragment = new ViewZenossDeviceFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction().replace(R.id.device_detail_container, fragment).commit();
            ab.setSubtitle("Viewing " + selectedDevice);

        }
        else
        {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ViewZenossDeviceActivity.class);
            detailIntent.putExtra(ViewZenossDeviceFragment.ARG_HOSTNAME, selectedDevice);
            detailIntent.putExtra(ViewZenossDeviceFragment.ARG_UID, selectedUID);
            detailIntent.putExtra(ViewZenossDeviceFragment.ARG_2PANE, false);
            detailIntent.putStringArrayListExtra(ViewZenossDeviceFragment.ARG_DEVICENAMES,DeviceNames);
            detailIntent.putStringArrayListExtra(ViewZenossDeviceFragment.ARG_DEVICEIDS,DeviceIDs);
            startActivityForResult(detailIntent,LAUNCHDETAILACTIVITY);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.devices, menu);
        return true;
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

            case R.id.search:
            {
                onSearchRequested();
                return true;
            }

            case R.id.refresh:
            {
                ViewZenossDeviceListFragment fragment = (ViewZenossDeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.device_list);
                fragment.Refresh();
                return true;
            }

        }
        return false;
    }
}
