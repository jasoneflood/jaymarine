package memory.thejasonengine.com;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;

public class Ram extends AbstractVerticle 
{
	
	private static final Logger LOGGER = LogManager.getLogger(Ram.class);
	private static LocalMap<String, String> ramSharedMap;
	private static HashMap<String, BasicDataSource> dataSourceMap;
	
	
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
	public HashMap<String, BasicDataSource> getDBPM()
	{
		return Ram.dataSourceMap;
	}
	public void setDBPM(HashMap<String, BasicDataSource> dataSourceMap)
	{
		Ram.dataSourceMap = dataSourceMap;
		LOGGER.info("Have set the RAM dataSourceMap");
	}
	/*********************************************************************/
}