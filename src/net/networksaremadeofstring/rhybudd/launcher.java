package net.networksaremadeofstring.rhybudd;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class launcher extends Activity
{
	private SharedPreferences settings = null;
	 @Override
	 public void onCreate(Bundle savedInstanceState) 
	 {
		 super.onCreate(savedInstanceState);
	     settings = getSharedPreferences("rhybudd", 0);
	     setContentView(R.layout.main);  
	        
		 if(settings.getString("URL", "").equals("") && settings.getString("userName", "").equals("") && settings.getString("passWord", "").equals(""))
	     {
			Intent SettingsIntent = new Intent(launcher.this, Settings.class);
			SettingsIntent.putExtra("firstRun", true);
     		launcher.this.startActivity(SettingsIntent);
     		finish();
	     }
	     else
	     {
	    	 Intent EventListIntent = new Intent(launcher.this, rhestr.class);
	    	 launcher.this.startActivity(EventListIntent);
	    	 finish();
	     }
	 }
	 
	 
}
