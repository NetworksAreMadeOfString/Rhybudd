package net.networksaremadeofstring.rhybudd;

import java.util.List;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;



public class ZenossEventsAdaptor extends BaseAdapter implements OnClickListener, OnLongClickListener 
{
	private Context context;
    private List<ZenossEvent> listZenossEvents;
	
    
    public ZenossEventsAdaptor(Context context, List<ZenossEvent> _listZenossEvents) 
    {
        this.context = context;
        this.listZenossEvents = _listZenossEvents;
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
        	AckImage.setImageResource(R.drawable.ack);
        
        convertView.setTag(Event.getEVID());
        convertView.setOnClickListener(this);
        convertView.setOnLongClickListener(this);
        return convertView;
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		Log.i("Click","Short Click");
	}
	
	public boolean onLongClick(View v)
	{
		//Log.i("LongClick","Long click: " + v.getTag());
		Boolean test = ((rhestr)context).AcknowledgeEvent(v.getTag().toString(), v.getId());
		Log.i("R",test.toString());
		return true;
	}

}
