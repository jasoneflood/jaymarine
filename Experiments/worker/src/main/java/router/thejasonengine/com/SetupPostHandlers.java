package router.thejasonengine.com;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import authentication.thejasonengine.com.AuthUtils;
import database.thejasonengine.com.DatabaseController;
import io.vertx.core.Context;
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
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import io.vertx.core.http.HttpHeaders;

import session.thejasonengine.com.SetupSession;

public class SetupPostHandlers 
{
	private static final Logger LOGGER = LogManager.getLogger(SetupPostHandlers.class);
	
	
	public Handler<RoutingContext> simpleTest; 
	public Handler<RoutingContext> simpleDBTest;
	public Handler<RoutingContext> validateCredentials;
	public Handler<RoutingContext> createCookie;
	public Handler<RoutingContext> createSession;
	public Handler<RoutingContext> validateUserStatus;
	public Handler<RoutingContext> webLogin;
	public Handler<RoutingContext> addDatabaseQuery;
	public Handler<RoutingContext> getDatabaseQueryByDbType;
		
	public SetupPostHandlers(Vertx vertx)
    {
		
		simpleTest = SetupPostHandlers.this::handleSimpleTest;
		simpleDBTest = SetupPostHandlers.this::handleSimpleDBTest;
		validateCredentials = SetupPostHandlers.this::handleValidateCredentials;
		createCookie = SetupPostHandlers.this::handleCreateCookie;
		createSession = SetupPostHandlers.this::handleCreateSession;
		validateUserStatus =  SetupPostHandlers.this::handleValidateUserStatus;
		webLogin = SetupPostHandlers.this::handleWebLogin;
		addDatabaseQuery = SetupPostHandlers.this::handleAddDatabaseQuery;
		getDatabaseQueryByDbType = SetupPostHandlers.this::handleGetDatabaseQueryByDbType;
	}
	/****************************************************************/
	/*	
	 	Accessed via get route: /api/simpleTest
		- No Payload / Parameter - 
	*/
	/****************************************************************/
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
	/****************************************************************/
	/*	
	 	Accessed via get route: /api/simpleDBTest
	 	- No Payload / Parameter -
	 */
	/****************************************************************/
	private void handleSimpleDBTest(RoutingContext routingContext) 
	{
		
		LOGGER.info("Inside SetupPostHandlers.handleSimpleDBTest");  
		
		Context context = routingContext.vertx().getOrCreateContext();
		Pool pool = context.get("pool");
		
		if (pool == null)
		{
			LOGGER.debug("pull is null - restarting");
			DatabaseController DB = new DatabaseController(routingContext.vertx());
			LOGGER.debug("Taking the refreshed context pool object");
			pool = context.get("pool");
		}
		
		HttpServerResponse response = routingContext.response();
		JsonObject loginPayloadJSON = routingContext.getBodyAsJson();
		
		response
        .putHeader("content-type", "application/json");
		
		pool.getConnection(ar -> {
            if (ar.succeeded()) {
                SqlConnection connection = ar.result();
                
                JsonArray ja = new JsonArray();
                
                // Execute a SELECT query
                connection.query("SELECT * FROM public.tb_user")
                        .execute(res -> {
                            if (res.succeeded()) 
                            {
                                // Process the query result
                                RowSet<Row> rows = res.result();
                                rows.forEach(row -> {
                                    // Print out each row
                                    LOGGER.info("Row: " + row.toJson());
                                    try
                                    {
                                    	JsonObject jo = new JsonObject(row.toJson().encode());
                                    	ja.add(jo);
                                    	LOGGER.info("Successfully added json object to array");
                                    }
                                    catch(Exception e)
                                    {
                                    	LOGGER.error("Unable to add JSON Object to array: " + e.toString());
                                    }
                                    
                                });
                                response.send(ja.encodePrettily());
                            } 
                            else 
                            {
                                // Handle query failure
                            	LOGGER.error("error: " + res.cause() );
                            	response.send(res.cause().getMessage());
                                //res.cause().printStackTrace();
                            }
                            // Close the connection
                            //response.end();
                            connection.close();
                        });
            } else {
                // Handle connection failure
                ar.cause().printStackTrace();
                response.send(ar.cause().getMessage());
            }
            
        });
	}
	/****************************************************************/
	/*	
	 	Accessed via get route: /api/addDatabaseQuery
	 	{
	 		"jwt":"",
	 		"query_db_type":"postgres",
	 		"db_connection_string":""
	 		"query_string":"",
	 		"query_type":""
	 	}
	*/
	/****************************************************************/
	private void handleAddDatabaseQuery(RoutingContext routingContext) 
	{
		
		LOGGER.info("Inside SetupPostHandlers.handleAddDatabaseQuery");  
		
		Context context = routingContext.vertx().getOrCreateContext();
		Pool pool = context.get("pool");
		
		if (pool == null)
		{
			LOGGER.debug("pull is null - restarting");
			DatabaseController DB = new DatabaseController(routingContext.vertx());
			LOGGER.debug("Taking the refreshed context pool object");
			pool = context.get("pool");
		}
		
		HttpServerResponse response = routingContext.response();
		JsonObject JSONpayload = routingContext.getBodyAsJson();
		
		if (JSONpayload.getString("jwt") == null) 
	    {
	    	LOGGER.info("handleAddDatabaseQuery required fields not detected (jwt)");
	    	routingContext.fail(400);
	    } 
		else
		{
			if(validateJWTToken(JSONpayload))
			{
				LOGGER.info("jwt: " + JSONpayload.getString("jwt") );
				String [] chunks = JSONpayload.getString("jwt").split("\\.");
				
				JsonObject payload = new JsonObject(decode(chunks[1]));
				LOGGER.info("Payload: " + payload );
				int authlevel  = payload.getInteger("authlevel");
				String query = JSONpayload.getString("query_string");
				
				LOGGER.debug("Query recieved: " + query);
				
				utils.thejasonengine.com.Encodings Encodings = new utils.thejasonengine.com.Encodings();
				
				String encoded_query = Encodings.EscapeString(query);
				LOGGER.debug("Query recieved: " + query);
				LOGGER.debug("Query encoded: " + encoded_query);
				
				//The map is passed to the SQL query
				Map<String,Object> map = new HashMap<String, Object>();
				
				
				map.put("query_db_type", JSONpayload.getValue("query_db_type"));
				map.put("encoded_query", encoded_query);
				map.put("db_connection_string", JSONpayload.getValue("db_connection_string"));
				
				LOGGER.info("Accessible Level is : " + authlevel);
		        LOGGER.info("username: " + map.get("username"));
		        
		        if(authlevel >= 1)
		        {
		        	LOGGER.debug("User allowed to execute the API");
		        	response
			        .putHeader("content-type", "application/json");
					
					pool.getConnection(ar -> 
					{
			            if (ar.succeeded()) 
			            {
			                SqlConnection connection = ar.result();
			                JsonArray ja = new JsonArray();
			                
			                // Execute a SELECT query
			                
			                connection.preparedQuery("Insert into public.tb_query(query_db_type, query_string, query_type) VALUES($1,$2,$3);")
			                        .execute(Tuple.of(1, map.get("encoded_query"), map.get("db_connection_string")),
			                        res -> {
			                            if (res.succeeded()) 
			                            {
			                                // Process the query result
			                                RowSet<Row> rows = res.result();
			                                rows.forEach(row -> {
			                                    // Print out each row
			                                    LOGGER.info("Row: " + row.toJson());
			                                    try
			                                    {
			                                    	JsonObject jo = new JsonObject(row.toJson().encode());
			                                    	ja.add(jo);
			                                    	LOGGER.info("Successfully added json object to array");
			                                    }
			                                    catch(Exception e)
			                                    {
			                                    	LOGGER.error("Unable to add JSON Object to array: " + e.toString());
			                                    }
			                                    
			                                });
			                                response.send(ja.encodePrettily());
			                            } 
			                            else 
			                            {
			                                // Handle query failure
			                            	LOGGER.error("error: " + res.cause() );
			                            	response.send(res.cause().getMessage());
			                                //res.cause().printStackTrace();
			                            }
			                            // Close the connection
			                            //response.end();
			                            connection.close();
			                        });
			            } else {
			                // Handle connection failure
			                ar.cause().printStackTrace();
			                response.send(ar.cause().getMessage());
			            }
			            
			        });
		        }
		        else
		        {
		        	JsonArray ja = new JsonArray();
		        	JsonObject jo = new JsonObject();
		        	jo.put("Error", "Issufficent authentication level to run API");
		        	ja.add(jo);
		        	response.send(ja.encodePrettily());
		        }
		        
		        
			}
		}
	}
	/****************************************************************/
	private void handleGetDatabaseQueryByDbType(RoutingContext routingContext) 
	{
		
		LOGGER.info("Inside SetupPostHandlers.handleGetDatabaseQueryByDbType");  
		
		Context context = routingContext.vertx().getOrCreateContext();
		Pool pool = context.get("pool");
		
		if (pool == null)
		{
			LOGGER.debug("pull is null - restarting");
			DatabaseController DB = new DatabaseController(routingContext.vertx());
			LOGGER.debug("Taking the refreshed context pool object");
			pool = context.get("pool");
		}
		
		HttpServerResponse response = routingContext.response();
		JsonObject JSONpayload = routingContext.getBodyAsJson();
		
		if (JSONpayload.getString("jwt") == null) 
	    {
	    	LOGGER.info("handleAddDatabaseQuery required fields not detected (jwt)");
	    	routingContext.fail(400);
	    } 
		else
		{
			if(validateJWTToken(JSONpayload))
			{
				LOGGER.info("jwt: " + JSONpayload.getString("jwt") );
				String [] chunks = JSONpayload.getString("jwt").split("\\.");
				
				JsonObject payload = new JsonObject(decode(chunks[1]));
				LOGGER.info("Payload: " + payload );
				int authlevel  = payload.getInteger("authlevel");
				String query_db_type = JSONpayload.getString("query_db_type");
				
				LOGGER.debug("Query recieved: " + query_db_type);
				
				utils.thejasonengine.com.Encodings Encodings = new utils.thejasonengine.com.Encodings();
				
				//The map is passed to the SQL query
				Map<String,Object> map = new HashMap<String, Object>();
				
				map.put("query_db_type", query_db_type);
				
				LOGGER.info("Accessible Level is : " + authlevel);
		       
				if(authlevel >= 1)
		        {
		        	LOGGER.debug("User allowed to execute the API");
		        	response
			        .putHeader("content-type", "application/json");
					
					pool.getConnection(ar -> 
					{
			            if (ar.succeeded()) 
			            {
			                SqlConnection connection = ar.result();
			                JsonArray ja = new JsonArray();
			                
			                // Execute a SELECT query
			                
			                connection.preparedQuery("select * from public.tb_query where query_db_type = $1;")
			                        .execute(Tuple.of(Integer.parseInt(map.get("query_db_type").toString())),
			                        res -> {
			                            if (res.succeeded()) 
			                            {
			                                // Process the query result
			                                RowSet<Row> rows = res.result();
			                                rows.forEach(row -> {
			                                    // Print out each row
			                                    LOGGER.info("Row: " + row.toJson());
			                                    try
			                                    {
			                                    	JsonObject jo = new JsonObject(row.toJson().encode());
			                                    	ja.add(jo);
			                                    	LOGGER.info("Successfully added json object to array");
			                                    }
			                                    catch(Exception e)
			                                    {
			                                    	LOGGER.error("Unable to add JSON Object to array: " + e.toString());
			                                    }
			                                    
			                                });
			                                response.send(ja.encodePrettily());
			                            } 
			                            else 
			                            {
			                                // Handle query failure
			                            	LOGGER.error("error: " + res.cause() );
			                            	response.send(res.cause().getMessage());
			                                //res.cause().printStackTrace();
			                            }
			                            // Close the connection
			                            //response.end();
			                            connection.close();
			                        });
			            } else {
			                // Handle connection failure
			                ar.cause().printStackTrace();
			                response.send(ar.cause().getMessage());
			            }
			            
			        });
		        }
		        else
		        {
		        	JsonArray ja = new JsonArray();
		        	JsonObject jo = new JsonObject();
		        	jo.put("Error", "Issufficent authentication level to run API");
		        	ja.add(jo);
		        	response.send(ja.encodePrettily());
		        }
		        
		        
			}
		}
	}
	/****************************************************************/
	/****************************************************************/
	/*	
	 	Accessed via post route: /api/login
	 	handleRegisterUser takes in a POST JSON Body Payload as:
	 	{
     		"username":"theUsername",
     		"password":"thePassword"
 		}
	 */
	/****************************************************************/
	private void handleValidateCredentials(RoutingContext routingContext) 
	{
		
		LOGGER.info("Inside SetupPostHandlers.handleValidateCredentials");  
		HttpServerResponse response = routingContext.response();
		JsonObject loginPayloadJSON = routingContext.getBodyAsJson();
		LOGGER.info(loginPayloadJSON);
		try 
		  { 
			//Make sure the context has the parameter that is expected.
				if (loginPayloadJSON.getString("username") == null || loginPayloadJSON.getString("password") == null) 
			    {
			    	LOGGER.info("Login ( username or password ) required fields not detected" + ", at IP:" + routingContext.request().remoteAddress());
			    	routingContext.fail(400);
			    } 
			    else 
			    {
			    	//JsonObject temp = new JsonObject();
			    	boolean verified = true;
			    		
			    	
			    		
			    	Map<String,Object> map = new HashMap<String, Object>();  
				    map.put("username", loginPayloadJSON.getValue("username"));
				    map.put("password", hashAndSaltPass(loginPayloadJSON.getValue("password").toString()));
				   
				    LOGGER.info("Setting user session for username: " + loginPayloadJSON.getValue("username"));
				    LOGGER.info("salty password: " + map.get("password"));
				   
				    /*****************************************************************************/
				    Context context = routingContext.vertx().getOrCreateContext();
					Pool pool = context.get("pool");
					
					if (pool == null)
					{
						LOGGER.debug("pull is null - restarting");
						DatabaseController DB = new DatabaseController(routingContext.vertx());
						LOGGER.debug("Taking the refreshed context pool object");
						pool = context.get("pool");
					}
					response
			        .putHeader("content-type", "application/json");
					
					pool.getConnection(ar -> {
			            if (ar.succeeded()) {
			                SqlConnection connection = ar.result();
			                
			                JsonArray ja = new JsonArray();
			                
			                // Execute a SELECT query
			                connection.preparedQuery("select * FROM function_login($1,$2)")
	                        .execute(Tuple.of(map.get("username"), map.get("password")), 
	                        		res -> {
	                        			if (res.succeeded()) 
			                            {
			                                // Process the query result
			                            	
			                                RowSet<Row> rows = res.result();
			                                
			                                rows.forEach(row -> 
			                                {
			                                	JsonObject jo = new JsonObject(row.toJson().encode());
			                                	ja.add(jo);
			                                	LOGGER.debug("Found user: " + ja.encodePrettily());
			                                });
			                                
			                                LOGGER.debug("Result size: " + ja.size());
			                                
			                                if(ja.size() > 0)
			                                {
			                                	LOGGER.debug("Found user: " + ja.encodePrettily());
			                                	
			                                	JsonObject dbObject = ja.getJsonObject(0);
			                                	
			                                	JWTAuth jwt;
			        						    // Set up the authentication tokens 
			        						    String name = "JWT";
			        						    
			        						    AuthUtils AU = new AuthUtils();
			        						    jwt = AU.createJWTToken(context);
			        						        	
			        						    JsonObject tokenObject = new JsonObject();
			        						        	
			        						    tokenObject.put("id", dbObject.getValue("id"));
			        						    tokenObject.put("firstname", dbObject.getValue("firstname"));
			        						    tokenObject.put("surname", dbObject.getValue("surname"));
			        						    tokenObject.put("email", dbObject.getValue("email"));
			        						    tokenObject.put("username", dbObject.getValue("username"));
			        						    tokenObject.put("active", dbObject.getValue("active"));
			        						    tokenObject.put("authlevel", dbObject.getValue("authlevel"));
			        						    		
			        						    String token = jwt.generateToken(tokenObject, new JWTOptions().setExpiresInSeconds(60));
			        						    LOGGER.info("JWT TOKEN: " + token);
			        						        	
			        						    response
			        					        .putHeader("content-type", "application/json")
			        					        .end("{\"result\":\"ok\", \"jwt\": \""+token+"\"}");
			                                	
			                                	
			                                }
			                                else
			                                {
			                                	 response
			     						        .putHeader("content-type", "application/json")
			     						        .end("{\"result\":\"fail\", \"jwt\": \"Invalid credentials\"}");
			                                }
			                            } 
			                            else 
			                            {
			                                // Handle query failure
			                            	LOGGER.error("error: " + res.cause() );
			                            	response
			     						    .putHeader("content-type", "application/json")
			     						    .end("{\"result\":\"fail\", \"jwt\": \"Invalid credentials\"}");
			                                //res.cause().printStackTrace();
			                            }
			                            // Close the connection
			                            //response.end();
			                            connection.close();
			                        });
			            } else {
			                // Handle connection failure
			                ar.cause().printStackTrace();
			                response
 						    .putHeader("content-type", "application/json")
 						    .end("{\"result\":\"fail\", \"jwt\": \"Invalid credentials\"}");
			            }
			            
			        });
				   
				}
		}
		catch(Exception e)
		{
		  		LOGGER.info("ERROR: " + e.toString());
		  		response
				    .putHeader("content-type", "application/json")
				    .end("{\"result\":\"fail\", \"jwt\": \"Invalid credentials\"}");
		}
	}
	/****************************************************************/
	/*	
	 	Accessed via route: /api/createCookie
	 	handleRegisterUser takes in a POST JSON Body Payload as:
	 	{
     		"jwt":"theJWT"
 		}
	 */
	private void handleCreateCookie(RoutingContext routingContext) 
	{
		LOGGER.info("Inside SetupPostHandlers.handleCreateCookie");  
		HttpServerResponse response = routingContext.response();
		try
		{
		  String CookiePayload = new String(routingContext.getBodyAsString().getBytes("ISO-8859-1"), "UTF-8");
		  LOGGER.info("Cookie Payload:" + CookiePayload);
		  JsonObject createCookieJSON = routingContext.getBodyAsJson();
		  LOGGER.info(createCookieJSON);
		  
		  if (createCookieJSON.getString("jwt") == null) 
		  {
		    	LOGGER.info("Create Cookie required fields (jwt) not detected" + ", at IP:" + routingContext.request().remoteAddress());
		    	routingContext.fail(400);
		  } 
		  else
		  {
			  String token = createCookieJSON.getString("jwt");
			  
			  
			  
			  Vertx vertx = routingContext.vertx();					        	
	      	  Context context = vertx.getOrCreateContext();
	      	  
	      	  AuthUtils au = new AuthUtils();
	      	  
	      	  /*****************************************************************/
	      	  /* We need to validate the JWT token before we add it as a cookie
	      	  /*****************************************************************/
	      	  
	      	  Cookie cookie  = au.createCookie(routingContext, 600, "JWT", token, "/");
    		
	      	  response
	      	  .addCookie(cookie)
	      	  .setChunked(true)
	      	  .putHeader("content-type", "application/json")
	      	  .end("{\"result\":\"OK\", \"reason\": \"Cookie created\"}");
	      }	 
		}
		catch(Exception e)
		{
			LOGGER.error("Unable to get body of post as string: " + e.getMessage() + ", at IP:" + routingContext.request().remoteAddress());
			response
            .putHeader("content-type", "application/json")
            .end("{\"result\":\"Fail\", \"reason\": \""+e.toString()+"\"}");
		}
	
	}
	/****************************************************************/
	/*	
	 	Accessed via route: /api/createSession
	 	handleRegisterUser takes in a POST JSON Body Payload as:
	 	{
     		"jwt":"theJWT"
 		}
	 */
	private void handleCreateSession(RoutingContext routingContext) 
	{
		LOGGER.info("Inside SetupPostHandlers.handleCreateSession");  
		HttpServerResponse response = routingContext.response();
		try
		{
		  String SessionPayload = new String(routingContext.getBodyAsString().getBytes("ISO-8859-1"), "UTF-8");
		  LOGGER.info("Session Payload:" + SessionPayload);
		  
		  JsonObject createSessionJSON = routingContext.getBodyAsJson();
		  LOGGER.info(createSessionJSON);
		  
		  if (createSessionJSON.getString("jwt") == null) 
		  {
		    	LOGGER.info("CreateSession required fields (jwt) not detected" + ", at IP:" + routingContext.request().remoteAddress());
		    	routingContext.fail(400);
		  } 
		  else
		  {
			  String token = createSessionJSON.getString("jwt");
			  Vertx vertx = routingContext.vertx();					        	
	      	  Context context = vertx.getOrCreateContext();
			  SetupSession setupSession = new SetupSession(vertx);
	      	  setupSession.putTokenInSession(routingContext, "jwt", token);
	      	  LOGGER.info("Session created");
	      	  response
	      	  .putHeader("content-type", "application/json")
	      	  .end("{\"result\":\"OK\", \"reason\": \"Session Added To Context\"}");
	      }	  
		  
		}
		catch(Exception e)
		{
			LOGGER.error("Unable to get body of post as string: " + e.getMessage());
			response
            .putHeader("content-type", "application/json")
            .end("{\"result\":\"Fail\", \"reason\": \""+e.toString()+"\"}");
		}
	
	}
	/****************************************************************/
	/*	
	 	Accessed via route: /api/login
	 	handleRegisterUser takes in a POST JSON Body Payload as:
	 	{
     		"username":"theUsername"
 		}
	 */
	private void handleWebLogin(RoutingContext routingContext) 
	{
		LOGGER.info("Inside SetupPostHandlers.handleWebLogin");  
		HttpServerResponse response = routingContext.response();
		try
		{
		  String webLoginPayload = new String(routingContext.getBodyAsString().getBytes("ISO-8859-1"), "UTF-8");
		  LOGGER.info("WebLoginPayload Payload:" + webLoginPayload);
		}
		catch(Exception e)
		{
			LOGGER.error("Unable to get body of webLoginPayload post as string: " + e.getMessage());
		}
		JsonObject loginPayloadJSON = routingContext.getBodyAsJson();
		LOGGER.info(loginPayloadJSON);
		
		try 
		  { 
			//Make sure the context has the parameter that is expected.
				if (loginPayloadJSON.getString("username") == null) 
			    {
			    	LOGGER.info("webLoginPayload required fields not detected (username)");
			    	routingContext.fail(400);
			    } 
			    else 
			    {
			    		//JsonObject temp = new JsonObject();
			    		boolean verified = true;
			    		LOGGER.info("Starting login prep for username: " + loginPayloadJSON.getValue("username") );
			    		
			    		Map<String,Object> map = new HashMap<String, Object>();  
				    	map.put("username", loginPayloadJSON.getValue("username"));
				    	map.put("password", hashAndSaltPass(loginPayloadJSON.getValue("password").toString()));
				    	
				    	LOGGER.debug("map username: " + map.get("username"));
				    	LOGGER.debug("map password: " + map.get("password"));
				    	/*****************************************************************************/
					    Context context = routingContext.vertx().getOrCreateContext();
						Pool pool = context.get("pool");
						
						if (pool == null)
						{
							LOGGER.debug("pool is null - restarting");
							DatabaseController DB = new DatabaseController(routingContext.vertx());
							LOGGER.debug("Taking the refreshed context pool object");
							pool = context.get("pool");
						}
						
						pool.getConnection(ar -> 
						{
				            if (ar.succeeded()) 
				            {
				                SqlConnection connection = ar.result();
				                JsonArray ja = new JsonArray();
				                
				                // Execute a SELECT query
				                connection.preparedQuery("select * FROM function_login($1,$2)")
		                        .execute(Tuple.of(map.get("username"), map.get("password")), 
		                        	res -> 
		                        		{
		                        			if (res.succeeded()) 
				                            {
				                                // Process the query result
				                            	
				                                RowSet<Row> rows = res.result();
				                                
				                                rows.forEach(row -> 
				                                {
				                                	JsonObject jo = new JsonObject(row.toJson().encode());
				                                	ja.add(jo);
				                                	LOGGER.debug("Found user: " + ja.encodePrettily());
				                                });
				                                
				                                LOGGER.debug("Result size: " + ja.size());
				                                
				                                if(ja.size() > 0)
				                                {
				                                	LOGGER.debug("Found user: " + ja.encodePrettily());
				                                	JsonObject dbObject = ja.getJsonObject(0);
				                                	
				                                	
				                                	LOGGER.info("Successfully ran query: webLogin");
										        	 

										        	Vertx vertx = routingContext.vertx();					        	
										        	FreeMarkerTemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);
				                                	
				                                	
										        	JWTAuth jwt;
										        	// Set up the authentication tokens 
										        	String name = "JWT";
										        	AuthUtils AU = new AuthUtils();
										        	jwt = AU.createJWTToken(context);
										        	
										        	JsonObject tokenObject = new JsonObject();
										        	
										        	// We would need to tweak these values to determine the authorization rights for the user
										        	
										    		tokenObject.put("username", dbObject.getValue("username"));
										    		tokenObject.put("authlevel", dbObject.getValue("authlevel"));
										    		
										    		String token = jwt.generateToken(tokenObject, new JWTOptions().setExpiresInSeconds(60));
										    		LOGGER.info("JWT TOKEN CREATED AT WEB LOGIN: " + token + ", From IP:" + routingContext.request().remoteAddress());
					
										    		tokenObject.put("jwt", token);
										        	
										    		//Not going to use session variables
										    		
										    		//SetupSession setupSession = new SetupSession(vertx);
										        	//LOGGER.info("created the session object");
										        	//setupSession.putTokenInSession(routingContext, "token", token);
										        	//setupSession.putTokenInSession(routingContext, "tokenObject", tokenObject.toString());
										    		
										    		// Now we add the cookie values to the system such that they can be used for future manipulation
										    		AuthUtils au = new AuthUtils();
										    		
										    		Cookie cookie  = au.createCookie(routingContext, 600, name, token, "/");
										    		
										    		response.addCookie(cookie);
										    		response.setChunked(true);
										    		response.putHeader("Authorization", dbObject.getValue("authlevel").toString());
										    		response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
										    		//response.putHeader("content-type", "application/json");
										    		response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET");
										    		//response.send("{\"redirect\":\"loggedin\\dashboard.ftl\"}");
										    		
										    		
										    		engine.render(tokenObject, "templates/loggedIn/dashboard.ftl", 
											   			     resy -> 
											   				 {
											   					
											   					 if (resy.succeeded()) 
											   		             {
											   						String renderedContent = resy.result().toString();
											   						if (renderedContent.isEmpty()) 
											   						{
											   				            LOGGER.error("Rendered content is empty!");
											   				            
											   				        }
											   						LOGGER.info(renderedContent);
											   						routingContext.response().end(renderedContent);
											   		            	LOGGER.debug("Successfully sent template");
											   		             } 
											   		             else 
											   		             {
											   		            	routingContext.fail(resy.cause());
											   		            	LOGGER.error("Unable to send template : " + resy.cause().getMessage());
											   		             }
											   				 });
										         }
				                              }
				                              else
				                              {
				                                	LOGGER.error("*Potential security violation* Signin error for username: " + map.get("username") + ", at IP:" + routingContext.request().remoteAddress());
									        		response.sendFile("index.html");
				                              }
		                        		});
				            }
						});
			    }	
				           
			    
		  }
		  catch(Exception e)
		  {
			  LOGGER.info("ERROR on weblogin: " + e.toString());
			  response.sendFile("index.html");
		  }
	}
	/** **********************************************************/
	/* This function is called repeatidly to validate that the user
	 * has access to the site.
	 */
	/*************************************************************/ 
	private void handleValidateUserStatus(RoutingContext routingContext) 
	{
		LOGGER.info("Inside SetupPostHandlers.handleValidateUserStatus");  
		
		
		/*****************************************************************************/
	    
		Context context = routingContext.vertx().getOrCreateContext();
		Pool pool = context.get("pool");
		
		if (pool == null)
		{
			LOGGER.debug("pull is null - restarting");
			DatabaseController DB = new DatabaseController(routingContext.vertx());
			LOGGER.debug("Taking the refreshed context pool object");
			pool = context.get("pool");
		}
		
		/*****************************************************************************/
		HttpServerResponse response = routingContext.response();
		JsonObject JSONpayload = routingContext.getBodyAsJson();
		LOGGER.info(JSONpayload);
		if (JSONpayload.getString("jwt") == null) 
	    {
	    	LOGGER.info("handleValidateUserStatus required fields not detected (jwt)");
	    	routingContext.fail(400);
	    } 
		else
		{
			if(validateJWTToken(JSONpayload))
			{
				LOGGER.info("jwt: " + JSONpayload.getString("jwt") );
				String [] chunks = JSONpayload.getString("jwt").split("\\.");
				JsonObject payload = new JsonObject(decode(chunks[1]));
				LOGGER.info("Payload: " + payload );
				int authlevel  = payload.getInteger("authlevel");
				
				//The map is passed to the SQL query
				Map<String,Object> map = new HashMap<String, Object>();
				map.put("username", payload.getValue("username"));
				LOGGER.info("Accessible Level is : " + authlevel);
		        LOGGER.info("username: " + map.get("username"));
			   
			    pool.getConnection(ar -> {
		            if (ar.succeeded()) 
		            {
		                SqlConnection connection = ar.result();
		                
		                JsonArray ja = new JsonArray();
		                
		                // Execute a SELECT query
		                connection.preparedQuery("select username, active from tb_user where username = $1")
                        .execute(Tuple.of(map.get("username")), 
                        res -> 
                        {
                        			if (res.succeeded()) 
		                            {
                        				boolean active = true;
                        				RowSet<Row> rows = res.result();
			                            rows.forEach(row -> 
			                            {
			                               JsonObject jo = new JsonObject(row.toJson().encode());
			                               ja.add(jo);
			                               LOGGER.debug("Found user: " + ja.encodePrettily());
			                            });
			                            LOGGER.debug("Result size: " + ja.size());
			                            if(ja.size() > 0)
			                            {
			                            	LOGGER.debug("Found user: " + ja.encodePrettily());
			                                JsonObject dbObject = ja.getJsonObject(0);
			                                if(dbObject.getString("active").compareToIgnoreCase("inactive") == 0)
				    	        			{
				    	        				LOGGER.error("**Potential security violation* STATUS ERROR**, From IP:" + routingContext.request().remoteAddress() + " for username: " + dbObject.getString("username"));
				    	        				active = false;
				    	        			}
			                            }
			                            LOGGER.info("Successfully ran query: handleValidateUserStatus");
			        					if(!active)
			        					{
			        						response
			        			        	.putHeader("content-type", "application/json")
			        			        	.end("{\"result\":\"Fail\", \"reason\": \"inactive\"}");
			        			    	}
			        					else
			        					{
			        						response
			        			        	.putHeader("content-type", "application/json")
			        			        	.end("{\"result\":\"ok\", \"reason\": \"ok\"}");
			        					} 
		                            }
                        			else
                        			{
                        				response
                        				.putHeader("content-type", "application/json")
                        				.end("{\"result\":\"Fail\", \"reason\": \"invalid authorization token\"}"); 
                        			}
                        })
                        ;
		            }
		            else 
		            {
		            	LOGGER.error("Unable to validate user status: " + ar.cause().getMessage());
		            	response
        				.putHeader("content-type", "application/json")
        				.end("{\"result\":\"Fail\", \"reason\": \"invalid authorization token\"}"); 
		            }
				});
			}
			else
			{
				LOGGER.error("Invalid or no JWT token passed in payload : " + JSONpayload.encodePrettily());
				response
				.putHeader("content-type", "application/json")
				.end("{\"result\":\"Fail\", \"reason\": \"invalid authorization token\"}"); 
			}
		} 
	}
	/**
	 * @return **********************************************************/
	/*
	/* This function validates and returns true if JWT token validates
	/*
	/************************************************************/
	public boolean validateJWTToken(JsonObject JSONpayload)
	{
		boolean result = false;
		
		
		LOGGER.info("jwt: " + JSONpayload.getString("jwt") );
		
		String [] chunks = JSONpayload.getString("jwt").split("\\.");
		if(chunks.length > 2)
		{
			JsonObject header = new JsonObject(decode(chunks[0]));
			JsonObject payload = new JsonObject(decode(chunks[1]));
			
			LOGGER.info("Basic JWT structure test has passed with a payload of: " + payload );
			LOGGER.info("String to be base64Url Encoded: " + payload.encode());
			LOGGER.info("base64UrlEncoder: " + base64UrlEncoder(payload.encode()));
			LOGGER.debug("Basic JWT structure test has passed with a header of: " + header);
			
			result = true;
			//Now validate that the user account is still active
			
		}
		else
		{
			result = false;
		}
		
		if(result == true)
		{
			//next validate the signature
			
			String headerPlusPayload = chunks[0] + "." + chunks[1];
			LOGGER.debug("headerPlusPayload: " + headerPlusPayload);
			
			String signature = chunks[2];
			LOGGER.debug("signature: " + signature);
		
			try
			{
				String generateSignature = hmacSha256(chunks[0].toString() + "." + chunks[1].toString(), "keyboard cat");
				LOGGER.debug("Generated signature: " + generateSignature);
				
				if(signature.compareTo(generateSignature)== 0)
				{
					LOGGER.debug("*********** JWT Signature match ***************");
				}
				else
				{
					LOGGER.error("**Potential security violation* JWT SIGNATURE ERROR**");
					result = false;
				}
			}
			catch(Exception e)
			{
				LOGGER.error("Uable to perform signature match of JWT Token: " + e.toString());
				result = false;
			}
		}
		return result;
	}
	/*********************************************************************************/
	public String base64UrlEncoder(String originalInput)
	{
		String encodedString = Base64.getEncoder().encodeToString(originalInput.getBytes());
		return encodedString;
	}
	/*********************************************************************************/
	public String base64UrlDecoder(String encodedString)
	{
		byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
		String decodedString = new String(decodedBytes);
		return decodedString;
	}
	/*********************************************************************************/
	private static String encode(byte[] bytes) 
	{
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
	/*********************************************************************************/
	private static String decode(String encodedString) {
	    return new String(Base64.getUrlDecoder().decode(encodedString));
	}
	/*********************************************************************************/
	private String hmacSha256(String data, String secret) {
	    try {

	        byte[] hash = secret.getBytes(StandardCharsets.UTF_8);
	        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
	        SecretKeySpec secretKey = new SecretKeySpec(hash, "HmacSHA256");
	        sha256Hmac.init(secretKey);

	        byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

	        return encode(signedBytes);
	    } 
	    catch (Exception e) 
	    {
	        LOGGER.error("Unable to encode: " + e.toString());
	        return null;
	    }
	}
	/**
	 * Method to hash and salt password before use
	 * @param inputPass
	 * @returns actual password in db for match
	 * 
	 */
	public static String hashAndSaltPass (String inputPass)
	{
		String salt = "Rasputin";
		//hash the input password for later comparison with password in db
		MessageDigest md = null;
		try 
		{
			md = MessageDigest.getInstance("SHA-256");
		} 
		catch (NoSuchAlgorithmException e1) 
		{
			LOGGER.error("SHA-256 Not Found: " + e1.toString());
		}
		String text = inputPass+salt;
		try 
		{
			md.update(text.getBytes("UTF-8"));
		} 
		catch (UnsupportedEncodingException e) 
		{
			LOGGER.error("Could not convert Hash to Encoding: " + e.toString());
		} 
		// Change this to "UTF-16" if needed
		byte[] digest = md.digest();
	
		//convert the byte to hex format method 1
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < digest.length; i++) 
        {
          sb.append(Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1));
        }
        
        return sb.toString();
	}
}
	
	
	

