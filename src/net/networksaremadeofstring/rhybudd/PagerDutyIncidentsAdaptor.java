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
import android.widget.TextView;

public class PagerDutyIncidentsAdaptor extends BaseAdapter implements OnClickListener, OnLongClickListener
{
	private Context context;
    private List<PagerDutyIncident> listPagerDutyIncidents;
	
    public PagerDutyIncidentsAdaptor(Context context, List<PagerDutyIncident> _listPagerDutyIncidents) 
    {
        this.context = context;
        this.listPagerDutyIncidents = _listPagerDutyIncidents;
    }
    
	@Override
	public int getCount() {
		 return listPagerDutyIncidents.size();
	}

	@Override
	public Object getItem(int position) {
		return listPagerDutyIncidents.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		PagerDutyIncident Incident = listPagerDutyIncidents.get(position);
        
		if (convertView == null) 
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.zenoss_event_listitem, null);
        }
        TextView DeviceNameTextView = (TextView) convertView.findViewById(R.id.DeviceName);
        DeviceNameTextView.setText("PagerDuty Incident");
        
        
        TextView SummaryTextView = (TextView) convertView.findViewById(R.id.EventSummary);
        SummaryTextView.setText(Incident.gettrigger_summary_data());
        
        ImageView SeverityImage = (ImageView) convertView.findViewById(R.id.SeverityImageView);
        ImageView AckImage = (ImageView) convertView.findViewById(R.id.AckImage);
        
        SeverityImage.setImageResource(R.drawable.severity_critical);
        
        if(Incident.getStatus().equals("resolved") || Incident.getStatus().equals("acknowledged"))
        	AckImage.setImageResource(R.drawable.ack);
        
        convertView.setTag(Incident.getIncidentKey());
        convertView.setOnClickListener(this);
        convertView.setOnLongClickListener(this);
        
        
        return convertView;
	}
	@Override
	public boolean onLongClick(View v) {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public void onClick(View v) {
		((RhestrPagerDuty)context).AcknowledgeIncident(v.getTag().toString(), v.getId());
	}
}
