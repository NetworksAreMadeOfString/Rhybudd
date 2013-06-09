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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import org.apache.http.conn.ConnectTimeoutException;

import java.util.ArrayList;
import java.util.List;

public class ViewZenossDeviceListFragment extends ListFragment
{

    ZenossDeviceAdaptor adapter = null;
    //TrapsDataSource datasource = null;
    public List<ZenossDevice> listOfDevices = null;
    ArrayList<String> DeviceNames;
    ArrayList<String> DeviceIDs;
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private Callbacks mCallbacks = sDummyCallbacks;
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private Handler handler;
    private ProgressDialog dialog;
    private String mSelectedDevice = null;
    Thread dataLoad;

    public interface Callbacks
    {
        //public void onItemSelected(ZenossDevice ZenossDevice);

        public void onItemSelected(ZenossDevice ZenossDevice, ArrayList<String> DeviceNames, ArrayList<String> DeviceIDs);
    }

    private static Callbacks sDummyCallbacks = new Callbacks()
    {
        /*@Override
        public void onItemSelected(ZenossDevice ZenossDevice)
        {
        }*/
        public void onItemSelected(ZenossDevice ZenossDevice, ArrayList<String> DeviceNames, ArrayList<String> DeviceIDs)
        {
        }
    };

    public ViewZenossDeviceListFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                if(msg.what == 0)
                {
                    Toast.makeText(getActivity(), "An error was encountered;\r\n" + msg.getData().getString("exception"), Toast.LENGTH_LONG).show();
                }
                else if(msg.what == 1)
                {
                    dialog.setMessage("Refresh Complete!");
                    this.sendEmptyMessageDelayed(2,1000);
                }
                else if(msg.what == 2)
                {
                    dialog.dismiss();
                    adapter = new ZenossDeviceAdaptor(getActivity(), listOfDevices);
                    //((TextView) findViewById(R.id.ServerCountLabel)).setText("Monitoring " + DeviceCount + " servers");
                    setListAdapter(adapter);
                }
            }
        };

        try
        {
            listOfDevices = (List<ZenossDevice>) getActivity().getLastNonConfigurationInstance();
        }
        catch(Exception e)
        {
            listOfDevices = null;
            //BugSenseHandler.log("DeviceList", e);
        }

        if(listOfDevices == null || listOfDevices.size() < 1)
        {
            listOfDevices = new ArrayList<ZenossDevice>();
            DBGetThread();
        }
        else
        {
            adapter = new ZenossDeviceAdaptor(getActivity(), listOfDevices);
            setListAdapter(adapter);
        }
    }

    private void PopulateMetaLists()
    {
        DeviceNames = new ArrayList<String>();
        DeviceIDs = new ArrayList<String>();

        for(ZenossDevice device : listOfDevices)
        {
            DeviceNames.add(device.getname());
            DeviceIDs.add(device.getuid());
        }
    }
    public void DBGetThread()
    {
        dialog = new ProgressDialog(getActivity());
        dialog.setTitle("Querying DB");
        dialog.setMessage("Please wait:\nLoading Infrastructure....");
        dialog.setCancelable(false);
        dialog.show();

        listOfDevices.clear();
        dataLoad = new Thread()
        {
            public void run()
            {
                try
                {
                    //listOfZenossDevices = rhybuddCache.GetRhybuddDevices();
                    RhybuddDataSource datasource = new RhybuddDataSource(getActivity());
                    datasource.open();
                    listOfDevices = datasource.GetRhybuddDevices();
                    datasource.close();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    listOfDevices.clear();
                }

                if(listOfDevices!= null && listOfDevices.size() > 0)
                {
                    PopulateMetaLists();
                    //DeviceCount = listOfZenossDevices.size();
                    //Log.i("DeviceList","Found DB Data!");
                    handler.sendEmptyMessage(2);
                }
                else
                {
                    //Log.i("DeviceList","No DB data found, querying API directly");
                    Refresh();
                }
            }
        };
        dataLoad.start();
    }

    public void Refresh()
    {
        if(dialog != null)
        {
            dialog.setTitle("Contacting Zenoss...");
            dialog.setMessage("Please wait:\nLoading Infrastructure....");
            //TODO make cancelable
            dialog.setCancelable(false);

            if(!dialog.isShowing())
            {
                dialog.show();
            }
        }
        else
        {
            dialog = new ProgressDialog(getActivity());
            dialog.setTitle("Contacting Zenoss...");
            dialog.setMessage("Please wait:\nLoading Infrastructure....");
            dialog.setCancelable(false);
            dialog.show();
        }

        if(listOfDevices != null)
            listOfDevices.clear();

        ((Thread) new Thread()
        {
            public void run()
            {
                String MessageExtra = "";
                try
                {
                    Message msg = new Message();
                    Bundle bundle = new Bundle();

                    ZenossAPIv2 API = null;
                    SharedPreferences settings = null;
                    settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    try
                    {
                        API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
                    }
                    catch(ConnectTimeoutException cte)
                    {
                        if(cte.getMessage() != null)
                        {

                            bundle.putString("exception","The connection timed out;\r\n" + cte.getMessage().toString());
                            msg.setData(bundle);
                            msg.what = 0;
                            handler.sendMessage(msg);
                            //Toast.makeText(DeviceList.this, "The connection timed out;\r\n" + cte.getMessage().toString(), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            bundle.putString("exception","A time out error was encountered but the exception thrown contains no further information.");
                            msg.setData(bundle);
                            msg.what = 0;
                            handler.sendMessage(msg);
                            //Toast.makeText(DeviceList.this, "A time out error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
                        }
                    }
                    catch(Exception e)
                    {
                        if(e.getMessage() != null)
                        {
                            bundle.putString("exception","An error was encountered;\r\n" + e.getMessage().toString());
                            msg.setData(bundle);
                            msg.what = 0;
                            handler.sendMessage(msg);
                            //Toast.makeText(DeviceList.this, "An error was encountered;\r\n" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            bundle.putString("exception","An error was encountered but the exception thrown contains no further information.");
                            msg.setData(bundle);
                            msg.what = 0;
                            handler.sendMessage(msg);
                            //Toast.makeText(DeviceList.this, "An error was encountered but the exception thrown contains no further information.", Toast.LENGTH_LONG).show();
                        }
                    }

                    try
                    {
                        if(API != null)
                        {
                            listOfDevices = API.GetRhybuddDevices();
                        }
                        else
                        {
                            listOfDevices = null;
                        }
                    }
                    catch(Exception e)
                    {
                        if(e.getMessage() != null)
                            MessageExtra = e.getMessage();

                        listOfDevices = null;
                    }

                    if(listOfDevices != null && listOfDevices.size() > 0)
                    {
                        PopulateMetaLists();

                        //DeviceCount = listOfZenossDevices.size();
                        Message.obtain();
                        handler.sendEmptyMessage(1);

                        RhybuddDataSource datasource = new RhybuddDataSource(getActivity());
                        datasource.open();
                        datasource.UpdateRhybuddDevices(listOfDevices);
                        datasource.close();
                    }
                    else
                    {
                        //Message msg = new Message();
                        //Bundle bundle = new Bundle();
                        bundle.putString("exception","A query to both the local DB and Zenoss API returned no devices. " + MessageExtra );
                        msg.setData(bundle);
                        msg.what = 0;
                        handler.sendMessage(msg);
                    }

                }
                catch (Exception e)
                {
                    //BugSenseHandler.log("DeviceList", e);
                    Message msg = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putString("exception",e.getMessage());
                    msg.setData(bundle);
                    msg.what = 0;
                    handler.sendMessage(msg);
                }
            }
        }).start();
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

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    /*@Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
    }*/

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id)
    {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        //mCallbacks.onItemSelected(DummyContent.ITEMS.get(position).id);
        //mCallbacks.onItemSelected(listOfDevices.get(position));
        mCallbacks.onItemSelected(listOfDevices.get(position),DeviceNames,DeviceIDs);

        //We want to keep track of this for our own purposes
        //mActivatedPosition = position;
        mSelectedDevice = listOfDevices.get(position).getname();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.list_fragment, null);
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
}
