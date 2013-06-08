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

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bugsense.trace.BugSenseHandler;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Gareth on 04/06/13.
 */
public class ViewZenossDeviceFragment extends Fragment
{
    public static String ARG_HOSTNAME = "hostname";
    public static String ARG_UID = "uid";
    public static String ARG_2PANE = "twopane";
    public static String ARG_DEVICENAMES = "devicenames";
    public static String ARG_DEVICEIDS = "deviceids";
    private static int GRAPH_LOAD_SUCCESS = 1;

    Drawable loadAverageGraph;
    Drawable CPUGraph;
    Drawable MemoryGraph;

    Handler firstLoadHandler, eventsHandler, errorHandler,loadAverageHandler,CPUGraphHandler,MemoryGraphHandler;

    //Per fragment UI elements
    TextView deviceTitle = null;
    ImageView loadAverageGraphView;
    ImageView CPUGraphView;
    ImageView MemoryGraphView;

    ZenossCredentials credentials = null;

    public ViewZenossDeviceFragment()
    {

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        BugSenseHandler.initAndStartSession(getActivity(), "44a76a8c");

        loadAverageHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                try
                {
                    loadAverageGraphView.setImageDrawable(loadAverageGraph);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        CPUGraphHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                try
                {
                    CPUGraphView.setImageDrawable(CPUGraph);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };

        MemoryGraphHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                try
                {
                    MemoryGraphView.setImageDrawable(MemoryGraph);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        ((Thread) new Thread(){
            public void run()
            {
                ZenossAPI API;

                if( PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
                {
                    API = new ZenossAPIZaas();
                }
                else
                {
                    API = new ZenossAPICore();
                }

                try
                {
                    credentials = new ZenossCredentials(getActivity());
                    API.Login(credentials);
                    //API.getDe
                    API.GetDevice(getArguments().getString(ARG_UID));
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();

        ((Thread) new Thread()
        {
            public void run()
            {
                try
                {
                    ZenossAPI API;
                    Message msg = new Message();
                    Bundle bundle = new Bundle();


                    if( PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
                    {
                        API = new ZenossAPIZaas();
                    }
                    else
                    {
                        API = new ZenossAPICore();
                    }


                    credentials = new ZenossCredentials(getActivity());
                    API.Login(credentials);

                    JSONObject graphURLs = API.GetDeviceGraphs(getArguments().getString(ARG_UID));

                    int urlCount = graphURLs.getJSONObject("result").getJSONArray("data").length();

                    for(int i = 0; i < urlCount; i++)
                    {
                        JSONObject currentGraph = null;
                        try
                        {
                            currentGraph = graphURLs.getJSONObject("result").getJSONArray("data").getJSONObject(i);

                            if(currentGraph.getString("title").equals("Load Average"))
                            {
                                loadAverageGraph = API.GetGraph(currentGraph.getString("url"));
                                loadAverageHandler.sendEmptyMessage(GRAPH_LOAD_SUCCESS);
                            }
                            else if(currentGraph.getString("title").equals("CPU Utilization"))
                            {
                                CPUGraph = API.GetGraph(currentGraph.getString("url"));
                                CPUGraphHandler.sendEmptyMessage(GRAPH_LOAD_SUCCESS);
                            }
                            else if(currentGraph.getString("title").equals("Memory Utilization"))
                            {
                                MemoryGraph = API.GetGraph(currentGraph.getString("url"));
                                MemoryGraphHandler.sendEmptyMessage(GRAPH_LOAD_SUCCESS);
                            }
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.view_zenoss_device_fragment, container, false);
        deviceTitle = (TextView) rootView.findViewById(R.id.DeviceTitle);
        deviceTitle.setTypeface(Typeface.create("sans-serif-light", Typeface.BOLD));
        loadAverageGraphView = (ImageView) rootView.findViewById(R.id.loadAverageGraph);
        CPUGraphView = (ImageView) rootView.findViewById(R.id.CPUGraph);
        MemoryGraphView = (ImageView) rootView.findViewById(R.id.MemoryGraph);


        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        super.onActivityCreated(bundle);

        deviceTitle.setText(getArguments().getString(ARG_HOSTNAME));

        //Removed due to next pager item 'stealing' the actionbar
        /*try
        {
            getActivity().getActionBar().setSubtitle(getArguments().getString("DeviceName"));
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment", "onActivityCreated", e);
        }*/

    }



}
