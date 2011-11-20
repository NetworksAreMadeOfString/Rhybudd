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

public class PagerDutyIncident 
{
	private String last_status_change_on;
	private int incident_number;
	private String assigned_to_user;
	private String trigger_summary_data;
	private String trigger_details_html_url;
	private String created_on;
	private String incident_key;
	private String status;
	
	public PagerDutyIncident(String _last_status_change_on, String _trigger_summary_data, String _status, String _incident_key)
	{
		this.last_status_change_on = _last_status_change_on;
		this.trigger_summary_data = _trigger_summary_data;
		this.status = _status;
		this.incident_key = _incident_key;
	}
	
	public String getlast_status_change_on()
    {
    	return this.last_status_change_on;
    }
	
	public String gettrigger_summary_data()
    {
    	return this.trigger_summary_data;
    }
	
	public int getIncidentNumber()
    {
    	return this.incident_number;
    }
    
    public String getTriggerDetailsURL()
    {
    	return this.trigger_details_html_url;
    }
    
    public String getCreatedDate()
    {
    	return this.created_on;
    }
	
	public String getStatus()
    {
    	return this.status;
    }
	
	public String getAssignedUser()
    {
    	return this.assigned_to_user;
    }
	
	public String getIncidentKey()
    {
    	return this.incident_key;
    }
}
