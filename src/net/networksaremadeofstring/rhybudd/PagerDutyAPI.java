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

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Base64;
import android.util.Log;

public class PagerDutyAPI 
{
	private String PAGERDUTY_INSTANCE = null;
    private String PAGERDUTY_USERNAME = null;
    private String PAGERDUTY_PASSWORD = null;
    private DefaultHttpClient httpclient = new DefaultHttpClient();  
    private ResponseHandler responseHandler = new BasicResponseHandler();
    private int reqCount = 1;
    private boolean LoginSuccessful = false;
    
	// Constructor logs in to the Zenoss instance (getting the auth cookie)
    public PagerDutyAPI(String UserName, String Password, String URL) throws Exception 
    {
    	this.PAGERDUTY_INSTANCE = URL;
    	this.PAGERDUTY_USERNAME = UserName;
    	this.PAGERDUTY_PASSWORD = Password;
    }
    
    public JSONObject GetIncidents() throws ClientProtocolException, IOException, JSONException
    {
    	//Log.i("Constructor","Entering GetIncidents");
    	Calendar c = Calendar.getInstance(); 
    	HttpGet httpget = new HttpGet("https://" + this.PAGERDUTY_INSTANCE + 
    									".pagerduty.com/api/v1/incidents?since=" +
    									c.get(Calendar.YEAR) + "-" + 
    									Integer.toString((c.get(Calendar.MONTH) + 1)) + "-" + 
    									c.get(Calendar.DAY_OF_MONTH));
        
        httpclient.getCredentialsProvider().setCredentials(
		new AuthScope(null, -1),
		new UsernamePasswordCredentials(this.PAGERDUTY_USERNAME, this.PAGERDUTY_PASSWORD));
        
        //Log.i("URL:",httpget.getURI().toString());
        
        String test = httpclient.execute(httpget, responseHandler);
    	//Log.i("Test:", test);
		
		JSONObject json = new JSONObject(test);
    	//Log.i("PagerDuty -------------> ", json.toString());
    	return json;
    }
    
    public JSONObject AcknowledgeIncident(String incident_key)
    {
		return null;
    }
}
