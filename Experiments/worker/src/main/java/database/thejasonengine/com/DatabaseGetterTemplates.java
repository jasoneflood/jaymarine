package database.thejasonengine.com;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.templates.SqlTemplate;

public class DatabaseGetterTemplates {

	
	private static final Logger LOGGER = LogManager.getLogger(DatabaseGetterTemplates.class);
	
	public SqlTemplate<Map<String, Object>, RowSet<JsonObject>> getUserDataByUsernameAndPassword;
	
	
	public DatabaseGetterTemplates(JDBCClient client)
	{
		getUserDataByUsernameAndPassword = SqlTemplate
				.forQuery((SqlClient) client, "SELECT * from tb_users where username = #{username}")
				.mapTo(Row::toJson);
		LOGGER.info("Created Query template getUserDataByUsernameAndPassword");
	}
}