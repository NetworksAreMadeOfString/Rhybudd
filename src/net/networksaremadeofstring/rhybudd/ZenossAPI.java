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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.text.format.Time;
import android.util.Log;
import com.bugsense.trace.BugSenseHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
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
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ZenossAPI
{
    String ZENOSS_INSTANCE = null;
    String ZENOSS_USERNAME = null;
    String ZENOSS_PASSWORD = null;

    public static String SENDER_ID = "228666382181";
    public static String MSG_DEBUG = "debugmsg";

    public static String PREFERENCE_PUSHKEY = "pushkey";
    public static String PREFERENCE_USERNAME = "userName";
    public static String PREFERENCE_PASSWORD = "passWord";
    public static String PREFERENCE_URL = "URL";
    public static String PREFERENCE_BASIC_AUTH_USER = "BAUser";
    public static String PREFERENCE_BASIC_AUTH_PASSWORD = "BAPassword";
    public static String PREFERENCE_IS_ZAAS = "isZaas";
    public static String SETTINGS_PUSH = "push";
    public static String PREFERENCE_PUSH_ENABLED = "pushkey_enabled";
    public static String PREFERENCE_PUSH_SENDERID = "pushkey_senderid";

    public static final int HANDLER_REDOREFRESH = 800;

    public static final int ACTIVITYRESULT_PUSHCONFIG = 900;


    private String lastException = "";

    //These don't get used directly
    protected DefaultHttpClient client;
    protected ThreadSafeClientConnManager mgr;
    protected DefaultHttpClient httpclient = null;

    protected ResponseHandler responseHandler = new BasicResponseHandler();
    protected int reqCount = 1;
    protected boolean LoginSuccessful = false;

    public ZenossAPI()
    {
    }

    public boolean isHTTPClientAlive()
    {
        if(null == httpclient)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public static void updateLastChecked(Context mContext)
    {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        Time now = new Time();
        now.setToNow();
        Log.e("toMillis", Long.toString(now.toMillis(true)));
        editor.putLong("lastCheck", now.toMillis(true));
        editor.commit();
    }

    public static boolean shouldRefresh(Context mContext)
    {
        Time now = new Time();
        now.setToNow();

        if((now.toMillis(true) - PreferenceManager.getDefaultSharedPreferences(mContext).getLong("lastCheck",now.toMillis(true))) > 900000)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public static String getPushKey() throws IOException, JSONException
    {
        DefaultHttpClient client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        registry.register(new Scheme("https", socketFactory, 443));
        ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpclient = new DefaultHttpClient(mgr, client.getParams());

        HttpPost httpost = new HttpPost("https://api.coldstart.io/1/getrhybuddpushkey");

        //httpost.addHeader("Content-type", "application/json; charset=utf-8");
        httpost.setHeader("Accept", "application/json");

        HttpResponse response = httpclient.execute(httpost);
        String rawJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        JSONObject json = new JSONObject(rawJSON);
        //Log.i("getPushKey", rawJSON);

        if(json.has("pushkey"))
        {
            return json.getString("pushkey");
        }
        else
        {
            return null;
        }
    }

    public boolean UnregisterWithZenPack(String DeviceID)
    {
        try
        {
            HttpPost httpost = new HttpPost(getURL() + "/zport/dmd/rhybudd_gcm_router");

            httpost.addHeader("Content-type", "application/json; charset=utf-8");
            httpost.setHeader("Accept", "application/json");

            JSONObject dataContents = new JSONObject();
            dataContents.put("device_id", DeviceID);

            JSONArray data = new JSONArray();
            data.put(dataContents);

            JSONObject reqData = new JSONObject();
            reqData.put("action", "RhybuddGCMRouter");
            reqData.put("method", "remove_gcm_regid");
            reqData.put("data", data);
            reqData.put("type", "rpc");
            reqData.put("tid", String.valueOf(this.reqCount++));
            httpost.setEntity(new StringEntity(reqData.toString()));
            HttpResponse response = httpclient.execute(httpost);
            String RawJSON = EntityUtils.toString(response.getEntity());

            try
            {
                JSONObject json = new JSONObject(RawJSON);
                if(json.has("uuid") || json.has("result") && json.has("success") && json.getBoolean("success"))
                {
                    return true;
                }
            }
            catch (Exception e)
            {
                return false;
            }
        }
        catch (Exception e)
        {
            return false;
        }

        return false;
    }

    public boolean RegisterWithZenPack(String DeviceID, String GCMID)
    {
        try
        {
            HttpPost httpost = new HttpPost(getURL() + "/zport/dmd/rhybudd_gcm_router");

            httpost.addHeader("Content-type", "application/json; charset=utf-8");
            httpost.setHeader("Accept", "application/json");

            JSONObject dataContents = new JSONObject();
            dataContents.put("gcm_reg_id",GCMID);
            dataContents.put("device_id", DeviceID);

            JSONArray data = new JSONArray();
            data.put(dataContents);

            JSONObject reqData = new JSONObject();
            reqData.put("action", "RhybuddGCMRouter");
            reqData.put("method", "update_gcm_regid");
            reqData.put("data", data);
            reqData.put("type", "rpc");
            reqData.put("tid", String.valueOf(this.reqCount++));
            httpost.setEntity(new StringEntity(reqData.toString()));
            HttpResponse response = httpclient.execute(httpost);
            String RawJSON = EntityUtils.toString(response.getEntity());

            try
            {
                JSONObject json = new JSONObject(RawJSON);
                if(json.has("uuid") || json.has("result"))
                {
                    return true;
                }
            }
            catch (Exception e)
            {
                return false;
            }
        }
        catch (Exception e)
        {
            return false;
        }

        return false;
    }

    public ZenPack CheckZenPack()
    {
        ZenPack zp = new ZenPack();

        try
        {
            HttpPost httpost = new HttpPost(getURL() + "/zport/dmd/rhybudd_gcm_router");

            httpost.addHeader("Content-type", "application/json; charset=utf-8");
            httpost.setHeader("Accept", "application/json");
            JSONObject dataContents = new JSONObject();
            JSONArray data = new JSONArray();
            data.put(dataContents);

            JSONObject reqData = new JSONObject();
            reqData.put("action", "RhybuddGCMRouter");
            reqData.put("method", "ping");
            //reqData.put("data", "[{}]");
            reqData.put("data",data);
            reqData.put("type", "rpc");
            reqData.put("tid", String.valueOf(this.reqCount++));
            httpost.setEntity(new StringEntity(reqData.toString()));
            HttpResponse response = httpclient.execute(httpost);
            String RawJSON = EntityUtils.toString(response.getEntity());
            Log.i("API",RawJSON);
            try
            {
                JSONObject json = new JSONObject(RawJSON);
                if(json.has("uuid") || json.has("result"))
                {
                    if(json.getJSONObject("result").getJSONObject("data").getBoolean("pong"))
                    {
                        try
                        {
                            zp.GCMID = json.getJSONObject("result").getJSONObject("data").getString("gcmid");
                        }
                        catch (Exception e)
                        {
                            zp.GCMID = null;
                        }

                        try
                        {
                            zp.SenderID = json.getJSONObject("result").getJSONObject("data").getString("senderid");

                            if(zp.SenderID.equals(""))
                                zp.SenderID = ZenossAPI.SENDER_ID;
                        }
                        catch (Exception e)
                        {
                            zp.SenderID = ZenossAPI.SENDER_ID;
                        }

                        zp.Installed = true;
                    }
                    else
                    {
                        zp.Installed = false;
                    }
                }
                else
                {
                    zp.Installed = false;
                    zp.Msg = "Payload didn't indicate a correct response";
                }
            }
            catch(JSONException j)
            {
                if(j.getMessage().contains("Value <html> of type java.lang.String cannot"))
                {
                    zp.Msg = "Login failed, the Zenoss login page was returned";
                }
                else
                {
                    zp.Msg = j.getMessage();
                }

                j.printStackTrace();
                zp.Installed = false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            zp.Msg = e.getMessage();
            zp.Installed = false;
        }

        return zp;
    }

    public void AddDevice(String FQDN, String Title, String DeviceClass, String ProductionState,String DevicePriority) throws JSONException, IOException {
        HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/device_router");
        httpost.addHeader("Content-type", "application/json; charset=utf-8");
        httpost.setHeader("Accept", "application/json");

        JSONObject dataContents = new JSONObject();
        dataContents.put("deviceName",FQDN);
        dataContents.put("deviceClass", DeviceClass);
        dataContents.put("title", Title);
        //Look this up
        dataContents.put("productionState", 1000);

        //Look this up
        dataContents.put("priority", 3);

        //Defaults
        dataContents.put("collector", "localhost");
        dataContents.put("model", true);
        dataContents.put("snmpCommunity", "");
        dataContents.put("snmpPort", "161");
        dataContents.put("tag", "");
        dataContents.put("rackSlot", "");
        dataContents.put("serialNumber", "");
        dataContents.put("hwManufacturer", "");
        dataContents.put("hwProductName", "");
        dataContents.put("osManufacturer", "");
        dataContents.put("osProductName", "");
        dataContents.put("comments", "");
        dataContents.put("groupPaths", new JSONArray());
        dataContents.put("systemPaths", new JSONArray());

        JSONArray data = new JSONArray();
        data.put(dataContents);

        JSONObject reqData = new JSONObject();
        reqData.put("action", "DeviceRouter");
        reqData.put("method", "addDevice");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));

        httpost.setEntity(new StringEntity(reqData.toString()));
        HttpResponse response = httpclient.execute(httpost);
        String rawJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        //Log.e("rawJSON",rawJSON);
        //JSONObject json = new JSONObject(rawJSON);
        //return json.getJSONObject("result").getBoolean("success");
    }

    public static String md5(String s)
    {
        try
        {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean registerPushKey(String PushKey, String GCMID, String DeviceID)
    {
        DefaultHttpClient client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        registry.register(new Scheme("https", socketFactory, 443));
        ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpclient = new DefaultHttpClient(mgr, client.getParams());

        HttpPost httpost = new HttpPost("https://api.coldstart.io/1/updaterhybuddpushkey");

        //httpost.addHeader("Content-type", "application/json; charset=utf-8");
        httpost.setHeader("Accept", "application/json");

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("pushkey", PushKey));
        nvps.add(new BasicNameValuePair("gcmid", GCMID));
        nvps.add(new BasicNameValuePair("deviceid", DeviceID));

        JSONObject json;
        try
        {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

            HttpResponse response = httpclient.execute(httpost);
            String rawJSON = EntityUtils.toString(response.getEntity());
            response.getEntity().consumeContent();
            //Log.e("rawJSON",rawJSON);
            json = new JSONObject(rawJSON);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }

        if(json.has("uuid") && json.has("success"))
        {
            Boolean success;
            try
            {
                success = json.getBoolean("success");
            }
            catch(Exception e)
            {
                success = false;
            }

            if(success)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public String getLastException()
    {
        return lastException;
    }

    protected void PrepareSSLHTTPClient() throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException
    {
        client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        SocketFactory socketFactory = null;
        socketFactory = TrustAllSSLSocketFactory.getDefault();
        registry.register(new Scheme("https", socketFactory, 443));
        mgr = new ThreadSafeClientConnManager(client.getParams(), registry);
        httpclient = new DefaultHttpClient(mgr, client.getParams());
        setTimeouts();
    }

    protected void PrepareHTTPClient()
    {
        HttpParams params = new BasicHttpParams();
        client = new DefaultHttpClient();
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        mgr = new ThreadSafeClientConnManager(params, registry);
        httpclient = new DefaultHttpClient(mgr, client.getParams());
        setTimeouts();
    }

    protected void setTimeouts()
    {
        HttpParams httpParameters = new BasicHttpParams();
        int timeoutConnection = 20000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
        int timeoutSocket = 30000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        httpclient.setParams(httpParameters);
    }

    public boolean getLoggedInStatus()
    {
        return this.LoginSuccessful;
    }

    protected String getURL()
    {
        return ZENOSS_INSTANCE;
    }

    public boolean CheckLoggedIn()
    {
        try
        {
            /*HttpGet httget = new HttpGet(ZENOSS_INSTANCE + "/zport/dmd/");
            HttpResponse response = httpclient.execute(httget);

            Log.e("CheckLoggedIn",Integer.toString(response.getStatusLine().getStatusCode()));

            if(response.getStatusLine().getStatusCode() == 200)
            {
                this.LoginSuccessful = true;
                return true;
            }
            else
            {
                this.LoginSuccessful = false;
                return false;
            }*/

            /*List<Cookie> cookies = httpclient.getCookieStore().getCookies();
            if(cookies != null)
            {
                for(Cookie cookie : cookies)
                {
                    Log.e("cookies",cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain());
                }
                return false;
            }
            else
            {
                this.LoginSuccessful = false;
                return false;
            }*/

            HttpPost httpost = new HttpPost(getURL() + "/zport/dmd/messaging_router");

            httpost.addHeader("Content-type", "application/json; charset=utf-8");
            httpost.setHeader("Accept", "application/json");
            /*JSONObject dataContents = new JSONObject();
            JSONArray data = new JSONArray();
            data.put(dataContents);*/

            JSONObject reqData = new JSONObject();
            reqData.put("action", "MessagingRouter");
            reqData.put("method", "getUserMessages");
            reqData.put("data", "[{}]");
            reqData.put("type", "rpc");
            reqData.put("tid", String.valueOf(this.reqCount++));
            httpost.setEntity(new StringEntity(reqData.toString()));
            HttpResponse response = httpclient.execute(httpost);
            String RawJSON = EntityUtils.toString(response.getEntity());

            //Log.e("eventsRawJSON",RawJSON);

            try
            {
                JSONObject json = new JSONObject(RawJSON);
                if(json.has("uuid") || json.has("message"))
                {
                    this.LoginSuccessful = true;
                    return true;
                }
                else
                {
                    lastException = "JSON Object didn't contain any expect objects (uuid or message) got (" + RawJSON + ") instead";
                    this.LoginSuccessful = false;
                    return false;
                }
            }
            catch(JSONException j)
            {
                if(j.getMessage().contains("Value <html> of type java.lang.String cannot"))
                {
                    lastException = "Login failed, the Zenoss login page was returned";
                }
                else
                {
                    lastException = j.getMessage();
                }

                j.printStackTrace();
                this.LoginSuccessful = false;
                return false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            lastException = e.getMessage();
            this.LoginSuccessful = false;
            return false;
        }
    }

    public static Boolean CheckIfNotify(String prodState, String UID, Context context, Boolean onlyAlertOnProd)
    {
        //We always return true if the device is production as specified by a 4.1 event JSON prodState
        if(prodState != null && prodState.toLowerCase().equals("production"))
        {
            return true;
        }

        RhybuddDataSource datasource = new RhybuddDataSource(context);
        datasource.open();
        ZenossDevice thisDevice = datasource.getDevice(UID);
        datasource.close();

        if(thisDevice != null)
        {
            if(thisDevice.getproductionState().equals("Production"))
            {
                return true;
            }
            else
            {
                if(onlyAlertOnProd)
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
        }
        else
        {
            if(onlyAlertOnProd)
            {
                return false;
            }
            else
            {
                return true;
            }
        }

    }

    public List<ZenossDevice> GetRhybuddDevices() throws JSONException, ClientProtocolException, IOException
    {
        List<ZenossDevice> ZenossDevices = new ArrayList<ZenossDevice>();
        JSONObject devices = this.GetDevices();
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
            BugSenseHandler.sendExceptionMessage("ZenossAPI", "GetRhybuddDevices", e);
        }

        for(int i = 0; i < DeviceCount; i++)
        {
            JSONObject CurrentDevice = null;
            int IPAddress = 0;

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

                try
                {
                    IPAddress = CurrentDevice.getInt("ipAddress");
                }
                catch(JSONException j)
                {
                    IPAddress = 0;
                }

                String OS;
                try
                {
                    OS = CurrentDevice.getJSONObject("osModel").getString("name");

                    if(OS.contains("inux"))
                    {
                        OS = "Linux";
                    }
                    else if(OS.contains("indows"))
                    {
                        OS = "Windows";
                    }
                    else if (OS.contains("IOS"))
                    {
                        OS = "Cisco IOS";
                    }
                    else if(OS.contains("EOS"))
                    {
                        OS = "Arista EOS";
                    }
                    else
                    {
                        OS = "Unknown";
                    }
                }
                catch(Exception e)
                {
                    OS = "Unknown";
                }

                try
                {
                    ZenossDevices.add(new ZenossDevice(CurrentDevice.getString("productionState"),
                            IPAddress,
                            events,
                            CurrentDevice.getString("name"),
                            CurrentDevice.getString("uid"),
                            OS
                    ));
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

    protected JSONObject GetDevices() throws JSONException, IOException
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
        HttpResponse response = httpclient.execute(httpost);
        String rawDevicesJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();
        JSONObject json = new JSONObject(rawDevicesJSON);
        return json;
    }

    public JSONObject GetEvent(String _EventID) throws JSONException, ClientProtocolException, IOException
    {
        HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/Events/evconsole_router");

        httpost.addHeader("Content-type", "application/json; charset=utf-8");
        httpost.setHeader("Accept", "application/json");

        JSONObject dataContents = new JSONObject();
        dataContents.put("evid",_EventID);

        JSONArray data = new JSONArray();
        data.put(dataContents);

        JSONObject reqData = new JSONObject();
        reqData.put("action", "EventsRouter");
        reqData.put("method", "detail");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));

        httpost.setEntity(new StringEntity(reqData.toString()));
        HttpResponse response = httpclient.execute(httpost);
        String rawJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();

        return new JSONObject(rawJSON);
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
        HttpResponse response = httpclient.execute(httpost);
        String rawJSON = EntityUtils.toString(response.getEntity());
        response.getEntity().consumeContent();

        JSONObject json = new JSONObject(rawJSON);

        return json.getJSONObject("result").getBoolean("success");
    }

    public List<ZenossEvent> GetRhybuddEvents(Context context) throws JSONException, ClientProtocolException, IOException, SocketTimeoutException, SocketException
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        boolean Critical = settings.getBoolean("SeverityCritical", true);
        boolean Error = settings.getBoolean("SeverityError", true);
        boolean Warning = settings.getBoolean("SeverityWarning", true);
        boolean Info = settings.getBoolean("SeverityInfo", false);
        boolean Debug = settings.getBoolean("SeverityDebug", false);
        boolean ProductionOnly = settings.getBoolean("onlyProductionEvents", true);
        String SummaryFilter = settings.getString("SummaryFilter", "");
        String DeviceFilter = settings.getString("DeviceFilter", "");
        boolean hideAckd = settings.getBoolean("hideAckdAlerts", false);
        String severityString = createSeverityString(Critical,Error,Warning,Info,Debug);
        List<ZenossEvent> listofZenossEvents = new ArrayList<ZenossEvent>();
        JSONArray Events = null;
        JSONObject jsonEvents;

        try
        {
            jsonEvents = GetEvents(severityString,ProductionOnly,false, SummaryFilter, DeviceFilter);

            if(null != jsonEvents)
            {
                Events = jsonEvents.getJSONObject("result").getJSONArray("events");
            }
            else
            {
                return null;
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            BugSenseHandler.sendExceptionMessage("ZenossAPI","GetRhybuddEvents (#ZEN-2812 ?)",e);
            try
            {
                //FIXME If we got an exception it may be because of JIRA #ZEN-2812
                jsonEvents = GetEvents(severityString,ProductionOnly,true, SummaryFilter, DeviceFilter);
                Events = jsonEvents.getJSONObject("result").getJSONArray("events");

                //If this worked then it's possible we're on 4.x
                //TODO set a custom variable in preferences to always use 4.x code?
            }
            catch(Exception e1)
            {
                e.printStackTrace();
                BugSenseHandler.sendExceptionMessage("ZenossAPI","GetRhybuddEvents",e1);
                //Nope something bad happened
                return null;
            }
        }
        catch(Exception e1)
        {
            BugSenseHandler.sendExceptionMessage("ZenossAPI","GetRhybuddEvents",e1);
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
            e.printStackTrace();
            EventCount = 0;

            BugSenseHandler.sendExceptionMessage("ZenossAPI","GetRhybuddEvents",e);
        }

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
                e.printStackTrace();
                BugSenseHandler.sendExceptionMessage("ZenossAPI","GetRhybuddEvents",e);
            }
        }

        if(null != listofZenossEvents && listofZenossEvents.size() > 0)
        {
            return listofZenossEvents;
        }
        else
        {
            listofZenossEvents = new ArrayList<ZenossEvent>();
            return listofZenossEvents;
        }
    }

    protected JSONObject GetEvents(String Severity, boolean ProductionOnly, boolean Zenoss41, String SummaryFilter, String DeviceFilter) throws JSONException, ClientProtocolException, IOException, SocketTimeoutException, SocketException
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
        //Log.i("Raw", eventsRawJSON);
        response.getEntity().consumeContent();

        JSONObject json = new JSONObject(eventsRawJSON);
        return json;
    }

    private String createSeverityString(Boolean Critical, Boolean Error, Boolean Warning, Boolean Info, Boolean Debug)
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

        return SeverityLevels;
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
            json.toString(3);
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
        Log.e("GetDeviceGraphs",test);
        JSONObject json = new JSONObject(test);
        return json;
    }


    public String[] GetGroups() throws JSONException, ClientProtocolException, IOException
    {
        HttpPost httpost = new HttpPost(ZENOSS_INSTANCE + "/zport/dmd/device_router");

        httpost.addHeader("Content-type", "application/json; charset=utf-8");
        httpost.setHeader("Accept", "application/json");

        JSONArray data = new JSONArray();

        JSONObject dataObject = new JSONObject();
        //dataObject.put("drange", "129600");
        data.put(dataObject);

        JSONObject reqData = new JSONObject();
        reqData.put("action", "DeviceRouter");
        reqData.put("method", "getGroups");
        reqData.put("data", data);
        reqData.put("type", "rpc");
        reqData.put("tid", String.valueOf(this.reqCount++));

        httpost.setEntity(new StringEntity(reqData.toString()));

        /*JSONArray Wrapper = new JSONArray();
        Wrapper.put(reqData);
        httpost.setEntity(new StringEntity(Wrapper.toString()));*/

        String groupsJSON = (String) httpclient.execute(httpost, responseHandler);
        //Log.e("groupsJSON",groupsJSON);
        JSONObject json = new JSONObject(groupsJSON);

        JSONArray groups = json.getJSONObject("result").getJSONArray("groups");

        int groupLength = groups.length();

        String[] groupsList = new String[groupLength];

        for(int i = 0; i < groupLength; i++)
        {
            JSONObject groupObj = (JSONObject) groups.get(i);

            groupsList[i] = groupObj.getString("name");
        }

        return groupsList;
    }


    public Drawable GetGraph(String urlString) throws IOException, URISyntaxException
    {
        HttpGet httpRequest = new HttpGet(new URL(ZENOSS_INSTANCE + urlString).toURI());
        HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
        HttpEntity entity = response.getEntity();
        BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
        final long contentLength = bufHttpEntity.getContentLength();
        //Log.e("GetGraph",Long.toString(contentLength));
        if (contentLength >= 0)
        {
            InputStream is = bufHttpEntity.getContent();
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            /*ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, out);

            return new BitmapDrawable(BitmapFactory.decodeStream(new ByteArrayInputStream(out.toByteArray())));*/
            is.close();
            return new BitmapDrawable(bitmap);
        }
        else
        {
            return null;
        }
    }

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

    public static String ntoa(long raw)
    {
        byte[] b = new byte[] {(byte)(raw >> 24), (byte)(raw >> 16), (byte)(raw >> 8), (byte)raw};

        try
        {
            return InetAddress.getByAddress(b).getHostAddress();
        }
        catch (UnknownHostException e)
        {
            //No way here
            return null;
        }
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
        Log.i("AcknowledgeEvent",json.toString(2));
        return json;
    }















    //-------------------------------------------------------------------------------
    //
    //                      Dummy objects
    public boolean Login(ZenossCredentials credentials) throws Exception
    {
        reqCount++;
        return false;
    }
}
