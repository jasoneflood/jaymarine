package database.thejasonengine.com;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnection;

public class DatabaseController 
{
	private static final Logger LOGGER = LogManager.getLogger(DatabaseController.class);
	
	
	
	public DatabaseController(Vertx vertx) 
	{
		 // PostgreSQL connection options
		PgConnectOptions connectOptions = new PgConnectOptions()
			      .setHost("localhost")
			      .setPort(5432)
			      .setDatabase("SLP")
			      .setUser("postgres")
			      .setPassword("postgres");

		// Create a connection pool (this uses the Pool interface from SqlClient)
        PoolOptions poolOptions = new PoolOptions().setMaxSize(10); // Max pool size
        LOGGER.debug("Set pool options");
        Pool pool = Pool.pool(vertx, connectOptions, poolOptions);
        
        LOGGER.debug("Pool Created");
        
        Context context = vertx.getOrCreateContext();
        context.put("pool", pool);
        LOGGER.debug("Pool added to context");
        
        
        LOGGER.info("JDBC Pool SET");
        

    }

}
