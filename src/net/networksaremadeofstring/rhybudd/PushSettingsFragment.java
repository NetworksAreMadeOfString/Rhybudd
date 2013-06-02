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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.bugsense.trace.BugSenseHandler;
import org.json.JSONException;

import java.io.IOException;

/**
 * Created by Gareth on 30/05/13.
 */
public class PushSettingsFragment extends Fragment
{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final String ARG_SECTION_NUMBER = "section_number";

    public PushSettingsFragment()
    {
    }

    EditText PushKey;
    TextView PushKeyDesc;
    Button SaveKey;
    Button ExitButton;
    Handler getIDhandler;
    ProgressBar progressbar;
    AlertDialog alertDialog;

    @Override
    public void onResume()
    {
        super.onResume();
        Log.e("onResume","Hello");
        if(!PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(ZenossAPI.PREFERENCE_URL, "").equals(""))
        {
            Log.e("onResume","Set visible");
            ExitButton.setVisibility(View.VISIBLE);
        }
        else
        {
            Log.e("onResume","Set invisible");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_push_first_settings, container, false);

        PushKey = (EditText) rootView.findViewById(R.id.pushKeyEditText);
        PushKeyDesc = (TextView) rootView.findViewById(R.id.pushIDDesc);
        SaveKey = (Button) rootView.findViewById(R.id.saveKeyButton);

        ExitButton = (Button) rootView.findViewById(R.id.exitButton);

        /*Log.i("forceRefreshonCreateView","setting forceRefresh to " + Boolean.toString(getArguments().getBoolean("forceRefresh")));
        if(getArguments().getBoolean("forceRefresh"))
        {
            ExitButton.setVisibility(View.VISIBLE);
        }*/

        progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        String pushKey = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(ZenossAPIv2.PREFERENCE_PUSHKEY, "");

        if(!pushKey.equals(""))
        {
            PushKey.setText(pushKey);
            PushKey.setVisibility(View.VISIBLE);
            PushKeyDesc.setVisibility(View.VISIBLE);
        }

        ExitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(ZenossAPI.PREFERENCE_URL, "").equals(""))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("Rhybudd needs details such as the URL and credentials in order to interact with Zenoss")
                            .setTitle("Zenoss Credentials Needed")
                            .setCancelable(false)
                            .setPositiveButton("Configure Zenoss Core", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    ((FirstRunSettings) getActivity()).setPushTab(0);
                                    alertDialog.cancel();
                                }
                            })
                            .setNeutralButton("Configure ZaaS", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    ((FirstRunSettings) getActivity()).setPushTab(1);
                                    alertDialog.cancel();
                                }
                            })
                            .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    Intent in = new Intent();
                                    getActivity().setResult(2,in);
                                    getActivity().finish();
                                }
                            });
                    alertDialog = builder.create();
                    alertDialog.show();
                }
                else
                {
                    progressbar.setVisibility(View.VISIBLE);

                    //if(PushKey.getText().length() == 32)
                   // {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                        editor.putString(ZenossAPI.PREFERENCE_PUSHKEY, PushKey.getText().toString());
                        editor.commit();
                    /*}
                    else
                    {
                        Toast.makeText(getActivity(), getString(R.string.PushKeyLengthError), Toast.LENGTH_SHORT).show();
                    }*/
                    progressbar.setVisibility(View.GONE);

                    Intent in = new Intent();
                    in.putExtra("forceRefresh",true);
                    in.putExtra(ZenossAPI.PREFERENCE_PUSHKEY,PushKey.getText().toString());
                    getActivity().setResult(1,in);
                    getActivity().finish();
                }

            }
        });

        SaveKey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                progressbar.setVisibility(View.VISIBLE);
                if(PushKey.getText().length() == 32)
                {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putString(ZenossAPI.PREFERENCE_PUSHKEY, PushKey.getText().toString());
                    editor.commit();
                }
                else
                {
                    Toast.makeText(getActivity(), getString(R.string.PushKeyLengthError), Toast.LENGTH_SHORT).show();
                }
                progressbar.setVisibility(View.GONE);
            }
        });

        Button setID = (Button) rootView.findViewById(R.id.setPushIDButton);
        setID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                PushKey.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                PushKey.setVisibility(View.VISIBLE);

                /*SaveKey.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                SaveKey.setVisibility(View.VISIBLE);*/
                PushKeyDesc.setVisibility(View.INVISIBLE);
                PushKey.setText("");
            }
        });

        Button getID = (Button) rootView.findViewById(R.id.GenerateNewIDButton);
        getID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                SaveKey.setVisibility(View.GONE);
                PushKey.setText("");
                progressbar.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                progressbar.setVisibility(View.VISIBLE);

                ((Thread) new Thread(){
                    public void run()
                    {
                        try
                        {
                            String PushKey = ZenossAPI.getPushKey();
                            if(null != PushKey)
                            {
                                Message msg = new Message();
                                Bundle bundle = new Bundle();

                                msg.what = RhybuddHandlers.msg_generic_success;
                                bundle.putString(ZenossAPIv2.PREFERENCE_PUSHKEY, PushKey);
                                msg.setData(bundle);
                                Message.obtain();
                                getIDhandler.sendMessage(msg);
                            }
                            else
                            {
                                getIDhandler.sendEmptyMessage(RhybuddHandlers.msg_generic_failure);
                            }

                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            BugSenseHandler.sendExceptionMessage("PushSettingsFragment", "GetKey", e);
                            getIDhandler.sendEmptyMessage(RhybuddHandlers.msg_generic_http_transport_error);
                        }
                        catch (JSONException e)
                        {
                            e.printStackTrace();
                            BugSenseHandler.sendExceptionMessage("PushSettingsFragment", "GetKey", e);
                            getIDhandler.sendEmptyMessage(RhybuddHandlers.msg_json_error);
                        }
                    }
                }).start();
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getIDhandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                switch(msg.what)
                {
                    case RhybuddHandlers.msg_generic_success:
                    {
                        String pushKey = msg.getData().getString(ZenossAPIv2.PREFERENCE_PUSHKEY);

                        progressbar.setVisibility(View.GONE);
                        PushKey.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                        PushKey.setVisibility(View.VISIBLE);
                        PushKeyDesc.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                        PushKeyDesc.setVisibility(View.VISIBLE);
                        PushKey.setText(pushKey);

                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                        editor.putString(ZenossAPI.PREFERENCE_PUSHKEY, pushKey);
                        editor.commit();
                    }
                    break;

                    case RhybuddHandlers.msg_json_error:
                    {
                        progressbar.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), getString(R.string.JSONExceptionMessage), Toast.LENGTH_LONG).show();
                    }
                    break;

                    case RhybuddHandlers.msg_generic_http_transport_error:
                    {
                        progressbar.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), getString(R.string.HttpTransportExceptionMessage), Toast.LENGTH_LONG).show();
                    }
                    break;

                    case RhybuddHandlers.msg_generic_failure:
                    {
                        progressbar.setVisibility(View.GONE);
                        Toast.makeText(getActivity(), getString(R.string.GenericExceptionMessage), Toast.LENGTH_LONG).show();
                    }
                    break;
                }
            }
        };
    }
}