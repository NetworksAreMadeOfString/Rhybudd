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
import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Search extends Activity
{
	private SharedPreferences settings = null;
	ListView list;
	String query, index;
	Handler searchResultsHandler;
	ArrayList<String> SearchResults = new ArrayList<String>();
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.search);
        ((TextView)findViewById(R.id.HomeHeaderTitle)).setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
        
        list = (ListView)findViewById(R.id.searchResultsListView);
        
        Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) 
	    {
	    	query = intent.getStringExtra(SearchManager.QUERY);
	    	index = "device";
	    	
	    	PerformSearch(false);
	    }
	    
	    searchResultsHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(SearchResults.size() > 0)
    			{
	    	        list.setAdapter(new ArrayAdapter<String>(Search.this, R.layout.search_simple,SearchResults));
    			}
    			else
    			{
    			}
    			
    			((TextView)findViewById(R.id.CurrentTaskLabel)).setVisibility(8);
    			((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(8);
    		}
    	};
    }
	
	public void PerformSearch(Boolean Crafted)
	{
		Thread dataPreload = new Thread() 
    	{  
    		public void run() 
    		{
    			String Filter = "name like \"%"+query.replaceAll(" ", "%")+"%\"";
    			
    			SQLiteDatabase rhybuddCache = Search.this.openOrCreateDatabase("rhybuddCache", MODE_PRIVATE, null);

    			Cursor dbResults = rhybuddCache.query("devices",new String[]{"rhybuddDeviceID","productionState","ipAddress","name","uid","infoEvents","debugEvents","warningEvents","errorEvents","criticalEvents"},Filter, null, null, null, null);
    			
    			while(dbResults.moveToNext())
    			{
    				//Log.i("SearchResults",dbResults.getString(3));
    				SearchResults.add(dbResults.getString(3));
    			}
    			dbResults.close();
    			rhybuddCache.close();
    			searchResultsHandler.sendEmptyMessage(0);
    		}
    	};
    	
    	dataPreload.start();
	}
}
