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
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.*;
import com.bugsense.trace.BugSenseHandler;
import com.google.android.gcm.GCMRegistrar;

import org.json.JSONException;

import java.io.IOException;

import static android.nfc.NdefRecord.createMime;

public class PushSettingsFragment extends Fragment implements NfcAdapter.CreateNdefMessageCallback
{
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final String ARG_SECTION_NUMBER = "section_number";

    public PushSettingsFragment()
    {
    }

    EditText FilterKey;
    TextView PushKeyDesc;
    Button SaveKey;
    Handler getIDhandler, checkZPHandler;
    ProgressBar progressbar;
    AlertDialog alertDialog;
    Switch enabledSwitch;
    Button registerWithZenPack, checkZenPack, exitButton;
    ImageView ZPInstalledImg;
    ImageView ZPDeviceRegistered;
    TextView ZPVersion;
    String regId = "";
    Boolean checkZPImmediately = true;
    ZenossAPI API = null;
    NfcAdapter mNfcAdapter;
    Boolean pushEnabled = false;
    String pushKey = "";
    String senderID = "";
    boolean hasZenPack = false;
    ZenPack zp = new ZenPack();

    @Override
    public void onResume()
    {
        super.onResume();
        //Log.e("onResume","Hello");
        /*if(!PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(ZenossAPI.PREFERENCE_URL, "").equals(""))
        {
            //Log.e("onResume","Set visible");
            ExitButton.setVisibility(View.VISIBLE);
        }
        else
        {
            //Log.e("onResume","Set invisible");
        }*/
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getActivity().getIntent().getAction())) {
            processIntent(getActivity().getIntent());
        }
    }


    void processIntent(Intent intent)
    {
        //textView = (TextView) findViewById(R.id.textView);
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        // record 0 contains the MIME type, record 1 is the AAR, if present
        FilterKey.setText(new String(msg.getRecords()[0].getPayload()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_push_config, container, false);

        progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        pushKey = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(ZenossAPIv2.PREFERENCE_PUSHKEY, "");
        pushEnabled = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ZenossAPIv2.PREFERENCE_PUSH_ENABLED,false);
        senderID = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(ZenossAPIv2.PREFERENCE_PUSH_SENDERID, "");

        FilterKey = (EditText) rootView.findViewById(R.id.pushFilterEditText);
        enabledSwitch = (Switch) rootView.findViewById(R.id.PushEnabledSwitch);
        registerWithZenPack = (Button) rootView.findViewById(R.id.RegisterButton);
        checkZenPack = (Button) rootView.findViewById(R.id.CheckButton);
        exitButton = (Button) rootView.findViewById(R.id.ExitButton);

        ZPInstalledImg = (ImageView) rootView.findViewById(R.id.ZPInstalledStatusImg);
        ZPDeviceRegistered = (ImageView) rootView.findViewById(R.id.ZPDevRegStatusImg);
        ZPVersion = (TextView) rootView.findViewById(R.id.ZenPackVersion);

        Log.e("pushKey",pushKey);

        //Set the filter
        if(!pushKey.equals(""))
        {
            FilterKey.setText(pushKey);
        }

        if(checkZPImmediately)
        {
            checkZenPack.setVisibility(View.GONE);
            exitButton.setVisibility(View.GONE);
            registerWithZenPack.setVisibility(View.VISIBLE);
        }
        else
        {
            checkZenPack.setVisibility(View.VISIBLE);
            exitButton.setVisibility(View.VISIBLE);
            registerWithZenPack.setVisibility(View.GONE);
        }

        //Set the enabled setting
        enabledSwitch.setChecked(pushEnabled);

        enabledSwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putBoolean(ZenossAPI.PREFERENCE_PUSH_ENABLED, b);
                editor.commit();

                if(b)
                {
                    RegisterWithZenPack();
                }
                else
                {
                    checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_not_registered);
                    GCMRegistrar.unregister(getActivity());
                }
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
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
                    /*progressbar.setVisibility(View.VISIBLE);

                    //if(PushKey.getText().length() == 32)
                   // {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putString(ZenossAPI.PREFERENCE_PUSHKEY, PushKey.getText().toString());
                    editor.commit();

                    progressbar.setVisibility(View.GONE);*/

                    Intent in = new Intent();
                    in.putExtra("forceRefresh",true);
                    //in.putExtra(ZenossAPI.PREFERENCE_PUSHKEY,PushKey.getText().toString());
                    getActivity().setResult(1,in);
                    getActivity().finish();
                }
            }
        });


        checkZenPack.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                checkZP();
            }
        });


        registerWithZenPack.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                if(hasZenPack)
                {
                    RegisterWithZenPack();
                }
                else
                {
                    Toast.makeText(getActivity(), getString(R.string.PushZPErrorNoZP), Toast.LENGTH_LONG).show();
                }
            }
        });



        /*Button getID = (Button) rootView.findViewById(R.id.GenerateNewIDButton);
        getID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                //SaveKey.setVisibility(View.GONE);
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
        });*/

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        if(checkZPImmediately)
        {
            Log.i("PushSettings","Checking ZenPack immediately");
            checkZP();
        }
        else
        {
            Log.i("PushSettings","Waiting for a button press to check ZenPack");
        }
    }

    private void RegisterWithZenPack()
    {
        progressbar.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
        progressbar.setVisibility(View.VISIBLE);

        (new Thread()
        {
            public void run()
            {
                try
                {
                    if(null == API)
                    {
                        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                        {
                            API = new ZenossAPIZaas();
                        }
                        else
                        {
                            API = new ZenossAPICore();
                        }

                        ZenossCredentials credentials = new ZenossCredentials(getActivity());

                        API.Login(credentials);
                    }

                    GCMRegistrar.checkDevice(getActivity());
                    GCMRegistrar.checkManifest(getActivity());
                    regId = GCMRegistrar.getRegistrationId(getActivity());

                    if (regId.equals(""))
                    {
                        int i = 10;

                        GCMRegistrar.register(getActivity(), zp.SenderID);

                        //I *really* want that regid
                        while(regId.equals("") || i > 0)
                        {
                            Log.i("pushsettings","sleeping");
                            regId = GCMRegistrar.getRegistrationId(getActivity());
                            i--;
                            sleep(500);
                        }
                    }

                    if(regId.equals(""))
                    {
                        checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_not_registered);
                    }
                    else
                    {
                        if(API.RegisterWithZenPack(ZenossAPI.md5(Settings.Secure.getString(getActivity().getContentResolver(),Settings.Secure.ANDROID_ID)),regId))
                        {
                            checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_registered);
                        }
                        else
                        {
                            checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_not_registered);
                        }
                    }
                }
                catch (Exception e)
                {
                    //firstLoadHandler.sendEmptyMessage(0);
                    BugSenseHandler.sendExceptionMessage("PushSettingsFragment", "RegisterWithZenPack", e);
                    checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_not_registered);
                }
            }
        }).start();
    }

    private void checkZP()
    {
        Log.i("PushSettings","Checking for ZenPack!");
        progressbar.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
        progressbar.setVisibility(View.VISIBLE);

        (new Thread()
        {
            public void run()
            {
                try
                {
                    if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                    {
                        API = new ZenossAPIZaas();
                    }
                    else
                    {
                        API = new ZenossAPICore();
                    }

                    ZenossCredentials credentials = new ZenossCredentials(getActivity());
                    API.Login(credentials);
                    zp = API.CheckZenPack();

                    if(null != zp && zp.Installed)
                    {
                        Log.e("ZP",zp.SenderID);

                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                        editor.putString("SenderID", zp.SenderID);
                        editor.commit();

                        checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_is_installed);

                        if(checkZPImmediately)
                            RegisterWithZenPack();
                    }
                    else
                    {
                        checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_not_installed);
                    }

                }
                catch(IllegalStateException i)
                {
                    checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_generic_http_transport_error);
                    checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_not_installed);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    checkZPHandler.sendEmptyMessage(RhybuddHandlers.msg_zp_not_installed);
                }
            }
        }).start();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if(null != getArguments() && getArguments().containsKey("checkZPImmediately"))
            checkZPImmediately = getArguments().getBoolean("checkZPImmediately");

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (mNfcAdapter == null)
        {
            /*Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;*/
        }
        else
        {
            // Register callback
            mNfcAdapter.setNdefPushMessageCallback(this, getActivity());
        }

        checkZPHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                progressbar.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
                progressbar.setVisibility(View.GONE);

                switch(msg.what)
                {
                    case RhybuddHandlers.msg_generic_http_transport_error:
                    {
                        Toast.makeText(getActivity(), getActivity().getString(R.string.PushZPNoHost), Toast.LENGTH_LONG).show();
                    }
                    break;

                    case RhybuddHandlers.msg_zp_is_installed:
                    {
                        hasZenPack = true;
                        ZPInstalledImg.setImageResource(R.drawable.ic_acknowledged);
                        ZPDeviceRegistered.setImageResource(R.drawable.ic_unacknowledged);
                        ZPVersion.setText("1.0.1");
                        enabledSwitch.setEnabled(hasZenPack);
                        registerWithZenPack.setEnabled(hasZenPack);

                        if(!checkZPImmediately)
                        {
                            checkZenPack.setVisibility(View.VISIBLE);
                            registerWithZenPack.setVisibility(View.GONE);
                        }
                    }
                    break;

                    case RhybuddHandlers.msg_zp_not_installed:
                    {
                        hasZenPack = false;
                        ZPInstalledImg.setImageResource(R.drawable.ic_unacknowledged);
                        ZPDeviceRegistered.setImageResource(R.drawable.ic_unacknowledged);
                        ZPVersion.setText("----");
                        enabledSwitch.setEnabled(hasZenPack);
                        registerWithZenPack.setEnabled(hasZenPack);
                    }
                    break;

                    case RhybuddHandlers.msg_zp_registered:
                    {
                        ZPDeviceRegistered.setImageResource(R.drawable.ic_acknowledged);
                    }
                    break;

                    case RhybuddHandlers.msg_zp_not_registered:
                    {
                        ZPDeviceRegistered.setImageResource(R.drawable.ic_unacknowledged);
                    }
                    break;
                }
            }
        };

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
                        //PushKey.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                        //PushKey.setVisibility(View.VISIBLE);
                        PushKeyDesc.startAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
                        PushKeyDesc.setVisibility(View.VISIBLE);
                        //PushKey.setText(pushKey);

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

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent)
    {
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { createMime(
                        "application/vnd.net.networksaremadeofstring.rhybudd.push", FilterKey.getText().toString().getBytes())
                        //,NdefRecord.createApplicationRecord("com.example.android.beam")
                });
        return msg;
    }
}