package database.thejasonengine.com;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

public class DatabaseController 
{
	private static final Logger LOGGER = LogManager.getLogger(DatabaseController.class);
	
	public static JDBCClient createSystemDatabase(Vertx vertx)
    {
    	// Configuration for the SQLite database
        JsonObject config = new JsonObject();
        config.put("url", "jdbc:sqlite:test.db");
        config.put("driver_class", "org.sqlite.JDBC");
        config.put("max_pool_size", 30); // Pool size

        // Create a JDBC client
        JDBCClient client = JDBCClient.createShared(vertx, config);
        return client;
        
        
        // Perform a database operation (e.g., creating a table)
        
        /*client.getConnection(conn -> {
            if (conn.succeeded()) {
                SQLConnection connection = conn.result();

                // Create a simple table
                String createTableQuery = "CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name TEXT)";
                connection.execute(createTableQuery, createResult -> {
                    if (createResult.succeeded()) {
                        LOGGER.debug("Table created successfully.");
                    } else {
                        LOGGER.error("Error creating table: " + createResult.cause());
                    }

                    // Insert a sample row
                    String insertQuery = "INSERT INTO users (name) VALUES ('Jason Doe')";
                    connection.execute(insertQuery, insertResult -> {
                        if (insertResult.succeeded()) {
                            LOGGER.debug("Row inserted successfully.");

                            // Query the data
                            connection.query("SELECT * FROM users", queryResult -> {
                                if (queryResult.succeeded()) {
                                    queryResult.result().getRows().forEach(row -> {
                                        LOGGER.debug("User: " + row.getString("name"));
                                    });
                                } else {
                                    LOGGER.error("Query failed: " + queryResult.cause());
                                }

                                // Close the connection
                                connection.close();
                            });
                        } else {
                            LOGGER.error("Insert failed: " + insertResult.cause());
                        }
                    });
                });
            } else {
                LOGGER.error("Connection failed: " + conn.cause());
            }
        });*/
    }

}
