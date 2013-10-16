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
import android.app.ProgressDialog;
import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
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
    private static final int EVENTSLISTHANDLER_DISMISS = 8;
    private static final int ACKEVENTHANDLER_SUCCESS = 9;
    private static final int ACKEVENTHANDLER_FAILURE = 10;
    private static final int ACKEVENTHANDLER_PROGRESS = 11;
    private static final int EVENTSLISTHANDLER_DELAYED_AB_STATUS = 12;
    private static final int EVENTSLISTHANDLER_SERVICE_NOT_STARTED = 13;

    ZenossAPI API = null;
    List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
    ZenossPoller mService;
    boolean mBound = false;
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private Callbacks mCallbacks = sDummyCallbacks;
    private int mActivatedPosition = ListView.INVALID_POSITION;
    ProgressDialog dialog;
    int retryCount = 0;
    Handler eventsListHandler, AckEventsHandler, AckSingleEventHandler;
    ZenossEventsAdaptor adapter;
    SwipeDismissListViewTouchListener touchListener = null;
    ActionMode mActionMode;

    MenuItem refreshStatus = null;
    View abprogress = null;
    int cabNumSelected = 0;


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

    public void setInProgress(int position)
    {
        listOfZenossEvents.get(position).setProgress(true);
        adapter.notifyDataSetChanged();
    }

    public void acknowledgeSingleEvent(final int position)
    {
        //Log.e("acknowledgeSingleEvent","Am in acknowledgeSingleEvent with position " + Integer.toString(position));

        setInProgress(position);

        new Thread()
        {
            public void run()
            {
                Message msg = new Message();
                Bundle bndle = new Bundle();
                msg.what = ACKEVENTHANDLER_FAILURE;
                bndle.putInt("position",position);
                msg.setData(bndle);
                try
                {
                    if (null != mService && mBound)
                    {
                        try
                        {
                            if(null == mService.API)
                            {
                                //Log.e("acknowledgeSingleEvent","mService.API was null");
                                mService.PrepAPI(true,true);
                            }
                            //Log.e("acknowledgeSingleEvent","Logging in...");
                            ZenossCredentials credentials = new ZenossCredentials(getActivity());
                            mService.API.Login(credentials);

                            //Log.e("acknowledgeSingleEvent","Acknowledging event");
                            mService.API.AcknowledgeEvent(listOfZenossEvents.get(position).getEVID());

                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","AckAllThread",e);
                            msg.what = ACKEVENTHANDLER_FAILURE;
                        }
                    }
                    else
                    {
                        //AckSingleEventHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                        msg.what = ACKEVENTHANDLER_FAILURE;
                    }

                    //TODO Check it actually succeeded
                    //AckSingleEventHandler.sendEmptyMessage(ACKEVENTHANDLER_SUCCESS);
                    msg.what = ACKEVENTHANDLER_SUCCESS;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    //AckSingleEventHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                    msg.what = ACKEVENTHANDLER_FAILURE;
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","AckAllThread outer catch",e);
                }

                //Log.e("acknowledgeSingleEvent","Sending Handler message");
                AckSingleEventHandler.sendMessage(msg);
            }
        }.start();
    }

    public ViewZenossEventsListFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        LayoutInflater inflater2 = (LayoutInflater) getActivity().getSystemService(ViewZenossEventsListActivity.LAYOUT_INFLATER_SERVICE);
        abprogress = inflater2.inflate(R.layout.progress_wheel, null);

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

        ListView listView = getListView();

        touchListener =
                new SwipeDismissListViewTouchListener(
                        listView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks()
                        {
                            @Override
                            public boolean canDismiss(int position)
                            {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions)
                            {
                                for (int position : reverseSortedPositions)
                                {
                                    //Log.e("onDismiss",Integer.toString(position));
                                    DimissEvent((ZenossEvent) adapter.getItem(position));
                                    adapter.remove(position);

                                    //listOfZenossEvents.remove(position);
                                    RhybuddDataSource datasource = null;
                                    try
                                    {
                                        datasource = new RhybuddDataSource(getActivity());
                                        datasource.open();
                                        datasource.UpdateRhybuddEvents(listOfZenossEvents);
                                    }
                                    catch (Exception e)
                                    {
                                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","SwipetoDismiss",e);
                                        e.printStackTrace();
                                    }
                                    finally
                                    {
                                        if(null != datasource)
                                            datasource.close();
                                    }
                                }
                                adapter.notifyDataSetChanged();
                            }
                        });

        listView.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        listView.setOnScrollListener(touchListener.makeScrollListener());

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                //Log.e("setOnItemLongClickListener","In long click!");
                try
                {
                    if(listOfZenossEvents.get(i).isSelected())
                    {
                        cabNumSelected--;
                        listOfZenossEvents.get(i).SetSelected(false);
                    }
                    else
                    {
                        cabNumSelected++;
                        listOfZenossEvents.get(i).SetSelected(true);
                    }

                    adapter.notifyDataSetChanged();
                    if(null == mActionMode)
                    {
                        mActionMode = getActivity().startActionMode(mActionModeCallback);
                    }
                    else
                    {
                        mActionMode.invalidate();
                    }
                    return true;
                }
                catch(Exception e)
                {
                    Toast.makeText(getActivity(), "There was an internal error. A report has been sent.", Toast.LENGTH_SHORT).show();
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","long item press",e);
                    return false;
                }
            }
        });
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
        //Log.e("onAttach","Attached");
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.home_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        refreshStatus = menu.findItem(R.id.refresh);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.resolveall:
            {
                AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_PROGRESS);
                final List<String> EventIDs = new ArrayList<String>();


                for (ZenossEvent evt : listOfZenossEvents)
                {
                    if(!evt.getEventState().equals("Acknowledged"))
                    {
                        evt.setProgress(true);
                        //AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_SUCCESS);
                        EventIDs.add(evt.getEVID());
                    }
                }

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
                                AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                            }

                            //TODO Check it actually succeeded
                            AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_SUCCESS);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","resolve all outer catch",e);
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

        //Log.e("onListItemClick","I got clicked");

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
        AckSingleEventHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                //Log.e("AckSingleEventHandler",Integer.toString(msg.what) + " / " + Integer.toString(msg.getData().getInt("position")));
                switch(msg.what)
                {
                    case ACKEVENTHANDLER_PROGRESS:
                    {
                        listOfZenossEvents.get(msg.getData().getInt("position")).setProgress(true);

                        if(adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                    break;

                    case ACKEVENTHANDLER_SUCCESS:
                    {
                        listOfZenossEvents.get(msg.getData().getInt("position")).setProgress(false);
                        listOfZenossEvents.get(msg.getData().getInt("position")).setAcknowledged();
                        listOfZenossEvents.get(msg.getData().getInt("position")).setownerID("by you");

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
                        listOfZenossEvents.get(msg.getData().getInt("position")).setProgress(false);

                        if(adapter != null)
                            adapter.notifyDataSetChanged();

                        Toast.makeText(getActivity(), "There was an error trying to ACK those events.", Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }
        };


        AckEventsHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch(msg.what)
                {
                    case ACKEVENTHANDLER_PROGRESS:
                    {
                        for (ZenossEvent evt : listOfZenossEvents)
                        {
                            if(!evt.getEventState().equals("Acknowledged"))
                            {
                                evt.setProgress(true);
                            }
                        }

                        if(adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                    break;


                    case ACKEVENTHANDLER_SUCCESS:
                    {
                        for (ZenossEvent evt : listOfZenossEvents)
                        {
                            //if(!evt.getEventState().equals("Acknowledged") && evt.getProgress())
                            if(evt.getProgress())
                            {
                                evt.setProgress(false);
                                evt.setAcknowledged();
                                evt.setownerID("by you");
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
                            BugSenseHandler.sendExceptionMessage("RhybuddHome","AckEventsHandler",e);
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

                        Toast.makeText(getActivity(), "There was an error trying to ACK that event.", Toast.LENGTH_SHORT).show();
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
                    case EVENTSLISTHANDLER_DISMISS:
                    {

                    }
                    break;

                    case EVENTSLISTHANDLER_DELAYED_AB_STATUS:
                    {
                        try
                        {
                            if(null != refreshStatus)
                            {
                                refreshStatus.setActionView(abprogress);
                            }
                            else
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
                        }
                        catch(Exception e)
                        {
                            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","EVENTSLISTHANDLER_DELAYED_AB_STATUS",e);
                        }
                    }
                    break;

                    case EVENTSLISTHANDLER_ERROR:
                    {
                        Toast.makeText(getActivity(), "An error was encountered;\r\n" + msg.getData().getString("exception"), Toast.LENGTH_LONG).show();
                    }
                    break;

                    case EVENTSLISTHANDLER_DIALOGUPDATE:
                    {
                        if(dialog != null && dialog.isShowing())
                        {
                            dialog.setMessage("Refresh Complete!");
                            this.sendEmptyMessageDelayed(EVENTSLISTHANDLER_SUCCESS,1000);
                        }
                        else
                        {
                            if(null != refreshStatus)
                            {
                                refreshStatus.setIcon(R.drawable.ic_action_refresh);
                                getActivity().invalidateOptionsMenu();
                            }
                        }
                    }
                    break;

                    case EVENTSLISTHANDLER_SUCCESS:
                    {
                        if(null != refreshStatus)
                        {
                            refreshStatus.setIcon(R.drawable.ic_action_refresh);
                            getActivity().invalidateOptionsMenu();
                        }

                        if(dialog != null && dialog.isShowing())
                        {
                            try
                            {
                                dialog.dismiss();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }

                        adapter = new ZenossEventsAdaptor(getActivity(), listOfZenossEvents);
                        setListAdapter(adapter);
                    }
                    break;

                    case EVENTSLISTHANDLER_DB_EMPTY:
                    {
                        if(null != refreshStatus)
                        {
                            refreshStatus.setActionView(abprogress);
                            getActivity().invalidateOptionsMenu();
                        }
                        else
                        {
                           // Log.e("refreshStatus","refreshStatus was null reverting to using dialog");

                            if(dialog != null && dialog.isShowing())
                            {
                                dialog.setMessage("DB Cache incomplete.\r\nQuerying Zenoss directly.\r\nPlease wait....");
                            }
                            else
                            {
                                dialog = new ProgressDialog(getActivity());
                                dialog.setMessage("DB Cache incomplete.\r\nQuerying Zenoss directly.\r\nPlease wait....");
                                //dialog.setCancelable(false);
                                //Log.e("EVENTSLISTHANDLER_DB_EMPTY", "Showing a dialog");

                                dialog.show();
                            }
                        }
                    }
                    break;

                    case EVENTSLISTHANDLER_NO_EVENTS:
                    {
                        if(null != refreshStatus)
                        {
                            refreshStatus.setIcon(R.drawable.ic_action_refresh);
                            getActivity().invalidateOptionsMenu();
                        }

                        if(dialog != null && dialog.isShowing())
                        {
                            try
                            {
                                dialog.dismiss();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }

                        try
                        {
                            listOfZenossEvents.clear();
                            adapter.notifyDataSetChanged();
                        }
                        catch (Exception e)
                        {
                            BugSenseHandler.sendExceptionMessage("EventsListFragment","no events adapter notify",e);
                        }

                        Toast.makeText(getActivity(), "No events found. Must be a quiet day!", Toast.LENGTH_LONG).show();
                    }
                    break;

                    case EVENTSLISTHANDLER_TOTAL_FAILURE:
                    {
                        if(null != refreshStatus)
                        {
                            try
                            {
                                refreshStatus.setIcon(R.drawable.ic_action_refresh);
                                getActivity().invalidateOptionsMenu();
                            }
                            catch (NullPointerException npe)
                            {
                                //Don't care
                            }
                            catch (Exception e)
                            {
                                BugSenseHandler.sendExceptionMessage("EventsListFragment","RefreshThreadSleep",e);
                            }
                        }

                        if(dialog != null && dialog.isShowing())
                        {
                            try
                            {
                                dialog.dismiss();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }

                        try
                        {
                            Toast.makeText(getActivity(),"Last Error: " + API.getLastException(),Toast.LENGTH_LONG);
                        }
                        catch (Exception e)
                        {
                            try
                            {
                                Toast.makeText(getActivity(),"Attempted to show the last received error but it was null.",Toast.LENGTH_LONG);
                            }
                            catch (Exception e1)
                            {
                                BugSenseHandler.sendExceptionMessage("EventsListFragment","Error toast",e1);
                            }
                        }

                        mCallbacks.fetchError();
                    }
                    break;

                    case EVENTSLISTHANDLER_SERVICE_NOT_STARTED:
                    {
                        dialog = new ProgressDialog(getActivity());

                        dialog.setTitle("Querying Zenoss Directly");
                        //Log.e("Refresh","The service wasn't running for some reason");
                        dialog.setMessage("The backend service wasn't running.\n\nStarting...");
                        //ToDo set cancellable

                        if(!dialog.isShowing())
                            dialog.show();
                    }
                    break;

                    case EVENTSLISTHANDLER_SERVICE_FAILURE:
                    {
                        if(null != refreshStatus)
                        {
                            refreshStatus.setIcon(R.drawable.ic_action_refresh);
                            getActivity().invalidateOptionsMenu();
                        }

                        if(dialog != null && dialog.isShowing())
                        {
                            try
                            {
                                dialog.dismiss();
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
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
        //Log.e("DBGetThread", "Doing a DB lookup");

        if((PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("URL", "").equals("") ||
                PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("userName", "").equals("") ||
                PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("passWord", "").equals("")))
        {
            //Log.e("DBGetThread", "Well we can't do this because we don't have any credentials ");
            return;
        }

        if(null != refreshStatus)
        {
            refreshStatus.setActionView(abprogress);
        }
        else
        {
            //Log.e("DBGetThread", "refreshStatus was null!");
        }

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
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","DBGetThread",e);
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
                    //Log.i("EventList","No DB data found, querying API directly");
                    try
                    {
                        eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_DB_EMPTY);

                        if(tempZenossEvents != null)
                            tempZenossEvents.clear();

                        //Can we get away with just calling refresh now?

                        getActivity().runOnUiThread(new Runnable() {
                            public void run()
                            {
                                //TODO This needs to be sent as a runnable or something
                                Refresh();
                            }
                        });

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

    private void DimissEvent(final ZenossEvent thisEvent)
    {
        new Thread()
        {
            public void run()
            {
                //This is a bit dirty but hell it saves an extra API call
                if (null == mService && !mBound)
                {
                    //Log.e("Refresh","Service was dead or something so sleeping");
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
                    //Log.e("Refresh","yay not dead");
                    try
                    {
                        if(null == mService.API)
                        {
                            mService.PrepAPI(true,true);
                        }

                        ZenossCredentials credentials = new ZenossCredentials(getActivity());
                        mService.API.Login(credentials);

                        mService.API.DismissEvent(thisEvent.getEVID());
                        //TODO maybe do something?
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","DimissEvent",e);
                        //TODO Create a handler
                        //eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_TOTAL_FAILURE);
                    }
                }
                else
                {
                    //TODO Lets warn them with a host
                    //TODO Or make it more resiliant
                }
            }
        }.start();
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback()
    {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.events_cab, menu);
            mode.setTitle("Manage "+ cabNumSelected +" Events");
            mode.setSubtitle("Select multiple events for mass acknowledgement");
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            mode.setTitle("Manage "+ cabNumSelected +" Events");
            mode.setSubtitle("Select multiple events for mass acknowledgement");
            return true;
            //return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            switch (item.getItemId())
            {
                case R.id.Acknowledge:
                {
                    //AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_PROGRESS);
                    final List<String> EventIDs = new ArrayList<String>();


                    for (ZenossEvent evt : listOfZenossEvents)
                    {
                        if(evt.isSelected())
                        {
                            evt.setProgress(true);
                            evt.SetSelected(false);
                            EventIDs.add(evt.getEVID());
                        }
                    }

                    adapter.notifyDataSetChanged();

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
                                    AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                                }

                                //TODO Check it actually succeeded
                                AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_SUCCESS);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                                AckEventsHandler.sendEmptyMessage(ACKEVENTHANDLER_FAILURE);
                            }
                        }
                    }.start();
                    mode.finish();
                    return true;
                }

                case R.id.escalate:
                {
                    Intent intent=new Intent(android.content.Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

                    // Add data to the intent, the receiving app will decide what to do with it.
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Escalation of "+ cabNumSelected +" Zenoss Events");
                    String Events = "Escalated events;";
                    int size = listOfZenossEvents.size();
                    for (Integer i = 0; i < size; i++ )
                    {
                        if(listOfZenossEvents.get(i).isSelected())
                            Events += "\r\n" + listOfZenossEvents.get(i).getDevice() + " - " + listOfZenossEvents.get(i).getSummary();
                    }

                    intent.putExtra(Intent.EXTRA_TEXT, Events);
                    startActivity(Intent.createChooser(intent, "How would you like to escalate these events?"));
                    mode.finish();
                    return true;
                }

                default:
                {
                    adapter.notifyDataSetChanged();
                    return false;
                }
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            //Log.e("onDestroyActionMode","Called");
            int size = listOfZenossEvents.size();
            for (Integer i = 0; i < size; i++ )
            {
                listOfZenossEvents.get(i).SetSelected(false);
            }
            adapter.notifyDataSetChanged();
            cabNumSelected = 0;
            mActionMode = null;
        }
    };

    public void Refresh()
    {
        try {


        if((PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("URL", "").equals("") ||
                PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("userName", "").equals("") ||
                PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("passWord", "").equals("")))
        {
            //Log.e("Refresh()", "Well we can't do this because we don't have any credentials ");
            return;
        }
        }
        catch (Exception e)
        {
            try
            {
                if(null == getActivity())
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Refresh getActivity was null",e);
                }
                else
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Refresh getActivity not null",e);
                }
            }
            catch (Exception ie)
            {
                BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Refresh checking activity",e);
            }
            return;
        }

        try
        {
            if(null != refreshStatus)
            {
                refreshStatus.setActionView(abprogress);
            }
            else
            {
                Log.e("Refresh", "refreshStatus was null");
                eventsListHandler.sendEmptyMessageDelayed(EVENTSLISTHANDLER_DELAYED_AB_STATUS,500);
                /*if(dialog == null || !dialog.isShowing())
                {
                    dialog = new ProgressDialog(getActivity());
                }

                dialog.setTitle("Querying Zenoss Directly");
                dialog.setMessage("Refreshing Events...");
                //ToDo set cancellable

                if(!dialog.isShowing())
                    dialog.show();*/
            }
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

                //If we wait just a little bit then the UI should catch up
                try
                {
                    sleep(500);
                }
                catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                }

                List<ZenossEvent> tempZenossEvents = null;

                //This is a bit dirty but hell it saves an extra API call
                if (null == mService && !mBound)
                {
                    //Log.e("Refresh","Service was dead or something so sleeping");
                    try
                    {
                        sleep(500);
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("EventsListFragment","RefreshThreadSleep",e);
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

                        if(mService.API.Login(credentials))
                        {
                            tempZenossEvents = mService.API.GetRhybuddEvents(getActivity());

                            if(null == tempZenossEvents)
                            {
                                //Log.e("Refresh","We got a null return from the service API, lets try ourselves");
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
                                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Updating last setting",e);
                                }

                                try
                                {
                                    eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_SUCCESS);
                                }
                                catch (Exception e)
                                {
                                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Sending EVENTSLISTHANDLER_SUCCESS",e);
                                }
                            }
                            else if(tempZenossEvents!= null && tempZenossEvents.size() == 0)
                            {
                                try
                                {
                                    eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_NO_EVENTS);
                                }
                                catch (Exception e)
                                {
                                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Sending EVENTSLISTHANDLER_NO_EVENTS",e);
                                }
                            }
                            else
                            {
                                // TODO Send a proper message

                                try
                                {
                                    eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_TOTAL_FAILURE);
                                }
                                catch (Exception e)
                                {
                                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Sending EVENTSLISTHANDLER_TOTAL_FAILURE",e);
                                }
                            }
                        }
                        else
                        {
                            try
                            {
                                eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_TOTAL_FAILURE);
                            }
                            catch (Exception e)
                            {
                                BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Sending EVENTSLISTHANDLER_TOTAL_FAILURE",e);
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Refresh",e);

                        try
                        {
                            if(null != eventsListHandler)
                                eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_TOTAL_FAILURE);
                        }
                        catch (Exception e1)
                        {
                            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","Sending EVENTSLISTHANDLER_TOTAL_FAILURE from within catch",e1);
                        }
                    }
                }
                else
                {
                    if(null == refreshStatus)
                    {
                        if(dialog == null || !dialog.isShowing())
                        {
                            eventsListHandler.sendEmptyMessage(EVENTSLISTHANDLER_SERVICE_NOT_STARTED);
                        }
                    }

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
        //If it's been more than 15 minutes since we last updated we should do a full refresh
        if(ZenossAPI.shouldRefresh(getActivity()))
        {
            //Log.i("onResume","shouldRefresh() says we should do a full refresh");
            Refresh();
        }
        else
        {
            //Log.i("onResume","shouldRefresh() says we're good to do a DB fetch");
            DBGetThread();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();

        // Unbind from the service
        doUnbindService();

        try
        {
            //Log.e("onPause","Checking if dialog is null");
            if(null != dialog)
            {
                //Log.e("onPause","it wasn't");
                try
                {
                    //Log.e("onPause","Dismissing");
                    dialog.dismiss();
                    //Log.e("onPause","dismissed");
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","onPause",e);
                }
            }
            else
            {
                //Log.e("onPause","dialog was null");
            }
        }
        catch(Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossEventsListFragment","onPause outer",e);
            e.printStackTrace();
        }

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
