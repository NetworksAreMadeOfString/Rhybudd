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

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ZenossSearchAdaptor extends BaseAdapter implements OnClickListener, OnLongClickListener 
{
	private Context context;
    private List<ZenossDevice> listZenossDevices;
	
    
    public ZenossSearchAdaptor(Context context, List<ZenossDevice> _listZenossDevices) 
    {
        this.context = context;
        this.listZenossDevices = _listZenossDevices;
    }
    
	@Override
	public int getCount()
    {
        if(null != listZenossDevices)
        {
		    return listZenossDevices.size();
        }
        else
        {
            return 0;
        }
	}

	@Override
	public Object getItem(int position)
    {
        if(null != listZenossDevices)
        {
            return listZenossDevices.get(position);
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
		ZenossDevice Device = listZenossDevices.get(position);
		
        if (convertView == null) 
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.zenoss_device_listitem, null);
        }
        
        TextView DeviceNameTextView = (TextView) convertView.findViewById(R.id.DeviceName);
        DeviceNameTextView.setText(Device.getname());
        
        TextView CriticalTextView = (TextView) convertView.findViewById(R.id.CriticalCount);
        CriticalTextView.setText(Device.getevents().get("critical").toString());
        
        TextView ErrorTextView = (TextView) convertView.findViewById(R.id.ErrorCount);
        ErrorTextView.setText(Device.getevents().get("error").toString());
        
        TextView WarningTextView = (TextView) convertView.findViewById(R.id.WarningCount);
        WarningTextView.setText(Device.getevents().get("warning").toString());
        
        //convertView.setTag(Event.getEVID());
        convertView.setOnClickListener(this);
        convertView.setOnLongClickListener(this);
        
        convertView.setTag(Device.getuid());
        
        return convertView;
	}

	@Override
	public boolean onLongClick(View v)
    {
        //We don't support long click for search items
		return false;
	}

	@Override
	public void onClick(View v) 
	{
		if(v.getTag() != null)
			((Search)context).ViewDevice(v.getTag().toString());
	}

}
