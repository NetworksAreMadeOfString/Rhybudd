package net.networksaremadeofstring.rhybudd;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service
{
    // Instance field that stores the authenticator object
    private Authenticator mAuthenticator;
    @Override
    public void onCreate()
    {
        try
        {
            // Create a new authenticator object
            mAuthenticator = new Authenticator(this);
        }
        catch (Exception e)
        {
            mAuthenticator = null;
        }

    }
    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        if(null != mAuthenticator)
        {
            return mAuthenticator.getIBinder();
        }
        else
        {
            return null;
        }
    }
}