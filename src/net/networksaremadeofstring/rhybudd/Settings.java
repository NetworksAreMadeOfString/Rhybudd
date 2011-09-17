package net.networksaremadeofstring.rhybudd;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Settings extends Activity
{
	private SharedPreferences settings = null;
	ProgressDialog dialog;
	Thread peformLogin;
	Handler handler;
	ZenossAPIv2 API = null;
	
	 /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	dialog = new ProgressDialog(this);
		dialog.setMessage("Checking Details.....");
		
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);

        setContentView(R.layout.settings);
        
        EditText urlET = (EditText) findViewById(R.id.ZenossURL);
        EditText nameET = (EditText) findViewById(R.id.ZenossUserName);
        EditText passwordET = (EditText) findViewById(R.id.ZenossPassword);
        EditText pagerDutyET = (EditText) findViewById(R.id.PagerDutyAPIKey);
        
        if(settings.getString("URL", "--").equals("--") == false)
        	urlET.setText(settings.getString("URL",""));
        
        if(settings.getString("userName", "--").equals("--") == false)
        	nameET.setText(settings.getString("userName",""));
        
        if(settings.getString("passWord", "--").equals("--") == false)
        	passwordET.setText(settings.getString("passWord",""));
        
        if(settings.getString("passWord", "--").equals("--") == false)
        	passwordET.setText(settings.getString("passWord",""));
        
        
        handler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			dialog.dismiss();
    			
    			if(API.getLoggedInStatus() == true)
    			{
            		Toast.makeText(Settings.this, "Login Successful", Toast.LENGTH_SHORT).show();
            		if(getIntent().getBooleanExtra("firstRun", false) == true)
            		{
            			Intent EventListIntent = new Intent(Settings.this, rhestr.class);
            			Settings.this.startActivity(EventListIntent);
           	    	 	finish();
            		}
            		else
            		{
            			finish();
            		}
    			}
    			else
    			{
    				Toast.makeText(Settings.this, "Login Failed", Toast.LENGTH_SHORT).show();
    			}
    		}
    	};
        
        
        
        Button LoginButton = (Button) findViewById(R.id.SaveButton);
        LoginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) 
            {
            	dialog.show();
            	EditText urlET = (EditText) findViewById(R.id.ZenossURL);
                EditText nameET = (EditText) findViewById(R.id.ZenossUserName);
                EditText passwordET = (EditText) findViewById(R.id.ZenossPassword);
            	
            	SharedPreferences.Editor editor = settings.edit();
                editor.putString("URL", urlET.getText().toString());
                editor.putString("userName", nameET.getText().toString());
                editor.putString("passWord", passwordET.getText().toString());
                editor.commit();
                
                CreateThread();
                peformLogin.start();
                    
            }
        });
        
    }
    
    public void CreateThread()
    {
    	peformLogin = new Thread() 
    	{  
    		public void run() 
    		{
    			try 
    			{
					API = new ZenossAPIv2(settings.getString("userName", "--"), settings.getString("passWord", "--"), settings.getString("URL", "--"));
				} 
    			catch (Exception e) 
    			{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    			//Double check it all worked
    			API.CheckLoggedIn();
            	
    			handler.sendEmptyMessage(0);
            	return;
    		}
    	};
    	
    }
}
