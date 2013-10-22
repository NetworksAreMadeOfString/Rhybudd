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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;


public class GCMIntentService extends com.google.android.gcm.GCMBaseIntentService
{
    private NotificationManager mNM;

    public GCMIntentService()
    {
        super(ZenossAPI.SENDER_ID);
    }

    @Override
    protected void onError(Context arg0, String arg1)
    {
        //Log.e("GCMIntentService", "Error");
    }

    @Override
    protected void onMessage(Context arg0, Intent intent)
    {
        Bundle extras = intent.getExtras();
        String purpose = extras.getString("purpose","push");

        if(purpose.equals("push"))
        {
            //GCM Payload
            String alertCount = extras.getString("count","0");
            String evid = extras.getString("evid","");
            String device = extras.getString("device","");
            String summary = extras.getString("summary","");
            String status = extras.getString("status","");
            String severity = extras.getString("severity","");
            String event_class = extras.getString("event_class","");
            String event_class_key = extras.getString("event_class_key","");
            String sent = extras.getString("sent","");
            String firstTime = extras.getString("firsttime","");
            String ownerID = extras.getString("ownerid","");

            //TODO Outstanding from ZenPack
            String componentText = extras.getString("componenttext","");
            String prodState = extras.getString("prodstate","Production");

            if(null == alertCount)
                alertCount = "0";

            if(null == evid)
                evid = "0";

            if(null == severity)
                severity = "2";

            if(null == summary)
                summary = "No summary available";

            if(null == status)
                status = "unacknowledged";

            if(null == device)
                device = "localhost.localdomain";

            if(null == event_class)
                event_class = "/Status";

            if(null == sent)
                sent = "--/--/---- --:--:--";

            if(null == prodState || prodState.equals(""))
                prodState = "Production";

            if(null == firstTime)
                firstTime = "";

            if(null == componentText)
                componentText = "";

            if(null == ownerID)
                ownerID = "";


            try
            {
                /*Log.v("GCMPayload",evid + " / " +
                        alertCount + " / " +
                        prodState + " / " +
                        firstTime + " / " +
                        severity + " / " +
                        componentText + " /  /" +
                        summary + " / " +
                        status + " / " +
                        device + " / " +
                        event_class + " / " +
                        ownerID + " / " +
                        sent
                );*/

                int Count = 0;
                try
                {
                    Count = Integer.valueOf(alertCount);
                }
                catch (NullPointerException npe)
                {
                    try
                    {
                        Count = Integer.getInteger(alertCount,0);
                    }
                    catch (Exception e)
                    {
                        Count = 0;
                    }
                }
                catch (Exception e)
                {
                    Count = 0;
                }

                ZenossEvent gcmEvent = new ZenossEvent( evid, Count, prodState, firstTime, severity, componentText, "", summary, status, device, event_class, sent,  ownerID);

                RhybuddDataSource datasource = new RhybuddDataSource(arg0);
                datasource.open();
                datasource.addEvent(gcmEvent);
                datasource.close();

                Notifications.SendGCMNotification(gcmEvent,arg0);

                //TODO Broadcast if the UI is in the foreground?
            }
            catch(Exception e)
            {
                BugSenseHandler.initAndStartSession(getApplicationContext(), "44a76a8c");
                BugSenseHandler.sendExceptionMessage("GCMIntentService","onMessage",e);
                e.printStackTrace();
            }
        }
        else if(purpose.equals("ack"))
        {
            try
            {
                RhybuddDataSource datasource = new RhybuddDataSource(arg0);
                try
                {
                    datasource.open();
                    datasource.ackEvent(extras.getString("evid",""));
                }
                catch(Exception e)
                {
                    BugSenseHandler.sendExceptionMessage("GCMIntentService","onMessage",e);
                }
                finally
                {
                    datasource.close();
                }
            }
            catch (Exception e)
            {
                BugSenseHandler.sendExceptionMessage("GCMIntentService","onMessage ack",e);
            }
        }
        //Why is this a duplicate?
        else if(purpose.equals("ack"))
        {
            try
            {
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_POLLED_ALERTS);
                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_GCM_GENERIC);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            //Log.i("GCMIntentService","Unknown message");
        }

        //For DB freshness / stampede control
        try
        {
            ZenossAPI.updateLastChecked(arg0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRegistered(final Context arg0, final String regid)
    {
        //Log.e("GCMIntentService", "onRegistered");

        (new Thread()
        {
            public void run()
            {
                ZenossAPI API = null;
                try
                {
                        if (PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                        {
                            API = new ZenossAPIZaas();
                        }
                        else
                        {
                            API = new ZenossAPICore();
                        }

                        ZenossCredentials credentials = new ZenossCredentials(arg0);

                        if(API.Login(credentials))
                        {
                            API.RegisterWithZenPack(ZenossAPI.md5(Settings.Secure.getString(arg0.getContentResolver(),Settings.Secure.ANDROID_ID)),regid);
                        }
                        else
                        {
                            //Need to do something here
                            //Log.e("onRegistered","API Login failed");
                        }
                }
                catch (Exception e)
                {
                    BugSenseHandler.initAndStartSession(getApplicationContext(), "44a76a8c");
                    BugSenseHandler.sendExceptionMessage("GCMIntentService","onMessage",e);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onUnregistered(final Context arg0, String arg1)
    {
       Log.e("GCMIntentService", "onUnregistered");

        ((Thread) new Thread(){
            public void run()
            {
                try
                {
                    ZenossAPI API = null;

                    if (PreferenceManager.getDefaultSharedPreferences(arg0).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                    {
                        API = new ZenossAPIZaas();
                    }
                    else
                    {
                        API = new ZenossAPICore();
                    }

                    ZenossCredentials credentials = new ZenossCredentials(arg0);
                    API.Login(credentials);
                    API.UnregisterWithZenPack(ZenossAPI.md5(Settings.Secure.getString(arg0.getContentResolver(),Settings.Secure.ANDROID_ID)));
                }
                catch (Exception e)
                {
                    BugSenseHandler.initAndStartSession(getApplicationContext(), "44a76a8c");
                    BugSenseHandler.sendExceptionMessage("GCMIntentService","onMessage",e);
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /*@Override
	protected void onRecoverableError(Context context, String errorId)
	{

	}*/
}
