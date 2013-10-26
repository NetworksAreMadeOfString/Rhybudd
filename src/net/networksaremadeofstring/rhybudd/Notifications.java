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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import java.util.Calendar;
import java.util.List;

public class Notifications
{
    public static int NOTIFICATION_POLLED_ALERTS = 43523;
    public static int NOTIFICATION_POLLED_STICKY = 20;

    public static int NOTIFICATION_GCM_CRITICAL = 900;
    public static int NOTIFICATION_GCM_ERROR = 910;
    public static int NOTIFICATION_GCM_WARNING = 920;
    public static int NOTIFICATION_GCM_INFO = 930;
    public static int NOTIFICATION_GCM_DEBUG = 940;
    public static int NOTIFICATION_GCM_RATELIMIT = 950;
    public static int NOTIFICATION_GCM_COLLAPSE = 960;
    public static int NOTIFICATION_GCM_GENERIC = 970;

    /*@TargetApi(16)
    public static void SendInboxStyleNotification(int EventCount, List<String> EventDetails, Context context)
    {
        NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String Event1 = "--", Event2 = "---";
        int remainingCount = 0;

        if(EventDetails.size() > 1)
        {
            Event1 = EventDetails.get(0);
            Event2 = EventDetails.get(1);
            remainingCount = EventCount - 2;
        }
        else
        {
            Event1 = EventDetails.get(0);
            remainingCount = EventCount - 1;
        }

        Notification notification = new Notification.InboxStyle(
                new Notification.Builder(context)
                        .setContentTitle(Integer.toString(EventCount) + " New Zenoss Alerts")
                        .setContentText("Click to start Rhybudd")
                        .setSmallIcon(R.drawable.ic_stat_alert)
                        .setVibrate(new long[] {0,100,200,300})
                        .setAutoCancel(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, RhybuddHome.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("forceRefresh", true), 0)))
                .addLine(Event1)
                .addLine(Event2)
                .setBigContentTitle(Integer.toString(EventCount) + " New Zenoss Alerts")
                .setSummaryText("+"+Integer.toString(remainingCount)+" more")
                .build();

        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notificationSound", true))
        {
            if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notificationSoundInsistent", false))
                notification.flags |= Notification.FLAG_INSISTENT;

            if(PreferenceManager.getDefaultSharedPreferences(context).getString("notificationSoundChoice", "").equals(""))
            {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
            else
            {
                try
                {
                    notification.sound = Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("notificationSoundChoice", ""));
                }
                catch(Exception e)
                {
                    notification.defaults |= Notification.DEFAULT_SOUND;
                }
            }
        }

        mNM.notify(NOTIFICATION_POLLED_ALERTS, notification);
    }*/



    public static void SendGCMNotification(ZenossEvent Event, Context context)
    {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        Time now = new Time();
        now.setToNow();

        //We don't need to overwhelm the user with their notification sound / vibrator
        if((now.toMillis(true) - PreferenceManager.getDefaultSharedPreferences(context).getLong("lastCheck",now.toMillis(true))) < 3000)
        {
            //Log.e("SendGCMNotification", "Not publishing a notification due to stampede control");
            return;
        }

        Intent notificationIntent = new Intent(context, ViewZenossEventsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("forceRefresh", true);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Uri soundURI = null;
        try
        {
            if(settings.getString("notificationSoundChoice", "").equals(""))
            {
                soundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            else
            {
                try
                {
                    soundURI = Uri.parse(settings.getString("notificationSoundChoice", ""));
                }
                catch(Exception e)
                {
                    soundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
        }
        catch (Exception e)
        {

        }

        String notifTitle = "New Events Received";
        int notifPriority = Notification.PRIORITY_DEFAULT;
        //int AlertType = NOTIFICATION_GCM_GENERIC;

        try
        {
            if(Event.getSeverity().equals("5"))
            {
                notifTitle = context.getString(R.string.CriticalNotificationTitle);
                notifPriority = Notification.PRIORITY_MAX;
                //AlertType = NOTIFICATION_GCM_CRITICAL;
            }
            else if(Event.getSeverity().equals("4"))
            {
                notifTitle = context.getString(R.string.ErrorNotificationTitle);
                notifPriority = Notification.PRIORITY_HIGH;
                //AlertType = NOTIFICATION_GCM_ERROR;
            }
            else if(Event.getSeverity().equals("3"))
            {
                notifTitle = context.getString(R.string.WarnNotificationTitle);
                notifPriority = Notification.PRIORITY_DEFAULT;
                //AlertType = NOTIFICATION_GCM_WARNING;
            }
            else if(Event.getSeverity().equals("2"))
            {
                notifTitle = context.getString(R.string.InfoNotificationTitle);
                notifPriority = Notification.PRIORITY_LOW;
                //AlertType = NOTIFICATION_GCM_INFO;
            }
            else if(Event.getSeverity().equals("1"))
            {
                notifTitle = context.getString(R.string.DebugNotificationTitle);
                notifPriority = Notification.PRIORITY_MIN;
                //AlertType = NOTIFICATION_GCM_DEBUG;
            }
        }
        catch (Exception e)
        {

        }

        long[] vibrate = {0,100,200,300};

        try
        {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            //Log.e("audio.getRingerMode()",Integer.toString(audio.getRingerMode()));
            switch( audio.getRingerMode() )
            {
                case AudioManager.RINGER_MODE_SILENT:
                    //Do nothing to fix GitHub issue #13
                    //Log.e("AudioManager","Doing nothing because we are silent");
                    vibrate = new long[] {0,0};
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Intent broadcastMassAck = new Intent();
        broadcastMassAck.setAction(MassAcknowledgeReceiver.BROADCAST_ACTION);
        PendingIntent pBroadcastMassAck = PendingIntent.getBroadcast(context,0,broadcastMassAck,0);

        if(Build.VERSION.SDK_INT >= 16)
        {
            Notification noti = new Notification.BigTextStyle(
                    new Notification.Builder(context)
                            .setContentTitle(notifTitle)
                            .setPriority(notifPriority)
                            .setAutoCancel(true)
                            .setSound(soundURI)
                            .setVibrate(vibrate)
                            .setContentText(Event.getDevice())
                            .setContentIntent(contentIntent)
                            .addAction(R.drawable.ic_action_resolve_all,"Acknowledge all Events",pBroadcastMassAck)
                            .setSmallIcon(R.drawable.ic_stat_alert))
                            .bigText(Event.getSummary() + "\r\n" +
                                    Event.getComponentText() + "\r\n" +
                                    Event.geteventClass())
                    .build();

            if(settings.getBoolean("notificationSoundInsistent", false))
                noti.flags |= Notification.FLAG_INSISTENT;

            noti.tickerText = notifTitle;

            NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNM.notify(NOTIFICATION_GCM_GENERIC, noti);
        }
        else
        {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_stat_alert)
                            .setContentTitle(notifTitle)
                            .setContentText(Event.getDevice() + ": " + Event.getSummary())
                            .setContentIntent(contentIntent)
                            .setSound(soundURI)
                            .setVibrate(vibrate)
                            .addAction(R.drawable.ic_action_resolve_all,"Acknowledge all Events",pBroadcastMassAck)
                            .setAutoCancel(true)
                            .setPriority(notifPriority);

            NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNM.notify(NOTIFICATION_GCM_GENERIC, mBuilder.build());
        }
    }

    public static void SendPollNotification(int EventCount, List<String> EventDetails, Context context)
    {
        Intent notificationIntent = new Intent(context, ViewZenossEventsListActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("forceRefresh", true);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Intent broadcastMassAck = new Intent();
        broadcastMassAck.setAction(MassAcknowledgeReceiver.BROADCAST_ACTION);
        PendingIntent pBroadcastMassAck = PendingIntent.getBroadcast(context,0,broadcastMassAck,0);

        /*Intent broadcastIgnore = new Intent();
        broadcastIgnore.setAction(BatchIgnoreReceiver.BROADCAST_ACTION);
        PendingIntent pBroadcastIgnore = PendingIntent.getBroadcast(this,0,broadcastIgnore,0);*/

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Uri soundURI;
        int Flags = -1;
        String Event1 = "--", Event2 = "---";
        int remainingCount = 0;

        if(settings.getString("notificationSoundChoice", "").equals(""))
        {
            soundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        else
        {
            try
            {
                soundURI = Uri.parse(settings.getString("notificationSoundChoice", ""));
            }
            catch(Exception e)
            {
                soundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }


        try
        {
            if(EventDetails.size() > 1)
            {
                Event1 = EventDetails.get(0);
                Event2 = EventDetails.get(1);
                remainingCount = EventCount - 2;
            }
            else
            {
                Event1 = EventDetails.get(0);
                remainingCount = EventCount - 1;
            }
        }
        catch (Exception e)
        {

        }

        long[] vibrate = {0,100,200,300};

        try
        {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            //Log.e("audio.getRingerMode()",Integer.toString(audio.getRingerMode()));
            switch( audio.getRingerMode() )
            {
                case AudioManager.RINGER_MODE_SILENT:
                    //Do nothing to fix GitHub issue #13
                    //Log.e("AudioManager","Doing nothing because we are silent");
                    vibrate = new long[] {0,0};
                    break;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_stat_alert)
                            .setContentTitle(Integer.toString(EventCount) + " new Zenoss Events!")
                            .setContentText("Tap to start Rhybudd")
                            .setContentIntent(contentIntent)
                            .setNumber(EventCount)
                            .setSound(soundURI)
                            .setVibrate(vibrate)
                            .setAutoCancel(true)
                            .setOngoing(false)
                            .addAction(R.drawable.ic_action_resolve_all, "Acknowledge all Events", pBroadcastMassAck)
                            .setPriority(Notification.PRIORITY_HIGH);

            Notification notif = mBuilder.build();
            notif.tickerText = Integer.toString(EventCount) + " new Zenoss Events!";

            if(settings.getBoolean("notificationSoundInsistent", false))
                notif.flags |= Notification.FLAG_INSISTENT;

            NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNM.notify(NOTIFICATION_POLLED_ALERTS, notif);
        }
        catch (Exception e)
        {

        }
    }


    public static void SendStickyNotification(Context context)
    {
        try
        {
            Calendar date = Calendar.getInstance();
            String strDate = "";
            if(date.get(Calendar.MINUTE) < 10)
            {
                strDate = Integer.toString(date.get(Calendar.HOUR_OF_DAY)) + ":0" + Integer.toString(date.get(Calendar.MINUTE));
            }
            else
            {
                strDate = date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE);
            }

            Intent notificationIntent = new Intent(context, ViewZenossEventsListActivity.class);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent.putExtra("forceRefresh", true);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_stat_polling)
                            .setContentTitle("Rhybudd is actively polling for events")
                            .setContentText("Last query: " + strDate + " (Moving to Rhybudd Push would reduce battery drain & data usage)")
                            .setOngoing(true)
                            .setOnlyAlertOnce(true)
                            .setContentIntent(contentIntent)
                            .setPriority(Notification.PRIORITY_LOW);

            NotificationManager mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNM.notify(NOTIFICATION_POLLED_STICKY, mBuilder.build());
        }
        catch (Exception e)
        {

        }
    }
}
