/*
* Copyright (C) 2011 - Gareth Llewellyn
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

import java.util.List;

import android.content.Context;
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
    private Boolean isRhestr = true;
    private OnClickListener listener;
    private OnLongClickListener listenerLong;
    private OnClickListener cablistener;
    
    
    public ZenossEventsAdaptor(Context context, List<ZenossEvent> _listZenossEvents, Boolean _isRhestr) 
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
	public int getCount() {
    	 return listZenossEvents.size();
	}
	@Override
	public Object getItem(int position) {
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
            convertView = inflater.inflate(R.layout.zenoss_event_listitem, null);
        }
        
        ((TextView) convertView.findViewById(R.id.dateTime)).setText(Event.getlastTime());
        
        TextView DeviceNameTextView = (TextView) convertView.findViewById(R.id.DeviceName);
        DeviceNameTextView.setText(Event.getDevice());
        
        
        TextView SummaryTextView = (TextView) convertView.findViewById(R.id.EventSummary);
        SummaryTextView.setText(Event.getSummary());
        
        ImageView SeverityImage = (ImageView) convertView.findViewById(R.id.SeverityImageView);
        ImageView AckImage = (ImageView) convertView.findViewById(R.id.AckImage);
        
        
        if(Event.getSeverity().equals("5"))
        	SeverityImage.setImageResource(R.drawable.severity_critical);
        
        if(Event.getSeverity().equals("4"))
        	SeverityImage.setImageResource(R.drawable.severity_error);
        
        if(Event.getSeverity().equals("3"))
        	SeverityImage.setImageResource(R.drawable.severity_warning);
        
        if(Event.getSeverity().equals("2"))
        	SeverityImage.setImageResource(R.drawable.severity_info);
        
        if(Event.getSeverity().equals("1"))
        	SeverityImage.setImageResource(R.drawable.severity_info); //Technically debug
        
        if(Event.getEventState().equals("Acknowledged"))
        {
        	AckImage.setImageResource(R.drawable.ack);
        }
        else
        {
        	AckImage.setImageResource(R.drawable.nack);
        }
        
        
        if(Event.getProgress())
        {
        	((ProgressBar) convertView.findViewById(R.id.inProgressBar)).setVisibility(0);
        }
        else
        {
        	((ProgressBar) convertView.findViewById(R.id.inProgressBar)).setVisibility(4);
        }
        
        ((ToggleButton) convertView.findViewById(R.id.cabSelect)).setTag(R.integer.EventPositionInList,position);
        ((ToggleButton) convertView.findViewById(R.id.cabSelect)).setChecked(Event.isSelected());
        
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
	
	/*@Override
	public void onClick(View v) 
	{
		((rhestr)context).AcknowledgeEvent(v.getTag(R.integer.EventID).toString(),(Integer) v.getTag(R.integer.EventPositionInList), v.getId());
	}*/
	
	/*public boolean onLongClick(View v)
	{
		((rhestr)context).ViewEvent(v.getTag(R.integer.EventID).toString());
		return true;
	}*/

}
