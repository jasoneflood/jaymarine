package cluster.thejasonengine.com;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import authentication.thejasonengine.com.AuthUtils;
import database.thejasonengine.com.DatabaseController;
import file.thejasonengine.com.Read;
import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWT;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.sqlclient.Pool;
import memory.thejasonengine.com.Ram;
import router.thejasonengine.com.SetupPostHandlers;
import session.thejasonengine.com.SetupSession;
import database.thejasonengine.com.AgentDatabaseController;


import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;


public class ClusteredVerticle extends AbstractVerticle {
	
	private static final Logger LOGGER = LogManager.getLogger(ClusteredVerticle.class);
	private PrometheusMeterRegistry prometheusRegistry;
	private JWTAuth jwt;
	private AuthUtils AU;
	private SetupSession setupSession;
	private SetupPostHandlers setupPostHandlers;
	private AgentDatabaseController agentDatabaseController;
	private static Pool pool;
	
	
	@Override
    public void start()
	{
		
		LOGGER.info("This is an ClusteredVerticle 'INFO' TEST MESSAGE");
		LOGGER.debug("This is a ClusteredVerticle 'DEBUG' TEST MESSAGE");
		LOGGER.warn("This is a ClusteredVerticle 'WARN' TEST MESSAGE");
		LOGGER.error("This is an ClusteredVerticle 'ERROR' TEST MESSAGE");
				
		EventBus eventBus = vertx.eventBus();
		LOGGER.debug("Started the ClusteredVerticle Event Bus");
		/*Created a vertx context*/
		Context context = vertx.getOrCreateContext();
		
		/* Create a DB instance called jdbcClient and add it to the context */
		DatabaseController DB = new DatabaseController(vertx);
		
		/*Create the RAM object that will store and reference data for the worker*/
		Ram ram = new Ram();
	  	ram.initializeSharedMap(vertx);
		
		AU = new AuthUtils();
		jwt = AU.createJWTToken(context);
		
		pool = context.get("pool");
		
		setupPostHandlers = new SetupPostHandlers(vertx);
		LOGGER.info("Set Handlers Setup");
		
		agentDatabaseController = new AgentDatabaseController(vertx);
		LOGGER.info("Set up Agent based Controller");
		
		Router router = Router.router(vertx);
		
		
		LOGGER.debug("Setup the post handler");
			
		
		/*Create and add a session to the system*/
		setupSession = new SetupSession(context.owner());
		router.route().handler(setupSession.sessionHandler);
		
		/*
		Read.readFile(vertx, "mysettings.json");
		LOGGER.debug("Read the ClusteredVerticle mysettings.json");
		*/
		
		
		 
        /*********************************************************************************/
        FreeMarkerTemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);
		TemplateHandler templateHandler = TemplateHandler.create((TemplateEngine) engine);
		/*Set Template*/
		
		//router.get("/dynamic/*").handler(templateHandler);
		/*********************************************************************************/
		router.getWithRegex(".+\\.ftl")
		 .handler(ctx -> 
				 {
					 
					 LOGGER.info("verifying access to the logged in file .ftl");
			    		Cookie cookie = (Cookie) ctx.getCookie("JWT");
			    		if (cookie != null) 
			    		{
			    			LOGGER.info("Found a cookie with the correct name");
			    			if(verifyCookie(cookie))
			    			{
			    			 
			    			 LOGGER.info(">>>>>>>>>>>>>>LOADING UP THE FTL WITH REQUIRED LOGIC VARIABLES>>>>>>>>>>>>>>>> : "+ctx.normalizedPath());
			    			 String file2send = ctx.normalizedPath();
			    			 
			    			 	String tokenObjectString = cookie.getValue();
			    			 	LOGGER.info("Cookie Value: " + tokenObjectString);
			    			 	JWT jwtToken = new JWT();
			    			 	LOGGER.info("Token value from cookie: " + tokenObjectString);
			    				LOGGER.info("Token parsed: " + jwtToken.parse(tokenObjectString).toString());
			    				
			    				
			    				/*We create a basic signature test case to see what we can see*/
			    				JsonObject JWT_Validation_Test = new JsonObject(); 
			    				JWT_Validation_Test.put("jwt", tokenObjectString);
			    				
			    				
			    				
			    				JsonObject JSON_JWT = jwtToken.parse(tokenObjectString);
			    				
			    				
			    				
			    				if(setupPostHandlers.validateJWTToken(JWT_Validation_Test))
			    				{
			    						JsonObject tokenObject = new JsonObject();
					    				JsonObject hold = (JsonObject) JSON_JWT.getValue("payload");
					    				
					    				LOGGER.info("Username: " + hold.getString("username"));
					    				
					    				tokenObject.put("username", hold.getString("username"));
					    				tokenObject.put("authlevel", hold.getInteger("authlevel"));
					    				tokenObject.put("jwt", tokenObjectString);
					    			 
					    			 /* I wont use sessions lets just stick with the cookie
					    			 String tokenString = setupSession.getTokenFromSession(ctx, "token");
					    			 String tokenObjectString = setupSession.getTokenFromSession(ctx, "tokenObject");
					    			 
					    			 LOGGER.info("Session JWT: " + tokenString);
					    			 LOGGER.info("Session tokenObject: " + tokenObjectString);
					    			 */
					    			 //JsonObject tokenObject = new JsonObject(tokenObjectString);
					    			 
					    			 
					    			 
					   				 engine.render(tokenObject, "templates/"+file2send.substring(1), 
					   			     res -> 
					   				 {
					   		             if (res.succeeded()) 
					   		             {
					   		            	 LOGGER.info("Successfully rendered template: " + file2send.substring(1));
					   		            	 ctx.response().end(res.result());
					   		             } 
					   		             else 
					   		             {
					   		            	 ctx.fail(res.cause());
					   		             }
					   				 });
			    				
			    				}
			    				else
			    				{
			    					LOGGER.error("**Potential security violation* The JWT from the cookie did not pass basic testing: " + ctx.normalizedPath() + ", From IP:" + ctx.request().remoteAddress());
					    			LOGGER.info("Redirecting to: " + ctx.normalizedPath().substring(0,ctx.normalizedPath().indexOf("/")).concat("index.htm"));
					    			ctx.redirect("../index.html");
			    				}
			    				
			    			}
			    		}
			    		else if (cookie == null) 
			    		{
			    			LOGGER.error("Did not find a cookie name JWT when calling the (.+\\\\.ftl) webpage: " + ctx.normalizedPath());
			    			LOGGER.info("Redirecting to: " + ctx.normalizedPath().substring(0,ctx.normalizedPath().indexOf("/")).concat("index.htm"));
			    			
			    			ctx.redirect("../index.html");
			    			//ctx.response().sendFile("webroot/index.htm"); //drop starting slash
			    		}
				 });
		/***************************************************************************************/
		router.get("/api/passwordGenerator/:password").handler(
    		    ctx -> 
    		    	{
    		    		
    		    		String password = ctx.request().getParam("password");
    		    		
    		    		ctx.response().putHeader("Content-Type", "application/json");
    		    		
    		    		String result = SetupPostHandlers.hashAndSaltPass(password.toString());
    		    		
    		    		JsonObject response = new JsonObject();
    	   	         	response.put("password", result);
    	   	         	LOGGER.info("Successfully generated password token " + result);
    		    		ctx.response().end(response.toString());
    		    		
    		    	}
    		    );
        
		/***************************************************************************************/
    	/*
    	 * This simply creates a cookie JWT token
    	 */
		/***************************************************************************************/
    	router.get("/api/newToken").handler(
    		    ctx -> 
    		    	{
    		    		
    		    		String name = "JWT";
    		    		ctx.response().putHeader("Content-Type", "application/json");
    		    		JsonObject tokenObject = new JsonObject();
    		    		tokenObject.put("endpoint", "sensor1");
    		    		tokenObject.put("someKey", "someValue");
    		    		String token = jwt.generateToken(tokenObject, new JWTOptions().setExpiresInSeconds(60));
    		    		LOGGER.info("JWT TOKEN: " + token);
    		    		
    		    		LOGGER.debug("Creating cookie");
    		    		AU.createCookie(ctx, 600, name, token, "/");
    		    		LOGGER.debug("Cookie created");
    		    		
    	   	         	ctx.response().setChunked(true);
    	   	         	ctx.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    	   	         	ctx.response().putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET");
    	   	         	//ctx.response().write("Cookie Stamped -> " + name + " : " +token);
    	   	         	
    	   	         	JsonObject response = new JsonObject();
    	   	         	response.put("token", token);
    	   	         	LOGGER.info("Successfully generated token and added cookie " + token);
    		    		ctx.response().end(response.toString());
    		    		
    		    	}
    		    );
    	/***************************************************************************************/
    	/*
    	 * This is really flawed - it simply checks for the actual presence of a cookie.
    	 * The verify cookie just looks at the user field in the payload
    	 */
    	/***************************************************************************************/
    	router.get("/api/protected").handler(
    		    ctx -> 
    		    	{
    		    		ctx.response().putHeader("Content-Type", "application/json");
    		    		Cookie cookie = (Cookie) ctx.getCookie("JWT");
    		    		if (cookie != null) 
    		    		{
    		    			LOGGER.info("Found a cookie with the correct name");
    		    			if(verifyCookie(cookie))
    		    			{
    		    				ctx.response().end("OK");
    		    			}
    		    		}
    		    		else if (cookie == null) 
    		    		{
    		    			LOGGER.error("Did not find a cookie name JWT when calling the (api/protected) webpage: " + ctx.normalizedPath());
    		    			ctx.response().sendFile("webroot/index.htm"); //drop starting slash
    		    		}
    		      });
    	/***************************************************************************************/
    	/*
    	 * This "secures" the route to particular assets by looking for the presence of a cookie
    	 */
    	/***************************************************************************************/
    	router.route("/loggedin/*").handler(
    		    ctx -> 
    		    	{
    		    		LOGGER.info("verifying access to the logged in file");
    		    		Cookie cookie = (Cookie) ctx.getCookie("JWT");
    		    		if (cookie != null) 
    		    		{
    		    			LOGGER.info("Found a cookie with the correct name");
    		    			if(verifyCookie(cookie))
    		    			{
    		    				LOGGER.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> : "+ctx.normalizedPath());
    		    				String file2send = ctx.normalizedPath();
    		    				LOGGER.info("Session: " + setupSession.getTokenFromSession(ctx, "username"));
    		    				ctx.response().sendFile("webroot/"+file2send.substring(1)); //drop starting slash
    		    			}
    		    		}
    		    		else if (cookie == null) 
    		    		{
    		    			LOGGER.error("Did not find a cookie name JWT when calling the (loggedin/*) webpage: " + ctx.normalizedPath());
    		    			//ctx.response().end("NO JWT TOKEN");
    		    			ctx.response().sendFile("webroot/index.htm"); //drop starting slash
    		    		}
    		    	}).failureHandler(frc-> 
    		    	{
    		  		  	//frc.response().setStatusCode( 400 ).end("Sorry! Not today");
    		    		frc.redirect("../index.htm");
    		    		
    		    	});;
        
        
        
		setRoutes(router);
		
		
		
		
		
		LOGGER.debug("Started the ClusteredVerticle Router");
		
		
		
		vertx.createHttpServer()
        .requestHandler(router)
        .listen(8080, res -> {
            if (res.succeeded()) {
                LOGGER.info("Server started on port 8080");
            } else {
                LOGGER.error("Failed to start server: " + res.cause());
            }
        });
		
		
	}
	/*****************************************************************************/
	
	/*****************************************************************************/
    public void setRoutes(Router router)
    {
    	
    	
    	 /*********************************************************************************/
	  	 /*This sets up a static HTML route			   */
	  	 /*********************************************************************************/
    	 router.route("/*").handler(StaticHandler.create().setCachingEnabled(false).setWebRoot("webroot"));   
    	
	     /*********************************************************************************/
	  	 /*This will log a user in {"result":"ok", "reason": "ok"}     				   */
	  	 /*********************************************************************************/
	  	 router.post("/api/validateCredentials").handler(BodyHandler.create()).handler(setupPostHandlers.validateCredentials);
	  	 router.post("/api/validateUserStatus").handler(BodyHandler.create()).handler(setupPostHandlers.validateUserStatus);
	  	 router.post("/api/createCookie").handler(BodyHandler.create()).handler(setupPostHandlers.createCookie);
	  	 router.post("/api/createSession").handler(BodyHandler.create()).handler(setupPostHandlers.createSession);
	  	/***************************************************************************************/
    	
	  	 router.post("/api/addDatabaseQuery").handler(BodyHandler.create()).handler(setupPostHandlers.addDatabaseQuery);
	  	 router.post("/api/getDatabaseQueryByDbType").handler(BodyHandler.create()).handler(setupPostHandlers.getDatabaseQueryByDbType);
	  	 router.post("/api/getDatabaseConnections").handler(BodyHandler.create()).handler(setupPostHandlers.getDatabaseConnections);
	  	 router.post("/api/setDatabaseConnections").handler(BodyHandler.create()).handler(setupPostHandlers.setDatabaseConnections);
	  	 
	  	/*********************************************************************************/
	  	/*These are the controller APIs for the various databases we want to drive     				     */
	  	/*********************************************************************************/
	  	
	  	
	  	/*This requires a post with payload {"databaseId":"postgres"}     				   */
	  	router.post("/api/sendDatabaseSelect").handler(BodyHandler.create()).handler(agentDatabaseController.agentDatabase);
	  	
	  	 
	  	 
  	  	/***************************************************************************************/
    	router.get("/api/simpleTest").handler(setupPostHandlers.simpleTest);
    	router.get("/api/simpleDBTest").handler(setupPostHandlers.simpleDBTest);
    	
    	
    	/***************************************************************************************/
    	// Define the WebSocket route
        router.get("/websocket").handler(ctx -> {
            // Upgrade the HTTP request to a WebSocket
            ctx.request().toWebSocket(socket -> {
                if (socket.succeeded()) {
                    // Handle WebSocket connection
                    LOGGER.debug("New WebSocket connection: " + socket.result().remoteAddress());

                    // Get the WebSocket instance
                    ServerWebSocket webSocket = socket.result();

                    // When a message is received from the client
                    webSocket.handler(buffer -> {
                        String message = buffer.toString();
                        LOGGER.debug("Received message: " + message);

                        // Echo the message back to the client
                        webSocket.writeTextMessage("Echo: " + message);
                    });

                    // Handle WebSocket close
                    webSocket.closeHandler(v -> {
                    	LOGGER.debug("WebSocket closed");
                    });

                    // Handle WebSocket error
                    webSocket.exceptionHandler(err -> {
                    	LOGGER.debug("WebSocket error: " + err.getMessage());
                    });
                } else {
                	LOGGER.error("Failed to create WebSocket: " + socket.cause());
                }
            });
        });
        
       
        
		
    	/*********************************************************************************/
    	/* This will be the routes for the website activity
    	/**********************************************************************************/
    	/* In this route we stack the individual fuctions of login to carry out the login */
    	 router.post("/web/login").handler(BodyHandler.create()).handler(setupPostHandlers.webLogin);
    	/***************************************************************************************/	    	
    	router.get("/get").handler(ctx -> {
    		            // Respond with a simple JSON object
    		            ctx.response()
    		                .putHeader("content-type", "application/json")
    		                .end("{\"message\":\"Hello get request!\"}");
    		        });
    	/***************************************************************************************/	    	 
    	router.post("/post").handler(ctx -> {
    		    		// Respond with a simple JSON object
    		            ctx.response()
    		                .putHeader("content-type", "application/json")
    		                .end("{\"message\":\"Hello post request!\"}");
    		        });
    }
    /*****************************************************************************/
 // Handle the login logic
    private void handleLogin(RoutingContext context) {
        // Get the username and password from the request body
    	
    	Buffer body = context.getBody();
    	String bodyString = body.toString();

    	JsonObject jsonBody = new JsonObject(bodyString);
    	
        String username = jsonBody.getString("username");
        String password = jsonBody.getString("password");

        LOGGER.debug("username: " + username);
        LOGGER.debug("password: " + password);
        
        // Simple validation (in production, never hardcode credentials, and always hash passwords)
        if ("user".equals(username) && "password123".equals(password)) {
            // Respond with success message
            context.response().putHeader("Content-Type", "application/json")
                    .end("{\"message\": \"Login successful!\"}");
        } else {
            // Respond with error message for invalid credentials
            context.response().setStatusCode(401).putHeader("Content-Type", "application/json")
                    .end("{\"message\": \"Invalid credentials\"}");
        }
    }
    
    /*****************************************************************************/
    private boolean verifyCookie(Cookie cookie)
    {
  	  	/*
  	  	 * The cookie is used to verify:
  	  	 *  	1. the access rights to the API
  	  	 *  	2. the freshness of the login
  	  	 * Both properties are to be played with.
  	  	 */
  	  
  	  	String token = cookie.getValue();
  	  	LOGGER.info("Token value: " + token);
  		JWT jwtToken = new JWT();
  		LOGGER.info("Token parsed: " + jwtToken.parse(token).toString());
  		JsonObject JSON_JWT = jwtToken.parse(token);
  		
  		Integer cookieExpiry = (Integer)JSON_JWT.getJsonObject("payload").getValue("exp");
  		
  		long cookExp = Long.valueOf(cookieExpiry.longValue());
  		cookExp = cookExp * 10000; //convert from an int to a long
  		
  		LOGGER.info("Cookie Expires: " + cookExp);
  		long currentTimestamp = System.currentTimeMillis();
  		LOGGER.info("Current Time:" + currentTimestamp);
  		
  		
  		if(currentTimestamp < cookExp)
  		{
  			LOGGER.info("Cookie is still alive");
  		}
  		else
  		{
  			LOGGER.info("Cookie is too old and will not be accepted");
  		}
  		

  		LOGGER.info("Verify Cookie Username: "  + JSON_JWT.getJsonObject("payload").getValue("username").toString());
  		
  		
  		return true;
    }
}