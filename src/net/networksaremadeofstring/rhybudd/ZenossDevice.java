package net.networksaremadeofstring.rhybudd;

import java.util.HashMap;
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
public class ZenossDevice 
{
	private String productionState;
	private int ipAddress;
	private HashMap<String, Integer> events;
	private String name;
	private String uid;
	
	public ZenossDevice(String _productionState, int _ipAddress, HashMap<String, Integer> _events, String _name, String _uid)
	{
		 super();
         this.productionState = _productionState;
         this.ipAddress = _ipAddress;
         this.events = _events;
         this.name = _name;
         this.uid = _uid;
	}
	
	public String getproductionState()
    {
    	return this.productionState;
    }
	
	public int getipAddress()
    {
    	return this.ipAddress;
    }
	
	public HashMap<String, Integer> getevents()
    {
    	return this.events;
    }
	
	public String getname()
    {
    	return this.name;
    }
	
	public String getuid()
    {
    	return this.uid;
    }
}
