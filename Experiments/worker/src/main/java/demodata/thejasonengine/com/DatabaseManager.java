package demodata.thejasonengine.com;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Context;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;


public class DatabaseManager {
	private static final Logger LOGGER = LogManager.getLogger(DatabaseManager.class);
	public JDBCPool client;

	
	
	private void createDBConnectionPoolArray(Context context)
	{
		  JsonArray ja = context.get("ConnectionData");
		  
		  JsonArray connectionPool = new JsonArray();
		  
		  for (int i = 0; i < ja.size(); i ++)
		  {
			  JsonObject jo = ja.getJsonObject(i);
	          String dpName = jo.getString("db_type")+"_"+jo.getString("db_url")+"_"+jo.getString("db_database")+"_"+jo.getString("db_username"); 	
	          JsonObject connection = new JsonObject();
	          
	          connection.put("id", dpName);
	          connection.put("type", jo.getString("db_type"));
	          
	          connectionPool.add(connection);
		  }
		  context.put("connectionPoolArray", connectionPool);
		 	
	}
	/**************************************************************/
	/* READ Connection data as a FILE
	/**************************************************************/
	public static JsonArray readConnectionsJSONDataFromFile(String filename)
	{
		
		String path = "./" + filename;
		JsonArray ja = new JsonArray();
		try
		{
			String result = readFile(path, StandardCharsets.UTF_8);
		    ja = new JsonArray(result);
		    LOGGER.debug(ja.encodePrettily());
		}
		catch(Exception e)
		{
			LOGGER.error("Error reading :" + filename + ", reason : " + e.toString());
		}
		return ja;
	}
	/***************************************************/
	static String readFile(String path, Charset encoding)throws IOException
	{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
	}
	/**************************************************************/
	/* READ Connection data as a database
	/**************************************************************/
	public static JsonArray readConnectionsJSONDataFromDB()
	{
		
		JsonArray ja = new JsonArray();
		try
		{
		
		}
		catch(Exception e)
		{
			LOGGER.error("Error reading DB" + " , reason : " + e.toString());
		}
		return ja;
	}
	
}
