package net.networksaremadeofstring.rhybudd;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

public class rhestr extends Activity 
{
	ZenossAPIv2 API = null;
	private SharedPreferences settings = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
       
        
        setContentView(R.layout.main);
        
        try
    	{
        	Resources res = getResources();
    		//API = new ZenossAPIv2(settings.getString("userName", "--"), settings.getString("passWord", "--"), settings.getString("URL", "--"));
    	}
    	catch(Exception e)
    	{
    		
    	}
        
        try 
        {
        	//API.GetEvents();
		} catch (Exception e) {
			Log.e("Error", e.getMessage().toString());
		}
    }
}