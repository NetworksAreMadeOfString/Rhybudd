package net.networksaremadeofstring.rhybudd;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;


public class ViewZenossGroupsFragment extends Fragment
{
    GridView gridview = null;
    Handler handler;
    String[] groups;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,  Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.view_zenoss_groups_fragment, null);
        gridview = (GridView) view.findViewById(R.id.GroupsGrid);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                try
                {
                    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener()
                    {
                        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                            Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
                        }
                    });

                    gridview.setAdapter(new ZenossGroupsGridAdapter(getActivity(),groups));
                }
                catch(Exception e)
                {
                    Toast.makeText(getActivity(), "There was a problem getting the list of groups or rendering the grid", Toast.LENGTH_LONG).show();
                }
            }
        };

        ((Thread) new Thread(){
            public void run()
            {
                ZenossCredentials credentials = null;
                ZenossAPI API = null;

                if(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(ZenossAPI.PREFERENCE_IS_ZAAS, false))
                {
                    API = new ZenossAPIZaas();
                }
                else
                {
                    API = new ZenossAPICore();
                }

                try
                {
                    credentials = new ZenossCredentials(getActivity());
                    API.Login(credentials);

                    groups = API.GetGroups();

                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }

                handler.sendEmptyMessage(1);
            }
        }).start();
    }
}
