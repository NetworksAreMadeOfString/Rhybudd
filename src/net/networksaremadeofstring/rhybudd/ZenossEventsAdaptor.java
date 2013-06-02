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

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;



public class ZenossEventsAdaptor extends BaseAdapter
{
	private Context context;
    private List<ZenossEvent> listZenossEvents;
    boolean isRhestr = true;
    private OnClickListener listener;
    private OnLongClickListener listenerLong;
    private OnClickListener cablistener;
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Calendar date;
	String strDate = "";
	Date today = Calendar.getInstance().getTime();
	String[] shortMonths = new DateFormatSymbols().getShortMonths();
    
    public ZenossEventsAdaptor(Context context, List<ZenossEvent> _listZenossEvents, boolean _isRhestr) 
    {
        this.context = context;
        this.listZenossEvents = _listZenossEvents;
        this.isRhestr = _isRhestr;
    }
    
    public ZenossEventsAdaptor(Context context, List<ZenossEvent> _listZenossEvents, OnClickListener _listener, OnLongClickListener _listenerLong, OnClickListener _cablistener) 
    {
        this.context = context;
        this.listZenossEvents = _listZenossEvents;
        this.listener = _listener;
        this.listenerLong = _listenerLong;
        this.cablistener = _cablistener;
    }
    
    
    @Override
	public int getCount() 
    {
    	 return listZenossEvents.size();
	}
    
	@Override
	public Object getItem(int position) 
	{
		return listZenossEvents.get(position);
	}
	
	@Override
	public long getItemId(int position) 
	{
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		ZenossEvent Event = listZenossEvents.get(position);
        if (convertView == null) 
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.zenoss_event_listitem_large, null);
        }
        
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
	        	((ImageView) convertView.findViewById(R.id.fragment_indicator)).setVisibility(View.VISIBLE);
	        }
	        else
	        {
	        	((ImageView) convertView.findViewById(R.id.fragment_indicator)).setVisibility(View.GONE);
	        }
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
        
        ((TextView) convertView.findViewById(R.id.dateTime)).setText(strDate);
        
        TextView DeviceNameTextView = (TextView) convertView.findViewById(R.id.DeviceName);
        DeviceNameTextView.setText(Event.getDevice());
        //DeviceNameTextView.setText("Example Device");
        
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
        
        if(Event.getEventState().equals("Acknowledged"))
        {
        	AckImage.setImageResource(R.drawable.ic_acknowledged);
        	SummaryTextView.setTypeface(Typeface.DEFAULT);
        	((TextView) convertView.findViewById(R.id.dateTime)).setTypeface(Typeface.DEFAULT);
        }
        else
        {
        	SummaryTextView.setTypeface(Typeface.DEFAULT_BOLD);
        	((TextView) convertView.findViewById(R.id.dateTime)).setTypeface(Typeface.DEFAULT_BOLD);
        	AckImage.setImageResource(R.drawable.ic_unacknowledged);
        }
        
        
        if(Event.getProgress())
        {
        	((ProgressBar) convertView.findViewById(R.id.inProgressBar)).setVisibility(0);
            AckImage.setVisibility(View.INVISIBLE);
        }
        else
        {
        	((ProgressBar) convertView.findViewById(R.id.inProgressBar)).setVisibility(4);
            AckImage.setVisibility(View.VISIBLE);
        }
        
        if(null == Event.getownerID() || Event.getownerID().equals("") || Event.getownerID().equals("null") )
        {
        	try
        	{
        		((TextView) convertView.findViewById(R.id.ackAuthor)).setText("Not Ack'd");
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
                    ((TextView) convertView.findViewById(R.id.ackAuthor)).setText("Not Ack'd");
                }
                else
                {
        		    ((TextView) convertView.findViewById(R.id.ackAuthor)).setText("Ack'd " + owner);
                }
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        }
        
        try
        {
            String count = "";
            if(Event.getCount() > 9999)
            {
                count = "-";
            }
            else
            {
                count = Integer.toString(Event.getCount());
            }

        	((TextView) convertView.findViewById(R.id.EventCount)).setText("Count: " + count);
        }
    	catch(Exception e)
    	{
    		
    	}
        
        try
        {
        	((TextView) convertView.findViewById(R.id.prodState)).setText(Event.getProdState());
        }
    	catch(Exception e)
    	{
    		
    	}
        
        
        ((ToggleButton) convertView.findViewById(R.id.cabSelect)).setTag(R.integer.EventPositionInList,position);
        ((ToggleButton) convertView.findViewById(R.id.cabSelect)).setChecked(Event.isSelected());
        
        //TODO this needs some serious tidying up (like not cheating on R.integer.*
        //convertView.setTag(Event.getEVID());
        convertView.setTag(R.integer.EventID,Event.getEVID());
        convertView.setTag(R.integer.EventPositionInList,position);
        
        
        convertView.setClickable(true);
        
        if(listener != null)
        	convertView.setOnClickListener((OnClickListener) listener);
        
        if(listenerLong != null)
        	convertView.setOnLongClickListener((OnLongClickListener) listenerLong);
        
        if(cablistener != null)
        	((ToggleButton) convertView.findViewById(R.id.cabSelect)).setOnClickListener(cablistener);
        
        return convertView;
	}
}
