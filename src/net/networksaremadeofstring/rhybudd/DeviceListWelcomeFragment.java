package net.networksaremadeofstring.rhybudd;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class DeviceListWelcomeFragment extends Fragment
{
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.device_list_welcome_fragment, null);
        ((TextView) view.findViewById(R.id.WelcomeTitle)).setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        ((TextView) view.findViewById(R.id.DeviceListWelcomeIntro)).setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return view;
    }
}
