/*
* Copyright (C) 2011 - Gareth Llewellyn
*
* This file is part of Rhybudd - http://blog.NetworksAreMadeOfString.co.uk/Rhybudd/
*
* This program is free software: you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License along with
* this program. If not, see <http://www.gnu.org/licenses/>
*/
package net.networksaremadeofstring.rhybudd;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class launcher extends Activity
{
	private SharedPreferences settings = null;
	Thread StartService;
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
	    	 //Start the background service
	    	 if(settings.getBoolean("AllowBackgroundService", true) == true)
	    	 {
	    		 StartService = new Thread() 
	    		 {  
	    			 public void run() 
					{
					startService(new Intent(launcher.this.getApplicationContext(), ZenossPoller.class));
					}
	    		 };
	    	 }
	    	 else
	    	 {
	    		 stopService(new Intent(this, ZenossPoller.class));
	    	 }
	    	 
	    	 Intent EventListIntent = new Intent(launcher.this, rhestr.class);
	    	 launcher.this.startActivity(EventListIntent);
	    	 finish();
	     }
	 }
	 
	 
}
