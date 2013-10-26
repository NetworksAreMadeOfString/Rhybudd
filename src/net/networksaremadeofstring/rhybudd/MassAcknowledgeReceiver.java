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
import android.content.*;
import android.preference.PreferenceManager;
import com.bugsense.trace.BugSenseHandler;
import java.util.ArrayList;
import java.util.List;


public class MassAcknowledgeReceiver extends BroadcastReceiver
{
    public static String BROADCAST_ACTION = "net.networksaremadeofstring.rhybudd.broadcast.massacknowledge";
    SharedPreferences settings;

    @Override
    public void onReceive(final Context context, Intent intent)
    {
        try
        {
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_GCM_GENERIC);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Notifications.NOTIFICATION_POLLED_ALERTS);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("MassAcknowledgeReceiver", "onReceive", e);
        }

        try
        {
            settings = PreferenceManager.getDefaultSharedPreferences(context);
        }
        catch (Exception e)
        {
            BugSenseHandler.sendExceptionMessage("MassAcknowledgeReceiver", "onReceive", e);
        }

        ((Thread) new Thread()
        {
            public void run()
            {
                try
                {
                    ZenossAPI API;

                    if(settings.getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS,false))
                    {
                        API = new ZenossAPIZaas();
                    }
                    else
                    {
                        API = new ZenossAPICore();
                    }

                    List<ZenossEvent> listOfZenossEvents = null;

                    ZenossCredentials credentials = new ZenossCredentials(context);
                    API.Login(credentials);

                    listOfZenossEvents = API.GetRhybuddEvents(context);

                    if(listOfZenossEvents!= null && listOfZenossEvents.size() > 0)
                    {
                        final List<String> EventIDs = new ArrayList<String>();

                        for (ZenossEvent evt : listOfZenossEvents)
                        {
                            if(!evt.getEventState().equals("Acknowledged"))
                            {
                                EventIDs.add(evt.getEVID());
                            }
                        }

                        API.AcknowledgeEvents(EventIDs);


                        RhybuddDataSource datasource = null;
                        try
                        {
                            datasource = new RhybuddDataSource(context);
                            datasource.open();
                            datasource.ackAllEvents(EventIDs);
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                            BugSenseHandler.sendExceptionMessage("MassAcknowledgeReceiver", "DBUpdate", e);
                        }
                        finally
                        {
                            if(null != datasource)
                                datasource.close();
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    BugSenseHandler.sendExceptionMessage("MassAcknowledgeReceiver", "onReceive", e);
                }
            }
        }).start();
    }
}
