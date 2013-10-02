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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ZaasSettingsFragment extends Fragment
{

    public static final String ARG_SECTION_NUMBER = "section_number";

    SharedPreferences settings = null;
    ProgressDialog dialog;
    Thread peformLogin;
    Handler handler;
    ZenossAPIZaas API;
    EditText zURL;
    EditText zUserName;
    EditText zPasword;
    EditText BAUser;
    EditText BAPassword;
    SimpleDateFormat s = new SimpleDateFormat("dd/MM/yyy hh:mm:ss");
    ZenossCredentials zenossCredentials;
    AlertDialog alertDialog;
    TextView debugLabel;
    EditText debugOutput;


    public ZaasSettingsFragment()
    {
    }

    private void UpdateDebugMessage(String Message)
    {
        android.os.Message msg = new Message();
        Bundle bundle = new Bundle();

        msg.what = RhybuddHandlers.msg_initial_verify_debug_output;
        bundle.putString(ZenossAPI.MSG_DEBUG, Message);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_zaas_first_settings, container, false);

        zURL = (EditText) rootView.findViewById(R.id.ZenossURL);
        zUserName = (EditText) rootView.findViewById(R.id.ZenossUserName);
        zPasword = (EditText) rootView.findViewById(R.id.ZenossPassword);
        debugOutput = (EditText) rootView.findViewById(R.id.debugOutput);
        debugLabel = (TextView) rootView.findViewById(R.id.debugLabel);

        Button LoginButton = (Button) rootView.findViewById(R.id.SaveButton);
        LoginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                dialog.show();

                UpdateDebugMessage("Starting Login Process");

                ((Thread) new Thread(){
                    public void run()
                    {
                        try
                        {
                            UpdateDebugMessage("API initialised");
                            API = new ZenossAPIZaas();


                            if(API != null)
                            {
                                zenossCredentials = new ZenossCredentials(zUserName.getText().toString(),
                                        zPasword.getText().toString(),
                                        "https://" + zURL.getText().toString() +".zenaas.com");

                                if(API.Login(zenossCredentials))
                                {
                                    UpdateDebugMessage("Login process returned");
                                    handler.sendEmptyMessage(RhybuddHandlers.msg_initial_login_successful);
                                }
                                else
                                {
                                    UpdateDebugMessage(API.getLastException());
                                    handler.sendEmptyMessage(RhybuddHandlers.msg_initial_verify_error);
                                }
                            }
                            else
                            {
                                //HandleException(null, "Initialising the API Failed. An error message has been logged.");
                                UpdateDebugMessage("API initialisation failed!");
                                handler.sendEmptyMessage(RhybuddHandlers.msg_initial_verify_error);
                            }
                        }
                        catch(Exception e)
                        {
                            BugSenseHandler.sendExceptionMessage("ZaasSettingsFragment", "LoginButton", e);
                            e.printStackTrace();
                            //HandleException(e, "Initialising the API Failed. An error message has been logged.");
                            if(null != e.getMessage())
                            {
                                UpdateDebugMessage(e.getMessage() + " " + e.getLocalizedMessage().toString() + " " + e.toString());
                            }
                            else
                            {
                                UpdateDebugMessage(e.getLocalizedMessage() + " " + e.toString());
                            }
                            handler.sendEmptyMessage(RhybuddHandlers.msg_initial_verify_error);
                        }
                    }}).start();
            }
        });

        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        dialog = new ProgressDialog(getActivity());
        dialog.setTitle("");
        dialog.setMessage("Checking Details.....");
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface)
            {
                Intent in = new Intent();
                getActivity().setResult(2,in);
                getActivity().finish();
            }
        });

        settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                if(msg.what == RhybuddHandlers.msg_initial_login_successful && API != null)// && API.getLoggedInStatus() == true
                {
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("credentialsSuccess", true);
                    editor.commit();

                    try
                    {
                        dialog.setMessage("Logged in successfully.\n\nPreparing database...");
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ZaasSettingsFragment", "OnCreate", e);
                        Toast.makeText(getActivity(), "Logged in Successfully. Preparing Database!", Toast.LENGTH_SHORT).show();
                    }

                    try
                    {
                        zenossCredentials.Zaas = true;
                        zenossCredentials.saveCredentials(getActivity());
                        handler.sendEmptyMessage(RhybuddHandlers.msg_caching_complete);
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ZaasSettingsFragment", "General success path", e);
                        //HandleException(e, "Initialising the Database failed. An error message has been logged.");
                    }
                }
                else if(msg.what == RhybuddHandlers.msg_events_cached)
                {
                    dialog.setMessage("Events Cached! Now caching Devices.\r\nPlease wait...");
                }
                else if (msg.what == RhybuddHandlers.msg_caching_complete)
                {
                    try
                    {
                        dialog.dismiss();
                    }
                    catch(Exception e)
                    {
                        //Not much else we can do here :/
                        BugSenseHandler.sendExceptionMessage("ZaasSettingsFragment","Dismissing dialog in msg_caching_complete",e);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage("Would you like to configure Rhybudd Push to enable instant alert delivery?")
                            .setTitle("Extra Configuration")
                            .setCancelable(false)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    ((FirstRunSettings) getActivity()).setPushTab(2);
                                    alertDialog.cancel();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    alertDialog.cancel();
                                    Intent in = new Intent();
                                    in.putExtra("forceRefresh",true);
                                    getActivity().setResult(1,in);
                                    getActivity().finish();
                                }
                            });
                    alertDialog = builder.create();
                    alertDialog.show();

                }
                else if(msg.what == RhybuddHandlers.msg_caching_complete)
                {
                    dialog.setMessage("Caching complete!\r\nVerifying.");
                }
                else if(msg.what == RhybuddHandlers.msg_initial_verify_debug_output)
                {
                    //EditText debugOutput = (EditText) getActivity().findViewById(R.id.debugOutput);

                    debugOutput.setText(debugOutput.getText() + s.format(new Date()) + " " + msg.getData().getString(ZenossAPI.MSG_DEBUG) + "\r\n");
                }
                else if(msg.what == RhybuddHandlers.msg_initial_verify_error)
                {
                    //((TextView) getActivity().findViewById(R.id.debugOutput)).setText(msg.getData().getString("exception") + "\n");

                    try
                    {
                        dialog.dismiss();
                    }
                    catch(Exception e)
                    {
                        //Not much else we can do here :/
                        BugSenseHandler.sendExceptionMessage("ZaasSettingsFragment","Dismissing dialog in msg_initial_verify_error",e);
                    }

                    try
                    {
                        Toast.makeText(getActivity(), "An error was encountered. The debug output can be seen below", Toast.LENGTH_SHORT).show();
                        debugLabel.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                        debugLabel.setVisibility(View.VISIBLE);
                        debugOutput.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                        debugOutput.setVisibility(View.VISIBLE);
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ZaasSettingsFragment","Displaying toast with error message",e);
                        Toast.makeText(getActivity(), "An unknown error occured. It has been reported.", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    try
                    {
                        dialog.dismiss();
                    }
                    catch(Exception e)
                    {
                        //Not much else we can do here :/
                        BugSenseHandler.sendExceptionMessage("ZaasSettingsFragment","Dismissing dialog in final handler else",e);
                    }

                    Toast.makeText(getActivity(), "Login Failed - Please check details.", Toast.LENGTH_SHORT).show();
                    debugOutput.setVisibility(View.VISIBLE);
                }
            }
        };
    }
}