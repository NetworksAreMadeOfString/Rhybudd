package net.networksaremadeofstring.rhybudd;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.bugsense.trace.BugSenseHandler;

import java.util.HashMap;

/**
 * Created by Gareth on 04/06/13.
 */
public class ViewZenossDeviceFragment extends Fragment
{
    public static String ARG_HOSTNAME = "hostname";
    public static String ARG_UID = "uid";
    public static String ARG_2PANE = "twopane";
    public static String ARG_DEVICENAMES = "devicenames";
    public static String ARG_DEVICEIDS = "deviceids";

    TextView deviceTitle = null;

    public ViewZenossDeviceFragment()
    {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.view_zenoss_device_fragment, container, false);
        deviceTitle = (TextView) rootView.findViewById(R.id.textView);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle bundle)
    {
        super.onActivityCreated(bundle);
        deviceTitle.setText(getArguments().getString("DeviceName"));

        //Removed due to next pager item 'stealing' the actionbar
        /*try
        {
            getActivity().getActionBar().setSubtitle(getArguments().getString("DeviceName"));
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("ViewZenossDeviceFragment", "onActivityCreated", e);
        }*/
    }



}
