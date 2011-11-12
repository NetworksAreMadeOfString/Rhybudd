package net.networksaremadeofstring.rhybudd;

import java.io.ByteArrayOutputStream;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.widget.RemoteViews;

public class ZenossWidgetGraph extends AppWidgetProvider
{
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) 
    {
        final int N = appWidgetIds.length;
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.zenoss_widget_graph);
		Intent intent = new Intent(context, rhestr.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        
        Bitmap emptyBmap = Bitmap.createBitmap(290,150, Config.ARGB_8888); 
        
		int width =  emptyBmap.getWidth();
		int height = emptyBmap.getHeight();
		Bitmap charty = Bitmap.createBitmap(width , height , Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(charty);
		final int color = 0xff0B0B61; 
		final Paint paint = new Paint();
		/*final Rect rect = new Rect(0, 0, emptyBmap.getWidth(), emptyBmap.getHeight());
		final RectF rectF = new RectF(rect);   
		final float roundPx = 12;
		paint.setAntiAlias(true);
		canvas.drawARGB(128, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);*/

		paint.setStyle(Paint.Style.FILL); 
		paint.setColor(Color.WHITE ); 
		
		canvas.drawText("100",  0, 10 , paint );
		
		//y
		canvas.drawLine(25,0,25,289, paint);
		//x
		canvas.drawLine(25,149,289,149, paint);
		
		int CritArray[] = { 5, 4, 6, 10, 10, 6, 4, 4};
		int curX = 25;
		
		int divisor = 148 / 10;
		paint.setColor(Color.RED);
		int curY = 148 - (CritArray[0] * divisor);
		
		for (int a : CritArray) 
		{
			canvas.drawLine(curX,curY,curX + 32, (148 - (a * divisor)), paint);
			curX += 32;
			curY = 148 - (a * divisor);
        }
		
		int ErrArray[] = { 1, 2, 2, 2, 4, 2, 1, 0};
		curX = 25;
		
		paint.setColor(Color.rgb(255, 102, 0));
		curY = 148 - (ErrArray[0] * divisor);
		
		for (int a : ErrArray) 
		{
			canvas.drawLine(curX,curY,curX + 32, (148 - (a * divisor)), paint);
			curX += 32;
			curY = 148 - (a * divisor);
        }
		
		int WarnArray[] = { 0, 2, 4, 8, 10, 4, 2, 2};
		curX = 25;
		
		paint.setColor(Color.YELLOW);
		curY = 148 - (WarnArray[0] * divisor);
		
		Path myPath = new Path();
	    
	    
	    
		for (int a : WarnArray) 
		{
			//myPath.moveTo(curX - 2, curY - 2);
			//myPath.quadTo(curX, curY, curX + 32, (148 - (a * divisor)));
			canvas.drawLine(curX,curY,curX + 32, (148 - (a * divisor)), paint);
			curX += 32;
			curY = 148 - (a * divisor);
        }
		//canvas.drawPath(myPath, paint);
		
		
		/*int x = 0, label = 0;
		//Grid
		for(int i=1; i < 45 ; i++)
		{
			x = i * 10;
			canvas.drawLine(x,0,x,80, paint);
			//canvas.drawLine(drawSizes[0]+ (i * drawSizes[2] / 5), drawSizes[1], drawSizes[0] + (i * drawSizes[2] / 5), drawSizes[1] + drawSizes[3], paint);

			if(label == 0)
			{
				canvas.drawText(Integer.toString((int)Math.round(x / 2.5)) + "m",  (float)x, 95 , paint );
				label = 3;
			}
			else
			{
				label--;
			}
		}*/
		
		ByteArrayOutputStream out =  new ByteArrayOutputStream();
		charty.compress(CompressFormat.PNG, 50, out);
		
		Bitmap finalBMP = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
		Log.i("GraphWidget","Drawing Graph!");
		for (int i=0; i<N; i++) 
        {
			int appWidgetId = appWidgetIds[i];
			views.setImageViewBitmap(R.id.graphCanvas, finalBMP);
			appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
}
