/*
 * Copyright (C) 2012 - Gareth Llewellyn
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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class TrustAllManager implements X509TrustManager 
{
	public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException 
	{
		
	}

	public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException 
	{
		
	}

	public X509Certificate[] getAcceptedIssuers() 
	{
		return null;
	}
}