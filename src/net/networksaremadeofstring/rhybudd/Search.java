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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        //settings = getSharedPreferences("rhybudd", 0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.search);
        ((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
        
        list = (ListView)findViewById(R.id.searchResultsListView);
        
        ((Button) findViewById(R.id.SearchButton)).setOnClickListener(new OnClickListener() 
        {
			@Override
			public void onClick(View v) 
			{
				query = ((EditText) findViewById(R.id.searchTermEditText)).getText().toString();
				PerformSearch(true);
			}	
		}
		);
        
        ImageView showSearchButton = (ImageView) findViewById(R.id.showSearchImageView);
        showSearchButton.setClickable(true);
        showSearchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				RelativeLayout searchContainer = (RelativeLayout) findViewById(R.id.searchContainer);
				
				if(searchContainer.isShown())
				{
					anim = AnimationUtils.loadAnimation(Search.this, android.R.anim.fade_out);
					anim.setDuration(500);
					searchContainer.setAnimation(anim);
					searchContainer.setVisibility(8);
				}
				else
				{
					anim = AnimationUtils.loadAnimation(Search.this, android.R.anim.fade_in);
					anim.setDuration(500);
					searchContainer.setAnimation(anim);
					searchContainer.setVisibility(0);
				}
			}
        });
        
        
        Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) 
	    {
	    	((RelativeLayout) findViewById(R.id.searchContainer)).setVisibility(8);
	    	query = intent.getStringExtra(SearchManager.QUERY);
	    	index = "device";
	    	
	    	PerformSearch(false);
	    }
	    else
	    {
	    	((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(4);
	    	((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(4);
	    }
	    
	    searchResultsHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 99)
    			{
    				listOfZenossDevices.clear();
    				((TextView)findViewById(R.id.CurrentTaskLabel)).setVisibility(0);
	    			((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(0);
    			}
    			else
    			{
	    			if(listOfZenossDevices.size() > 0)
	    			{
		    	        //list.setAdapter(new ArrayAdapter<String>(Search.this, R.layout.search_simple,SearchResults));
	    				list.setAdapter(new ZenossSearchAdaptor(Search.this, listOfZenossDevices));
	    			}
	    			else
	    			{
	    				Toast.makeText(Search.this, "No matches found", Toast.LENGTH_SHORT).show();
	    			}
	    			
	    			((TextView)findViewById(R.id.CurrentTaskLabel)).setVisibility(8);
	    			((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(8);
    			}
    		}
    	};
    }
	
	public void PerformSearch(Boolean Crafted)
	{
		Thread dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			searchResultsHandler.sendEmptyMessage(99);
    			
    			String Filter = "name like \"%"+query.replaceAll(" ", "%")+"%\"";
    			
    			SQLiteDatabase rhybuddCache = Search.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);

    			Cursor dbResults = rhybuddCache.query("devices",new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents"},Filter, null, null, null, null);
    			
    			while(dbResults.moveToNext())
    			{
    				HashMap<String, Integer> events = new HashMap<String, Integer>();
					events.put("info", dbResults.getInt(5));
					events.put("debug", dbResults.getInt(6));
					events.put("warning", dbResults.getInt(7));
					events.put("error", dbResults.getInt(8));
					events.put("critical", dbResults.getInt(9));
					
    				listOfZenossDevices.add(new ZenossDevice(dbResults.getString(1),
    						dbResults.getInt(2), 
    						events,
    						dbResults.getString(3),
    						dbResults.getString(4)));
    			}
    			dbResults.close();
    			rhybuddCache.close();
    			searchResultsHandler.sendEmptyMessage(0);
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
}
