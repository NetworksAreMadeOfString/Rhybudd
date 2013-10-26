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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.bugsense.trace.BugSenseHandler;


public class ZenossEventsAdaptor extends BaseAdapter
{
    //public static final int EVENTPOSITIONINLIST = 346345;
    //public static final int EVENTID = 678568;

	private Context context;
    private List<ZenossEvent> listZenossEvents;
    boolean isRhestr = true;
    /*private OnClickListener listener;
    private OnLongClickListener listenerLong;
    private OnClickListener cablistener;*/
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Calendar date;
	String strDate = "";
	Date today = Calendar.getInstance().getTime();
	//String[] shortMonths = new DateFormatSymbols().getShortMonths();

    public ZenossEventsAdaptor(Context context, List<ZenossEvent> _listZenossEvents)
    {
        this.context = context;
        this.listZenossEvents = _listZenossEvents;
    }

    public ZenossEventsAdaptor(Context context, List<ZenossEvent> _listZenossEvents, boolean _isRhestr) 
    {
        this.context = context;
        this.listZenossEvents = _listZenossEvents;
        this.isRhestr = _isRhestr;
    }
    
    /*public ZenossEventsAdaptor(Context context, List<ZenossEvent> _listZenossEvents, OnClickListener _listener, OnLongClickListener _listenerLong, OnClickListener _cablistener)
    {
        this.context = context;
        this.listZenossEvents = _listZenossEvents;
        this.listener = _listener;
        this.listenerLong = _listenerLong;
        this.cablistener = _cablistener;
    }*/
    
    public void remove(int position)
    {
        if(null != listZenossEvents)
            listZenossEvents.remove(position);
    }

    @Override
	public int getCount() 
    {
        if(null != listZenossEvents)
        {
    	    return listZenossEvents.size();
        }
        else
        {
            return 0;
        }
	}
    
	@Override
	public Object getItem(int position) 
	{
        if(null != listZenossEvents)
        {
		    return listZenossEvents.get(position);
        }
        else
        {
            return null;
        }
	}
	
	@Override
	public long getItemId(int position) 
	{
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
        ZenossEvent Event = new ZenossEvent("","");
        try
        {
            Event = listZenossEvents.get(position);
        }
        catch (Exception e)
        {
            //TODO We should probably bomb out here
            BugSenseHandler.sendExceptionMessage("ZenossEventsAdaptor", "getView", e);
            Event = new ZenossEvent("","None");
        }

        if (convertView == null) 
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.zenoss_event_listitem_large, null);
        }

        Typeface acknowledged = Typeface.create("sans-serif-light", Typeface.NORMAL);
        Typeface open = Typeface.create("sans-serif", Typeface.BOLD);
        Typeface normal = Typeface.create("sans-serif", Typeface.NORMAL);

        try 
		{
			
			date = Calendar.getInstance();
			date.setTime(sdf.parse(Event.getlastTime()));
			if(date.before(today) )
			{
				strDate = date.get(Calendar.DAY_OF_MONTH) + " " + date.get(Calendar.MONTH);
			}
			else
			{
				if(date.get(Calendar.MINUTE) < 10)
				{
					strDate = date.get(Calendar.HOUR_OF_DAY) + ":0" + Integer.toString(date.get(Calendar.MINUTE));
				}
				else
				{
					strDate = date.get(Calendar.HOUR_OF_DAY) + ":" + date.get(Calendar.MINUTE);
				}
			}
		} 
		catch (Exception e) 
		{
			strDate = "";
		}
        
        try
        {
	        if(Event.isFragmentDisplayed())
	        {
	        	convertView.findViewById(R.id.fragment_indicator).setVisibility(View.VISIBLE);
	        }
	        else
	        {
	        	convertView.findViewById(R.id.fragment_indicator).setVisibility(View.GONE);
	        }
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
        
        ((TextView) convertView.findViewById(R.id.dateTime)).setText(strDate);
        
        TextView DeviceNameTextView = (TextView) convertView.findViewById(R.id.DeviceName);
        DeviceNameTextView.setText(Event.getDevice());

        TextView SummaryTextView = (TextView) convertView.findViewById(R.id.EventSummary);
        SummaryTextView.setText(Event.getSummary());
        //SummaryTextView.setText("Example summary of an event (maybe with some details)");
        
        ImageView SeverityImage = (ImageView) convertView.findViewById(R.id.SeverityImageView);
        ImageView AckImage = (ImageView) convertView.findViewById(R.id.AckImage);
        
        
        if(Event.getSeverity().equals("5"))
        	SeverityImage.setImageResource(R.drawable.ic_critical);
        
        if(Event.getSeverity().equals("4"))
        	SeverityImage.setImageResource(R.drawable.ic_error);
        
        if(Event.getSeverity().equals("3"))
        	SeverityImage.setImageResource(R.drawable.ic_warning);
        
        if(Event.getSeverity().equals("2"))
        	SeverityImage.setImageResource(R.drawable.ic_info);
        
        if(Event.getSeverity().equals("1"))
        	SeverityImage.setImageResource(R.drawable.ic_debug);

        if(Event.getProgress())
        {
        	convertView.findViewById(R.id.inProgressBar).setVisibility(View.VISIBLE);
            AckImage.setVisibility(View.INVISIBLE);
        }
        else
        {
        	convertView.findViewById(R.id.inProgressBar).setVisibility(View.INVISIBLE);
            AckImage.setVisibility(View.VISIBLE);
        }

        TextView ackAuthor = ((TextView) convertView.findViewById(R.id.ackAuthor));
        ackAuthor.setTypeface(normal);

        if(null == Event.getownerID() || Event.getownerID().equals("") || Event.getownerID().equals("null") )
        {
        	try
        	{
                ackAuthor.setText("Not Ack'd");
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        }
        else
        {
        	try
        	{
                String owner = "";
                if(Event.getownerID().contains("@"))
                {
                    owner = Event.getownerID().substring(0,Event.getownerID().indexOf("@"));
                }
                else
                {
                    owner = Event.getownerID();
                }

                if(null == owner)
                {
                    ackAuthor.setText("Not Ack'd");
                }
                else
                {
                    ackAuthor.setText("Ack'd " + owner);
                }
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        }

        TextView eventCount = ((TextView) convertView.findViewById(R.id.EventCount));
        eventCount.setTypeface(normal);
        try
        {
            String count = "";
            if(Event.getCount() > 9999)
            {
                //count = "10k+";
                count = Integer.toString(Event.getCount() / 1000) + "k+";
            }
            else
            {
                count = Integer.toString(Event.getCount());
            }

            eventCount.setText("Count: " + count);
        }
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}


        TextView prodState = ((TextView) convertView.findViewById(R.id.prodState));
        prodState.setTypeface(normal);

        try
        {
            prodState.setText(Event.getProdState());
        }
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}


        if(Event.getEventState().equals("Acknowledged"))
        {
            AckImage.setImageResource(R.drawable.ic_acknowledged);
        	/*SummaryTextView.setTypeface(Typeface.DEFAULT);
        	((TextView) convertView.findViewById(R.id.dateTime)).setTypeface(Typeface.DEFAULT);*/

            DeviceNameTextView.setTypeface(normal);
            SummaryTextView.setTypeface(normal);
            ((TextView) convertView.findViewById(R.id.dateTime)).setTypeface(acknowledged);
            prodState.setTypeface(acknowledged);
            eventCount.setTypeface(acknowledged);
            ackAuthor.setTypeface(acknowledged);

            convertView.findViewById(R.id.relativeLayout1).setBackgroundResource(R.color.FlatGray);
        }
        else
        {
        	/*SummaryTextView.setTypeface(Typeface.DEFAULT_BOLD);
        	((TextView) convertView.findViewById(R.id.dateTime)).setTypeface(Typeface.DEFAULT_BOLD);*/
            AckImage.setImageResource(R.drawable.ic_unacknowledged);


            DeviceNameTextView.setTypeface(open);
            ((TextView) convertView.findViewById(R.id.dateTime)).setTypeface(open);

            SummaryTextView.setTypeface(normal);
            prodState.setTypeface(open);
            eventCount.setTypeface(open);
            ackAuthor.setTypeface(open);
            convertView.findViewById(R.id.relativeLayout1).setBackgroundResource(R.color.ZenossWhite);
        }

        //Selected
        /*((ToggleButton) convertView.findViewById(R.id.cabSelect)).setTag(R.id.EVENTPOSITIONINLIST,position);
        ((ToggleButton) convertView.findViewById(R.id.cabSelect)).setChecked(Event.isSelected());*/
        if(Event.isSelected())
        {
            convertView.findViewById(R.id.relativeLayout1).setBackgroundResource(R.color.CABSelected);
        }

        convertView.setTag(R.id.EVENTID,Event.getEVID());
        convertView.setTag(R.id.EVENTPOSITIONINLIST,position);
        
        /*convertView.setClickable(true);
        
        if(listener != null)
        	convertView.setOnClickListener((OnClickListener) listener);
        
        if(listenerLong != null)
        	convertView.setOnLongClickListener((OnLongClickListener) listenerLong);
        
        if(cablistener != null)
        	((ToggleButton) convertView.findViewById(R.id.cabSelect)).setOnClickListener(cablistener);*/
        
        return convertView;
	}
}
