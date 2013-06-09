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

import java.util.ArrayList;
import java.util.List;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Toast;

public class Search extends Activity
{
	//private SharedPreferences settings = null;
	ListView list;
	String query, index;
	Handler searchResultsHandler;
	ArrayList<String> SearchResults = new ArrayList<String>();
	List<ZenossDevice> listOfZenossDevices = new ArrayList<ZenossDevice>();
	Animation anim;
	ActionBar actionbar;
	ProgressDialog dialog;

	int DeviceCount = 0;
	SharedPreferences settings = null;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		settings = PreferenceManager.getDefaultSharedPreferences(this);
		setContentView(R.layout.search);

		actionbar = getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setHomeButtonEnabled(true);


		list = (ListView)findViewById(R.id.searchResultsListView);

		((Button) findViewById(R.id.SearchButton)).setOnClickListener(new OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				query = ((EditText) findViewById(R.id.searchTermEditText)).getText().toString();
				ConfigureHandler();
				PerformSearch(true);
			}	
		}
				);


		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) 
		{
			//((RelativeLayout) findViewById(R.id.searchContainer)).setVisibility(8);
			query = intent.getStringExtra(SearchManager.QUERY);
			((EditText) findViewById(R.id.searchTermEditText)).setText(query);
			index = "device";
			ConfigureHandler();
			PerformSearch(false);
		}
		else
		{
			//((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(4);
			//((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(4);
		}
	}

	private void ConfigureHandler()
	{
		searchResultsHandler = new Handler() 
		{
			public void handleMessage(Message msg) 
			{
				if(dialog != null && dialog.isShowing())
					dialog.dismiss();
				
				/*if(msg.what == 99)
				{
					listOfZenossDevices.clear();
					((TextView)findViewById(R.id.CurrentTaskLabel)).setVisibility(0);
					((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(0);
				}
				else */if (msg.what == 1)
				{
					Toast.makeText(Search.this, "An error was encountered;\r\n" + msg.getData().getString("exception"), Toast.LENGTH_LONG).show();
				}
				else
				{
					if(listOfZenossDevices.size() > 0)
					{
						list.setAdapter(new ZenossSearchAdaptor(Search.this, listOfZenossDevices));
					}
					else
					{
						Toast.makeText(Search.this, "No matches found", Toast.LENGTH_SHORT).show();
					}

					//((TextView)findViewById(R.id.CurrentTaskLabel)).setVisibility(8);
					//((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(8);
				}
			}
		};
	}

	public void PerformSearch(Boolean Crafted)
	{
		dialog = new ProgressDialog(this);
		dialog.setTitle("Contacting Zenoss");
		dialog.setMessage("Please wait:\nLoading Infrastructure....");
		dialog.setCancelable(false);
		dialog.show();
		Thread dataPreload = new Thread() 
		{  
			public void run() 
			{
				if(listOfZenossDevices != null)
					listOfZenossDevices.clear();
				
				try
				{
					//listOfZenossDevices = rhybuddCache.SearchRhybuddDevices(query);
                    RhybuddDataSource datasource = new RhybuddDataSource(Search.this);
                    datasource.open();
                    listOfZenossDevices = datasource.SearchRhybuddDevices(query);
                    datasource.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
					listOfZenossDevices.clear();
				}

				if(listOfZenossDevices!= null && listOfZenossDevices.size() > 0)
				{
					DeviceCount = listOfZenossDevices.size();
					//Log.i("DeviceList","Found DB Data!");
					searchResultsHandler.sendEmptyMessage(0);
				}
				else
				{
					//Log.i("DeviceList","No DB data found, querying API directly");
					Message msg = new Message();
					Bundle bundle = new Bundle();
					bundle.putString("exception","A query to both the local DB returned no devices");
					msg.setData(bundle);
					msg.what = 1;
					searchResultsHandler.sendMessage(msg);
					
					//TODO Query the API directly
					/*if(listOfZenossDevices != null)
						listOfZenossDevices.clear();

					try 
					{
						ZenossAPIv2 API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
						listOfZenossDevices = API.GetRhybuddDevices();

						if(listOfZenossDevices != null && listOfZenossDevices.size() > 0)
						{
							DeviceCount = listOfZenossDevices.size();
							searchResultsHandler.sendEmptyMessage(1);
							rhybuddCache.UpdateRhybuddDevices(listOfZenossDevices);
						}
						else
						{
							Message msg = new Message();
							Bundle bundle = new Bundle();
							bundle.putString("exception","A query to both the local DB and Zenoss API returned no devices");
							msg.setData(bundle);
							msg.what = 99;
							searchResultsHandler.sendMessage(msg);
						}

					} 
					catch (Exception e) 
					{
						BugSenseHandler.log("DeviceList", e);
						Message msg = new Message();
						Bundle bundle = new Bundle();
						bundle.putString("exception",e.getMessage());
						msg.setData(bundle);
						msg.what = 99;
						searchResultsHandler.sendMessage(msg);
					}
					
					searchResultsHandler.sendEmptyMessage(0);*/
				}
			}
		};

		dataPreload.start();
	}

	public void ViewDevice(String UID)
	{
		Intent ViewDeviceIntent = new Intent(Search.this, ViewZenossDevice.class);
		ViewDeviceIntent.putExtra("UID", UID);
		startActivity(ViewDeviceIntent);
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
