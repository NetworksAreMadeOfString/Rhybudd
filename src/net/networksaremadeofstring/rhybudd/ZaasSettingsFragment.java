package net.networksaremadeofstring.rhybudd;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Gareth on 30/05/13.
 */
public class ZaasSettingsFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    public static final String ARG_SECTION_NUMBER = "section_number";

    public ZaasSettingsFragment()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_zaas_first_settings, container, false);
        TextView dummyTextView = (TextView) rootView.findViewById(R.id.textView);
        dummyTextView.setText(Integer.toString(getArguments().getInt( ARG_SECTION_NUMBER)));
        return rootView;
    }
}