/*  Notification [Common Notification]
*
*	Author(s): Jason Flood/John Clarke
*  	Licence: Apache 2
*  
*   
*/

package com.thejasonengine.router;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.vertx.core.MultiMap;
import io.vertx.core.Promise;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import io.vertx.ext.web.FileUpload;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;

import com.thejasonengine.database.DatabaseController;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.http.HttpHeaders;


public class SetupPostHandlers 
{
	private static final Logger LOGGER = LogManager.getLogger(SetupPostHandlers.class);
	
	public Handler<RoutingContext> simpleTest; 
	
	
	public SetupPostHandlers(Vertx vertx)
    {
		simpleTest = SetupPostHandlers.this::handleSimpleTest;
	}
	/***********************************************************************/
	private void handleSimpleTest(RoutingContext routingContext)
	{
	
		JsonObject PayloadJSON = new JsonObject();
		PayloadJSON.put("username", "myusername");
		PayloadJSON.put("password", "mypassword");
		
		LOGGER.info("Inside SetupPostHandlers.handleSimpleTest");
		HttpServerResponse response = routingContext.response();
		try 
		{ 
			
		}
		catch(Exception e)
		{
			LOGGER.error("Unable to complete simple test: " + e.toString());
		}
	}
	public Future<Void> validateSystemPool(Pool pool, String method)
	{
		LOGGER.debug("Validating system pool from method: " + method);
		Promise<Void> promise = Promise.promise();
	    pool.query("SELECT 1").execute(ar -> 
	    {
	      if (ar.succeeded()) 
	      {
	    	  promise.complete();
	        
	      } 
	      else 
	      {
	        promise.fail("Connection pool validation failed: " + ar.cause().getMessage());
	      }
	      
	    });

	    return promise.future();
	}
}
	
	

