/*  Notification [Common Notification]
*
*	Author(s): Jason Flood/John Clarke
*  	Licence: Apache 2
*  
*   
*/


package com.thejasonengine.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.ext.web.Router;
import io.vertx.sqlclient.Pool;

import com.thejasonengine.memory.Ram;

public class Ram extends AbstractVerticle 
{
	
	private static final Logger LOGGER = LogManager.getLogger(Ram.class);
	private static LocalMap<String, String> ramSharedMap;
	private static JsonObject systemConfig;
	private static Pool pool;
	
	/*******************************************************************************/
	public Ram()
	{
		
	}
	public void initializeSharedMap(Vertx vertx) 
	{
	    SharedData sharedData = vertx.sharedData();
	    ramSharedMap = sharedData.getLocalMap("ram-map");
	}
	public LocalMap<String, String> getRamSharedMap()
	{
		LOGGER.info("Have retrieved the RAM LocalMap");
		return Ram.ramSharedMap;
	}
	public void setRamSharedMap(LocalMap<String, String> ramSharedMap)
	{
		ramSharedMap = Ram.ramSharedMap;
		LOGGER.info("Have set the RAM ramSharedMap");
	}
	/*********************************************************************/
	public JsonObject getSystemConfig()
	{
		return Ram.systemConfig;
	}
	public void setSystemConfig(JsonObject systemConfig)
	{
		Ram.systemConfig = systemConfig;
		LOGGER.info("Have set the RAM systemConfig");
	}
	/*********************************************************************/
	public Pool getPostGresSystemPool()
	{
		return Ram.pool;
	}
	public void setPostGresSystemPool(Pool pool)
	{
		Ram.pool = pool;
		LOGGER.info("Have set the RAM PostGresSystemPool");
	}
}