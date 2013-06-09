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
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.util.ArrayList;
import java.util.List;

public class ZenossAPICore extends ZenossAPI
{
    public ZenossAPICore()
    {

    }

    @Override
    public boolean Login(ZenossCredentials credentials) throws Exception
    {
        if(credentials.URL.contains("https://"))
        {
            this.PrepareSSLHTTPClient();
        }
        else
        {
            this.PrepareHTTPClient();
            //httpclient = new DefaultHttpClient();
        }

        if(!credentials.BAUser.equals("") || !credentials.BAPassword.equals(""))
        {
            //Log.i("Auth","We have some auth credentials");
            CredentialsProvider credProvider = new BasicCredentialsProvider();
            credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(credentials.BAUser, credentials.BAPassword));
            httpclient.setCredentialsProvider(credProvider);
        }

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

        //Set the variables for later
        this.ZENOSS_INSTANCE = credentials.URL;
        this.ZENOSS_USERNAME = credentials.UserName;
        this.ZENOSS_PASSWORD = credentials.Password;

        Log.e("CheckLoggedIn", Integer.toString(response.getStatusLine().getStatusCode()));

        return this.CheckLoggedIn();
    }
}
