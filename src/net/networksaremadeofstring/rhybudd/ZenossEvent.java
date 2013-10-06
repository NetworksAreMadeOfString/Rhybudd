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

import android.content.Intent;
import android.os.Bundle;
import org.json.JSONObject;

public class ZenossEvent 
{
	private String evid = "01234567890ABCDEF";
	private int Count = 0;
	private String lastTime = "1983-09-20 01:00:00";
	private String device = "unknown";
	private String summary = "unknown";
	private String eventState = "unacknowledged";
	private String firstTime = "1983-09-20 01:00:00";
	private String severity = "debug";
	private Boolean inProgress = false;
	private Boolean Selected = false;
	private String prodState = "Production";
	private String ownerID = "";
	private String component_text = "unknown";
	private String component_uid = "unknown";
	private String eventClass = "unknown";
	private boolean fragmentDisplay = false;


    public ZenossEvent(String _evid, String _summary)
    {
        this.evid = _evid;
        this.summary = _summary;
    }
	 // Constructor for the Ticket class
    public ZenossEvent(String _evid, String _device, String _summary, String _eventState, String _severity, String _prodState) 
    {
            super();
            this.evid = _evid;
            this.device = _device;
            this.summary = _summary;
            this.eventState = _eventState;
            this.severity = _severity;
            this.prodState = _prodState;
    }

    public ZenossEvent(Intent intent)
    {
        this.Count = Integer.getInteger(intent.getExtras().getString("count"));
        this.evid = intent.getExtras().getString("evid");
        this.device = intent.getExtras().getString("device");
        this.summary = intent.getExtras().getString("summary");
        this.severity = intent.getExtras().getString("severity");
        this.eventClass = intent.getExtras().getString("event_class");
        //String event_class_key = intent.getExtras().getString("event_class_key");
        //String sent = intent.getExtras().getString("sent");
    }

    public ZenossEvent(Bundle extras)
    {
        if(null != extras.getString("count"))
            this.Count = Integer.getInteger(extras.getString("count"));

        this.evid = extras.getString("evid");
        this.device = extras.getString("device");
        this.summary = extras.getString("summary");
        this.severity = extras.getString("severity");
        this.eventClass = extras.getString("event_class");
        //String event_class_key = intent.getExtras().getString("event_class_key");
        //String sent = intent.getExtras().getString("sent");
    }

    public ZenossEvent(JSONObject Event) 
    {
            super();
            try
            {
            	this.evid = Event.getString("evid");
            }
            catch(Exception e)
            {
            	this.evid = "Unknown EVID";
            }
            
            try
            {
            	this.device = Event.getJSONObject("device").getString("text");
            }
            catch(Exception e)
            {
            	this.device = "Unknown Device";
            }
            
            try
            {
            	this.summary = Event.getString("summary");
            }
            catch(Exception e)
            {
            	this.summary = "No Summary found.";
            }
            
            try
            {
            	this.eventState = Event.getString("eventState");
            }
            catch(Exception e)
            {
            	this.eventState = "New";
            }
            
            try
            {
            	this.severity = Event.getString("severity");
            }
            catch(Exception e)
            {
            	this.severity = "5";
            }
            
            try
            {
            	this.prodState = Event.getString("prodState");
            }
            catch(Exception e)
            {
            	this.prodState = "Unknown";
            }

            try
            {
                this.Count = Event.getInt("count");
            }
            catch(Exception e)
            {
                this.Count = 0;
            }

            try
            {
                this.firstTime = Event.getString("firstTime");
            }
            catch(Exception e)
            {
                this.firstTime = "Unknown";
            }

            try
            {
                this.component_text = Event.getJSONObject("component").getString("text");
            }
            catch(Exception e)
            {
                this.component_text = "Unknown";
            }

            try
            {
                this.component_uid = Event.getJSONObject("component").getString("uid");
            }
            catch(Exception e)
            {
                this.component_uid = "unknown";
            }

            try
            {
                this.eventClass = Event.getJSONObject("eventClass").getString("text");
            }
            catch(Exception e)
            {
                this.eventClass = "unknown";
            }

            try
            {
                this.lastTime = Event.getString("lastTime");
            }
            catch(Exception e)
            {
                this.lastTime = "unknown";
            }

            try
            {
                this.ownerID = Event.getString("ownerid");
            }
            catch(Exception e)
            {
                this.ownerID = "unknown";
            }
    }
    
    // Constructor for the Ticket class
    public ZenossEvent(String _evid, String _device, String _summary, String _eventState, String _severity, String _LastTime, String _prodState) 
    {
            super();
            this.evid = _evid;
            this.device = _device;
            this.summary = _summary;
            this.eventState = _eventState;
            this.severity = _severity;
            this.lastTime = _LastTime;
            this.prodState = _prodState;
    }
    
    //Ultimate constructor of ultimate destiny
    public ZenossEvent(String evid, int count, String prodState, String firstTime, String severity, String component_text, String component_uid,
    					String summary, String eventState,  String device, String eventClass, String lastTime, String ownerid) 
    {
	    super();
	    this.evid = evid;
	    this.Count = count;
	    this.prodState = prodState;
	    this.firstTime = firstTime;
	    this.severity = severity;
	    this.component_text = component_text;
	    this.component_uid = component_uid;
	    this.summary = summary;
	    this.eventState = eventState;
	    this.device = device;
	    this.eventClass = eventClass;
	    this.lastTime = lastTime;
	    this.ownerID = ownerid;
    }
    
    public void SetSelected(Boolean _selected)
    {
    	this.Selected = _selected;
    }
    
    public boolean isSelected()
    {
    	return this.Selected;
    }
	
    public void setFragmentDisplay(boolean fragmentDisplayed)
    {
    	this.fragmentDisplay = fragmentDisplayed;
    }
    
    public boolean isFragmentDisplayed()
    {
    	return this.fragmentDisplay;
    }
    
	public String getComponentText()
    {
    	return this.component_text;
    }
	public void setComponentText(String CompText)
    {
    	this.component_text = CompText;
    }
	
	public String getComponentUID()
    {
    	return this.component_uid;
    }
	public void setComponentUID(String CompUID)
    {
    	this.component_uid = CompUID;
    }
	
	public String geteventClass()
    {
    	return this.eventClass;
    }
	public void seteventClass(String eventClass)
    {
    	this.eventClass = eventClass;
    }
	
    public String getProdState()
    {
    	return this.prodState;
    }
    
    public Boolean isNew()
    {
    	return this.eventState.equals("New");	
    }
    
    public String getEVID()
    {
    	return this.evid;
    }
    
    public int getCount()
    {
    	return this.Count;
    }
    
    public Boolean getProgress()
    {
    	return this.inProgress;
    }
    
    public void setProgress(Boolean _inProgress)
    {
    	this.inProgress = _inProgress;
    }
    
    public void setAcknowledged()
    {
        this.inProgress = false;
    	this.eventState = "Acknowledged";
    }
    
    public String getlastTime()
    {
    	return this.lastTime;
    }
    
    public String getownerID()
    {
    	return this.ownerID;
    }
    
    public void setownerID(String OwnerID)
    {
    	this.ownerID = OwnerID;
    }
    
    public String getDevice()
    {
    	return this.device;
    }
    
    public String getSummary()
    {
    	return this.summary;
    }
    
    public String getEventState()
    {
    	return this.eventState;
    }
    
    public String getSeverity()
    {
    	return this.severity;
    }
    
    public String getfirstTime()
    {
    	return this.firstTime;
    }
}
