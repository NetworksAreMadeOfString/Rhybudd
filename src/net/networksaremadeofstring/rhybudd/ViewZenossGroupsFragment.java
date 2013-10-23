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


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;


public class ViewZenossGroupsFragment extends Fragment
{
    GridView gridview = null;
    Handler handler;
    String[] groups;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState)
    {
        try
        {
            View view = inflater.inflate(R.layout.view_zenoss_groups_fragment, null);
            gridview = (GridView) view.findViewById(R.id.GroupsGrid);
            return view;
        }
        catch (Exception v)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossGroupsFragment", "onCreateView", v);
            return null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                try
                {
                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener()
                    {
                        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                            Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
                        }
                    });

                    gridview.setAdapter(new ZenossGroupsGridAdapter(getActivity(),groups));
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("ViewZenossGroupsFragment", "onCreate", e);

                    try
                    {
                        Toast.makeText(getActivity(), "There was a problem getting the list of groups or rendering the grid", Toast.LENGTH_LONG).show();
                    }
                    catch (Exception t)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossGroupsFragment", "onCreate toast", t);
                    }

                }
            }
        };

        ((Thread) new Thread(){
            public void run()
            {
                try
                {
                    ZenossCredentials credentials = null;
                    ZenossAPI API = null;

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

                    groups = API.GetGroups();

                }
                catch(Exception e)
                {
                    //Toast.makeText(getActivity(), "There was a problem getting the list of groups or rendering the grid", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("ViewZenossGroupsFragment", "onCreate", e);
                }

                handler.sendEmptyMessage(1);
            }
        }).start();
    }
}
