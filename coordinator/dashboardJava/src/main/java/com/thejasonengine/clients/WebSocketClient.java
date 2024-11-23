package com.thejasonengine.clients;

import java.security.Timestamp;

import io.vertx.core.http.ServerWebSocket;

public class WebSocketClient 
{
	
	String connectionId;
	ServerWebSocket wsConnection;
	String endpoint;
	String email;
	boolean active;
	String timestamp;
	
	/******************************************************/	
	public String getConnectionId() 
	{
		return connectionId;
	}
	public void setConnectionId(String connectionId) 
	{
		this.connectionId = connectionId;
	}
	/******************************************************/
	public ServerWebSocket getWsConnection() 
	{
		return wsConnection;
	}
	public void setWsConnection(ServerWebSocket wsConnection) 
	{
		this.wsConnection = wsConnection;
	}
	/******************************************************/
	public String getEndpoint() 
	{
		return endpoint;
	}
	public void setEndpoint(String endpoint) 
	{
		this.endpoint = endpoint;
	}
	/******************************************************/
	public String getEmail() 
	{
		return email;
	}
	public void setEmail(String email) 
	{
		this.email = email;
	}
	/******************************************************/
	public boolean isActive() 
	{
		return active;
	}
	public void setActive(boolean active) 
	{
		this.active = active;
	}
	/******************************************************/
	public String getTimestamp() 
	{
		return timestamp;
	}
	public void setTimestamp(String timestamp) 
	{
		this.timestamp = timestamp;
	}
	/******************************************************/
	
}
