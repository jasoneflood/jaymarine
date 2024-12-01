package database.thejasonengine.com;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;

public class DatabaseSetterTemplates {

	
	private static final Logger LOGGER = LogManager.getLogger(DatabaseSetterTemplates.class);
	public SqlTemplate<JsonObject, SqlResult<Void>> setUpDatabaseTables;
	
	
	public DatabaseSetterTemplates(JDBCClient client)
	{
		setUpDatabaseTables = SqlTemplate
				.forUpdate((SqlClient) client, "drop table if exists tb_users; create table tb_users(emp_id varchar(255) PRIMARY KEY, emp_name varchar(255), dept varchar(255), duration varchar(255));")
		      	.mapFrom(TupleMapper.jsonObject());
		LOGGER.debug("Created Query setUpDatabaseTables");
	}
}