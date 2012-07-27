package net.networksaremadeofstring.rhybudd;

import java.util.List;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Button;

public class SettingsFragment extends PreferenceActivity 
{
   /* @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        // Add a button to the header list.
        if (hasHeaders()) 
        {
            Button button = new Button(this);
            button.setText("Some action");
            setListFooter(button);
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) 
    {
        loadHeadersFromResource(R.xml.preferences, target);
    }
    */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}
