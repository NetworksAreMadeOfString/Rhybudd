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

public class ZenossEvent {
	private String evid;
	private int Count;
	private String lastTime;
	private String device;
	private String summary;
	private String eventState;
	private String firstTime;
	private String severity;
	private Boolean inProgress = false;
	
	
	 // Constructor for the Ticket class
    public ZenossEvent(String _evid, String _device, String _summary, String _eventState, String _severity) 
    {
            super();
            this.evid = _evid;
            this.device = _device;
            this.summary = _summary;
            this.eventState = _eventState;
            this.severity = _severity;
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
    	this.eventState = "Acknowledged";
    }
    
    public String getlastTime()
    {
    	return this.lastTime;
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
