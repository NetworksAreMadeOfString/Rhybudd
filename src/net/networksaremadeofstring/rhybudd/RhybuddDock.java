package net.networksaremadeofstring.rhybudd;

import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class RhybuddDock extends Activity
{
	private Handler GaugeHandler = null, runnablesHandler = null;
	private Runnable updateEvents = null, updateDevices = null;
	private SharedPreferences settings = null;
	int EventCount = 0, DeviceCount = 0;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.dock);
        settings = getSharedPreferences("rhybudd", 0);

        ConfigureHandler();
        //Draw some empty graphs
        GaugeHandler.sendEmptyMessage(1);
        GaugeHandler.sendEmptyMessage(2);
        
        //Let's populate those graphs
        ConfigureRunnable();
        updateEvents.run();
        updateDevices.run();
        
        ConfigureImageViews();
    }
	
	private void ConfigureImageViews()
	{
		ImageView ExitDockButton = (ImageView) findViewById(R.id.ExitDockImageView);
        ExitDockButton.setClickable(true);
        ExitDockButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				finish();
			}
        });
        
		ImageView EventsButton = (ImageView) findViewById(R.id.EventsImageView);
		EventsButton.setClickable(true);
		EventsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent EventsIntent = new Intent(RhybuddDock.this, rhestr.class);
				RhybuddDock.this.startActivity(EventsIntent);
			}
        });
		
		ImageView DevicesButton = (ImageView) findViewById(R.id.DevicesImageView);
		DevicesButton.setClickable(true);
		DevicesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent DeviceListIntent = new Intent(RhybuddDock.this, DeviceList.class);
				RhybuddDock.this.startActivity(DeviceListIntent);
			}
        });
		
		ImageView ReportsButton = (ImageView) findViewById(R.id.ReportsImageView);
		ReportsButton.setClickable(true);
		ReportsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent ReportsIntent = new Intent(RhybuddDock.this, rhestr.class);
				RhybuddDock.this.startActivity(ReportsIntent);
			}
        });
	}
	
	private void ConfigureHandler() {
		runnablesHandler = new Handler();

		GaugeHandler = new Handler() {
			public void handleMessage(Message msg) 
			{
				if(msg.what == 1)
				{
					DrawEvents();
				}
				else
				{
					DrawDevices();
				}
		        
			}
		};
	}
	
	private void DrawDevices()
	{
		Bitmap charty = Bitmap.createBitmap(200 , 200 , Bitmap.Config.ARGB_8888);
		Canvas DeviceCanvas = new Canvas(charty);
		final Paint paint = new Paint();

		paint.setStyle(Paint.Style.FILL); 
		paint.setColor(R.color.DarkBlue);
		paint.setAntiAlias(true);
		DeviceCanvas.drawOval(new RectF(1, 1, 199, 199), paint);
		
		RadialGradient gradient = new RadialGradient(100, 50, 150, 0xFF6b7681, 0xFF000000, android.graphics.Shader.TileMode.CLAMP);
		paint.setDither(true);
		paint.setShader(gradient);
		DeviceCanvas.drawOval(new RectF(6, 6, 194, 194), paint);
		
		int Scale = 10;
		
		//Special Bits
		if(DeviceCount < 500)
			Scale = 750;
		
		if(DeviceCount < 250)
			Scale = 350;
		
		if(DeviceCount < 100)
			Scale = 150;
		
		if(DeviceCount < 50)
			Scale = 50;
		
		drawScale(DeviceCanvas, false, DeviceCount, Scale);
		drawGaugeTitle(DeviceCanvas,"Device Count");
		drawGaugeNeedle(DeviceCanvas,DeviceCount, Scale);
		//drawGloss(EventsCanvas);
		
		((ImageView) findViewById(R.id.DeviceGauge)).setImageBitmap(charty);
	}
	
	private void DrawEvents()
	{
		Bitmap charty = Bitmap.createBitmap(200 , 200 , Bitmap.Config.ARGB_8888);
		Canvas EventsCanvas = new Canvas(charty);
		final Paint paint = new Paint();

		paint.setStyle(Paint.Style.FILL); 
		paint.setColor(R.color.DarkBlue);
		paint.setAntiAlias(true);
		EventsCanvas.drawOval(new RectF(1, 1, 199, 199), paint);
		
		RadialGradient gradient = new RadialGradient(200, 200, 200, 0xFF6b7681, 0xFF000000, android.graphics.Shader.TileMode.CLAMP);
		paint.setDither(true);
		paint.setShader(gradient);
		EventsCanvas.drawOval(new RectF(6, 6, 194, 194), paint);
		
		//Special Bits
		int Scale = 100;
		
		if(EventCount > 100);
			Scale = EventCount + 50;
			
		drawScale(EventsCanvas, true, EventCount, Scale);
		drawGaugeTitle(EventsCanvas,"Events Count");
		drawGaugeNeedle(EventsCanvas,EventCount, Scale);
		//drawGloss(EventsCanvas);
		
		((ImageView) findViewById(R.id.EventsGauge)).setImageBitmap(charty);
	}
	
	@SuppressWarnings("unused")
	private void drawGloss(Canvas canvas)
	{
		final Paint paint = new Paint();
		LinearGradient gradient = new LinearGradient(50, 50, 176,110,0x80c0c0c0,0x00000000, android.graphics.Shader.TileMode.CLAMP);
		paint.setDither(true);
		paint.setAntiAlias(true);
		paint.setShader(gradient);
		canvas.drawOval(new RectF(24, 6, 176, 110), paint);
		
	}
	
	private void drawGaugeNeedle(Canvas canvas, int count, int Scale)
	{
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		float divisor = 360.0f / Scale;
		
		canvas.rotate((float) (divisor * count), 100, 100);
		
		//Inside
		Paint needleInsidePaint = new Paint();
		needleInsidePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		needleInsidePaint.setColor(Color.WHITE);
		needleInsidePaint.setStrokeWidth(4);
		needleInsidePaint.setAntiAlias(true);

		Paint needleEdgePaint = new Paint();
		needleEdgePaint.setStyle(Paint.Style.STROKE);
		needleEdgePaint.setColor(Color.DKGRAY);
		needleEdgePaint.setStrokeWidth(0.5f);
		needleEdgePaint.setAntiAlias(true);
		
		canvas.drawOval(new RectF(95,95,105,105), needleInsidePaint);
		canvas.drawOval(new RectF(95,96,105,105), needleEdgePaint);
		
		Path needleInside = new Path();
		needleInside.moveTo(98, 98);
		needleInside.lineTo(100, 20);
		needleInside.lineTo(102, 102);
		canvas.drawPath(needleInside, needleInsidePaint);
	
		Path needleEdge = new Path();
		needleInside.moveTo(99, 99);
		needleInside.lineTo(99, 19);
		needleInside.lineTo(103, 103);
		
		canvas.drawPath(needleEdge, needleEdgePaint);
		canvas.restore();
	}
	
	private void drawGaugeTitle(Canvas canvas, String Title)
	{
		Paint titlePaint = new Paint();
		titlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
		titlePaint.setColor(Color.LTGRAY);//0x9f004d0f
		titlePaint.setStrokeWidth(1);//0.005f
		titlePaint.setAntiAlias(true);
		titlePaint.setTextSize(14);
		titlePaint.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
		
		/*Path titlePath = new Path();
		titlePath.addArc(new RectF(40, 90, 196, 155), -180.0f, -180.0f);
		canvas.drawTextOnPath(Title, titlePath, 0.0f,0.0f, titlePaint);*/
		
		canvas.drawText(Title, 60, 160, titlePaint);
	}
	
	
	private void drawScale(Canvas canvas, Boolean Colors, int Count, int Max) 
	{
		RectF faceRect = new RectF();
		faceRect.set(10, 10, 190, 190);
		
		Paint scalePaint = new Paint();
		scalePaint.setStyle(Paint.Style.STROKE);
		scalePaint.setColor(getResources().getColor(R.color.WarningGreen));//0x9f004d0f
		scalePaint.setStrokeWidth(1);//0.005f
		scalePaint.setAntiAlias(true);
		
		scalePaint.setTextSize(12);
		scalePaint.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
		scalePaint.setTextAlign(Paint.Align.CENTER);		
		
		float scalePosition = 10;
		RectF scaleRect = new RectF();
		scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
					  faceRect.right - scalePosition, faceRect.bottom - scalePosition);

		if(!Colors)
			scalePaint.setColor(Color.WHITE);
		
		scalePaint.setStrokeWidth(2);
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		for (int i = 0; i < Max; ++i) 
		{
			if(Colors)
			{
				if(i > 20)
					scalePaint.setColor(getResources().getColor(R.color.WarningYellow));
				
				if(i > 40)
					scalePaint.setColor(getResources().getColor(R.color.WarningOrange));
				
				if(i > 60)
					scalePaint.setColor(getResources().getColor(R.color.WarningRed));
			}
			
			//float y1 = scaleRect.top;
			
			canvas.drawLine(100, 20, 100, 18, scalePaint);
			int divisor = 5;
			
			if(Max > 100)
				divisor = 25;
			
			if (i % divisor == 0) 
			{
				canvas.drawText(Integer.toString(i), 100, 16, scalePaint);
			}
			
			canvas.rotate((360.0f / Max), 100, 100);
		}

		canvas.restore();		
	}

	
	private void ConfigureRunnable() 
	{
		updateDevices = new Runnable() 
		{
			public void run() 
			{
				Thread devicesRefreshThread = new Thread() 
				{
					public void run() 
					{
						ZenossAPIv2 API = null;
						JSONObject DeviceObject = null;
						
						try 
						{
							API = new ZenossAPIv2(settings.getString("userName", ""), settings.getString("passWord", ""), settings.getString("URL", ""));
							if(API != null)
							{
								DeviceObject = API.GetDevices();
								
								DeviceCount = DeviceObject.getJSONObject("result").getInt("totalCount");
								GaugeHandler.sendEmptyMessage(99);
							}
						} 
						catch (ClientProtocolException e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} 
						catch (JSONException e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} 
						catch (IOException e1) 
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						catch(Exception e)
						{
							
						}
						
						// Try again later if the app is still live
						runnablesHandler.postDelayed(this, 300000);// 1 hour
					}
				};
				devicesRefreshThread.start();
			}
		};

		updateEvents = new Runnable() 
		{
			public void run() 
			{
				Thread eventsRefreshThread = new Thread() 
				{
					public void run() 
					{
						JSONObject EventsObject = null;

						try 
						{
							ZenossAPIv2 API = new ZenossAPIv2(settings.getString("userName", ""),settings.getString("passWord", ""),settings.getString("URL", ""));

							EventsObject = API
									.GetEvents(settings.getBoolean(
											"SeverityCritical", true), settings
											.getBoolean("SeverityError", true),
											settings.getBoolean(
													"SeverityWarning", true),
											settings.getBoolean("SeverityInfo",
													false), settings
													.getBoolean(
															"SeverityDebug",
															false));
							EventCount = EventsObject.getJSONObject("result").getInt("totalCount");
							GaugeHandler.sendEmptyMessage(1);
						} 
						catch (Exception e) 
						{
							e.printStackTrace();
						}
						
						runnablesHandler.postDelayed(this, 30000);
					}
				};
				
				eventsRefreshThread.start();
			}
		};
	}
}
