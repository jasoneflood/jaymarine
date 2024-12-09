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
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import session.thejasonengine.com.SetupSession;

public class SetupPostHandlers 
{
	private static final Logger LOGGER = LogManager.getLogger(SetupPostHandlers.class);
	
	
	public Handler<RoutingContext> simpleTest; 
	public Handler<RoutingContext> simpleDBTest;

		
	public SetupPostHandlers(Vertx vertx)
    {
		
		simpleTest = SetupPostHandlers.this::handleSimpleTest;
		simpleDBTest = SetupPostHandlers.this::handleSimpleDBTest;
		
		
		/*
		setupSystemDatabase = SetupPostHandlers.this::handleSetupSystemDatabase;
		validateCredentials = SetupPostHandlers.this::handleValidateCredentials;
		createCookie = SetupPostHandlers.this::handleCreateCookie;
		createSession = SetupPostHandlers.this::handleCreateSession;
		validateUserStatus =  SetupPostHandlers.this::handleValidateUserStatus;
		webLogin = SetupPostHandlers.this::handleWebLogin;
		setupSystemDatabase = SetupPostHandlers.this::handleSetupSystemDatabase;*/
		//DST = v_DST;
		
	}
	private static String decode(String encodedString) 
	{
	    return new String(Base64.getUrlDecoder().decode(encodedString));
	}
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
	 	Accessed via route: /api/login
	 	handleRegisterUser takes in a POST JSON Body Payload as:
	 	{
     		"username":"theUsername"
     		"password":"thePassword"
 		}
	 */
	private void handleSimpleDBTest(RoutingContext routingContext) 
	{
		
		LOGGER.info("Inside SetupPostHandlers.handleSimpleDBTest");  
		
		Context context = routingContext.vertx().getOrCreateContext();
		
		
	/*PgConnectOptions connectOptions = new PgConnectOptions()
			      .setHost("localhost")
			      .setPort(5432)
			      .setDatabase("SLP")
			      .setUser("postgres")
			      .setPassword("postgres");

		// Create a connection pool (this uses the Pool interface from SqlClient)
      PoolOptions poolOptions = new PoolOptions().setMaxSize(10); // Max pool size
      LOGGER.debug("Set pool options");
      Pool pool = Pool.pool(routingContext.vertx(), connectOptions, poolOptions);
	
		*/
		Pool pool = context.get("pool");
		
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
                                    response.send(row.toJson().encodePrettily());
                                });
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
	
	/****************************************************************/
	/*	
	 	Accessed via route: /api/login
	 	handleRegisterUser takes in a POST JSON Body Payload as:
	 	{
     		"username":"theUsername"
     		"password":"thePassword"
 		}
	 */
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
			    		
			    	LOGGER.info("Setting user session for username: " + loginPayloadJSON.getValue("username"));
			    		
			    	Map<String,Object> map = new HashMap<String, Object>();  
				    map.put("username", loginPayloadJSON.getValue("username"));
				    map.put("password", hashAndSaltPass(loginPayloadJSON.getValue("password").toString()));
				   	/*
			    	DST.login
					    .execute(map)
					    .onSuccess(result -> 
					    {
					    	Object obj = null; //We will use this to Create a JSON object of all the data we will use to drive a session.
					        Iterator itor = result.iterator(); 
					        while(itor.hasNext())                  // checks if there is an element to be visited.
					        {
					        	obj = itor.next(); //There should only be one.
					        	LOGGER.debug("Recieved: " + obj);
					        }
					    if(obj == null)
					    {
					    	 response
						        .putHeader("content-type", "application/json")
						        .end("{\"result\":\"fail\", \"jwt\": \"Invalid credentials\"}");
					    }
					    else
					    {
					    	JsonObject dbObject = new JsonObject(obj.toString());
						    LOGGER.info("Successfully ran query: login");
						        	
						    // Now we rediscover the context 
						    Vertx vertx = routingContext.vertx();					        	
						    Context context = vertx.getOrCreateContext();
						        	
						    FreeMarkerTemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);
					    	    	
						    JWTAuth jwt;
						    // Set up the authentication tokens 
						    String name = "JWT";
						    
						    AuthenticationUtils AU = new AuthenticationUtils();
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
						     
						    
				    }).onFailure(err -> 
				    {
				   		LOGGER.info("Unable to get connect: " + err + ", at IP:" + routingContext.request().remoteAddress());
				   		response
				             .putHeader("content-type", "application/json")
				             .end("{\"result\":\"Fail\", \"reason\": \""+err.toString()+"\"}"); 
				      		//routingContext.fail(500);
				    });
				    */
				}
		}
		catch(Exception e)
		{
		  		LOGGER.info("ERROR: " + e.toString());
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
			    		LOGGER.info("Starting login prep for username: " + loginPayloadJSON.getValue("username"));
			    		
			    		Map<String,Object> map = new HashMap<String, Object>();  
				    	map.put("username", loginPayloadJSON.getValue("username"));
				    	map.put("password", hashAndSaltPass(loginPayloadJSON.getValue("password").toString()));
				    	
			    		/*
			    		DST.login
					    	.execute(map)
					        .onSuccess(result -> 
					        {
					        	
					        	 Object obj = null; //We will use this to Create a JSON object of all the data we will use to drive a session.
					        	 Iterator itor = result.iterator(); 
					        	 int resultSet = 0;
					        	 while(itor.hasNext())                  // checks if there is an element to be visited.
					        	 {
					        		  obj = itor.next(); //There should only be one.
					        		  resultSet = resultSet+1;
					        		  LOGGER.debug("Recieved: " + obj);
					        	 }
					        	if(resultSet == 0)
					        	{
					        		LOGGER.error("*Potential security violation* Signin error for username: " + map.get("username") + ", at IP:" + routingContext.request().remoteAddress());
					        		response.sendFile("index.html");
					        	}
					        	else
					        	{
					        		JsonObject dbObject = new JsonObject(obj.toString());
						        	
							        LOGGER.info("Successfully ran query: webLogin");
						        	
						        	// Now we rediscover the context 
						        	Vertx vertx = routingContext.vertx();					        	
						        	Context context = vertx.getOrCreateContext();
						        	
						        	FreeMarkerTemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);
					    	    	 
						        	JWTAuth jwt;
						        	// Set up the authentication tokens 
						        	String name = "JWT";
						        	AuthenticationUtils AU = new AuthenticationUtils();
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
						    		AuthenticationUtils au = new AuthenticationUtils();
						    		
						    		Cookie cookie  = au.createCookie(routingContext, 600, name, token, "/");
						    		
						    		
						    		
						    		
						    		response.addCookie(cookie);
						    		response.setChunked(true);
						    		response.putHeader("Authorization", dbObject.getValue("authlevel").toString());
						    		response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
						    		//response.putHeader("content-type", "application/json");
						    		response.putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET");
						    		//response.send("{\"redirect\":\"loggedin\\dashboard.ftl\"}");
						    		
						    		
						    		engine.render(tokenObject, "templates/loggedin/dashboard.ftl", 
							   			     res -> 
							   				 {
							   		             if (res.succeeded()) 
							   		             {
							   		            	routingContext.response().end(res.result());
							   		             } 
							   		             else 
							   		             {
							   		            	routingContext.fail(res.cause());
							   		             }
							   				 });
						         }
					        }
					        ).onFailure(err -> 
					      	{
					      		LOGGER.info("Unable to run database query webLogin: " + err);
					      		response
					              .putHeader("content-type", "application/json")
					              .end("{\"result\":\"Fail\", \"reason\": \""+err.toString()+"\"}"); 
					      		//routingContext.fail(500);
					      	}
					        );*/
			    		}
			    		
			    
		  }
		  catch(Exception e)
		  {
			  LOGGER.info("ERROR: " + e.toString());
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
		        		
		        /*DST.validateUserStatus
		        .execute(map)
		    	.onSuccess(result -> 
		    	{
		    		Object obj_inner = null; //We will use this to Create a JSON object of all the data we will use to drive a session.
		    	    Iterator itor_inner = result.iterator();
		    	    JsonArray body = new JsonArray();
		    	    boolean active = true;
		    	    while(itor_inner.hasNext())                  // checks if there is an element to be visited.
				    {
		    	        			obj_inner = itor_inner.next(); //There should only be one.
		    	        			JsonObject dbObject_inner = new JsonObject(obj_inner.toString());
		    	        			LOGGER.debug("Recieved (handleValidateUserStatus): " + obj_inner);
		    	        			if(dbObject_inner.getString("active").compareToIgnoreCase("inactive") == 0)
		    	        			{
		    	        				LOGGER.error("**Potential security violation* STATUS ERROR**, From IP:" + routingContext.request().remoteAddress() + " for username: " + dbObject_inner.getString("username"));
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
		    	}); */
		    }
			else
			{
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
	private static String hashAndSaltPass (String inputPass)
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
	
	
	

