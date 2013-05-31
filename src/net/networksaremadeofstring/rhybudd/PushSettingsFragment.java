package net.networksaremadeofstring.rhybudd;

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
    Handler getIDhandler;
    ProgressBar progressbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_push_first_settings, container, false);

        PushKey = (EditText) rootView.findViewById(R.id.pushKeyEditText);
        PushKeyDesc = (TextView) rootView.findViewById(R.id.pushIDDesc);
        SaveKey = (Button) rootView.findViewById(R.id.saveKeyButton);
        progressbar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        String pushKey = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(ZenossAPIv2.PREFERENCE_PUSHKEY,"");

        if(!pushKey.equals(""))
        {
            PushKey.setText(pushKey);
            PushKey.setVisibility(View.VISIBLE);
            PushKeyDesc.setVisibility(View.VISIBLE);
        }

        SaveKey.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                progressbar.setVisibility(View.VISIBLE);
                if(PushKey.getText().length() == 32)
                {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                    editor.putString(ZenossAPIv2.PREFERENCE_PUSHKEY, PushKey.getText().toString());
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
                PushKey.setVisibility(View.VISIBLE);
                SaveKey.setVisibility(View.VISIBLE);
                Log.e("setOnClickListener", "Button!");
            }
        });

        Button getID = (Button) rootView.findViewById(R.id.GenerateNewIDButton);
        getID.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                SaveKey.setVisibility(View.GONE);
                progressbar.setVisibility(View.VISIBLE);

                ((Thread) new Thread(){
                    public void run()
                    {
                        try
                        {
                            String PushKey = ZenossAPIv2.getPushKey();
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
                        PushKey.setVisibility(View.VISIBLE);
                        PushKeyDesc.setVisibility(View.VISIBLE);
                        PushKey.setText(pushKey);

                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                        editor.putString(ZenossAPIv2.PREFERENCE_PUSHKEY, pushKey);
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