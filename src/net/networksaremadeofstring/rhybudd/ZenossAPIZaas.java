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

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gareth on 31/05/13.
 */
public class ZenossAPIZaas extends ZenossAPI
{

    //TODO Uncomment this for release
    /*@Override
    protected void PrepareSSLHTTPClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
    {
        client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        registry.register(new Scheme("https", socketFactory, 443));
        mgr = new ThreadSafeClientConnManager(client.getParams(), registry);
        httpclient = new DefaultHttpClient(mgr, client.getParams());
        this.setTimeouts();
    }*/

    @Override
    public boolean Login(ZenossCredentials credentials) throws Exception
    {
        this.PrepareSSLHTTPClient();

        Log.e("Login","LoginLoginLoginLoginLoginLoginLogin");
        Log.e("Login","LoginLoginLoginLoginLoginLoginLogin");
        Log.e("Login","LoginLoginLoginLoginLoginLoginLogin");
        Log.e("Login","LoginLoginLoginLoginLoginLoginLogin");
        Log.e("Login","LoginLoginLoginLoginLoginLoginLogin");

        HttpPost httpost = new HttpPost(credentials.URL + "/zport/acl_users/cookieAuthHelper/login");

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("__ac_name", credentials.UserName));
        nvps.add(new BasicNameValuePair("__ac_password", credentials.Password));
        nvps.add(new BasicNameValuePair("submitted", "true"));
        nvps.add(new BasicNameValuePair("came_from", credentials.URL + "/zport/dmd"));

        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));


        // Response from POST not needed, just the cookie
        HttpResponse response = httpclient.execute(httpost);

        // Consume so we can reuse httpclient
        response.getEntity().consumeContent();

        Log.e("ZAASLogin",credentials.URL);
        //Set the variables for later
        this.ZENOSS_INSTANCE = credentials.URL;
        this.ZENOSS_USERNAME = credentials.UserName;
        this.ZENOSS_PASSWORD = credentials.Password;

        Log.e("CheckLoggedIn", Integer.toString(response.getStatusLine().getStatusCode()));

        return this.CheckLoggedIn();
    }

    @Override
    protected JSONObject GetEvents(String Severity, boolean ProductionOnly, boolean Zenoss41, String SummaryFilter, String DeviceFilter) throws JSONException, ClientProtocolException, IOException, SocketTimeoutException, SocketException
    {
        HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");

        httpost.addHeader("Content-type", "application/json; charset=utf-8");
        httpost.setHeader("Accept", "application/json");

        JSONObject dataContents = new JSONObject();
        dataContents.put("start", 0);
        dataContents.put("limit", 2000);
        dataContents.put("dir", "DESC");
        dataContents.put("sort", "severity");

        //Zaas guaranteed to be a minimum of 4.2.3
        dataContents.put("keys", new JSONArray("[evid,count,prodState,firstTime,severity,component,summary,eventState,device,eventClass,lastTime,ownerid]"));

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

        if(null == httpclient || null == ZENOSS_INSTANCE)
            return null;

        HttpResponse response = httpclient.execute(httpost);
        String eventsRawJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();

        return  new JSONObject(eventsRawJSON);
    }
}
