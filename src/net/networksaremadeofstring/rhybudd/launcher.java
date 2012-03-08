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
import com.bugsense.trace.BugSenseHandler;


public class launcher extends Activity
{
	private SharedPreferences settings = null;

	 @Override
	 public void onCreate(Bundle savedInstanceState) 
	 {
		 super.onCreate(savedInstanceState);
	     settings = getSharedPreferences("rhybudd", 0);
	     setContentView(R.layout.main);  
	     
	     BugSenseHandler.setup(this, "44a76a8c");		
	     
		 if((settings.getString("URL", "").equals("") && settings.getString("userName", "").equals("") && settings.getString("passWord", "").equals("")) || settings.getBoolean("DBCreated", false) == false)
	     {
			Intent SettingsIntent = new Intent(launcher.this, RhybuddSettings.class);
			SettingsIntent.putExtra("firstRun", true);
     		launcher.this.startActivity(SettingsIntent);
     		finish();
	     }
	     else
	     {
	    	 if(getIntent().hasCategory(Intent.CATEGORY_DESK_DOCK) || getIntent().hasCategory("android.intent.category.HE_DESK_DOCK") || getIntent().hasCategory("android.intent.category.LE_DESK_DOCK"))
	    	 {
	    		 Intent RhybuddDockIntent = new Intent(launcher.this, RhybuddDock.class);
		    	 launcher.this.startActivity(RhybuddDockIntent);
	    	 }
	    	 else
	    	 {
		    	 Intent RhybuddHomeIntent = new Intent(launcher.this, RhybuddHome.class);
		    	 launcher.this.startActivity(RhybuddHomeIntent);
	    	 }
	    	 finish();
	     }
	 }
}
