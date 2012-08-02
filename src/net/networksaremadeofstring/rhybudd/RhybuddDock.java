/*
 * Copyright (C) 2012 - Gareth Llewellyn
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

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;

public class RhybuddDock extends SherlockFragmentActivity
{
	private Handler GaugeHandler = null;
	SharedPreferences settings = null;
	int EventCount = 0, DeviceCount = 0;
	public Intent callingIntent;
	public Integer DockMode;

	//New
	ZenossAPIv2 API = null;
	JSONObject EventsObject = null;
	JSONArray Events = null;
	List<ZenossEvent> listOfZenossEvents = new ArrayList<ZenossEvent>();
	List<Integer> selectedEvents = new ArrayList<Integer>();
	Thread dataPreload,AckEvent,dataReload;
	Handler handler, AckEventHandler;
	ProgressDialog dialog;
	ListView list;
	ZenossEventsAdaptor adapter;
	Cursor dbResults = null;
	ActionBar actionbar;
	RhybuddDatabase rhybuddCache;

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		rhybuddCache.Close();
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.RGBA_8888);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		setContentView(R.layout.dock);
		actionbar = getSupportActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setHomeButtonEnabled(true);
		actionbar.setTitle("Rhybudd Dock");
		//actionbar.setSubtitle("Find Stat");

		rhybuddCache = new RhybuddDatabase(this);

		settings = PreferenceManager.getDefaultSharedPreferences(this);
		list = (ListView)findViewById(R.id.ZenossEventsList);

		ConfigureHandler();
		//Draw some empty graphs
		GaugeHandler.sendEmptyMessage(1);
		GaugeHandler.sendEmptyMessage(2);

		if(settings.getBoolean("AllowBackgroundService", false))
		{
			handler.sendEmptyMessageDelayed(1, 1000);
		}
		else
		{
			//rhybuddCache.RefreshEvents();
			//handler.sendEmptyMessageDelayed(1, 1000);
		}


		/*((Thread) new Thread() 
		{  
			public void run() 
			{
				Cursor devicesCursor = rhybuddCache.getDevices();
				if(devicesCursor != null)
				{
					try
					{
						DeviceCount = devicesCursor.getCount();
						GaugeHandler.sendEmptyMessage(2);
					}
					catch(Exception e)
					{
						BugSenseHandler.log("RhybuddDock", e);
					}
				}
			}
		}).start();*/
	}


	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.dock, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		case android.R.id.home:
		{
			finish();
			return true;
		}

		case R.id.settings:
		{
			Intent SettingsIntent = new Intent(RhybuddDock.this, SettingsFragment.class);
			//RhybuddHome.this.startActivity(SettingsIntent);
			this.startActivityForResult(SettingsIntent, 99);
			return true;
		}

		case R.id.Help:
		{
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse("http://www.android-zenoss.info/help.php"));
			startActivity(i);
			return true;
		}

		case R.id.devices:
		{
			Intent DeviceList = new Intent(RhybuddDock.this, DeviceList.class);
			RhybuddDock.this.startActivity(DeviceList);
			return true;
		}

		case R.id.search:
		{
			onSearchRequested();
			return true;
		}

		default:
		{
			return false;
		}
		}
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		if(requestCode == 99)
		{
			Intent intent = new Intent(this, ZenossPoller.class);
			intent.putExtra("settingsUpdate", true);
			startService(intent);
		}
	}


	public void DBGetThread()
	{
		/*listOfZenossEvents.clear();
		dataPreload = new Thread() 
		{  
			public void run() 
			{
				try
				{
					dbResults = rhybuddCache.getEvents();
				}
				catch(Exception e)
				{
					BugSenseHandler.log("RhybuddDock", e);
					dbResults = null;
				}

				if(dbResults != null)
				{
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					Date date;
					String strDate = "";
					Date today = Calendar.getInstance().getTime();
					String[] shortMonths = new DateFormatSymbols().getShortMonths();
					int ExceptionCount = 0;

					//Update the graph
					EventCount = dbResults.getCount();
					GaugeHandler.sendEmptyMessage(1);

					while(dbResults.moveToNext())
					{
						try 
						{
							date = sdf.parse(dbResults.getString(2));
							if(date.getDate() < today.getDate())
							{
								strDate = date.getDate() + " " + shortMonths[date.getMonth()];
							}
							else
							{
								if(date.getMinutes() < 10)
								{
									strDate = date.getHours() + ":0" + Integer.toString(date.getMinutes());
								}
								else
								{
									strDate = date.getHours() + ":" + date.getMinutes();
								}
							}

						} 
						catch (ParseException e) 
						{
							strDate = "";
						}

						try
						{
							listOfZenossEvents.add(new ZenossEvent(dbResults.getString(0),
									dbResults.getString(3),
									dbResults.getString(4), 
									dbResults.getString(5),
									dbResults.getString(7),
									strDate,//dbResults.getString(2)
									dbResults.getString(8)));
						}
						catch(Exception e)
						{
							ExceptionCount++;

							if(ExceptionCount > 10)
							{
								BugSenseHandler.log("RhybuddDock", e);
								ExceptionCount = 0;
							}
						}
					}
				}
				handler.sendEmptyMessage(0);
			}
		};
		dataPreload.start();*/
	}

	private void ConfigureHandler() 
	{
		handler = new Handler() 
		{
			public void handleMessage(Message msg) 
			{
				if(msg.what == 0)
				{
					adapter = new ZenossEventsAdaptor(RhybuddDock.this, listOfZenossEvents,false);
					list.setAdapter(adapter);
				}
				else if(msg.what == 1)
				{
					this.sendEmptyMessageDelayed(2,1000);
				}
				else if(msg.what == 2)
				{
					DBGetThread();
					handler.sendEmptyMessageDelayed(1, 120000);
				}
				else
				{
					//Toast.makeText(RhybuddHome.this, "Timed out communicating with host. Please check protocol, hostname and port.", Toast.LENGTH_LONG).show();
				}
			}
		};

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
		paint.setColor(getResources().getColor(R.color.DarkBlue));
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
		try
		{
			drawScale(DeviceCanvas, false, DeviceCount, Scale);
			drawGaugeTitle(DeviceCanvas,"Device Count");
			drawGaugeNeedle(DeviceCanvas,DeviceCount, Scale);
			//drawGloss(EventsCanvas);
			((ImageView) findViewById(R.id.DeviceGauge)).setImageBitmap(charty);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void DrawEvents()
	{
		Bitmap charty = Bitmap.createBitmap(200 , 200 , Bitmap.Config.ARGB_8888);
		Canvas EventsCanvas = new Canvas(charty);
		final Paint paint = new Paint();

		paint.setStyle(Paint.Style.FILL); 
		paint.setColor(getResources().getColor(R.color.DarkBlue));
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

	/*private void drawGloss(Canvas canvas)
	{
		final Paint paint = new Paint();
		LinearGradient gradient = new LinearGradient(50, 50, 176,110,0x80c0c0c0,0x00000000, android.graphics.Shader.TileMode.CLAMP);
		paint.setDither(true);
		paint.setAntiAlias(true);
		paint.setShader(gradient);
		canvas.drawOval(new RectF(24, 6, 176, 110), paint);

	}*/

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
		scalePaint.setColor(getResources().getColor(R.color.WarningGreen));
		scalePaint.setStrokeWidth(1);
		scalePaint.setAntiAlias(true);

		scalePaint.setTextSize(12);
		scalePaint.setTypeface(Typeface.createFromAsset(this.getAssets(), "fonts/chivo.ttf"));
		scalePaint.setTextAlign(Paint.Align.CENTER);		

		float scalePosition = 10;
		RectF scaleRect = new RectF();
		scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,faceRect.right - scalePosition, faceRect.bottom - scalePosition);

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
}
