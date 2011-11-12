package net.networksaremadeofstring.rhybudd;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

public class Search extends Activity
{
	private SharedPreferences settings = null;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
        setContentView(R.layout.view_zenoss_device);
    }
}
