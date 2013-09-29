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
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import com.bugsense.trace.BugSenseHandler;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class RhybuddDream extends DreamService
{
    ZenossPoller mService;
    boolean mBound = false;
    List<ZenossDevice> listOfDevices = null;
    GridView theGrid = null;
    int numGraphs = 6;
    int refreshDelay = 5000; //15000
    Handler newGraphHandler = null;
    Random r = new Random();
    private ArrayList<HostGraph> mGraphs = new ArrayList<HostGraph>();
    DreamGridAdapter adapter;

    public void onDreamingStarted()
    {
        super.onDreamingStarted();

        BugSenseHandler.initAndStartSession(RhybuddDream.this, "44a76a8c");

        doBindService();

        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.dream);
        theGrid = (GridView) findViewById(R.id.DreamGrid);

        newGraphHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                if(msg.what == 0)
                {
                    adapter.notifyDataSetChanged();
                }
                else if(msg.what == 1)
                {
                    //newGraphHandler.sendEmptyMessageDelayed(0,(r.nextInt(5000) + 5000));
                    adapter = new DreamGridAdapter(RhybuddDream.this,mGraphs);
                    theGrid.setAdapter(adapter);
                    newGraphHandler.sendEmptyMessageDelayed(10,refreshDelay);
                }
                else
                {
                    new Thread()
                    {
                        public void run()
                        {
                            ZenossDevice thisDevice = listOfDevices.get(r.nextInt(listOfDevices.size()));

                            try
                            {
                                JSONObject graphURLs = mService.API.GetDeviceGraphs(thisDevice.getuid());

                                int urlCount = graphURLs.getJSONObject("result").getJSONArray("data").length();

                                if(urlCount > 0)
                                {
                                    for(int i = 0; i < urlCount; i++)
                                    {
                                        try
                                        {
                                            JSONObject currentGraph = graphURLs.getJSONObject("result").getJSONArray("data").getJSONObject(i);
                                            if(currentGraph.getString("title").equals("Load Average"))
                                            {
                                                int posToUpdate = r.nextInt(numGraphs);


                                                mGraphs.get(posToUpdate).thisGraph = mService.API.GetGraph(currentGraph.getString("url"));
                                                mGraphs.get(posToUpdate).thisHostName = thisDevice.getname();

                                                newGraphHandler.sendEmptyMessage(0);
                                                break;
                                            }
                                        }
                                        catch(Exception e)
                                        {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            catch(Exception e)
                            {
                                e.printStackTrace();
                            }
                            finally
                            {
                                newGraphHandler.sendEmptyMessageDelayed(10,refreshDelay);
                            }
                        }
                    }.start();
                }
            }
        };


        new Thread()
        {
            public void run()
            {
                while(mBound == false || null == mService || null == mService.API)
                {
                    try
                    {
                        Thread.sleep(500);
                        Log.i("Sleeping","Sleeping");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                for(int i = 0; i < numGraphs; i++)
                {
                    ZenossDevice thisDevice = listOfDevices.get(r.nextInt(listOfDevices.size()));
                    //Log.e("Processing",Integer.toString(i) + " / " + thisDevice.getname());

                    try
                    {
                        JSONObject graphURLs = mService.API.GetDeviceGraphs(thisDevice.getuid());

                        int urlCount = graphURLs.getJSONObject("result").getJSONArray("data").length();

                        if(urlCount > 0)
                        {
                            for(int j = 0; j < urlCount; j++)
                            {
                                try
                                {
                                    JSONObject currentGraph = graphURLs.getJSONObject("result").getJSONArray("data").getJSONObject(j);
                                    if(currentGraph.getString("title").equals("Load Average"))
                                    {
                                        mGraphs.add(new HostGraph(mService.API.GetGraph(currentGraph.getString("url")), thisDevice.getname()));
                                        break;
                                    }
                                    else
                                    {
                                        continue;
                                    }
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                    //mGraphs.add(new HostGraph(null,"..."));
                                    i--;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            //mGraphs.add(new HostGraph(null,"..."));
                            i--;
                        }
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        //mGraphs.add(new HostGraph(null,"..."));
                        i--;
                    }
                }

                newGraphHandler.sendEmptyMessage(1);
            }
        }.start();

    }


    public void onDreamingStopped()
    {
        doUnbindService();

        newGraphHandler.removeMessages(1);
        newGraphHandler.removeMessages(10);
        newGraphHandler.removeMessages(0);
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
            unbindService(mConnection);
            mBound = false;
        }
    }

    void doBindService()
    {
        bindService(new Intent(this, ZenossPoller.class), mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;

        RhybuddDataSource datasource = new RhybuddDataSource(this);
        datasource.open();
        listOfDevices = datasource.GetRhybuddDevices();
        datasource.close();
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






    //!-----------------------------------------------

    private class HostGraph
    {
        public Drawable thisGraph = null;
        public String thisHostName = null;

        public HostGraph(Drawable Graph, String HostName)
        {
            thisGraph = Graph;
            thisHostName = HostName;
        }
    }

    public class DreamGridAdapter  extends BaseAdapter
    {
        private Context mContext;
        private int imgWidth = 0;
        private int imgHeight = 0;
        private ArrayList<HostGraph> mGraphs = null;
        private int mCount = 0;
        private String[] hostnames;

        public DreamGridAdapter(Context c,ArrayList<HostGraph> Graphs)
        {
            mContext = c;
            mGraphs = Graphs;
            hostnames = new String[Graphs.size()];
            int i = 0;
            for(HostGraph Graph : Graphs)
            {
                hostnames[i] = Graph.thisHostName;
                i++;
            }
        }

        public int getCount()
        {
            return mGraphs.size();
        }

        public Object getItem(int position)
        {
            return mGraphs.get(position);
        }

        public long getItemId(int position)
        {
            return position;
        }

        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent)
        {
            HostGraph thisGraphObject = mGraphs.get(position);

            if(imgWidth == 0)
            {
                imgWidth = (parent.getMeasuredWidth() / mContext.getResources().getInteger(R.integer.GridColumns));
                imgHeight = (300 * (imgWidth / 500));
            }


            if (convertView == null)
            {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.dream_grid_item, null);
            }

            //((RelativeLayout) convertView.findViewById(R.id.gridItemContainer)).set
            ImageView groupImg = ((ImageView) convertView.findViewById(R.id.groupImage));
            //groupImg.setLayoutParams(new ViewGroup.LayoutParams(imgWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            groupImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            groupImg.setMinimumHeight(imgHeight);
            groupImg.setMinimumWidth(imgWidth);
            Animation fadeInAnimation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);

            if(null == thisGraphObject.thisGraph)
            {
                Random r = new Random();
                //groupImg.setImageResource(mThumbIds[r.nextInt(mThumbIds.length)]);
                groupImg.setImageResource(R.drawable.graph);
            }
            else
            {
                groupImg.setImageDrawable(thisGraphObject.thisGraph);
                if(!hostnames[position].equals(thisGraphObject.thisHostName))
                {
                    groupImg.setAnimation(fadeInAnimation);
                    hostnames[position] = thisGraphObject.thisHostName;
                }
            }

            ((TextView) convertView.findViewById(R.id.groupTitle)).setText(thisGraphObject.thisHostName);
            ((TextView) convertView.findViewById(R.id.groupTitle)).setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

            return convertView;
        }

        // references to our images
        private Integer[] mThumbIds = {
                R.drawable.groups_a, R.drawable.groups_b,
                R.drawable.groups_c, R.drawable.groups_d,
                R.drawable.groups_e, R.drawable.groups_f,
                R.drawable.groups_g, R.drawable.groups_h,
                R.drawable.groups_i, R.drawable.groups_j,
                R.drawable.groups_k, R.drawable.groups_l
        };
    }
}
