package net.networksaremadeofstring.rhybudd;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

public class ZenossAPI 
{
    private final static String ZENOSS_INSTANCE = "http://public-demo.zenoss.com";
    private final static String ZENOSS_USERNAME = "admin";
    private final static String ZENOSS_PASSWORD = "zenoss";

    private final static HashMap ROUTERS = new HashMap();
    static {
        ROUTERS.put("MessagingRouter", "messaging");
        ROUTERS.put("EventsRouter", "evconsole");
        ROUTERS.put("ProcessRouter", "process");
        ROUTERS.put("ServiceRouter", "service");
        ROUTERS.put("DeviceRouter", "device");
        ROUTERS.put("NetworkRouter", "messaging");
        ROUTERS.put("TemplateRouter", "template");
        ROUTERS.put("DetailNavRouter", "detailnav");
        ROUTERS.put("ReportRouter", "report");
        ROUTERS.put("MibRouter", "mib");
        ROUTERS.put("ZenPackRouter", "zenpack");
    }

    private DefaultHttpClient httpclient = new DefaultHttpClient();
    private ResponseHandler<String> responseHandler = new BasicResponseHandler();
    private JSONParser jsonParser = new JSONParser();
    private int reqCount = 1;

    // Constructor logs in to the Zenoss instance (getting the auth cookie)
    public ZenossAPI() throws Exception {
        HttpPost httpost = new HttpPost(ZENOSS_INSTANCE +
                           "/zport/acl_users/cookieAuthHelper/login");

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("__ac_name", ZENOSS_USERNAME));
        nvps.add(new BasicNameValuePair("__ac_password", ZENOSS_PASSWORD));
        nvps.add(new BasicNameValuePair("submitted", "true"));
        nvps.add(new BasicNameValuePair("came_from", ZENOSS_INSTANCE +
                                        "/zport/dmd"));

        httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        // Response from POST not needed, just the cookie
        HttpResponse response = httpclient.execute(httpost);
        // Consume so we can reuse httpclient
        response.getEntity().consumeContent();
    }

    // routerRequest is the main method used to communicate with the Zenoss instance
    private JSONObject routerRequest(String router, String method, HashMap data) throws Exception 
    {
        // Construct standard URL for requests
        HttpPost httpost = new HttpPost(ZENOSS_INSTANCE +  "/zport/dmd/" +
                            ROUTERS.get(router) + "_router");
        // Content-type MUST be set to 'application/json'
        httpost.addHeader("Content-type", "application/json; charset=utf-8");

        ArrayList packagedData = new ArrayList();
        packagedData.add(data);

        HashMap reqData = new HashMap();
        reqData.put("action", router);
        reqData.put("method", method);
        reqData.put("data", packagedData);
        reqData.put("type", "rpc");
        // Increment the request count ('tid'). More important if sending multiple
        // calls in a single request
        reqData.put("tid", String.valueOf(this.reqCount++));

        // Set the POST content to be a JSON-serialized version of request data
        httpost.setEntity(new StringEntity(JSONValue.toJSONString(reqData)));

        // Execute the request, and return the JSON-deserialized data
        String response = httpclient.execute(httpost, responseHandler);
        StringReader myStringReader = new StringReader(response);
        return (JSONObject)jsonParser.parse(myStringReader);
    }

    public JSONObject getDevices(String deviceClass) throws Exception {
        HashMap data = new HashMap();
        data.put("uid", deviceClass);

        return (JSONObject) this.routerRequest("DeviceRouter",
                                               "getDevices", data).get("result");
    }

    public JSONObject getDevices() throws Exception {
        return this.getDevices("/zport/dmd/Devices");
    }

    public JSONObject getEvents(String device, String component, String eventClass)
            throws Exception {
        HashMap data = new HashMap();
        data.put("start", 0);
        data.put("limit", 100);
        data.put("dir", "DESC");
        data.put("sort", "severity");
        HashMap params = new HashMap();
        params.put("severity", new ArrayList()
                                   {{ add(5); add(4); add(3); add(2); }});
        params.put("eventState", new ArrayList()
                                     {{ add(0); add(1); }});

        if (device != null) params.put("device", device);
        if (component != null) params.put("component", component);
        if (eventClass != null) params.put("eventClass", eventClass);
        data.put("params", params);

        return (JSONObject) this.routerRequest("EventsRouter",
                                               "query", data).get("result");
    }

    public JSONObject getEvents() throws Exception {
        return getEvents(null, null, null);
    }

    public JSONObject addDevice(String deviceName, String deviceClass) throws Exception {
        HashMap data = new HashMap();
        data.put("deviceName", deviceName);
        data.put("deviceClass", deviceClass);

        return this.routerRequest("DeviceRouter", "addDevice", data);
    }

    public JSONObject createEventOnDevice(String device, String severity,
            String summary) throws Exception {
        HashMap data = new HashMap();
        data.put("device", device);
        data.put("severity", severity);
        data.put("summary", summary);
        data.put("component", "");
        data.put("evclasskey", "");
        data.put("evclass", "");

        return this.routerRequest("EventsRouter", "add_event", data);
    }

    // Close closes the httpclient instance, shutting down the connection
    public void close() throws Exception {
        httpclient.getConnectionManager().shutdown();
    }

}
