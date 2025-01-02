package demodata.thejasonengine.com;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import memory.thejasonengine.com.Ram;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;

public class DatabasePoolManager 
{

	private static final Logger LOGGER = LogManager.getLogger(DatabasePoolManager.class);
	
	public DatabasePoolManager(Context context) 
    {
        Ram ram = new Ram();
        HashMap<String, BasicDataSource> dataSourceMap = ram.getDBPM();
        if(dataSourceMap == null)
        {
        	dataSourceMap = new HashMap<String, BasicDataSource>();
        }
        try
		{
			JsonArray ja = context.get("ConnectionData");
			LOGGER.info("Number of context served ConnectionData elements: " + ja.size()); /*This will error if there is non found*/
			for (int i = 0; i < ja.size(); i ++)
			{
				JsonObject jo = ja.getJsonObject(i);
                BasicDataSource DataSource = new BasicDataSource();
		        LOGGER.debug("Creating Database pool for: jdbc:"+jo.getString("db_type")+"://"+jo.getString("db_url")+":"+jo.getString("db_port")+"/"+jo.getString("db_database")+ " using: " + jo.getString("db_username"));
		
		        DataSource.setUrl("jdbc:"+jo.getString("db_type")+"://"+jo.getString("db_url")+":"+jo.getString("db_port")+"/"+jo.getString("db_database"));
		    	DataSource.setUsername(jo.getString("db_username"));
		    	DataSource.setPassword(jo.getString("db_password"));
		    	DataSource.setDriverClassName(jo.getString("db_jdbcClassName"));
		    	DataSource.setInitialSize(5);
		    	DataSource.setMaxTotal(10);
		    	DataSource.setMinIdle(2);
		    	DataSource.setMaxIdle(5);
		    	DataSource.setMaxWaitMillis(10000);
		    	
		    	String dpName = jo.getString("db_type")+"_"+jo.getString("db_url")+"_"+jo.getString("db_database")+"_"+jo.getString("db_username"); 	
		
		    	dataSourceMap.put(dpName, DataSource);
		        LOGGER.debug("Database pool created  "+ dpName +"  and added to the dataSourceMap");
			}
			LOGGER.debug("All Database Pool Objects created");
			ram.setDBPM(dataSourceMap);
		}
        catch(Exception e)
        {
        	LOGGER.error("Unable to load connects from connections file: " + e.toString());
        }
    	
    }
	
	
	public DatabasePoolManager(JsonArray ja) 
    {
		Ram ram = new Ram();
		HashMap<String, BasicDataSource> dataSourceMap = ram.getDBPM();
		if(dataSourceMap == null)
		{
			LOGGER.debug("datasource map has not been initialized");
			dataSourceMap = new HashMap<>();
		}
		
        for (int i = 0; i < ja.size(); i ++)
		{
        	BasicDataSource DataSource = new BasicDataSource();
        	JsonObject jo = ja.getJsonObject(i);
        	LOGGER.debug("Creating Database pool for: jdbc:"+jo.getString("db_type")+"://"+jo.getString("db_url")+":"+jo.getString("db_port")+"/"+jo.getString("db_database")+ " using: " + jo.getString("db_username"));
            
        	DataSource.setUrl("jdbc:"+jo.getString("db_type")+"://"+jo.getString("db_url")+":"+jo.getString("db_port")+"/"+jo.getString("db_database"));
	    	DataSource.setUsername(jo.getString("db_username"));
	    	DataSource.setPassword(jo.getString("db_password"));
	    	DataSource.setDriverClassName(jo.getString("db_jdbcClassName"));
	    	DataSource.setInitialSize(5);
	    	DataSource.setMaxTotal(10);
	    	DataSource.setMinIdle(2);
	    	DataSource.setMaxIdle(5);
	    	DataSource.setMaxWaitMillis(10000);
    	
	    	String dpName = jo.getString("db_type")+"_"+jo.getString("db_url")+"_"+jo.getString("db_database")+"_"+jo.getString("db_username"); 	
	    	dataSourceMap.put(dpName, DataSource);
	    	ram.setDBPM(dataSourceMap);
	    	//dataSourceMap = ram.getDBPM();
	    	LOGGER.debug("Number of datasource elements: "+dataSourceMap.size());
	    	LOGGER.debug("Database pool created  "+ dpName +"  and added to the dataSourceMap");
		}
    	
    }
 	public Connection getConnectionFromPool(String dpName) throws SQLException 
    {
		Ram ram = new Ram();
		HashMap<String, BasicDataSource> dataSourceMap = ram.getDBPM();
		if(dataSourceMap == null)
		{
			LOGGER.debug("datasource map has not been initialized");
			dataSourceMap = new HashMap<>();
		}
		BasicDataSource dataSource = dataSourceMap.get(dpName);
        if (dataSource != null) 
        {
            return dataSource.getConnection();
        }
        throw new SQLException("No connection pool found for the database: " + dpName);
    }

    public void closeAllPools() throws SQLException 
    {
    	Ram ram = new Ram();
		Map<String, BasicDataSource> dataSourceMap = ram.getDBPM();
    	if(dataSourceMap == null)
    	{
    		LOGGER.debug("dataSourceMap is null");
    	}
    	else
    	{
			for (BasicDataSource dataSource : dataSourceMap.values()) 
	        {
	            if (dataSource != null) 
	            {
	                dataSource.close();
	            }
	        }
    	}
    }
    public static void main(String[] args) 
    {
    	JsonObject jo = new JsonObject();
    	
    	jo.put("status", "option");
    	jo.put("db_type", "db2");
    	jo.put("db_version", "14.00");
    	jo.put("db_username", "db2user");
    	jo.put("db_password", "dublinzoo2024X12345");
    	jo.put("db_port", "50000");
    	jo.put("db_database", "demodb");
    	jo.put("db_url", "9.30.116.184");
    	jo.put("db_jdbcClassName", "com.ibm.db2.jcc.DB2Driver");
    	jo.put("user_icon", "colorfemale.png");
    	jo.put("triggers", "");
    	
    	String dpName = jo.getString("db_type")+"_"+jo.getString("db_url")+"_"+jo.getString("db_database")+"_"+jo.getString("db_username"); 	

    	JsonArray ja = new JsonArray();
    	
    	ja.add(jo);
    	
    	DatabasePoolManager multiDBPool = new DatabasePoolManager(ja);

        try 
        {
            Connection db1Connection = multiDBPool.getConnectionFromPool(dpName);
            LOGGER.debug("Connected to: " + dpName);

            // Perform operations with db1Connection
            // ...

            // Close the connection (returns it to the pool)
            db1Connection.close();

            multiDBPool.closeAllPools();

        } catch (SQLException e) 
        {
            LOGGER.error("Unable to connect to database: " + e);
        }
    }
    
    /*This function is called at the start up of the system to make the subsequent connection calls faster */
	public void initialzeConnectionPool(Context context)
	{
		LOGGER.debug("inside DBPMgetHandlers.initialzeConnectionPool");
		
		JsonArray ja = context.get("connectionPoolArray");
    	Ram ram = new Ram();
    	
		for (int i = 0; i < ja.size(); i ++)
		{
			JsonObject jo = ja.getJsonObject(i);
            String dpName = jo.getString("id");
            LOGGER.debug("dpName/id:" + dpName + " type:" + jo.getString("type"));
            try 
            {
            	HashMap<String, BasicDataSource> dataSourceMap = ram.getDBPM();
        		if(dataSourceMap == null)
        		{
        			LOGGER.debug("datasource map has not been initialized");
        		}
        		else
        		{
        			LOGGER.debug("datasource map has been initialized");
        			if(dataSourceMap.containsKey(dpName))
        			{
        				LOGGER.debug("Found datasource: " +dpName+ " for datasource map.");
        				
        				
        				context.executeBlocking(promise -> 
        				{
        				    try 
        				    {
        				    	Connection dbpConnection = dataSourceMap.get(dpName).getConnection();
                	            LOGGER.debug("added a connection to the pool: " + dpName);
        				    }
        				    catch(Exception e)
        				    {
        				    	LOGGER.error("Unable to set up pool connection: " + e.toString());
        				    }
        				}, 
        				result -> 
        				{
        					if (result.succeeded()) 
        					{
        						LOGGER.debug("Successfully created connection");
        					}
        				});
        			
        			}
        		}
            }
            catch(Exception e)
            {
            	LOGGER.error("Unable to set the database pool connections: " + e.toString());
            }
		}
	}
	
}