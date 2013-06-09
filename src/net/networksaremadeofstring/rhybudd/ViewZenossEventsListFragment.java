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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.*;
import android.widget.ListView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gareth on 09/06/13.
 */
public class ViewZenossEventsListFragment extends ListFragment
{
    private static final int EVENTSLISTHANDLER_ERROR = 0;
    private static final int EVENTSLISTHANDLER_SUCCESS = 1;
    private static final int EVENTSLISTHANDLER_DIALOGUPDATE = 2;
    private static final int EVENTSLISTHANDLER_DB_EMPTY = 3;
    private static final int EVENTSLISTHANDLER_TOTAL_FAILURE = 4;
    private static final int EVENTSLISTHANDLER_NO_EVENTS = 5;
    private static final int EVENTSLISTHANDLER_SERVICE_FAILURE = 6;
    private static final int EVENTSLISTHANDLER_REDOREFRESH = 7;

    private static final int ACKEVENTHANDLER_SUCCESS = 9;
    private static final int ACKEVENTHANDLER_FAILURE = 10;
    private static final int ACKEVENTHANDLER_PROGRESS = 11;

    ZenossAPI API = null;
    List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
    ZenossPoller mService;
    boolean mBound = false;
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private Callbacks mCallbacks = sDummyCallbacks;
    private int mActivatedPosition = ListView.INVALID_POSITION;
    ProgressDialog dialog;
    int retryCount = 0;
    Handler eventsListHandler, AckEventHandler;
    ZenossEventsAdaptor adapter;

    public List<ZenossEvent> getListOfEvents()
    {
        return listOfZenossEvents;
    }

    public interface Callbacks
    {
        public void onItemSelected(ZenossEvent event,int position);

        public void fetchError();
    }

    private static Callbacks sDummyCallbacks = new Callbacks()
    {
        public void onItemSelected(ZenossEvent event, int position)
        {
        }

        public void fetchError()
        {
        }
    };

    public void onItemAcknowledged(int position)
    {
        listOfZenossEvents.get(position).setAcknowledged();
        adapter.notifyDataSetChanged();
    }

    public ViewZenossEventsListFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        configureHandlers();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION))
        {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks))
        {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        Log.e("onAttach","Attached");
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater)
    {
        inflater.inflate(R.menu.home_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.resolveall:
            {
                final List<String> EventIDs = new ArrayList<String>();


                for (ZenossEvent evt : listOfZenossEvents)
                {
                    if(!evt.getEventState().equals("Acknowledged"))
                    {
                        evt.setProgress(true);
                        //AckEventHandler.sendEmptyMessage(ACKEVENTHANDLER_SUCCESS);
                        EventIDs.add(evt.getEVID());
                    }
                }


                AckEventHandler.sendEmptyMessage(ACKEVENTHANDLER_PROGRESS);

                new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            if (null != mService && mBound)
                            {
                                try
                                {
                                    if(null == mService.API)
                                    {
                                        mService.PrepAPI(true,true);
                                    }

                                    ZenossCredentials credentials = new ZenossCredentials(getActivity());
                                    mService.API.Login(credentials);

                                    mService.API.AcknowledgeEvents(EventIDs);

                                }
                                catch (Exception e)
                                {
                                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","AckAllThread",e);
                                }
                            }
                            else
                            {
                                AckEventHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                            }

                            //TODO Check it actually succeeded
                            AckEventHandler.sendEmptyMessage(ACKEVENTHANDLER_SUCCESS);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            AckEventHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                        }
                    }
                }.start();

                return true;
            }


            case R.id.refresh:
            {
                Refresh();
                return true;
            }

            case R.id.escalate:
            {
                Intent intent=new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

                // Add data to the intent, the receiving app will decide what to do with it.
                intent.putExtra(Intent.EXTRA_SUBJECT, "Escalation of Zenoss Events");
                String Events = "";
                for (ZenossEvent evt : listOfZenossEvents)
                {
                    Events += evt.getDevice() + " - " + evt.getSummary() + "\r\n\r\n";
                }
                intent.putExtra(Intent.EXTRA_TEXT, Events);

                startActivity(Intent.createChooser(intent, "How would you like to escalate these events?"));
            }
        }

        return false;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id)
    {
        super.onListItemClick(listView, view, position, id);

        Log.e("onListItemClick","I got clicked");

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(listOfZenossEvents.get(position),position);

        //We want to keep track of this for our own purposes
        //mSelectedDevice = listOfZenossEvents.get(position).getname();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.events_list_fragment, null);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        if (mActivatedPosition != ListView.INVALID_POSITION)
        {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick)
    {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position)
    {
        if (position == ListView.INVALID_POSITION)
        {
            getListView().setItemChecked(mActivatedPosition, false);
        }
        else
        {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    private void configureHandlers()
    {
        AckEventHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch(msg.what)
                {
                    case ACKEVENTHANDLER_PROGRESS:
                    {
                        /*for (ZenossEvent evt : listOfZenossEvents)
                        {
                            if(!evt.getEventState().equals("Acknowledged") && evt.getProgress())
                            {
                                evt.setProgress(true);
                            }
                        }*/
                        if(adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                    break;

                    case ACKEVENTHANDLER_SUCCESS:
                    {
                        for (ZenossEvent evt : listOfZenossEvents)
                        {
                            if(!evt.getEventState().equals("Acknowledged") && evt.getProgress())
                            {
                                evt.setProgress(false);
                                evt.setAcknowledged();
                            }
                        }

                        RhybuddDataSource datasource = null;

                        try
                        {
                            //TODO maybe do this with the bunch of ack id's we have in the thread?
                            datasource = new RhybuddDataSource(getActivity());
                            datasource.open();
                            datasource.UpdateRhybuddEvents(listOfZenossEvents);

                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            BugSenseHandler.sendExceptionMessage("RhybuddHome","AckEventHandler",e);
                        }
                        finally
                        {
                            if(null != datasource)
                                datasource.close();
                        }

                        if(adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                    break;

                    case ACKEVENTHANDLER_FAILURE:
                    {
                        for (ZenossEvent evt : listOfZenossEvents)
                        {
                            if(!evt.getEventState().equals("Acknowledged") && evt.getProgress())
                            {
                                evt.setProgress(false);
                            }
                        }

                        if(adapter != null)
                            adapter.notifyDataSetChanged();

                        Toast.makeText(getActivity(), "There was an error trying to ACK those events.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        };

        eventsListHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch(msg.what)
                {
                    case EVENTSLISTHANDLER_ERROR:
                    {
                        Toast.makeText(getActivity(), "An error was encountered;\r\n" + msg.getData().getString("exception"), Toast.LENGTH_LONG).show();
                    }
                    break;

                    case EVENTSLISTHANDLER_DIALOGUPDATE:
                    {
                        dialog.setMessage("Refresh Complete!");
                        this.sendEmptyMessageDelayed(EVENTSLISTHANDLER_SUCCESS,1000);
                    }
                    break;

                    case EVENTSLISTHANDLER_SUCCESS:
                    {
                        try
                        {
                            if(null != dialog && dialog.isShowing())
                                dialog.dismiss();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        adapter = new ZenossEventsAdaptor(getActivity(), listOfZenossEvents);
                        setListAdapter(adapter);
                    }
                    break;

                    case EVENTSLISTHANDLER_DB_EMPTY:
                    {
                        if(dialog != null && dialog.isShowing())
                        {
                            dialog.setMessage("DB Cache incomplete.\r\nQuerying Zenoss directly.\r\nPlease wait....");
                        }
                        else
                        {
                            dialog = new ProgressDialog(getActivity());
                            dialog.setMessage("DB Cache incomplete.\r\nQuerying Zenoss directly.\r\nPlease wait....");
                            //dialog.setCancelable(false);
                            dialog.show();
                        }
                    }
                    break;

                    case EVENTSLISTHANDLER_NO_EVENTS:
                    {
                        try
                        {
                            dialog.dismiss();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        Toast.makeText(getActivity(), "No events found. Must be a quiet day!", Toast.LENGTH_LONG).show();
                    }
                    break;

                    case EVENTSLISTHANDLER_TOTAL_FAILURE:
                    {
                        try
                        {
                            dialog.dismiss();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        mCallbacks.fetchError();
                    }
                    break;

                    case EVENTSLISTHANDLER_SERVICE_FAILURE:
                    {
                        try
                        {
                            dialog.dismiss();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        Toast.makeText(getActivity(), "Rhybudd was unable to reach its internal service.\n\nThis will cause some issues with fetching events", Toast.LENGTH_LONG).show();
                    }
                    break;

                    case EVENTSLISTHANDLER_REDOREFRESH:
                    {
                        Refresh();
                    }
                    break;
                }
            }
        };
    }

    public void DBGetThread()
    {
        Log.e("DBGetThread", "Doing a DB lookup");
        listOfZenossEvents.clear();
        new Thread()
        {
            public void run()
            {
                Message msg = new Message();
                Bundle bndle = new Bundle();

                List<ZenossEvent> tempZenossEvents = null;

                try
                {
                    RhybuddDataSource datasource = new RhybuddDataSource(getActivity());
                    datasource.open();
                    tempZenossEvents = datasource.GetRhybuddEvents();
                    datasource.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    if(tempZenossEvents != null)
                        tempZenossEvents.clear();
                }

                if(tempZenossEvents!= null && tempZenossEvents.size() > 0)
                {
                    try
                    {
                        listOfZenossEvents = tempZenossEvents;

                        eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_SUCCESS);
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("RhybuddHome", "DBGetThread", e);
                        bndle.putString("exception",e.getMessage());
                        msg.setData(bndle);
                        msg.what = EVENTSLISTHANDLER_ERROR;
                        eventsListHandler.sendMessage(msg);
                    }
                }
                else
                {
                    Log.i("EventList","No DB data found, querying API directly");
                    try
                    {
                        eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_DB_EMPTY);

                        if(tempZenossEvents != null)
                            tempZenossEvents.clear();

                        //Can we get away with just calling refresh now?
                        //TODO This needs to be sent as a runnable or something
                        Refresh();
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("RhybuddHome","DBGetThread",e);
                        bndle.putString("exception",e.getMessage());
                        msg.setData(bndle);
                        msg.what = EVENTSLISTHANDLER_ERROR;
                        eventsListHandler.sendMessage(msg);
                    }
                }
            }
        }.start();
    }

    public void Refresh()
    {
        try
        {
            if(dialog == null || !dialog.isShowing())
            {
                dialog = new ProgressDialog(getActivity());
            }

            dialog.setTitle("Querying Zenoss Directly");
            dialog.setMessage("Refreshing Events...");
            //ToDo set cancellable

            if(!dialog.isShowing())
                dialog.show();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //TODO Handle this and tell the user
            BugSenseHandler.sendExceptionMessage("RhybuddHome","Refresh",e);
        }

        new Thread()
        {
            public void run()
            {
                List<ZenossEvent> tempZenossEvents = null;

                //This is a bit dirty but hell it saves an extra API call
                if (null == mService && !mBound)
                {
                    Log.e("Refresh","Service was dead or something so sleeping");
                    try
                    {
                        sleep(500);
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("RhybuddHome","RefreshThreadSleep",e);
                    }
                }

                if (null != mService && mBound)
                {
                    Log.e("Refresh","yay not dead");
                    try
                    {
                        if(null == mService.API)
                        {
                            mService.PrepAPI(true,true);
                        }

                        ZenossCredentials credentials = new ZenossCredentials(getActivity());
                        mService.API.Login(credentials);

                        tempZenossEvents = mService.API.GetRhybuddEvents(getActivity());

                        if(null == tempZenossEvents)
                        {
                            Log.e("Refresh","We got a null return from the service API, lets try ourselves");
                            ZenossAPI API;

                            if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                            {
                                API = new ZenossAPIZaas();
                            }
                            else
                            {
                                API = new ZenossAPICore();
                            }

                            credentials = new ZenossCredentials(getActivity());
                            API.Login(credentials);

                            tempZenossEvents = API.GetRhybuddEvents(getActivity());
                        }

                        if(tempZenossEvents != null && tempZenossEvents.size() > 0)
                        {
                            retryCount = 0;
                            listOfZenossEvents = tempZenossEvents;
                            eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_DIALOGUPDATE);

                            RhybuddDataSource datasource = new RhybuddDataSource(getActivity());
                            datasource.open();
                            datasource.UpdateRhybuddEvents(listOfZenossEvents);
                            datasource.close();

                            try
                            {
                                ZenossAPI.updateLastChecked(getActivity());
                            }
                            catch (Exception e)
                            {
                                BugSenseHandler.sendExceptionMessage("RhybuddHome","Updating last setting",e);
                            }

                        }
                        else if(tempZenossEvents!= null && tempZenossEvents.size() == 0)
                        {
                            eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_NO_EVENTS);
                        }
                        else
                        {
                            // TODO Send a proper message
                            eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_TOTAL_FAILURE);
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        BugSenseHandler.sendExceptionMessage("RhybuddHome","Refresh",e);
                        eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_TOTAL_FAILURE);
                    }
                }
                else
                {
                    Log.e("Refresh","The service wasn't running for some reason");
                    dialog.setMessage("The backend service wasn't running.\n\nStarting...");
                    Intent intent = new Intent(getActivity(), ZenossPoller.class);
                    getActivity().startService(intent);
                    retryCount++;

                    if(retryCount > 5)
                    {
                        eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_SERVICE_FAILURE);
                    }
                    else
                    {
                        doBindService();
                        eventsListHandler.sendEmptyMessageDelayed(EVENTSLISTHANDLER_REDOREFRESH,300);
                    }
                }
            }
        }.start();
    }

    //------------------------------------------------------------------//
    //                                                                  //
    //               Connection to the the ZenossPoller Service         //
    //                                                                  //
    //------------------------------------------------------------------//
    void doUnbindService()
    {
        if (mBound)
        {
            // Detach our existing connection.
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    void doBindService()
    {
        getActivity().bindService(new Intent(getActivity(), ZenossPoller.class), mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //Lets try and bind to our service (if it's alive)
        doBindService();

        //If we've binded let's try and do a refresh
        DBGetThread();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // Unbind from the service
        doUnbindService();
    }

    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ZenossPoller.LocalBinder binder = (ZenossPoller.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            //Toast.makeText(RhybuddHome.this, "Connected to Service", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            //Toast.makeText(RhybuddHome.this, "Disconnected to Service", Toast.LENGTH_SHORT).show();
            mBound = false;
        }
    };

}
