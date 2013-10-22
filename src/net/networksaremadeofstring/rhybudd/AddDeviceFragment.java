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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;

import org.apache.http.cookie.CookieAttributeHandler;

public class AddDeviceFragment extends Fragment
{
    public static final String TWOPANEINDICATOR = "twopane";
    boolean isTwoPane = true;

    ZenossPoller mService;
    boolean mBound = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.add_device_fragment, container, false);

        try
        {
            if(isTwoPane)
            {
                ((Button) rootView.findViewById(R.id.ExitButton)).setVisibility(View.INVISIBLE);
            }
            else
            {
                ((Button) rootView.findViewById(R.id.ExitButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        getActivity().finish();
                    }
                });
            }
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("AddDeviceFragment", "onCreateView", e);
        }

        ((Button) rootView.findViewById(R.id.AddDeviceButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                ((Thread) new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            if(mBound)
                            {
                                if(null == mService.API || null == mService.API.ZENOSS_INSTANCE || !mService.API.LoginSuccessful)
                                {
                                    //Log.e("onlick","Either the API or Zenoss Instance was wrong");
                                    mService.PrepAPI(true,true);
                                }

                                if(mService.API.isHTTPClientAlive() && mService.API.LoginSuccessful)
                                {
                                    mService.API.AddDevice(((EditText) getActivity().findViewById(R.id.FQDN)).getText().toString(),
                                            ((EditText) getActivity().findViewById(R.id.Title)).getText().toString(),
                                            ((Spinner) getActivity().findViewById(R.id.deviceClassSpinner)).getSelectedItem().toString(),
                                            ((Spinner) getActivity().findViewById(R.id.productionStateSpinner)).getSelectedItem().toString(),
                                            ((Spinner) getActivity().findViewById(R.id.devicePrioritySpinner)).getSelectedItem().toString());
                                }
                                else
                                {
                                    Toast.makeText(getActivity(), "Unable to add device.\n\nPlease try again later.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else
                            {
                                Toast.makeText(getActivity(), "Unable to add device.\n\nPlease try again later.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            BugSenseHandler.sendExceptionMessage("AddDeviceFragment", "OnAPICall", e);
                        }
                    }
                }).start();

                try
                {
                    Toast.makeText(getActivity(), "Device added to the Zenoss queue", Toast.LENGTH_SHORT).show();

                    ((EditText) getActivity().findViewById(R.id.FQDN)).setText("");
                    ((EditText) getActivity().findViewById(R.id.Title)).setText("");
                }
                catch (Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("AddDeviceFragment", "Toast and EditText", e);
                }
            }
        });
        return rootView;
    }

    @Override
    public void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        doBindService();

        try
        {
            if(getArguments().containsKey(TWOPANEINDICATOR))
                isTwoPane = getArguments().getBoolean(TWOPANEINDICATOR);
        }
        catch(Exception e)
        {
            BugSenseHandler.sendExceptionMessage("AddDeviceFragment", "onCreate", e);
        }
    }



    void doUnbindService()
    {
        try
        {
            if (mBound)
            {
                // Detach our existing connection.
                getActivity().unbindService(mConnection);
                mBound = false;
            }
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("AddDeviceFragment", "doUnbindService", e);
        }
    }

    void doBindService()
    {
        try
        {
            getActivity().bindService(new Intent(getActivity(), ZenossPoller.class), mConnection, Context.BIND_AUTO_CREATE);
            mBound = true;
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("AddDeviceFragment", "doBindService", e);
        }
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
            try
            {
                // We've bound to LocalService, cast the IBinder and get LocalService instance
                ZenossPoller.LocalBinder binder = (ZenossPoller.LocalBinder) service;
                mService = binder.getService();
                mBound = true;
                //Toast.makeText(getActivity(), "Connected to Service", Toast.LENGTH_SHORT).show();
            }
            catch (Exception e)
            {
                BugSenseHandler.sendExceptionMessage("AddDeviceFragment", "onServiceConnected", e);
                mBound = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            //Toast.makeText(getActivity(), "Disconnected from Service", Toast.LENGTH_SHORT).show();
            //Log.e("onServiceDisconnected", "Disconnecting");
            mBound = false;
        }
    };
}
