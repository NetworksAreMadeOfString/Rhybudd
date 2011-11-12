package net.networksaremadeofstring.rhybudd;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;


public class RhybuddHome extends Activity
{
	private SharedPreferences settings = null;
	private Handler HomeHandler = null, runnablesHandler = null;
	private Runnable updateEvents = null, updateDevices = null;
	private Boolean OneOff = true;
	
	public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences("rhybudd", 0);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.home);
        ConfigureHandler();
        ConfigureRunnable();
        //updateDevices.run();
        updateEvents.run();
    }
	
	private void ConfigureHandler()
	{
		runnablesHandler = new Handler();
		
		HomeHandler = new Handler() 
    	{
    		public void handleMessage(Message msg) 
    		{
    			if(msg.what == 0)
    			{
    				((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Refreshing Events...");
    			}
    			else if(msg.what == 1)
    			{
    				((TextView) findViewById(R.id.CurrentTaskLabel)).setText("Refreshing Infrastructure..");
    			}
    			else if(msg.what == 98)//One off case
    			{
    				updateDevices.run();
    			}
    			else if(msg.what == 99)//Hide
    			{
    				((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(8);
    				((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(8);
    			}
    			else if(msg.what == 100)//Show
    			{
    				((TextView) findViewById(R.id.CurrentTaskLabel)).setVisibility(0);
    				((ProgressBar) findViewById(R.id.progressBar1)).setVisibility(0);
    			}
    		}
    	};
	}
	
	private void ConfigureRunnable()
	{
		updateDevices = new Runnable() {
	           public void run() 
	           {
	        	   //Update the GUI
	        	   HomeHandler.sendEmptyMessage(100);
	        	   HomeHandler.sendEmptyMessage(1);
	        	   
	        	   //Thread
	        	   Thread test = new Thread(){
	        		   public void run() 
	    	           {
	        			   try {
	   						Thread.sleep(2000);
	   					} catch (InterruptedException e) {
	   						// TODO Auto-generated catch block
	   						e.printStackTrace();
	   					}
	        			   
	        			   //Hide progress
	        			   HomeHandler.sendEmptyMessage(99);
	    	        	   
	    	        	 	//Try again later if the app is still live
	    	        	 	runnablesHandler.postDelayed(this, 3604000);//1 hour
	    	           }
	        	   };
	        	   test.start();
	           }
		};
		
		updateEvents = new Runnable() {
	           public void run() 
	           {
	        	   //Update the GUI
	        	   HomeHandler.sendEmptyMessage(100);
	        	   HomeHandler.sendEmptyMessage(0);
	        	   
	        	   //Thread
	        	   Thread test = new Thread(){
	        		   public void run() 
	    	           {
	        			   try {
	   						Thread.sleep(2000);
	   					} catch (InterruptedException e) {
	   						// TODO Auto-generated catch block
	   						e.printStackTrace();
	   					}
    	        	 	//Hide progress
    		        	   HomeHandler.sendEmptyMessage(99);
    		        	   
    		        	   runnablesHandler.postDelayed(this, 300000);//5 mins
    		        	   
    		        	   if(OneOff)
    		        	   {
    		        		   //Kick off the infrastructure refresh now we're done with the other bit
    		        		   HomeHandler.sendEmptyMessage(98);
    		        		   OneOff = false;
    		        	   }
	    	           }
	        	   };
	        	   test.start();
	        	   
	        	   
	           }
		};
	}
}
