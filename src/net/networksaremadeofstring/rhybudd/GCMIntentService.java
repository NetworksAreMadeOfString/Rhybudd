package net.networksaremadeofstring.rhybudd;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
        //GCM Payload
        String alertCount = intent.getExtras().getString("count");
        String evid = intent.getExtras().getString("evid");
        String device = intent.getExtras().getString("device");
        String summary = intent.getExtras().getString("summary");
        String status = intent.getExtras().getString("status");

        String severity = intent.getExtras().getString("severity");
        String event_class = intent.getExtras().getString("event_class");
        String event_class_key = intent.getExtras().getString("event_class_key");
        String sent = intent.getExtras().getString("sent");

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
