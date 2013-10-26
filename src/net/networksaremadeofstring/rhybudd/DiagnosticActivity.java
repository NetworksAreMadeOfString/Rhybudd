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


import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticActivity extends FragmentActivity
{
    private Handler progressHandler;
    private static final String DIAGNOSTIC_OUTPUT = "diagnostic_output";
    private static final int DIAGNOSTIC_SUCCESS = 0;
    private static final int DIAGNOSTIC_COMPLETE = 1;
    private static final int DIAGNOSTIC_PRE = 2;
    private static final int DIAGNOSTIC_FAILURE = 99;

    TextView DiagnosticOutput;
    ProgressBar progressBar;
    int preProgress = 2;
    int Progess = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostics);
        try
        {
            getActionBar().setTitle("Diagnostics App");
            getActionBar().setSubtitle("Helps to pin point any problems getting events");
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        catch (Exception e)
        {

        }

        DiagnosticOutput = (TextView) findViewById(R.id.DiagOutput);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        ((Button) findViewById(R.id.StartDiagButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                findViewById(R.id.progressBar2).setVisibility(View.VISIBLE);
                DiagnosticOutput.setText("");
                DoDiagnostics();
                view.setEnabled(false);
            }
        });

        progressHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                try
                {
                    DiagnosticOutput.setText( DiagnosticOutput.getText().toString() + msg.getData().getString(DIAGNOSTIC_OUTPUT) + "\n");
                }
                catch (Exception e)
                {
                    DiagnosticOutput.setText(DiagnosticOutput.getText().toString() + "A diagnostic message was received but there was an issue displaying it\n");
                }

                if(msg.what == DIAGNOSTIC_PRE)
                {
                    try
                    {
                        if(progressBar.getSecondaryProgress() < 20)
                            progressBar.setSecondaryProgress((progressBar.getSecondaryProgress() + 1));
                    }
                    catch (Exception e)
                    {

                    }
                }
                else if(msg.what == DIAGNOSTIC_SUCCESS)
                {
                    try
                    {
                        if(progressBar.getProgress() < 20)
                            progressBar.setProgress((progressBar.getProgress() + 1));
                    }
                    catch (Exception e)
                    {

                    }
                }
                else if(msg.what == DIAGNOSTIC_FAILURE)
                {
                    try
                    {
                        DiagnosticOutput.setTextColor(getResources().getColor(R.color.WarningRed));
                        findViewById(R.id.progressBar2).setVisibility(View.INVISIBLE);
                        findViewById(R.id.StartDiagButton).setEnabled(true);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else if(msg.what == DIAGNOSTIC_COMPLETE)
                {
                    try
                    {
                        DiagnosticOutput.setTextColor(Color.rgb(50,102,50));
                        progressBar.setProgress(20);
                        findViewById(R.id.progressBar2).setVisibility(View.INVISIBLE);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }

                    try
                    {
                        Toast.makeText(DiagnosticActivity.this,"Diagnostics Complete!",Toast.LENGTH_SHORT).show();
                        findViewById(R.id.StartDiagButton).setEnabled(true);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                else
                {
                    //Not much else
                }

                try
                {
                    progressBar.invalidate();
                }
                catch (Exception e)
                {

                }
            }
        };
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    private void DoDiagnostics()
    {
        (new Thread()
        {
            Message msg = Message.obtain();
            //Bundle bundle = new Bundle();
            ZenossAPI API;
            SharedPreferences settings;
            public void run()
            {
                msg.what = DIAGNOSTIC_SUCCESS;

                try
                {
                    //Lets get started
                    SendUpdate("Getting Zenoss details", DIAGNOSTIC_PRE);

                    //Lets have a look at the preferences
                    settings = PreferenceManager.getDefaultSharedPreferences(DiagnosticActivity.this);

                    ZenossCredentials credentials = new ZenossCredentials(DiagnosticActivity.this);
                    SendUpdate("Credentials object initialized", DIAGNOSTIC_SUCCESS);

                    //Show them their credentials
                    SendUpdate("Auth options;\n" +
                            "\tURL: " + credentials.URL + "\n" +
                            "\tUser: " + credentials.UserName + "\n" +
                            "\tPassword: Not shown", DIAGNOSTIC_PRE);

                    //Sleep==============================================================================================================
                    sleep(1000);

                    SendUpdate("Initialising the Rhybudd API",DIAGNOSTIC_PRE);
                    //Connect to Zenoss
                    if(settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
                    {
                        SendUpdate("Detected a ZAAS Configuration. Prepping ZaaS API",DIAGNOSTIC_SUCCESS);
                        API = new ZenossAPIZaas();
                    }
                    else
                    {
                        SendUpdate("Detected a Zenoss Core Configuration. Prepping Core API",DIAGNOSTIC_SUCCESS);
                        API = new ZenossAPICore();
                    }

                    //Sleep==============================================================================================================
                    sleep(1000);

                    SendUpdate("Attempting to login...",DIAGNOSTIC_PRE);
                    Boolean loginSuccessful = API.Login(credentials);

                    if(loginSuccessful)
                    {
                        SendUpdate("Login appeared to be successful.",DIAGNOSTIC_SUCCESS);
                        SendUpdate("Getting a list of Events based on your settings via the Rhybudd API",DIAGNOSTIC_PRE);
                        List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();

                        //Sleep==============================================================================================================
                        sleep(1000);

                        listOfZenossEvents = API.GetRhybuddEvents(DiagnosticActivity.this);

                        try
                        {
                            //Sleep==============================================================================================================
                            sleep(1000);

                            if(listOfZenossEvents.size() > 0)
                            {
                                SendUpdate("We successfully got a list of events through the API interface!",DIAGNOSTIC_SUCCESS);
                            }
                            else
                            {
                                SendUpdate("We successfully queried Zenoss but got no devices back",DIAGNOSTIC_SUCCESS);
                            }
                        }
                        catch (NullPointerException npe)
                        {
                            //Sleep==============================================================================================================
                            sleep(1000);
                            SendUpdate("We received null back which indicates an internal issue.",DIAGNOSTIC_SUCCESS);
                        }

                        //Sleep==============================================================================================================
                        sleep(1000);

                        SendUpdate("Performing a manual query (not using the Rhybudd API) with very permissive filters (e.g. all Production states, all severities) without #ZEN-2812 fix",DIAGNOSTIC_PRE);

                        try
                        {
                            //Sleep==============================================================================================================
                            sleep(1000);

                            String eventsRawJSON = DoRawQuery(false);
                            SendUpdate("We received a payload from the query.",DIAGNOSTIC_SUCCESS);
                            if(null != eventsRawJSON)
                            {
                                SendUpdate("Attemping to convert payload to JSON",DIAGNOSTIC_PRE);
                                try
                                {
                                    JSONObject json = new JSONObject(eventsRawJSON);
                                    if(json instanceof  JSONObject)
                                    {
                                        SendUpdate("The Payload successfully parsed as JSON full payload was;\n\n" + json.toString(3),DIAGNOSTIC_SUCCESS);
                                    }
                                    else
                                    {
                                        SendUpdate("The data returned from the query wasn't valid. Attempting to display:", DIAGNOSTIC_FAILURE);
                                        SendUpdate("\t" + eventsRawJSON, DIAGNOSTIC_FAILURE);
                                    }
                                }
                                catch (Exception e)
                                {
                                    SendUpdate(e.getMessage(), DIAGNOSTIC_FAILURE);
                                }
                            }
                            else
                            {
                                SendUpdate("We received null back which indicates an issue but not an exception.",DIAGNOSTIC_SUCCESS);
                            }
                        }
                        catch (Exception e)
                        {
                            //Sleep==============================================================================================================
                            sleep(1000);
                            SendUpdate("There was an exception;\n" + e.getMessage(),DIAGNOSTIC_SUCCESS);
                            //Sleep==============================================================================================================
                            sleep(500);
                            SendUpdate("Trying a query with the fix for #ZEN-2812",DIAGNOSTIC_SUCCESS);
                            DoRawQuery(true);
                        }

                        //Sleep==============================================================================================================
                        sleep(1000);

                        //---------------------------------------------------------------------------------------------------------------------------------------------------------
                        // And we're done!
                        //---------------------------------------------------------------------------------------------------------------------------------------------------------
                        SendUpdate("Still here? Awesome.\n\n\nAll tests appear to have passed.",DIAGNOSTIC_COMPLETE);
                    }
                    else
                    {
                        SendUpdate("Login Failed.\n\nMessage was: " + API.getLastException() ,DIAGNOSTIC_FAILURE);
                    }
                }
                catch (Exception e)
                {
                    SendUpdate(e.getMessage(), DIAGNOSTIC_FAILURE);
                }
            }

            private String DoRawQuery(boolean Zenoss41) throws Exception
            {
                HttpPost httpost = new HttpPost(API.ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");

                httpost.addHeader("Content-type", "application/json; charset=utf-8");
                httpost.setHeader("Accept", "application/json");

                JSONObject dataContents = new JSONObject();
                dataContents.put("start", 0);
                dataContents.put("limit", 10);
                dataContents.put("dir", "DESC");
                dataContents.put("sort", "severity");

                if(Zenoss41)
                {
                    dataContents.put("keys", new JSONArray("[evid,count,prodState,firstTime,severity,component,summary,eventState,device,eventClass,lastTime,ownerid]"));
                }

                JSONObject params = new JSONObject();
                params.put("severity", new JSONArray("[5,4,3,2]"));
                params.put("eventState", new JSONArray("[0, 1]"));

                /*if(null != SummaryFilter && !SummaryFilter.equals(""))
                {
                    params.put("summary", SummaryFilter);
                }*/

                /*if(null != DeviceFilter && !DeviceFilter.equals(""))
                {
                    params.put("device", DeviceFilter);
                }*/

                /*if(ProductionOnly)
                {
                    params.put("prodState", new JSONArray("[1000]"));
                }*/

                dataContents.put("params", params);

                JSONArray data = new JSONArray();
                data.put(dataContents);

                JSONObject reqData = new JSONObject();
                reqData.put("action", "EventsRouter");
                reqData.put("method", "query");
                reqData.put("data", data);
                reqData.put("type", "rpc");
                reqData.put("tid", String.valueOf(API.reqCount++));

                httpost.setEntity(new StringEntity(reqData.toString()));
                HttpResponse response = API.httpclient.execute(httpost);
                String eventsRawJSON = EntityUtils.toString(response.getEntity());
                response.getEntity().consumeContent();
                //JSONObject json = new JSONObject(eventsRawJSON);
                return eventsRawJSON;
            }

            private void SendUpdate(String Message, int What)
            {
                msg = progressHandler.obtainMessage(What);
                //msg.what = What;
                Bundle bundle = new Bundle();
                bundle.putString(DIAGNOSTIC_OUTPUT,Message);
                msg.setData(bundle);
                progressHandler.sendMessage(msg);
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                finish();
                return true;
            }

            default:
            {
                return false;
            }
        }
    }
}
