package net.networksaremadeofstring.rhybudd;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class rhestr extends Activity 
{
	ZenossAPI API = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        try
    	{
    		API = new ZenossAPI();
    	}
    	catch(Exception e)
    	{
    		
    	}
        
        try {
			Log.i("Devices", API.getDevices().toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}