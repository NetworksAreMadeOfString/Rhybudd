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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.bugsense.trace.BugSenseHandler;
import org.json.JSONArray;
import org.json.JSONObject;


public class ViewZenossEventFragment extends Fragment
{
    JSONObject EventObject = null;
    JSONObject EventDetails = null;
    private SharedPreferences settings = null;
    Handler firstLoadHandler, addLogMessageHandler;
    ProgressDialog dialog, addMessageProgressDialog;
    Thread dataPreload, addLogMessageThread;
    Dialog addMessageDialog;
    String[] LogEntries;
    ZenossAPI API;

    TextView Title;
    TextView Component;
    TextView EventClass;
    TextView Summary;
    TextView FirstTime;
    TextView LastTime;
    LinearLayout logList;
    ImageView ackIcon;
    ImageView img;
    TextView EventCount;
    TextView agent;
    ProgressBar progressbar;
    Boolean isAcknowledged = false;
    private Callbacks mCallbacks = sDummyCallbacks;

    public ViewZenossEventFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.view_zenoss_event, container, false);

        Title = (TextView) rootView.findViewById(R.id.EventTitle);
        Component = (TextView) rootView.findViewById(R.id.Componant);
        EventClass = (TextView) rootView.findViewById(R.id.EventClass);
        Summary = (TextView) rootView.findViewById(R.id.Summary);
        FirstTime = (TextView) rootView.findViewById(R.id.FirstTime);
        LastTime = (TextView) rootView.findViewById(R.id.LastTime);
        ackIcon = (ImageView) rootView.findViewById(R.id.ackIcon);
        img = (ImageView) rootView.findViewById(R.id.summaryImage);
        EventCount = (TextView) rootView.findViewById(R.id.EventCount);
        agent = (TextView) rootView.findViewById(R.id.Agent);
        logList = (LinearLayout) rootView.findViewById(R.id.LogList);
        progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        ackIcon.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View view)
            {
                progressbar.setVisibility(View.VISIBLE);

                ((Thread) new Thread(){
                    public void run()
                    {
                        if (settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                        {
                            API = new ZenossAPIZaas();
                        }
                        else
                        {
                            API = new ZenossAPICore();
                        }

                        ZenossCredentials credentials = new ZenossCredentials(getActivity());
                        try
                        {
                            API.Login(credentials);
                            JSONObject ackJSON = API.AcknowledgeEvent(getArguments().getString("EventID"));

                            if(ackJSON.getJSONObject("result").getBoolean("success"))
                            {
                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        progressbar.setVisibility(View.INVISIBLE);
                                        ackIcon.setImageResource(R.drawable.ic_acknowledged);
                                        mCallbacks.onItemAcknowledged(getArguments().getInt("position"));
                                    }
                                });

                                RhybuddDataSource datasource = null;
                                try
                                {
                                    datasource = new RhybuddDataSource(getActivity());
                                    datasource.open();
                                    datasource.ackEvent(getArguments().getString("EventID"));
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                    BugSenseHandler.sendExceptionMessage("ViewZenossEventFragmentUpdate", "DBUpdate", e);
                                }
                                finally
                                {
                                    if(null != datasource)
                                        datasource.close();
                                }
                            }
                            else
                            {
                                Toast.makeText(getActivity(),"Unable to acknowledge alert",Toast.LENGTH_SHORT).show();
                            }
                        }
                        catch(Exception e)
                        {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    Toast.makeText(getActivity(),"Unable to acknowledge alert",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater)
    {
        inflater.inflate(R.menu.view_event, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    public interface Callbacks
    {
        public void onItemAcknowledged(int position);
    }

    private static Callbacks sDummyCallbacks = new Callbacks()
    {
        public void onItemAcknowledged(int position)
        {
        }
    };

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks))
        {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        Log.e("onAttach", "Attached");
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                getActivity().finish();
                return true;
            }

            case R.id.AddLog:
            {
                addMessageDialog = new Dialog(getActivity());
                addMessageDialog.setContentView(R.layout.add_message);
                addMessageDialog.setTitle("Add Message to Event Log");
                ((Button) addMessageDialog.findViewById(R.id.SaveButton)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AddLogMessage(((EditText) addMessageDialog.findViewById(R.id.LogMessage)).getText().toString());
                        addMessageDialog.dismiss();
                    }
                });

                addMessageDialog.show();
                return true;
            }

            case R.id.escalate:
            {
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

                // Add data to the intent, the receiving app will decide what to do with it.
                intent.putExtra(Intent.EXTRA_SUBJECT, "Escalation of Zenoss Event on " + Title.getText());
                String EventDetails = Summary.getText() + "\r\r\n" +
                        LastTime.getText() + "\r\r\n" +
                        "Count: " + EventCount.getText();

                intent.putExtra(Intent.EXTRA_TEXT, EventDetails);

                startActivity(Intent.createChooser(intent, "How would you like to escalate this event?"));
            }

            default:
            {
                return false;
            }
        }
    }

    /*@Override
    public void onPause()
    {
        super.onPause();
    }*/

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        //Log.e("Saving","Saving some data");

        outState.putString("Title",Title.getText().toString());
        outState.putString("Component",Component.getText().toString());
        outState.putString("EventClass",EventClass.getText().toString());
        outState.putString("Summary",Summary.getText().toString());
        outState.putString("FirstTime",FirstTime.getText().toString());
        outState.putString("LastTime",LastTime.getText().toString());
        outState.putString("EventCount",EventCount.getText().toString());
        outState.putString("agent",agent.getText().toString());

        outState.putStringArray("LogEntries",LogEntries);

        try
        {
            if (isAcknowledged)
            {
                outState.putBoolean("eventStateAcknowledged",true);
            }
            else
            {
                outState.putBoolean("eventStateAcknowledged",false);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            outState.putParcelable("img",((BitmapDrawable) img.getDrawable()).getBitmap());
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        if(null != bundle)
        {
            //Log.e("Saving","Found a bundle!!!!");

            if(bundle.containsKey("eventStateAcknowledged") && bundle.getBoolean("eventStateAcknowledged"))
            {
                ackIcon.setImageResource(R.drawable.ic_acknowledged);
                isAcknowledged = true;
            }

            if(bundle.containsKey("Title"))
                Title.setText(bundle.getString("Title"));

            if(bundle.containsKey("Component"))
                Component.setText(bundle.getString("Component"));

            if(bundle.containsKey("EventClass"))
                EventClass.setText(bundle.getString("EventClass"));

            if(bundle.containsKey("Summary"))
                Summary.setText(Html.fromHtml(bundle.getString("Summary"), null, null));

            if(bundle.containsKey("FirstTime"))
                FirstTime.setText(bundle.getString("FirstTime"));

            if(bundle.containsKey("LastTime"))
                LastTime.setText(bundle.getString("LastTime"));

            if(bundle.containsKey("EventCount"))
                EventCount.setText(bundle.getString("EventCount"));

            if(bundle.containsKey("agent"))
                agent.setText(bundle.getString("agent"));

            if(bundle.containsKey("LogEntries"))
            {
                try
                {
                    String[] LogEntries = bundle.getStringArray("LogEntries");
                    int LogEntryCount = LogEntries.length;

                    for (int i = 0; i < LogEntryCount; i++)
                    {
                        TextView newLog = new TextView(getActivity());
                        newLog.setText(Html.fromHtml(LogEntries[i]));
                        newLog.setPadding(0, 6, 0, 6);
                        logList.addView(newLog);
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }

            if(bundle.containsKey("img"))
            {
                try
                {
                    img.setImageBitmap((Bitmap) bundle.getParcelable("img"));
                    img.invalidate();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }

            progressbar.setVisibility(View.INVISIBLE);
        }
        else
        {
            //Log.e("Saving","Didn't find any data so getting it");
            preLoadData();
        }

        super.onActivityCreated(bundle);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        //Log.e("Saving","Resuming");
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        BugSenseHandler.initAndStartSession(getActivity(), "44a76a8c");

        settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        firstLoadHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                //dialog.dismiss();
                try
                {
                    if (EventObject.has("result") && EventObject.getJSONObject("result").getBoolean("success") == true)
                    {
                        EventDetails = EventObject.getJSONObject("result").getJSONArray("event").getJSONObject(0);

                        try
                        {
                            //Log.e("Ack",EventDetails.getString("eventState"));
                            if (EventDetails.getString("eventState").equals("Acknowledged") ||EventDetails.getString("eventState").equals("1") )
                            {
                                ackIcon.setImageResource(R.drawable.ic_acknowledged);
                                isAcknowledged = true;
                            }
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                        //Log.e("EventDetails", EventDetails.toString(3));

                        try {
                            Title.setText(EventDetails.getString("device_title"));
                        } catch (Exception e) {
                            Title.setText("Unknown Device - Event Details");
                        }

                        try {
                            Component.setText(EventDetails.getString("component"));
                        } catch (Exception e) {
                            Component.setText("Unknown Component");
                        }

                        try {
                            EventClass.setText(EventDetails.getString("eventClassKey"));
                        } catch (Exception e) {
                            EventClass.setText("Unknown Event Class");
                        }

                        try
                        {
                            //ImageView img = (ImageView) findViewById(R.id.summaryImage);

                            URLImageParser p = new URLImageParser(img, getActivity());

                            Spanned htmlSpan = Html.fromHtml(EventDetails.getString("message"), p, null);

                            Summary.setText(Html.fromHtml(EventDetails.getString("message"), null, null));

                            //Log.i("Summary", EventDetails.getString("message"));

                            try
                            {
                                Summary.setMovementMethod(LinkMovementMethod.getInstance());
                            }
                            catch (Exception e)
                            {
                                //Worth a shot
                            }
                        }
                        catch (Exception e)
                        {
                            Summary.setText("No Summary available");
                        }

                        try {
                            FirstTime.setText(EventDetails.getString("firstTime"));
                        } catch (Exception e) {
                            FirstTime.setText("No Start Date Provided");
                        }

                        try {
                            LastTime.setText(EventDetails.getString("stateChange"));
                        } catch (Exception e) {
                            LastTime.setText("No Recent Date Provided");
                        }

                        try {
                            EventCount.setText("Count: " + EventDetails.getString("count"));
                        } catch (Exception e) {
                            EventCount.setText("Count: ??");
                        }

                        try
                        {
                            agent.setText(EventDetails.getString("agent"));
                        }
                        catch (Exception e)
                        {
                            agent.setText("unknown");
                        }

                        try
                        {
                            JSONArray Log = EventDetails.getJSONArray("log");

                            int LogEntryCount = Log.length();

                            if (LogEntryCount == 0)
                            {
                                TextView newLog = new TextView(getActivity());
                                newLog.setText("No log entries could be found");
                                LogEntries = new String[1];
                                LogEntries[0] = "No log entries could be found";
                                logList.addView(newLog);
                            }
                            else
                            {
                                LogEntries = new String[LogEntryCount];


                                for (int i = 0; i < LogEntryCount; i++)
                                {
                                    LogEntries[i] = "<strong>" + Log.getJSONArray(i).getString(0) + "</strong> wrote " + Log.getJSONArray(i).getString(2) + "\n<br/><strong>At:</strong> " + Log.getJSONArray(i).getString(1);

                                    TextView newLog = new TextView(getActivity());
                                    newLog.setText(Html.fromHtml(LogEntries[i]));
                                    newLog.setPadding(0, 6, 0, 6);

                                    logList.addView(newLog);
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            TextView newLog = new TextView(getActivity());
                            newLog.setText("No log entries could be found");
                            newLog.setPadding(0, 6, 0, 6);
                            logList.addView(newLog);
                        }

                        progressbar.setVisibility(View.INVISIBLE);
                    }
                    else
                    {
                        Toast.makeText(getActivity(), "There was an error loading the Event details", Toast.LENGTH_LONG).show();
                    }
                }
                catch (Exception e)
                {
                    try
                    {
                        Toast.makeText(getActivity(), "An error was encountered parsing the JSON. An error report has been sent.", Toast.LENGTH_LONG).show();
                    }
                    catch(Exception e2)
                    {
                        e.printStackTrace();
                        e2.printStackTrace();
                    }
                }
            }
        };
    }

    private void preLoadData()
    {
        /*dialog = new ProgressDialog(getActivity());
        dialog.setTitle("Contacting Zenoss");
        dialog.setMessage("Please wait:\nLoading Event details....");
        dialog.show();*/

        dataPreload = new Thread()
        {
            public void run()
            {
                try
                {
                    if (settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                    {
                        API = new ZenossAPIZaas();
                    }
                    else
                    {
                        API = new ZenossAPICore();
                    }

                    ZenossCredentials credentials = new ZenossCredentials(getActivity());
                    API.Login(credentials);
                    EventObject = API.GetEvent(getArguments().getString("EventID"));
                }
                catch (Exception e)
                {
                    firstLoadHandler.sendEmptyMessage(0);
                    BugSenseHandler.sendExceptionMessage("ViewZenossEvent", "DataPreloadThread", e);
                }
                finally
                {
                    firstLoadHandler.sendEmptyMessage(1);
                }
            }
        };

        dataPreload.start();
    }

    private void AddLogMessage(final String Message)
    {
        addMessageProgressDialog = new ProgressDialog(getActivity());
        addMessageProgressDialog.setTitle("Contacting Zenoss");
        addMessageProgressDialog.setMessage("Please wait:\nProcessing Event Log Updates");
        addMessageProgressDialog.show();

        addLogMessageHandler = new Handler() {
            public void handleMessage(Message msg) {
                addMessageProgressDialog.dismiss();

                if (msg.what == 1)
                {
                    try
                    {
                        String[] tmp = LogEntries.clone();
                        final int NewArrlength = tmp.length + 1;
                        LogEntries = new String[NewArrlength];

                        LogEntries[0] = "<strong>" + settings.getString("userName", "") + "</strong> wrote " + Message + "\n<br/><strong>At:</strong> Just now";

						for (int i = 1; i < NewArrlength; ++i) //
						{
							LogEntries[i] = tmp[(i -1)];
						}

                        TextView newLog = new TextView(getActivity());
                        newLog.setText(Html.fromHtml(LogEntries[0]));
                        newLog.setPadding(0, 6, 0, 6);

                        logList.addView(newLog);
                    }
                    catch (Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("ViewZenossEvent", "AddMessageProgressHandler", e);
                        Toast.makeText(getActivity(), "The log message was successfully sent to Zenoss but an error occured when updating the UI", Toast.LENGTH_LONG).show();
                    }
                }
                else
                {
                    Toast.makeText(getActivity(), "An error was encountered adding your message to the log", Toast.LENGTH_LONG).show();
                }

            }
        };

        addLogMessageThread = new Thread() {
            public void run() {
                Boolean Success = false;

                try {
                    if (API == null) {
                        if (settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false)) {
                            API = new ZenossAPIZaas();
                        } else {
                            API = new ZenossAPICore();
                        }
                        ZenossCredentials credentials = new ZenossCredentials(getActivity());
                        API.Login(credentials);
                    }

                    Success = API.AddEventLog(getArguments().getString("EventID"), Message);
                } catch (Exception e) {
                    BugSenseHandler.sendExceptionMessage("ViewZenossEvent", "AddLogMessageThread", e);
                    addLogMessageHandler.sendEmptyMessage(0);
                }

                if (Success) {
                    addLogMessageHandler.sendEmptyMessage(1);
                } else {
                    addLogMessageHandler.sendEmptyMessage(0);
                }
            }
        };

        addLogMessageThread.start();
    }
}