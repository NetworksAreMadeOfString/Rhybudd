/*
 * Copyright (C) 2013 - Gareth Llewellyn
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

import android.app.ActionBar;
import android.app.Activity;
import android.view.MenuItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;

public class ManageDatabase extends Activity
{
	Thread FlushDBThread;
	Handler UIUpdate;
	ActionBar actionbar;
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.manage_database);
        actionbar = getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setHomeButtonEnabled(true);

		try
		{
			((TextView) findViewById(R.id.dbSize)).setText(Long.toString(ManageDatabase.this.getDatabasePath("rhybudd3Cache").length()) + " bytes");
		}
		catch(Exception e)
		{
            BugSenseHandler.sendExceptionMessage("ManageDatabase", "OnCreate", e);
		}
        
        
        UIUpdate = new Handler() 
        {
			public void handleMessage(Message msg) 
			{
				try
				{
					Toast.makeText(ManageDatabase.this, "Complete!", Toast.LENGTH_SHORT).show();
					/*if(msg.what == 0)
					{
						((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(0);
						((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(0);
					}
					else
					{
						((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(4);
				    	((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(4);
					}*/
				}
				catch(Exception e)
				{
                    BugSenseHandler.sendExceptionMessage("ManageDatabase", "UIUpdate", e);
				}
			}
        };
        
        ImageView refreshCacheButton = (ImageView) findViewById(R.id.refreshCacheImageView);
        refreshCacheButton.setClickable(true);
        refreshCacheButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				Toast.makeText(ManageDatabase.this, "There is no longer any need to manually invoke a cache refresh.", Toast.LENGTH_SHORT).show();
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
		    	 alertbox.setMessage("Are you sure you want to flush all local caches?\r\nThis can have bizarre consequences.");
		    	 alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
		    	 {
		             public void onClick(DialogInterface arg0, int arg1) 
		             {
		            	 ((Thread) new Thread() 
		        	    	{  
		        	    		public void run() 
		        	    		{
		        	    			try
		        	    			{
			        	    			UIUpdate.sendEmptyMessage(0);
                                        RhybuddDataSource datasource = new RhybuddDataSource(ManageDatabase.this);
                                        datasource.open();
                                        datasource.FlushDB();
                                        datasource.close();
			        	    			UIUpdate.sendEmptyMessage(1);
		        	    			}
		        					catch(Exception e)
		        					{
                                        BugSenseHandler.sendExceptionMessage("ManageDatabase", "UIUpdate", e);
		        					}
		        	    		}
		        	    	}).start();
		             }
		    	 });

		         alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() 
		         {
		             public void onClick(DialogInterface arg0, int arg1) 
		             {
		            	 //Do nothing
		             }
		         });
		         alertbox.show();
			}
        });
        
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        {
	        	//No need for crazy intents
	        	finish();
	            
	            return true;
	        }
	        
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
}
