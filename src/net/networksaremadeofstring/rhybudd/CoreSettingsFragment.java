package net.networksaremadeofstring.rhybudd;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;

import java.util.List;

/**
 * Created by Gareth on 30/05/13.
 */
public class CoreSettingsFragment extends Fragment
{
    SharedPreferences settings = null;
    ProgressDialog dialog;
    Thread peformLogin;
    Handler handler;
    ZenossAPIv2 API = null;

    public CoreSettingsFragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_core_first_settings, container, false);

        Button LoginButton = (Button) rootView.findViewById(R.id.SaveButton);
        LoginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                //DoSave();
                Log.e("setOnClickListener","Button!");
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
        dialog.setCancelable(false);

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
                        dialog.setMessage("Logged in successfully. Preparing database...");
                    }
                    catch(Exception e)
                    {
                        //BugSenseHandler.log("InitialSettings", e);
                        Toast.makeText(getActivity(), "Logged in Successfully. Preparing Database!", Toast.LENGTH_SHORT).show();
                    }

                    try
                    {
                        //rhybuddCache = new RhybuddDatabase(RhybuddInitialSettings.this);
                        ((Thread) new Thread(){
                            public void run()
                            {
                                //Events
                                try
                                {
                                    //ZenossAPIv2 API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
                                    if(settings.getBoolean("httpBasicAuth", false))
                                    {
                                        API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""),settings.getString("BAUser", ""), settings.getString("BAPassword", ""));
                                    }
                                    else
                                    {
                                        API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
                                    }

                                    if(API != null)
                                    {
                                        List<ZenossEvent> listOfZenossEvents = API.GetRhybuddEvents(settings.getBoolean("SeverityCritical", true),
                                                settings.getBoolean("SeverityError", true),
                                                settings.getBoolean("SeverityWarning", true),
                                                settings.getBoolean("SeverityInfo", false),
                                                settings.getBoolean("SeverityDebug", false),
                                                settings.getBoolean("onlyProductionEvents", true),
                                                settings.getString("SummaryFilter", ""),
                                                settings.getString("DeviceFilter", ""));

                                        if(listOfZenossEvents!= null && listOfZenossEvents.size() > 0)
                                        {
                                            //rhybuddCache.UpdateRhybuddEvents(listOfZenossEvents);
                                            handler.sendEmptyMessage(1);
                                        }
                                        else
                                        {
                                            Log.e("initialSettings","There was a problem processing the GetRhybuddEvents call");
                                            //HandleException(null, "Initialising the API Failed. An error message has been logged.");
                                        }
                                    }
                                    else
                                    {
                                        //HandleException(null, "Initialising the API Failed. An error message has been logged.");
                                    }
                                }
                                catch(Exception e)
                                {
                                    //HandleException(e, "Initialising the API Failed. An error message has been logged.");
                                }

                                //Devices
                                try
                                {
                                    List<ZenossDevice> listOfZenossDevices = API.GetRhybuddDevices();

                                    if(listOfZenossDevices != null)
                                    {
                                        handler.sendEmptyMessage(3);
                                        handler.sendEmptyMessageDelayed(2, 1500);
                                        //rhybuddCache.UpdateRhybuddDevices(listOfZenossDevices);
                                    }
                                    else
                                    {
                                        //TODO Bundle an error
                                    }
                                }
                                catch(Exception e)
                                {
                                    e.printStackTrace();
                                    //HandleException(e, "Caching devices as failed. An error message has been logged.");
                                }

                            }
                        }).start();
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("CoreSettingsFragment","General success path",e);
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
                        BugSenseHandler.sendExceptionMessage("CoreSettingsFragment","Dismissing dialog in msg_caching_complete",e);
                    }

                    Intent in = new Intent();
                    getActivity().setResult(1,in);
                    getActivity().finish();
                }
                else if(msg.what == RhybuddHandlers.msg_caching_complete)
                {
                    dialog.setMessage("Caching complete!\r\nVerifying.");
                }
                else if(msg.what == RhybuddHandlers.msg_initial_verify_debug_output)
                {
                    TextView debugOutput = (TextView) getActivity().findViewById(R.id.debugOutput);

                    debugOutput.setText(debugOutput.getText() + msg.getData().getString("debugMsg") + "\n");
                }
                else if(msg.what == RhybuddHandlers.msg_initial_verify_error)
                {
                    ((TextView) getActivity().findViewById(R.id.debugOutput)).setText(msg.getData().getString("exception") + "\n");

                    try
                    {
                        dialog.dismiss();
                    }
                    catch(Exception e)
                    {
                        //Not much else we can do here :/
                        BugSenseHandler.sendExceptionMessage("CoreSettingsFragment","Dismissing dialog in msg_initial_verify_error",e);
                    }

                    try
                    {
                        Toast.makeText(getActivity(), "An error was encountered;\r\n"+ msg.getData().getString("error"), Toast.LENGTH_SHORT).show();
                    }
                    catch(Exception e)
                    {
                        BugSenseHandler.sendExceptionMessage("CoreSettingsFragment","Displaying toast with error message",e);
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
                        BugSenseHandler.sendExceptionMessage("CoreSettingsFragment","Dismissing dialog in final handler else",e);
                    }

                    Toast.makeText(getActivity(), "Login Failed - Please check details.", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.save_settings:
            {
                //DoSave();
                Log.e("menu", "Lol stolen");
                return true;
            }

            default:
            {
                return false;
            }
        }
    }
}