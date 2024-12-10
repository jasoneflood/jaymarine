package cluster.thejasonengine.com;

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
import router.thejasonengine.com.SetupPostHandlers;
import session.thejasonengine.com.SetupSession;



public class ClusteredVerticle extends AbstractVerticle {
	
	private static final Logger LOGGER = LogManager.getLogger(ClusteredVerticle.class);
	private PrometheusMeterRegistry prometheusRegistry;
	private JWTAuth jwt;
	private AuthUtils AU;
	private SetupSession setupSession;
	private SetupPostHandlers setupPostHandlers;
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
		
		AU = new AuthUtils();
		jwt = AU.createJWTToken(context);
		
		pool = context.get("pool");
		
		setupPostHandlers = new SetupPostHandlers(vertx);
		LOGGER.info("Set Handlers Setup");
		
		
		
		Router router = Router.router(vertx);
		
		
		LOGGER.debug("Setup the post handler");
		
		
		
		/*Create and add a session to the system*/
		setupSession = new SetupSession(context.owner());
		router.route().handler(setupSession.sessionHandler);
		
		
		
		
		
		
		/*
		Read.readFile(vertx, "mysettings.json");
		LOGGER.debug("Read the ClusteredVerticle mysettings.json");
		*/
		setRoutes(router);
		LOGGER.debug("Started the ClusteredVerticle Router");
		
		
		
		//createSystemDatabase(vertx);
		
		
		
		

		/*
		JDBCClient client = database.thejasonengine.com.DatabaseController.createSystemDatabase(vertx);
		database.thejasonengine.com.DatabaseGetterTemplates DGT = new database.thejasonengine.com.DatabaseGetterTemplates(client);
		database.thejasonengine.com.DatabaseSetterTemplates DST = new database.thejasonengine.com.DatabaseSetterTemplates(client);
		
		setupPostHandlers = new SetupPostHandlers(vertx, DST);
		LOGGER.info("Set Handlers Setup");
		*/
		
		
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
    	
    	router.post("/login").handler(BodyHandler.create()).handler(this::handleLogin);
    	router.get("/handler/simpleTest").handler(setupPostHandlers.simpleTest);
    	router.get("/handler/simpleDBTest").handler(setupPostHandlers.simpleDBTest);
    	
    	//router.get("/api/setupSystemDatabase").handler(setupPostHandlers.setupSystemDatabase); 
    	
    	router.get("/get").handler(ctx -> {
            // Respond with a simple JSON object
            ctx.response()
                .putHeader("content-type", "application/json")
                .end("{\"message\":\"Hello get request!\"}");
        });
    	 
    	
    	//router.get("/simpleTest").handler(setupPostHandlers.simpleTest);
    	
    	router.post("/post").handler(ctx -> {
    		// Respond with a simple JSON object
            ctx.response()
                .putHeader("content-type", "application/json")
                .end("{\"message\":\"Hello post request!\"}");
        });
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
        
    
        /*router.get("/metrics").handler(ctx -> {
    		// Respond with a simple JSON object
            ctx.response()
                    .putHeader("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                    .end(prometheusRegistry.scrape());
        });*/
        
        router.route().handler(StaticHandler.create("src/main/resources/user")
                .setCachingEnabled(false)  // Optional: Disable caching for development purposes
                .setDirectoryListing(true));  // Optional: Allow directory listing

	    /*********************************************************************************/
	  	/*This will log a user in {"result":"ok", "reason": "ok"}     				   */
        /*********************************************************************************/
	    
       
        //router.post("/api/validateCredentials").handler(BodyHandler.create()).handler(setupPostHandlers.validateCredentials);
        
        /*
        router.post("/api/validateUserStatus").handler(BodyHandler.create()).handler(setupPostHandlers.validateUserStatus);
	  	router.post("/api/createCookie").handler(BodyHandler.create()).handler(setupPostHandlers.createCookie);
	  	router.post("/api/createSession").handler(BodyHandler.create()).handler(setupPostHandlers.createSession);
	  	*/  
	  	  
	  	//  router.post("/web/login").handler(BodyHandler.create()).handler(setupPostHandlers.webLogin);
        /******************************************************************************/
    	/*
    	 * This simply creates a cookie JWT token
    	 */
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
        
        
        
        
        router.route().handler(BodyHandler.create());
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