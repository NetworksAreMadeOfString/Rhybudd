/*
* Copyright (C) 2012 - Gareth Llewellyn
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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
//import android.util.Log;

import com.bugsense.trace.BugSenseHandler;

//TODO This needs some serious genericisation to remove all the duplicated code
public class ZenossAPIv2 
{
	String ZENOSS_INSTANCE = null;
    String ZENOSS_USERNAME = null;
    String ZENOSS_PASSWORD = null;

    public static String SENDER_ID = "228666382181";
    public static String PREFERENCE_PUSHKEY = "pushkey";

    //These don't get used directly
  	private DefaultHttpClient client;
  	private ThreadSafeClientConnManager mgr;
  	private DefaultHttpClient httpclient;
  	
    @SuppressWarnings("rawtypes")
	private ResponseHandler responseHandler = new BasicResponseHandler();
    private int reqCount = 1;
    private boolean LoginSuccessful = false;
    
    public ZenossAPIv2(String UserName, String Password, String URL, String BAUser, String BAPassword) throws Exception 
    {
    	if(URL.contains("https://"))
    	{
    		this.PrepareSSLHTTPClient();
    	}
    	else
    	{
    		this.PrepareHTTPClient();
    		//httpclient = new DefaultHttpClient();
    	}
    	
    	if(!BAUser.equals("") || !BAPassword.equals(""))
    	{
    		//Log.i("Auth","We have some auth credentials");
    		CredentialsProvider credProvider = new BasicCredentialsProvider();
    	    credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(BAUser, BAPassword));
    	    httpclient.setCredentialsProvider(credProvider);
    	}
    	
    	//Timeout ----------------------------------
    	HttpParams httpParameters = new BasicHttpParams(); 
    	int timeoutConnection = 20000;
    	HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
    	int timeoutSocket = 30000;
    	HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
    	httpclient.setParams(httpParameters);
    	//Timeout ----------------------------------
    	
        HttpPost httpost = new HttpPost(URL + "/zport/acl_users/cookieAuthHelper/login");

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("__ac_name", UserName));
        nvps.add(new BasicNameValuePair("__ac_password", Password));
        nvps.add(new BasicNameValuePair("submitted", "true"));
        nvps.add(new BasicNameValuePair("came_from", URL + "/zport/dmd"));

        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        // Response from POST not needed, just the cookie
        HttpResponse response = httpclient.execute(httpost);
        
        // Consume so we can reuse httpclient
        response.getEntity().consumeContent();
        
        //Set the variables for later
        this.ZENOSS_INSTANCE = URL;
        this.ZENOSS_USERNAME = UserName;
        this.ZENOSS_PASSWORD = Password;
    }
    
	// Constructor logs in to the Zenoss instance (getting the auth cookie)
    public ZenossAPIv2(String UserName, String Password, String URL) throws Exception 
    {
    	//Test
    	this(UserName,Password,URL,"", "");
    }
    
    private void PrepareSSLHTTPClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
	{
		client = new DefaultHttpClient(); 
		
        SchemeRegistry registry = new SchemeRegistry();
        SocketFactory socketFactory = null;
        
        //Check whether people are self signing or not
        /*if(AllowSelfSigned == true)
        {*/
        	//Log.i("SelfSigned","Allowing Self Signed Certificates");
			socketFactory = TrustAllSSLSocketFactory.getDefault();
        /*}
        else
        {
        	Log.i("SelfSigned","Enforcing Certificate checks");
        	socketFactory = SSLSocketFactory.getSocketFactory();
        }*/
        
        registry.register(new Scheme("https", socketFactory, 443));
        //mgr = new SingleClientConnManager(client.getParams(), registry); 
        mgr = new ThreadSafeClientConnManager(client.getParams(), registry); 
        httpclient = new DefaultHttpClient(mgr, client.getParams());
	}
    
    private void PrepareHTTPClient()
    {
    	HttpParams params = new BasicHttpParams();
    	client = new DefaultHttpClient(); 
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));

        mgr = new ThreadSafeClientConnManager(params, registry);
        httpclient = new DefaultHttpClient(mgr, client.getParams());
    }
    
    public boolean getLoggedInStatus()
    {
    	return this.LoginSuccessful;
    }

    public static String getPushKey() throws IOException, JSONException {
        DefaultHttpClient client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        registry.register(new Scheme("https", socketFactory, 443));
        ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpclient = new DefaultHttpClient(mgr, client.getParams());

        HttpPost httpost = new HttpPost("https://api.coldstart.io/1/getrhybuddpushkey");

        httpost.addHeader("Content-type", "application/json; charset=utf-8");
        httpost.setHeader("Accept", "application/json");

        HttpResponse response = httpclient.execute(httpost);
        String rawJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        JSONObject json = new JSONObject(rawJSON);
        Log.i("getPushKey",rawJSON);

        if(json.has("pushkey"))
        {
            return json.getString("pushkey");
        }
        else
        {
            return null;
        }
    }

	public boolean CheckLoggedIn()
    {
		//If we got JSON back rather than HTML then we are probably logged in
		//Why the fuck I can't hit an endpoint and get a 401 or 200 depending
		//on whether my cookie is valid appears to be waaaay too complicated
    	try 
    	{
			this.GetEvents("5",true,false,null,null);
			this.LoginSuccessful = true;
			return true;
		} 
    	catch (Exception e) 
    	{
			this.LoginSuccessful = false;
			return false;
		}
    }
	
	
	public List<ZenossDevice> GetRhybuddDevices() throws JSONException, ClientProtocolException, IOException
	{
		List<ZenossDevice> ZenossDevices = new ArrayList<ZenossDevice>();
		JSONObject devices = this.GetDevices();

		//Log.e("GetRhybyddDevices",devices.toString(2));
		int DeviceCount = devices.getJSONObject("result").getInt("totalCount");
		try
		{
			if(((int) devices.getJSONObject("result").getJSONArray("devices").length()) < DeviceCount)
			{
				DeviceCount = (int) devices.getJSONObject("result").getJSONArray("devices").length();
			}
		}
		catch(Exception e)
		{
			//BugSenseHandler.log("GetRhybuddDevices", e);
		}

		for(int i = 0; i < DeviceCount; i++)
		{
			JSONObject CurrentDevice = null;
			try
			{
				CurrentDevice = devices.getJSONObject("result").getJSONArray("devices").getJSONObject(i);
				
				HashMap<String, Integer> events = new HashMap<String, Integer>();
				try
				{
					events.put("info", CurrentDevice.getJSONObject("events").getInt("info"));
				}
				catch(JSONException j)
				{
					events.put("info", CurrentDevice.getJSONObject("events").getJSONObject("info").getInt("count"));
				}
				catch(Exception e)
				{
					events.put("info",0);
				}
				
				try
				{
					events.put("debug", CurrentDevice.getJSONObject("events").getInt("debug"));
				}
				catch(JSONException j)
				{
					events.put("debug", CurrentDevice.getJSONObject("events").getJSONObject("debug").getInt("count"));
				}
				catch(Exception e)
				{
					events.put("debug",0);
				}
				
				try
				{
					events.put("critical", CurrentDevice.getJSONObject("events").getInt("critical"));
				}
				catch(JSONException j)
				{
					events.put("critical", CurrentDevice.getJSONObject("events").getJSONObject("critical").getInt("count"));
				}
				catch(Exception e)
				{
					events.put("critical",0);
				}
				
				try
				{
					events.put("warning", CurrentDevice.getJSONObject("events").getInt("warning"));
				}
				catch(JSONException j)
				{
					events.put("warning", CurrentDevice.getJSONObject("events").getJSONObject("warning").getInt("count"));
				}
				catch(Exception e)
				{
					events.put("warning",0);
				}
				
				try
				{
					events.put("error", CurrentDevice.getJSONObject("events").getInt("error"));
				}
				catch(JSONException j)
				{
					events.put("error", CurrentDevice.getJSONObject("events").getJSONObject("error").getInt("count"));
				}
				catch(Exception e)
				{
					events.put("error",0);
				}
				
				/*events.put("info", CurrentDevice.getJSONObject("events").getInt("info"));
				events.put("debug", CurrentDevice.getJSONObject("events").getInt("debug"));
				events.put("critical", CurrentDevice.getJSONObject("events").getInt("critical"));
				events.put("warning", CurrentDevice.getJSONObject("events").getInt("warning"));
				events.put("error", CurrentDevice.getJSONObject("events").getInt("error"));*/
				int IPAddress = 0;
				
				try
				{
					IPAddress = CurrentDevice.getInt("ipAddress");
				}
				catch(JSONException j)
				{
					IPAddress = 0;
				}
				
				try
				{
					ZenossDevices.add(new ZenossDevice(CurrentDevice.getString("productionState"),
							IPAddress,
							events,
							CurrentDevice.getString("name"),
							CurrentDevice.getString("uid")));
				}
				catch(JSONException j)
				{
					//Don't care - keep going no point losing all devices
					//BugSenseHandler.log("GetRhybuddDevices", j);
				}
			}
			catch (JSONException j)
			{
				//BugSenseHandler.log("GetRhybuddDevices", j);
				//Keep going
				//throw j;
			}
			catch (Exception e)
			{
				//BugSenseHandler.log("GetRhybuddDevices", e);
			}
		}
		
		return ZenossDevices;
	}
	
	public JSONObject GetDevices() throws JSONException, ClientProtocolException, IOException
	{
		HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/device_router");

		httpost.addHeader("Content-type", "application/json; charset=utf-8");
		httpost.setHeader("Accept", "application/json");
		
		JSONObject dataContents = new JSONObject();
    	dataContents.put("start", 0);
    	dataContents.put("limit", 1000);
    	dataContents.put("dir", "ASC");
    	dataContents.put("sort", "name");
    	dataContents.put("uid", "/zport/dmd/Devices");
    	
    	JSONObject params = new JSONObject();
        //params.put("productionState", new JSONArray("[1000]"));
        dataContents.put("params", params);
        
    	JSONArray data = new JSONArray();
        data.put(dataContents);
        
		JSONObject reqData = new JSONObject();
        reqData.put("action", "DeviceRouter");
        reqData.put("method", "getDevices");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));
        
        httpost.setEntity(new StringEntity(reqData.toString()));
    	//String rawDevicesJSON = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String rawDevicesJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
		JSONObject json = new JSONObject(rawDevicesJSON);
    	return json; 
	}
    
	@SuppressWarnings("unchecked")
	public JSONObject AcknowledgeEvent(String _EventID) throws JSONException, ClientProtocolException, IOException
	{
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");
    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");
    	
    	JSONObject dataContents = new JSONObject();
    	dataContents.put("excludeIds", new JSONObject());
    	dataContents.put("selectState", null);
    	//dataContents.put("direction", "DESC");
    	//dataContents.put("field", "severity");
    	dataContents.put("asof", (System.currentTimeMillis()/1000));
    	
    	JSONArray evids = new JSONArray();
    	evids.put(_EventID);
    	dataContents.put("evids", evids);
    	
        JSONObject params = new JSONObject();
        params.put("severity", new JSONArray("[5, 4, 3, 2]"));
        params.put("eventState", new JSONArray("[0, 1]"));
        dataContents.put("params", params);
        
        JSONArray data = new JSONArray();
        data.put(dataContents);
        
        JSONObject reqData = new JSONObject();
        reqData.put("action", "EventsRouter");
        reqData.put("method", "acknowledge");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));
        
        httpost.setEntity(new StringEntity(reqData.toString()));
    	String ackEventReturnJSON = (String) httpclient.execute(httpost, responseHandler);
		JSONObject json = new JSONObject(ackEventReturnJSON);
		//Log.i("AcknowledgeEvent",json.toString(2));
    	return json;
	}
	
	public JSONObject AcknowledgeEvents(List<String> EventIDs) throws JSONException, ClientProtocolException, IOException
	{
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");
    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");
    	
    	JSONObject dataContents = new JSONObject();
    	dataContents.put("excludeIds", new JSONObject());
    	dataContents.put("selectState", null);
    	dataContents.put("asof", (System.currentTimeMillis()/1000));
    	
    	JSONArray evids = new JSONArray();
    	for(String EventID : EventIDs)
    	{
    		evids.put(EventID);
    	}
    	
    	dataContents.put("evids", evids);
    	
        JSONObject params = new JSONObject();
        params.put("severity", new JSONArray("[5, 4, 3, 2]"));
        params.put("eventState", new JSONArray("[0, 1]"));
        dataContents.put("params", params);
        
        JSONArray data = new JSONArray();
        data.put(dataContents);
        
        JSONObject reqData = new JSONObject();
        reqData.put("action", "EventsRouter");
        reqData.put("method", "acknowledge");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));
        
        httpost.setEntity(new StringEntity(reqData.toString()));
        
    	//String ackEventReturnJSON = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String ackEventReturnJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        
		JSONObject json = new JSONObject(ackEventReturnJSON);
		//Log.i("AcknowledgeEvent",json.toString(2));
    	return json;
	}
	
	public List<ZenossEvent> GetRhybuddEvents(boolean Critical, boolean Error, boolean Warning, boolean Info, boolean Debug, boolean ProductionOnly, String SummaryFilter, String DeviceFilter) throws JSONException, ClientProtocolException, IOException, SocketTimeoutException, SocketException
	{
		return GetRhybuddEvents(Critical,  Error,  Warning,  Info,  Debug,  ProductionOnly, SummaryFilter,  DeviceFilter, false);
	}
	
	public List<ZenossEvent> GetRhybuddEvents(boolean Critical, boolean Error, boolean Warning, boolean Info, boolean Debug, boolean ProductionOnly, String SummaryFilter, String DeviceFilter, boolean hideAckd) throws JSONException, ClientProtocolException, IOException, SocketTimeoutException, SocketException
	{
		List<ZenossEvent> listofZenossEvents = new ArrayList<ZenossEvent>();
		
		//FIXME Makes a valid call to the API but this breaks on 4.x ( JIRA #ZEN-2812 )
		JSONObject jsonEvents = GetEvents(Critical,Error,Warning,Info,Debug,ProductionOnly,false, SummaryFilter, DeviceFilter);
		
		JSONArray Events = null;
		try
		{
			Events = jsonEvents.getJSONObject("result").getJSONArray("events");
		}
		catch(JSONException e)
		{
			try
			{
				//FIXME If we got an exception it may be because of JIRA #ZEN-2812
				jsonEvents = GetEvents(Critical,Error,Warning,Info,Debug,ProductionOnly,true, SummaryFilter, DeviceFilter);
				Events = jsonEvents.getJSONObject("result").getJSONArray("events");
			}
			catch(Exception e1)
			{
				//BugSenseHandler.log("GetRhybuddEvents", e1);
				//Nope something bad happened
				return null;
			}
		}
		catch(Exception e1)
		{
			//BugSenseHandler.log("GetRhybuddEvents", e1);
			e1.printStackTrace();
			return null;
		}
		
		int EventCount = 0;
		
		try
		{
			if(jsonEvents != null)
				EventCount = jsonEvents.getJSONObject("result").getInt("totalCount");
		}
		catch(Exception e)
		{
			EventCount = 0;
		}

		//Log.i("Events",Events.toString(2));
		//If EventCount is 0 this will never process
		for(int i = 0; i < EventCount; i++)
		{
			JSONObject CurrentEvent = null;
			try 
			{
				CurrentEvent = Events.getJSONObject(i);
				
				//TODO Lots more error catching
				if(hideAckd && CurrentEvent.getString("eventState").equals("Acknowledged"))
				{
					continue;
				}
				else
				{
					listofZenossEvents.add(new ZenossEvent(CurrentEvent.getString("evid"),
														CurrentEvent.getInt("count"),
														CurrentEvent.getString("prodState"),
														CurrentEvent.getString("firstTime"),
														CurrentEvent.getString("severity"),
														CurrentEvent.getJSONObject("component").getString("text"),
														CurrentEvent.getJSONObject("component").getString("uid"),
														CurrentEvent.getString("summary"), 
														CurrentEvent.getString("eventState"),
														CurrentEvent.getJSONObject("device").getString("text"),
														CurrentEvent.getJSONObject("eventClass").getString("text"),
														CurrentEvent.getString("lastTime"),
														CurrentEvent.getString("ownerid")));
				}
			}
			catch(Exception e)
			{
				//TODO Do something
				//return null;
			}
		}
		
		if(listofZenossEvents.size() > 0)
		{
			return listofZenossEvents;
		}
		else
		{
			listofZenossEvents = new ArrayList<ZenossEvent>();
			return listofZenossEvents;
		}
	}
	
	/*public JSONObject GetEvents() throws JSONException, ClientProtocolException, IOException
	{
		return this.GetEvents("5,4,3,2",false);
	}
	
	public JSONObject GetEvents(Boolean Critical, Boolean Error, Boolean Warning, Boolean Info, Boolean Debug) throws JSONException, ClientProtocolException, IOException
	{
		return this.GetEvents(Critical,Error,Warning,Info,Debug,false);
	}*/
	
	public JSONObject GetEvents(Boolean Critical, Boolean Error, Boolean Warning, Boolean Info, Boolean Debug, boolean ProductionOnly, boolean Zenoss41,String SummaryFilter, String DeviceFilter) throws JSONException, ClientProtocolException, IOException, SocketTimeoutException, SocketException
	{
		String SeverityLevels = "";
        
        if(Critical)
        	SeverityLevels += "5,";
        
        if(Error)
        	SeverityLevels += "4,";
        
        if(Warning)
        	SeverityLevels += "3,";
        
        if(Info)
        	SeverityLevels += "2,";
        
        if(Debug)
        	SeverityLevels += "1,";
        
        //Remove last comma
        if(SeverityLevels.length() > 1)
        {
        	SeverityLevels = SeverityLevels.substring(0, SeverityLevels.length() - 1);
        }
        
		return this.GetEvents(SeverityLevels,ProductionOnly, Zenoss41, SummaryFilter, DeviceFilter);
	}
	
	private JSONObject GetEvents(String Severity, boolean ProductionOnly, boolean Zenoss41, String SummaryFilter, String DeviceFilter) throws JSONException, ClientProtocolException, IOException, SocketTimeoutException, SocketException
    {
    	//Log.i("Test:", Severity);
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");

    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");

    	JSONObject dataContents = new JSONObject();
    	dataContents.put("start", 0);
    	dataContents.put("limit", 2000);
    	dataContents.put("dir", "DESC");
    	dataContents.put("sort", "severity");
        
    	//4.1 stuff Jira #ZEN-2812
    	if(Zenoss41)
    	{
    		//Log.i("Zenoss41","true");
    		dataContents.put("keys", new JSONArray("[evid,count,prodState,firstTime,severity,component,summary,eventState,device,eventClass,lastTime,ownerid]"));
    	}
    	//4.1 stuff
    	
        JSONObject params = new JSONObject();
        params.put("severity", new JSONArray("["+Severity+"]"));
        params.put("eventState", new JSONArray("[0, 1]"));
        
        if(null != SummaryFilter && !SummaryFilter.equals(""))
        {
        	params.put("summary", SummaryFilter);
        }
        
        if(null != DeviceFilter && !DeviceFilter.equals(""))
        {
        	params.put("device", DeviceFilter);
        }
        
        if(ProductionOnly)
        {
        	params.put("prodState", new JSONArray("[1000]"));
        }
        
        dataContents.put("params", params);
        
        JSONArray data = new JSONArray();
        data.put(dataContents);
        
        JSONObject reqData = new JSONObject();
        reqData.put("action", "EventsRouter");
        reqData.put("method", "query");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));
        
        httpost.setEntity(new StringEntity(reqData.toString()));
    	
        
    	//String eventsRawJSON = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String eventsRawJSON = EntityUtils.toString(response.getEntity());
        Log.i("Raw", eventsRawJSON);
        response.getEntity().consumeContent();
        
		JSONObject json = new JSONObject(eventsRawJSON);
    	return json;
    }
    
	public JSONObject GetEvent(String _EventID) throws JSONException, ClientProtocolException, IOException
    {
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");

    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");
    	
    	JSONObject dataContents = new JSONObject();
    	//dataContents.put("excludeIds", new JSONObject());
    	//dataContents.put("selectState", null);
    	//dataContents.put("direction", "DESC");
    	//dataContents.put("field", "severity");
    	//dataContents.put("asof", (System.currentTimeMillis()/1000));
    	
    	/*JSONArray evids = new JSONArray();
    	evids.put(_EventID);
    	dataContents.put("evids", evids);*/
    	dataContents.put("evid",_EventID);
    	
    	//Disabled for 4.1 compatibility
    	//dataContents.put("history", false);
    	
        /*JSONObject params = new JSONObject();
        params.put("severity", new JSONArray("[5, 4, 3, 2]"));
        params.put("eventState", new JSONArray("[0, 1]"));
        dataContents.put("params", params);*/
        
        JSONArray data = new JSONArray();
        	data.put(dataContents);
        
        JSONObject reqData = new JSONObject();
	        reqData.put("action", "EventsRouter");
	        reqData.put("method", "detail");
	        reqData.put("data", data);
	        reqData.put("type", "rpc");
	        reqData.put("tid", String.valueOf(this.reqCount++));
        
        httpost.setEntity(new StringEntity(reqData.toString()));
    	//String test = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String test = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        
		JSONObject json = new JSONObject(test);
    	return json;
    }
    
	public Boolean AddEventLog(String _EventID, String Message) throws JSONException, ClientProtocolException, IOException
    {
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");
	    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
	    	httpost.setHeader("Accept", "application/json");
    	
    	JSONObject dataContents = new JSONObject();
	    	dataContents.put("evid",_EventID);
	    	dataContents.put("message", Message);
        
        JSONArray data = new JSONArray();
        	data.put(dataContents);
        
        JSONObject reqData = new JSONObject();
	        reqData.put("action", "EventsRouter");
	        reqData.put("method", "write_log");
	        reqData.put("data", data);
	        reqData.put("type", "rpc");
	        reqData.put("tid", String.valueOf(this.reqCount++));
        
        httpost.setEntity(new StringEntity(reqData.toString()));
    	//String test = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String test = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        
		JSONObject json = new JSONObject(test);

    	return json.getJSONObject("result").getBoolean("success");
    }
    
    
    @SuppressWarnings("unchecked")
    public JSONObject GetEventsHistory() throws JSONException, ClientProtocolException, IOException
    {
		HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");
			httpost.addHeader("Content-type", "application/json; charset=utf-8");
			httpost.setHeader("Accept", "application/json");
       	
       	JSONObject dataContents = new JSONObject();
	       	dataContents.put("start", 0);
	       	dataContents.put("limit", 100);
	       	dataContents.put("dir", "DESC");
	       	dataContents.put("sort", "severity");
           
       	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String DefaultDate = sdf.format(new Date(System.currentTimeMillis() - 28800000)).toString();
		
		JSONObject params = new JSONObject();
			params.put("severity", new JSONArray("[5,4,3]"));
			params.put("eventState", new JSONArray("[0, 1]"));
			params.put("lastTime", DefaultDate);
			dataContents.put("params", params);
       
       JSONArray data = new JSONArray();
       	data.put(dataContents);
       
       JSONObject reqData = new JSONObject();
	       reqData.put("action", "EventsRouter");
	       reqData.put("method", "queryHistory");
	       reqData.put("data", data);
	       reqData.put("type", "rpc");
	       reqData.put("tid", String.valueOf(this.reqCount++));
	       
       httpost.setEntity(new StringEntity(reqData.toString()));
       	
       String test = (String) httpclient.execute(httpost, responseHandler);
   		
       JSONObject json = new JSONObject(test);
		return json;
       }
    
	public JSONObject GetDevice(String UID) throws JSONException, ClientProtocolException, IOException
    {
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + UID.replace(" ", "%20") + "/device_router");

    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");

    	JSONArray keys = new JSONArray("[events,uptime,firstSeen,lastChanged,lastCollected,memory,name,productionState,systems,groups,location,tagNumber,serialNumber,rackSlot,osModel,links,comments,snmpSysName,snmpLocation,snmpContact,snmpAgent]");
    	JSONArray data = new JSONArray();
        
        JSONObject dataObject = new JSONObject();
	        dataObject.put("uid", UID);
	        dataObject.put("keys", keys);
        
        data.put(dataObject);
        
        JSONObject reqData = new JSONObject();
	        reqData.put("action", "DeviceRouter");
	        reqData.put("method", "getInfo");
	        reqData.put("data", data);
	        reqData.put("type", "rpc");
	        reqData.put("tid", String.valueOf(this.reqCount++));
        
        JSONArray Wrapper = new JSONArray();
        Wrapper.put(reqData);
        httpost.setEntity(new StringEntity(Wrapper.toString()));
    	
    	//String test = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String test = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        //Log.e("GetDevice",test);
        try
        {
			JSONObject json = new JSONObject(test);
	    	return json;
        }
        catch(Exception e)
        {
        	return null;
        }
    }
    
	public JSONObject GetDeviceEvents(String UID, boolean Zenoss41) throws JSONException, ClientProtocolException, IOException
    {
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + UID.replace(" ", "%20") + "/evconsole_router");

    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");

    	JSONArray data = new JSONArray();
        
        JSONObject dataObject = new JSONObject();
        dataObject.put("uid", UID);
        dataObject.put("start", 0);
        dataObject.put("limit", 100);
        dataObject.put("sort", "severity");
        dataObject.put("dir", "DESC");
        

    	//4.1 hack
        if(Zenoss41)
        {
	    	JSONArray keys = new JSONArray("[device,eventState,severity,component,eventClass,summary,firstTime,lastTime,count,evid,eventClassKey,message]");
	        dataObject.put("keys", keys);
        }
        //End 4.1 hack
        
        data.put(dataObject);
        
        JSONObject reqData = new JSONObject();
        reqData.put("action", "EventsRouter");
        reqData.put("method", "query");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));
        
        JSONArray Wrapper = new JSONArray();
        Wrapper.put(reqData);
        httpost.setEntity(new StringEntity(Wrapper.toString()));
    	
    	//String test = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String test = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
		//Log.e("GetDeviceEvents",test);
        try
        {
			JSONObject json = new JSONObject(test);
	    	return json;
        }
        catch(Exception e)
        {
        	return null;
        }
    }
	
	public JSONObject GetDeviceGraphs(String UID) throws JSONException, ClientProtocolException, IOException
    {
		HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + UID.replace(" ", "%20") + "/device_router");

    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");

    	JSONArray data = new JSONArray();
        
        JSONObject dataObject = new JSONObject();
        dataObject.put("uid", UID);
        dataObject.put("drange", "129600");
        
        data.put(dataObject);
        
        JSONObject reqData = new JSONObject();
        reqData.put("action", "DeviceRouter");
        reqData.put("method", "getGraphDefs");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));
        
        JSONArray Wrapper = new JSONArray();
        Wrapper.put(reqData);
        httpost.setEntity(new StringEntity(Wrapper.toString()));
    	
    	//String test = httpclient.execute(httpost, responseHandler);
        HttpResponse response = httpclient.execute(httpost);
        String test = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
		//Log.e("GetDeviceEvents",test);
		JSONObject json = new JSONObject(test);
    	return json;
    }
	
	public Drawable GetGraph(String urlString) throws IOException, URISyntaxException
	{
		HttpGet httpRequest = new HttpGet(new URL(ZENOSS_INSTANCE + urlString).toURI());
		HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
		HttpEntity entity = response.getEntity();
		BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity); 
		final long contentLength = bufHttpEntity.getContentLength();
		if (contentLength >= 0) 
		{
		    InputStream is = bufHttpEntity.getContent();
		    Bitmap bitmap = BitmapFactory.decodeStream(is);
		    return new BitmapDrawable(bitmap);
		} 
		else 
		{
		    return null;
		}
	}
}
