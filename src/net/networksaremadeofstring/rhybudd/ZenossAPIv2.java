package net.networksaremadeofstring.rhybudd;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;

import android.util.Log;

public class ZenossAPIv2 
{
	private String ZENOSS_INSTANCE = null;
    private String ZENOSS_USERNAME = null;
    private String ZENOSS_PASSWORD = null;
    private DefaultHttpClient httpclient = new DefaultHttpClient();  
    private ResponseHandler responseHandler = new BasicResponseHandler();
    private int reqCount = 1;
    private boolean LoginSuccessful = false;
    
	// Constructor logs in to the Zenoss instance (getting the auth cookie)
    public ZenossAPIv2(String UserName, String Password, String URL) throws Exception 
    {
    	Log.i("Constructor","Entering constructor");
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
        
        /*List<Cookie> cookies = httpclient.getCookieStore().getCookies();
        int i = 0;
        while(i < cookies.size())
        {
        	Log.i("Cookie: " + cookies.get(i).getName().toString(),cookies.get(i).getValue());
        	if(cookies.get(i).getName().equals("__ginger_snap"))
        	{

        	}
        	i++;
        }*/
        Log.i("Constructor","Leaving constructor");
    }
    
    public boolean getLoggedInStatus()
    {
    	return this.LoginSuccessful;
    }
    
	public boolean CheckLoggedIn()
    {
		//If we got JSON back rather than HTML then we are probably logged in
		//Why the fuck I can't hit an endpoint and get a 401 or 200 depending
		//on whether my cookie is valid appears to be waaaay too complicated
    	try 
    	{
			this.GetEvents();
			Log.i("CheckLoggedIn", "Success");
			this.LoginSuccessful = true;
			return true;
		} 
    	catch (Exception e) 
    	{
			Log.e("CheckLoggedIn", e.getMessage());
			this.LoginSuccessful = false;
			return false;
		}
    	
		
    }
    
    @SuppressWarnings("unchecked")
	public JSONObject GetEvents() throws JSONException, ClientProtocolException, IOException
    {
    	Log.i("Test:", "Entering Get Events");
    	HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");

    	httpost.addHeader("Content-type", "application/json; charset=utf-8");
    	httpost.setHeader("Accept", "application/json");
    	
    	JSONObject dataContents = new JSONObject();
    	dataContents.put("start", 0);
    	dataContents.put("limit", 100);
    	dataContents.put("dir", "DESC");
    	dataContents.put("sort", "severity");
        
        JSONObject params = new JSONObject();
        params.put("severity", new JSONArray("[5, 4, 3, 2]"));
        params.put("eventState", new JSONArray("[0, 1]"));
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
    	
    	Log.i("Execute","Executing with string: " + reqData.toString());
    	String test = httpclient.execute(httpost, responseHandler);
    	Log.i("Test:", test);
		
		JSONObject json = new JSONObject(test);
    	Log.i("ZenossEvents -------------> ", json.toString());
    	return json;
    }
}
