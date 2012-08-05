package net.networksaremadeofstring.rhybudd;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class ViewEventFragment extends SherlockFragment
{
	int postionInList = 0;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		postionInList = getArguments().getInt("Position");
        return inflater.inflate(R.layout.view_zenoss_event_fragment, container, false);
    }
    
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//setRetainInstance(true);
	}
	
	//Do stuff now we're loaded
    public void onActivityCreated(Bundle savedInstanceState)
    {
    	super.onActivityCreated(savedInstanceState);
    
    	((TextView) getView().findViewById(R.id.EventTitle)).setText(getArguments().getString("Device"));
    	((TextView) getView().findViewById(R.id.Componant)).setText(getArguments().getString("Component"));
    	((TextView) getView().findViewById(R.id.EventClass)).setText(getArguments().getString("EventClass"));
		((TextView) getView().findViewById(R.id.Summary)).setText(getArguments().getString("Summary"));
		((TextView) getView().findViewById(R.id.FirstTime)).setText(getArguments().getString("FirstSeen"));
		((TextView) getView().findViewById(R.id.LastTime)).setText(getArguments().getString("LastSeen"));
		((TextView) getView().findViewById(R.id.EventCount)).setText(Integer.toString(getArguments().getInt("EventCount")));

		((Button) getView().findViewById(R.id.AcknowledgeEvent)).setOnClickListener(new OnClickListener() 
    	{
			public void onClick(View v) 
			{
				((RhybuddHome) getActivity()).AcknowledgeSingleEvent(postionInList);
			}
		});
    	/*getArguments().getString("Device");
    	getArguments().getString("Component");
    	getArguments().getString("EventClass");
    	getArguments().getString("Summary");
    	getArguments().getString("FirstSeen");
    	getArguments().getString("LastSeen");
    	getArguments().getString("EventCount");*/
    }
}
