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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ManageDatabase extends Activity
{
	private SharedPreferences settings = null;
	Thread FlushDBThread;
	Handler UIUpdate;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.manage_database);
        ((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));

        ((TextView) findViewById(R.id.dbSize)).setText(Long.toString(ManageDatabase.this.getDatabasePath("rhybuddCache").length()) + "bytes");
        
        
        UIUpdate = new Handler() 
        {
			public void handleMessage(Message msg) 
			{
				if(msg.what == 0)
				{
					((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(0);
			    	((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(0);
				}
				else
				{
					((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(4);
			    	((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(4);
				}
				
				((TextView) findViewById(R.id.dbSize)).setText(Long.toString(ManageDatabase.this.getDatabasePath("rhybuddCache").length()) + "bytes");
			}
        };
        
        ImageView refreshCacheButton = (ImageView) findViewById(R.id.refreshCacheImageView);
        refreshCacheButton.setClickable(true);
        refreshCacheButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				Toast.makeText(ManageDatabase.this, "There is no need to manually invoke a cache refresh.", Toast.LENGTH_SHORT).show();
			}
        });
        
        
        ImageView moveToSDButton = (ImageView) findViewById(R.id.moveToSDImageView);
        moveToSDButton.setClickable(true);
        moveToSDButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS),0);
			}
        });
        
        ImageView emptyDBButton = (ImageView) findViewById(R.id.emptyDBImageView);
        emptyDBButton.setClickable(true);
        emptyDBButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				 AlertDialog.Builder alertbox = new AlertDialog.Builder(ManageDatabase.this);
				 alertbox.setTitle("Confirmation");
		    	 alertbox.setMessage("Are you sure you want to flush all local caches?");
		    	 alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
		    	 {
		             public void onClick(DialogInterface arg0, int arg1) 
		             {
		            	 FlushDBThread = new Thread() 
		        	    	{  
		        	    		public void run() 
		        	    		{
		        	    			UIUpdate.sendEmptyMessage(0);
		        	    			SQLiteDatabase cacheDB = ManageDatabase.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);
		        	    			cacheDB.delete("devices", null, null);
		        	    			cacheDB.delete("events", null, null);
		        	    			cacheDB.close();
		        	    			UIUpdate.sendEmptyMessage(1);
		        	    		}
		        	    	};
		        	    	
		        	    	FlushDBThread.start();
		             }
		    	 });

		         alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() 
		         {
		             public void onClick(DialogInterface arg0, int arg1) {
		             }
		         });

		         alertbox.show();
		    	 
			}
        });
        
    }
}
