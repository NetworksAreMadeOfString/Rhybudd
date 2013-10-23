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

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import org.json.JSONObject;
import java.util.Random;

public class ViewZenossDeviceFragment extends Fragment
{
    public static String ARG_HOSTNAME = "hostname";
    public static String ARG_UID = "uid";
    public static String ARG_2PANE = "twopane";
    public static String ARG_DEVICENAMES = "devicenames";
    public static String ARG_DEVICEIDS = "deviceids";
    private static int GRAPH_LOAD_SUCCESS = 1;
    private static int GRAPH_LOAD_FAILURE = 999;

    Drawable loadAverageGraph;
    Drawable CPUGraph;
    Drawable MemoryGraph;

    Handler firstLoadHandler, eventsHandler, errorHandler,loadAverageHandler,CPUGraphHandler,MemoryGraphHandler;
    //Per fragment UI elements
    TextView deviceTitle = null;
    ImageView loadAverageGraphView;
    ImageView CPUGraphView;
    ImageView MemoryGraphView;
    TextView snmpAgent = null;
    TextView snmpContact = null;
    TextView snmpLocation = null;
    TextView Uptime = null;
    TextView MemoryRAM = null;
    TextView MemorySwap = null;
    TextView LastCollected = null;
    HorizontalScrollView hsv = null;
    JSONObject deviceJSON = null;
    ZenossCredentials credentials = null;
    private Integer[] mThumbIds = {
            R.drawable.groups_a, R.drawable.groups_b,
            R.drawable.groups_c, R.drawable.groups_d,
            R.drawable.groups_e, R.drawable.groups_f,
            R.drawable.groups_g, R.drawable.groups_h,
            R.drawable.groups_i, R.drawable.groups_j,
            R.drawable.groups_k, R.drawable.groups_l
    };


    public ViewZenossDeviceFragment()
    {

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        try
        {
            if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            {
                inflater.inflate(R.menu.view_device, menu);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onCreateOptionsMenu",e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        //Log.e("ViewZenossDeviceFragment", Integer.toString(item.getItemId()) + " / " + Integer.toString(R.id.writenfc));

        switch (item.getItemId())
        {
            case R.id.writenfc:
            {
                try
                {
                    if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                    {
                        Intent NFCIntent = new Intent(getActivity(), WriteNFCActivity.class);
                        try
                        {
                            NFCIntent.putExtra(WriteNFCActivity.PAYLOAD_UID,deviceJSON.getJSONObject("result").getJSONObject("data").getString("uid"));
                            this.startActivity(NFCIntent);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                            Toast.makeText(getActivity(),"Sorry there was error parsing the UID for this device",Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onOptionsItemSelected",e);
                    return false;
                }
            }

            default:
                break;
        }

        return false;
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
                    loadAverageGraph = null;
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","loadAverageHandler",e);
                }
            }
        };

        errorHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                try
                {
                    CPUGraphView.setVisibility(View.GONE);
                    MemoryGraphView.setVisibility(View.GONE);
                    loadAverageGraphView.setVisibility(View.GONE);
                    hsv.setVisibility(View.GONE);
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","errorHandler",e);
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
                    CPUGraph = null;
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","CPUGraphHandler",e);
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
                    MemoryGraph = null;
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","MemoryGraphHandler",e);
                }
            }
        };

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.view_zenoss_device_fragment, container, false);

        Typeface titlesTypeFace = Typeface.create("sans-serif-light", Typeface.BOLD);

        deviceTitle = (TextView) rootView.findViewById(R.id.DeviceTitle);
        deviceTitle.setTypeface(titlesTypeFace);

        loadAverageGraphView = (ImageView) rootView.findViewById(R.id.loadAverageGraph);
        CPUGraphView = (ImageView) rootView.findViewById(R.id.CPUGraph);
        MemoryGraphView = (ImageView) rootView.findViewById(R.id.MemoryGraph);

        snmpAgent = (TextView) rootView.findViewById(R.id.snmpAgent);
        snmpAgent.setTypeface(titlesTypeFace);
        snmpContact = (TextView) rootView.findViewById(R.id.snmpContact);
        snmpContact.setTypeface(titlesTypeFace);
        snmpLocation = (TextView) rootView.findViewById(R.id.snmpLocation);
        snmpLocation.setTypeface(titlesTypeFace);
        Uptime = (TextView) rootView.findViewById(R.id.uptime);
        Uptime.setTypeface(titlesTypeFace);
        MemoryRAM = (TextView) rootView.findViewById(R.id.memory_ram);
        MemoryRAM.setTypeface(titlesTypeFace);
        MemorySwap = (TextView) rootView.findViewById(R.id.memory_swap);
        MemorySwap.setTypeface(titlesTypeFace);
        LastCollected = (TextView) rootView.findViewById(R.id.lastCollected);
        LastCollected.setTypeface(titlesTypeFace);

        hsv = (HorizontalScrollView) rootView.findViewById(R.id.horizontalScrollView);

        try
        {
            ImageView deviceRepImg = ((ImageView) rootView.findViewById(R.id.deviceRepImg));
            deviceRepImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Random r = new Random();
            deviceRepImg.setImageResource(mThumbIds[r.nextInt(mThumbIds.length)]);
        }
        catch(Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","ImageRand",e);
        }

        return rootView;
    }

    private void ProcessJSON()
    {
        try
        {
            JSONObject data = deviceJSON.getJSONObject("result").getJSONObject("data");

            snmpAgent.setText(data.getString("snmpAgent"));
            snmpContact.setText(data.getString("snmpContact"));
            snmpLocation.setText(data.getString("snmpLocation"));
            Uptime.setText("Uptime:" + data.getString("uptime"));
            MemoryRAM.setText("RAM: " + data.getJSONObject("memory").getString("ram"));
            MemorySwap.setText("Swap: " + data.getJSONObject("memory").getString("swap"));
            LastCollected.setText("Last Collected: " + data.getString("lastCollected"));

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        try
        {
            outState.putString("json", deviceJSON.toString());
        }
        catch(Exception e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onSaveInstanceState",e);
        }

        /*outState.putParcelable("cpuimg",((BitmapDrawable) CPUGraphView.getDrawable()).getBitmap());
        outState.putParcelable("loadavgimg",((BitmapDrawable) loadAverageGraphView.getDrawable()).getBitmap());
        outState.putParcelable("memimg",((BitmapDrawable) MemoryGraphView.getDrawable()).getBitmap());*/
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        try
        {
            deviceTitle.setText(getArguments().getString(ARG_HOSTNAME));
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onActivityCreated",e);
        }

        //Check if we have a bundle
        if(null != bundle)
        {
            if(bundle.containsKey("json"))
            {
                try
                {
                    deviceJSON = new JSONObject(bundle.getString("json"));
                    ProcessJSON();
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onActivityCreated",e);
                }
            }

            if(bundle.containsKey("cpuimg"))
            {
                try
                {
                    CPUGraphView.setImageBitmap((Bitmap) bundle.getParcelable("img"));
                    CPUGraphView.invalidate();
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onActivityCreated",e);
                }
            }

            if(bundle.containsKey("loadavgimg"))
            {
                try
                {
                    loadAverageGraphView.setImageBitmap((Bitmap) bundle.getParcelable("cpuimg"));
                    loadAverageGraphView.invalidate();
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onActivityCreated",e);
                }
            }

            if(bundle.containsKey("memimg"))
            {
                try
                {
                    MemoryGraphView.setImageBitmap((Bitmap) bundle.getParcelable("memimg"));
                    MemoryGraphView.invalidate();
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onActivityCreated",e);
                }
            }


        }
        else
        {
            (new Thread(){
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
                        deviceJSON = API.GetDevice(getArguments().getString(ARG_UID));

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ProcessJSON();
                            }
                        });

                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onActivityCreated",e);
                    }
                }
            }).start();
        }

        getGraphs();

        super.onActivityCreated(bundle);
    }

    private void getGraphs()
    {
        (new Thread()
        {
            public void run()
            {
                try
                {
                    ZenossAPI API;
                    /*Message msg = new Message();
                    Bundle bundle = new Bundle();*/

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

                    if(urlCount == 0)
                    {
                        errorHandler.sendEmptyMessage(GRAPH_LOAD_FAILURE);
                    }
                    else
                    {
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
                                BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","getGraphs",e);
                            }
                        }
                    }
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment","onActivityCreated",e);
                }
            }
        }).start();
    }



}
