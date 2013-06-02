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
import android.util.Log;
import com.bugsense.trace.BugSenseHandler;

/**
 * Created by Gareth on 26/05/13.
 */
public class GCMIntentService extends com.google.android.gcm.GCMBaseIntentService
{
    private NotificationManager mNM;

    public GCMIntentService()
    {
        super(ZenossAPIv2.SENDER_ID);
    }

    @Override
    protected void onError(Context arg0, String arg1)
    {
        Log.e("GCMIntentService", "Error");
    }

    @Override
    protected void onMessage(Context arg0, Intent intent)
    {
        Bundle extras = intent.getExtras();

        //GCM Payload
        String alertCount = extras.getString("count","");
        String evid = extras.getString("evid","");
        String device = extras.getString("device","");
        String summary = extras.getString("summary","");
        String status = extras.getString("status","");
        String severity = extras.getString("severity","");
        String event_class = extras.getString("event_class","");
        String event_class_key = extras.getString("event_class_key","");
        String sent = extras.getString("sent","");

        //TODO Outstanding from ZenPack
        String prodState = extras.getString("prodstate","Production");
        String firstTime = extras.getString("firsttime","");
        String componentText = extras.getString("componenttext","");
        String ownerID = extras.getString("ownerid","");

        if(alertCount == null)
            alertCount = "0";

        if(evid == null)
            evid = "0";

        if(severity == null)
            severity = "2";

        if(summary == null)
            summary = "No summary available";

        if(status == null)
            status = "unacknowledged";

        if(device == null)
            device = "localhost.localdomain";

        if(event_class == null)
            event_class = "/Status";

        if(sent == null)
            sent = "--/--/---- --:--:--";

        try
        {
            Log.v("GCMPayload",alertCount + " / " +
                    evid + " / " +
                    device + " / " +
                    summary + " / " +
                    status + " / " +
                    severity + " / " +
                    event_class + " / " +
                    event_class_key + " / " +
                    sent
            );

            //ZenossEvent gcmEvent = new ZenossEvent( evid,   Integer.getInteger(alertCount), "Production","",severity,"", "",summary,status,device,event_class,sent ,  "");
            ZenossEvent gcmEvent = new ZenossEvent( evid,   99999, prodState, firstTime, severity,componentText, "",summary,status,device,event_class,sent ,  ownerID);
            //ZenossEvent gcmEvent = new ZenossEvent(evid,summary);

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
            Log.e("GCMPayload",alertCount + " / " +
                    evid + " / " +
                    device + " / " +
                    summary + " / " +
                    status + " / " +
                    severity + " / " +
                    event_class + " / " +
                    event_class_key + " / " +
                    sent
            );
        }
    }

    @Override
    protected void onRegistered(Context arg0, String arg1)
    {
        //Log.e("GCMIntentService","onRegistered");
    }

    @Override
    protected void onUnregistered(Context arg0, String arg1)
    {
        //Log.e("GCMIntentService","onUnregistered");
    }

    /*@Override
	protected void onRecoverableError(Context context, String errorId)
	{

	}*/
}
