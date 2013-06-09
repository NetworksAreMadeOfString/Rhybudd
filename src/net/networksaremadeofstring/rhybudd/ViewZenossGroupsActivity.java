package net.networksaremadeofstring.rhybudd;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ViewZenossGroupsActivity extends FragmentActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_zenoss_groups_activity);

        getActionBar().setTitle("Rhybudd");
        getActionBar().setSubtitle("Zenoss Groups List");
        getActionBar().setDisplayHomeAsUpEnabled(true);

    }
}
