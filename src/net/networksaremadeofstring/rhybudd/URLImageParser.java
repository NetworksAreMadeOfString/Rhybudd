package net.networksaremadeofstring.rhybudd;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.Html.ImageGetter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;


public class URLImageParser implements ImageGetter 
{
    Context c;
    View container;
    Drawable drawable;
    
    /***
     * Construct the URLImageParser which will execute AsyncTask and refresh the container
     * @param t
     * @param c
     */
    public URLImageParser(View t, Context c) 
    {
        this.c = c;
        this.container = t;
    }

    public Drawable getDrawable(String source) 
    {
        URLDrawable urlDrawable = new URLDrawable();

        // get the actual source
        ImageGetterAsyncTask asyncTask = new ImageGetterAsyncTask( urlDrawable);

        asyncTask.execute(source);

        // return reference to URLDrawable where I will change with actual image from
        // the src tag
        return urlDrawable;
    }

    public class ImageGetterAsyncTask extends AsyncTask<String, Void, Drawable>
    {
        URLDrawable urlDrawable;

        public ImageGetterAsyncTask(URLDrawable d) 
        {
            this.urlDrawable = d;
        }

        @Override
        protected Drawable doInBackground(String... params) {
            String source = params[0];
            return fetchDrawable(source);
        }

        @Override
        protected void onPostExecute(Drawable result)
        {
        	if(null != result)
        	{
            // set the correct bound according to the result from HTTP call
            urlDrawable.setBounds(0, 0, 0 + result.getIntrinsicWidth(), 0 + result.getIntrinsicHeight()); 

            // change the reference of the current drawable to the result
            // from the HTTP call
            urlDrawable.drawable = result;
        	}
        	else
        	{
        		urlDrawable.drawable = null;
        	}
        
            // redraw the image by invalidating the container
            ((ImageView) URLImageParser.this.container).setImageDrawable(result);
            URLImageParser.this.container.invalidate();
        }

        /***
         * Get the Drawable from URL
         * @param urlString
         * @return
         */
        public Drawable fetchDrawable(String urlString) 
        {
            try 
            {
                InputStream is = fetch(urlString);
                drawable = Drawable.createFromStream(is, "src");
                drawable.setBounds(0, 0, 0 + drawable.getIntrinsicWidth(), 0 + drawable.getIntrinsicHeight()); 
                return drawable;
            } 
            catch (Exception e) 
            {
            	e.printStackTrace();
                return null;
            } 
        }

        private InputStream fetch(String urlString) throws MalformedURLException, IOException
        {
        	
        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        	
            DefaultHttpClient httpClient = new DefaultHttpClient();
            
            if(!settings.getString("suppBAUser", "").equals(""))
            {
	            CredentialsProvider credProvider = new BasicCredentialsProvider();
	    	    credProvider.setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials(settings.getString("suppBAUser", ""), settings.getString("suppBAPassword", "")));
	    	    httpClient.setCredentialsProvider(credProvider);
            }
            
            Log.i("fetchString",urlString);
            URL url = null;
            URI uri = null;
            
            try
            {
	            url = new URL(urlString);
	            uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
	            //url = uri.toURL();
            }
            catch(Exception e)
            {
            	return null;
            }
            
            HttpGet request = new HttpGet(uri);
            HttpResponse response = httpClient.execute(request);
            return response.getEntity().getContent();
        }
    }
}
