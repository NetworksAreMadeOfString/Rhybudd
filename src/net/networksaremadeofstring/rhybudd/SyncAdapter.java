package net.networksaremadeofstring.rhybudd;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;

import java.util.List;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter
{

    // Global variables
    // Define a variable to contain a content resolver instance
    ContentResolver mContentResolver;
    Context mContext;
    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        mContext = context;
    }


    /**
     * Set up the sync adapter. This form of the
     * constructor maintains compatibility with Android 3.0
     * and later platform versions
     */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it
         * from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        mContext = context;
    }

    /*
    * Specify the code you want to run in the sync adapter. The entire
    * sync adapter runs in a background thread, so you don't have to set
    * up your own background processing.
    */
    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult)
    {
        Log.e("onPerformSync", "----- PERFORMING SYNC!! ---------");

        try
        {
            ZenossAPI API;
            if(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
            {
                API = new ZenossAPIZaas();
            }
            else
            {
                API = new ZenossAPICore();
            }

            ZenossCredentials credentials = new ZenossCredentials(mContext);
            API.Login(credentials);

            List<ZenossDevice> listOfZenossDevices = API.GetRhybuddDevices();

            if(listOfZenossDevices != null && listOfZenossDevices.size() > 0)
            {
                RhybuddDataSource datasource = new RhybuddDataSource(mContext);
                datasource.open();
                datasource.UpdateRhybuddDevices(listOfZenossDevices);
                datasource.close();

                ZenossAPI.updateLastChecked(mContext);
            }
            else
            {
                //TODO Send Warning
            }

        }
        catch (Exception e)
        {
            //TODO Send Warning
            //BugSenseHandler.sendExceptionMessage("SyncAdapter", "onPerformSync", e);
        }
    }

}