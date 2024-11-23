package com.thejasonengine.jaymarine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.thejasonengine.clients.WebSocketClient;
import com.thejasonengine.dashboard.Dashboard;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.common.template.TemplateEngine;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.TemplateHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;


public class WebService extends AbstractVerticle 
{
	private static final Logger LOGGER = LogManager.getLogger(WebService.class);
	  private static final String CHAT_CHANNEL = "SocketChat";
	  private WebSocket wsCtx;
	  private int SocketPort = 3200;
	  private String SocketDomain = "127.0.0.1";
	  private String SocketConnectionId = "/websocket/coordinator/admin/administrator";
	  private HashSet<WebSocketClient> wsClients = new HashSet<WebSocketClient>();
	  private HashSet<WebSocketClient> dashboardWsClients = new HashSet<WebSocketClient>();
	  private String CompassVar = "0";
	  public void start(Promise<Void> startPromise) throws Exception 
	  {
		  
		  LOGGER.info("This is an JayMarine 'INFO' TEST MESSAGE");
		  LOGGER.debug("This is a JayMarine 'DEBUG' TEST MESSAGE");
		  LOGGER.warn("This is a JayMarine 'WARN' TEST MESSAGE");
		  LOGGER.error("This is an JayMarine 'ERROR' TEST MESSAGE");
		
		  
		  Router router = Router.router(vertx);
		  LOGGER.info("Created the router");
		  startWebServer(vertx,router);
		  LOGGER.info("Started the Webserver");
		  
		  
		  upgradeWebserverToSocket(router);
		  //LOGGER.info("Upgraded to websocket");
		  
		  webserverRoutes(router);
		  //LOGGER.info("added default routes");
		  
		 
		  
		  startClient(vertx);
		 
	  }
	  /******************************************************************************/
	  /* This is what we do to start a webserver								    */
	  /******************************************************************************/ 
	  private void startWebServer(Vertx vertx,Router router)
	  {
		  vertx.createHttpServer()                      // <8>
	      .requestHandler(router)
	      .listen(Integer.getInteger("port", SocketPort))
	      .onSuccess(server -> {
	        LOGGER.info("HTTP server started on port: " + server.actualPort());
	        
	      }).onFailure(failed -> {
	    	 LOGGER.error("Unable to start HTTP server on port: " + SocketPort);
	      });
	  }
	  private void webserverRoutes(Router router)
	  {
		  FreeMarkerTemplateEngine engine = FreeMarkerTemplateEngine.create(vertx);
		  TemplateHandler templateHandler = TemplateHandler.create((TemplateEngine) engine);
		  
		  router.get("/dynamic/*").handler(templateHandler).handler(ctx ->
		  {
			  ctx.data().put("user", "jasonOther");
	          ctx.next();
		  });
		  
		  router.getWithRegex(".+\\.ftl").handler(ctx -> 
		  {
			  
			  	String buildversion = "1.0.a";
			  	String filePath = "buildversion.txt";
			  	try
			  	{
			  		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
			        if (inputStream == null) 
			        {
			            LOGGER.error("File not found: " + filePath);
			        }
			        else
			        {
			        	BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			        	buildversion = reader.lines().collect(Collectors.joining("\n"));
			        	LOGGER.info("build version: " + buildversion);
			            
			        }
			  	}
			  	catch(Exception e)
			  	{
			  		LOGGER.error("unable to read file: " + e.toString());
			  	}

			  
			  
			  JsonObject tokenObject = new JsonObject();
			  tokenObject.put("user", "jason");
			  tokenObject.put("buildversion", buildversion);
			  
			  String file2send = ctx.normalizedPath();
			  
			  LOGGER.info("Template file to send: " + file2send);
			  engine.render(tokenObject, "templates/"+file2send.substring(1), res -> 
			  {
				  if (res.succeeded()) 
				  {
					  ctx.response().end(res.result());
				  } 
				  else 
				  {
					  ctx.fail(res.cause());
				  }
			  });
			});
		  
		  
		  router.post("/input/:datasource").handler(BodyHandler.create()).handler(rc -> 
		  {
			  String datasource = rc.pathParam("datasource");
			  LOGGER.info("datasource:" + datasource);
			  String jsonResponse = "{\"datasource\":\""+datasource+"\",\"response\":\"01110\"}";
			  HttpServerResponse response = rc.response();
			  try
			  {
				  JsonObject JSONpayload = rc.getBodyAsJson();
				  /*
				   * Example:
				   * {"timestamp":"1707166816","source":"controller","payload":{"status":"set", "target": 97}}
				   */
				  
				  String version = JSONpayload.getString("version");
				  String source = JSONpayload.getString("source");
				  JsonObject payload = JSONpayload.getJsonObject("payload");
				  
				  LOGGER.info("version:" + version +", source:" + source + ", payload:" + payload.encode()); 
				  //
				  if(datasource.compareToIgnoreCase("autopilotControl")== 0)
				  {
					  LOGGER.info("This is an autopilot instruction");
					  if(source.compareToIgnoreCase("autopilotControl")== 0)
					  {
						  LOGGER.info("This is an autopilot controller instruction");
						  JsonObject jo_worker =  new JsonObject();
						  jo_worker.put("workerName", source+"_status");
						  jo_worker.put("version", version);
						  jo_worker.put("data", payload.getValue("status"));
						  jo_worker.put("worker_ip", rc.request().remoteAddress().host());
						  
						  generateDashboardData(jo_worker);
						  
						  jo_worker =  new JsonObject();
						  jo_worker.put("workerName", source+"_target");
						  jo_worker.put("version", version);
						  jo_worker.put("data", payload.getValue("target"));
						  jo_worker.put("worker_ip", rc.request().remoteAddress().host());
						  
						  generateDashboardData(jo_worker);
						  
						  int difference = Integer.parseInt(payload.getValue("target").toString()) - Integer.parseInt(CompassVar);
						  
						  String turnData = calculateDirectionOfTurn(difference);
						  
						  jo_worker =  new JsonObject();
						  jo_worker.put("workerName", source+"_turn");
						  jo_worker.put("version", version);
						  jo_worker.put("data", turnData);
						  jo_worker.put("worker_ip", rc.request().remoteAddress().host());
						  
						  generateDashboardData(jo_worker);
						  
						  
					      difference = (difference + 360) % 360;
						  
					      jo_worker =  new JsonObject();
						  jo_worker.put("workerName", source+"_correction");
						  jo_worker.put("version", version);
						  jo_worker.put("data", difference);
						  jo_worker.put("worker_ip", rc.request().remoteAddress().host());
						  
						  generateDashboardData(jo_worker);
						}
				  }
				  jsonResponse = "{\"datasource\":\""+datasource+"\",\"response\":\"OK\"}";
			  }
			  catch(Exception e)
			  {
				  jsonResponse = "{\"datasource\":\""+datasource+"\",\"response\":\""+e.toString()+"\"}";
				  LOGGER.error("Error handling input data from source:"+ datasource + ": "+ e.toString());
			  }
			  rc.response().end(jsonResponse);
		  });
		  
		  StaticHandler staticHandler = StaticHandler.create().setCachingEnabled(false);
	      router.route("/*").handler(staticHandler);
		  
	  }
	  /**********************************************************************************/
	  private String calculateDirectionOfTurn(int delta)
	  {
		  String turnData = "|";
		  if(delta > 0)
		  {
			  turnData=">";
		  }
		  if(delta < 0)
		  {
			  turnData="<";
		  }
		  if(delta == 0)
		  {
			  turnData="|";
		  }
		  return turnData;
	  }
	  /**********************************************************************************/
	  /* Send message to active dashboard 				                                */
	  /**********************************************************************************/  
	  private void broadcastToDashboards(String Payload)
	  {
		  Iterator<WebSocketClient> dbClients = dashboardWsClients.iterator();
		  LOGGER.info("There are currently " + dashboardWsClients.size() + " active dashboard connections");
		  while(dbClients.hasNext())
		  {
			  WebSocketClient hold = dbClients.next();
			  LOGGER.info("Have found a dashboard client id: "+ hold.getWsConnection().textHandlerID());
			  hold.getWsConnection().writeTextMessage(Payload);
		  }
	  }
	  /******************************************************************************/
	  /* This is what we do when we want to handle a websocket connection           */
	  /******************************************************************************/ 
	  private void upgradeWebserverToSocket(Router router)
	  {
		 
		  LOGGER.info("Upgrading router for websocket");
		  /******************************************************************************/
		  /* This is how we handle data read/sent
		   * type: agent/worker
		   * access: read/write
		   * endpoint: temperature/compass/gps etc 
		   */
		  /******************************************************************************/
		  router.route("/websocket/:type/:access/:endpoint").handler(rc -> 
		  {
			  String type = rc.pathParam("type");
			  String access = rc.pathParam("access");
			  String endpoint = rc.pathParam("endpoint");
			  
			  LOGGER.info("websocket " + type + " connected for: " + access + " access on endpoint known as:" + endpoint);
			 
 			  rc.request().toWebSocket(ar -> 
			  {

				  LOGGER.info("upgrading to websocket");
		          if (ar.succeeded()) 
		          {
		            ServerWebSocket websocket = ar.result();
		            LOGGER.info("successfully upgraded to websocket");
		            
		            WebSocketClient wsClient = new WebSocketClient();
		            LOGGER.info("client connected: "+ websocket.textHandlerID());
		            String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
					  
					wsClient.setConnectionId(websocket.textHandlerID());
					wsClient.setWsConnection(websocket);
					wsClient.setTimestamp(timeStamp);
					wsClient.setEndpoint(endpoint);
					
					LOGGER.info("Sending client welcome message: "+ websocket.textHandlerID());
					wsClient.getWsConnection().writeTextMessage("Connected to coordinator as " + type + " connected for: " + access + " access on endpoint known as:" + endpoint);
					wsClients.add(wsClient);
					
					
					LOGGER.info("Have added client "+ websocket.textHandlerID() +" to the list of active connections");

					/******************************************************************************/
					/* This is what we do when we recieve a message from an entire chat channel   */
					/******************************************************************************/  
		            vertx.eventBus().consumer(CHAT_CHANNEL, message -> 
					{
						  LOGGER.info("Consuming message from "+ CHAT_CHANNEL + " client " +websocket.textHandlerID() + " sent to server by "+ wsClient.getEndpoint() +": " + (String)message.body());
						 //websocket.writeTextMessage((String)message.body());
					});
		            /*************************************************************************/
					/* This is what we do when we recieve a message to an individual client  */
					/*************************************************************************/  
		            websocket.textMessageHandler(message -> 
					{
						  LOGGER.info("Consuming message from client: " + websocket.textHandlerID() +" under control of: "+ wsClient.getEndpoint() + " :"+  (String)message); 
						  
						  JsonObject jo =  new JsonObject((String)message);
						  
						  LOGGER.info("endpoint:" + wsClient.getEndpoint());
						  LOGGER.info("version:" + jo.getString("version"));
						  LOGGER.info("data:" + jo.getString("data"));
						  LOGGER.info("ip:" + jo.getString("ip"));
						  
						  JsonObject jo_worker =  new JsonObject();
						  
						  jo_worker.put("endpoint", wsClient.getEndpoint());
						  jo_worker.put("version", jo.getString("version"));
						  jo_worker.put("data", jo.getString("data"));
						  jo_worker.put("ip", jo.getString("ip"));
						  jo_worker.put("epoch", generateEpoch());
						  
						  if(wsClient.getEndpoint().compareToIgnoreCase("compass")==0)
						  {
							  CompassVar = jo.getString("data");
						  }
					});
		            /*****************************************************************/
					/* This is how we handle a websocket closing                     */
					/*****************************************************************/ 
		            websocket.closeHandler(message ->
					{
						  LOGGER.info("client disconnection detected: "+websocket.textHandlerID());
						  cleanConnections(wsClients, websocket);
						  if(wsClient.getEndpoint().compareToIgnoreCase("dashboard") == 0)
						  {
			            		cleanConnections(dashboardWsClients, websocket);
						  }
						  LOGGER.info("Have removed active connection: "+ websocket.textHandlerID() +" from list that was under control of: " + wsClient.getEndpoint());
						  LOGGER.info("There are currently " + wsClients.size() + " active client connections");
					});
		            /**********************************************************************/
		            /*	This is how we handle exceptions in the websocket
		           	/**********************************************************************/
		            websocket.exceptionHandler(e->
		            {
		            	LOGGER.error("An exception has been raised for client: " + websocket.textHandlerID() + " error:" + e.getMessage());
		            	websocket.close();
		            	LOGGER.error("The broken connection to: " + websocket.textHandlerID() +" under control of " + wsClient.getEndpoint() + " has been removed");
		            	cleanConnections(wsClients, websocket);
		            	if(wsClient.getEndpoint().compareToIgnoreCase("dashboard") == 0)
		            	{
		            		cleanConnections(dashboardWsClients, websocket);
		            	}
		            });
		            
		          }
		          if (ar.failed())
		          {
		        	  LOGGER.error("Error" + ar.cause().getMessage());
		          }
		        });
			  });
	  }
	  /**********************************************************************************/
	  public void generateDashboardData(JsonObject jo_worker)
	  {
		  JsonArray ja = Dashboard.getWorkerData();
		  boolean workerExists = false;
		  
		  int heading = 0;
		  int destintation = 0;
		  int correction = 0;
		  int tillerMove = 0;
		  
		  LOGGER.info("Dashboard size: " + ja.size());
		  for (int i = 0; i < ja.size(); i++) 
		  {
			  JsonObject hold_jo = ja.getJsonObject(i);
			  LOGGER.info("Dashboard object selected");
			  if(hold_jo.containsKey("endpoint") == true)
			  {
				  String hold_jo_workerName = hold_jo.getString("endpoint");
				  LOGGER.info("endpoint: " + hold_jo_workerName);
				  LOGGER.info("wsClient endpoint: " + jo_worker.getString("workerName"));
				  if(hold_jo_workerName.compareToIgnoreCase(jo_worker.getString("endpoint")) == 0)
				  {
					  hold_jo.put("endpoint", jo_worker.getString("endpoint"));
					  hold_jo.put("version", jo_worker.getString("version"));
					  hold_jo.put("data", jo_worker.getString("data"));
					  hold_jo.put("ip", jo_worker.getString("ip"));
					  hold_jo.put("epoch", generateEpoch());
					  
					  LOGGER.info("Have updated an object inside the JsonArray");
					  workerExists = true;
				  }
				  /*Auto Heading Adjustment*/
				  if(hold_jo.getString("endpoint").compareToIgnoreCase("compass")==0)
				  {
					  heading = Integer.parseInt(hold_jo.getString("data"));
					  
					  LOGGER.info("heading:" + heading);
				  }
				  if(hold_jo.getString("endpoint").compareToIgnoreCase("autopilotControl_target")==0)
				  {
					  destintation = Integer.parseInt(hold_jo.getString("data"));
					  hold_jo.put("epoch", generateEpoch());
					  LOGGER.info("destintation:" + destintation);
				  }
				  correction = heading - destintation;
				  
				  if(hold_jo.getString("endpoint").compareToIgnoreCase("autopilotControl_turn")==0)
				  {
					  hold_jo.put("data", calculateDirectionOfTurn(correction));
					  hold_jo.put("epoch", generateEpoch());
				  }
				  
				  if(hold_jo.getString("endpoint").compareToIgnoreCase("autopilotControl_agent")==0)
				  {
					  tillerMove = Integer.parseInt(hold_jo.getString("data"));
					  
					  LOGGER.info("tillerMove:" + tillerMove);
				  }
				  
				  LOGGER.info("correction: " + correction);
				  
				  correction = Math.abs(correction);
				  LOGGER.info("positive correction: " + correction);
				  
				  correction = (correction + 360) % 360;
				  
				  LOGGER.info("correction modulus: " + correction);
				  if(hold_jo.getString("workerName").compareToIgnoreCase("autopilotControl_correction")==0)
				  {
					  hold_jo.put("data", String.valueOf(correction));
					  hold_jo.put("epoch", generateEpoch());
					  LOGGER.info("Have added a new correction:" + correction);
				  }
			  }
		  }
		  
		  
		  
	      if(!workerExists)
		  {
			  JsonObject hold_jo_worker =  new JsonObject();
			  hold_jo_worker.put("workerName", jo_worker.getString("workerName"));
			  hold_jo_worker.put("version", jo_worker.getString("version"));
			  hold_jo_worker.put("data", jo_worker.getString("data"));
			  hold_jo_worker.put("worker_ip", jo_worker.getString("worker_ip"));
			  hold_jo_worker.put("epoch", generateEpoch());
			  
			  
			  ja.add(hold_jo_worker);
			  
			  LOGGER.info(">>>>>>>>>>>>>>>>>> ---- Have created a new object inside the JsonArray");
		  }
		  
		  
		  //removeOldData(ja);
		  
		  Dashboard.setWorkerData(removeOldData(ja));
		  ja = Dashboard.getWorkerData();
		  LOGGER.info("Data JSON Array: " + ja.encodePrettily());
		  
		  updateDashboard(ja.encodePrettily());
	  }
	  /**********************************************************************************/
	  private JsonArray removeOldData(JsonArray ja)
	  {
		  Long epoch = Long.parseLong(generateEpoch(), 10);
		  Long timeWindow = 3L;
		  ArrayList<JsonObject> list = new ArrayList<>();

		  for (int i = 0; i < ja.size(); i++) 
		  {
		      String temp = ja.getJsonObject(i).getString("epoch");
		      Long testEpoch =  Long.parseLong(temp, 10);
		      /* Only add objects that are not more than 3 seconds old */
		      
		      Long allowedTime = Long.sum(testEpoch, timeWindow);
		      
		      int compareValue = allowedTime.compareTo(epoch);
		      LOGGER.debug("allowedTime: " + allowedTime + " < epoch: " + epoch +":" + compareValue);
		      
		      if(compareValue >= 0) 
		      {
		          LOGGER.debug("Adding Data Object: " + i);
		    	  list.add(ja.getJsonObject(i));
		      }
		      else
		      {
		    	  LOGGER.debug("Not Adding Data Object (Aged out): " + i);
		      }
		  }

		  JsonArray updatedJsonArray = new JsonArray(list);
		  
		  return updatedJsonArray;
	  }
	  /**********************************************************************************/
	  private String generateEpoch()
	  {
		  long unixTimestamp = Instant.now().getEpochSecond();
		  return String.valueOf(unixTimestamp);
	  }
	  
	  /**********************************************************************************/
	  /* This is how we manage active and inactive connections                          */
	  /**********************************************************************************/  
	  private HashSet<WebSocketClient> cleanConnections(HashSet<WebSocketClient> wsClients, ServerWebSocket websocket)
	  {
		
		  Iterator<WebSocketClient> clients = wsClients.iterator();
		  LOGGER.info("There are currently " + wsClients.size() + " active client connections");
		  while(clients.hasNext())
		  {
			  WebSocketClient hold = clients.next();
			  if(websocket.textHandlerID().compareToIgnoreCase(hold.getConnectionId())==0)
			  {
				  LOGGER.info("Have found a match and am removing it from the list of active connections");
				  clients.remove();
				  LOGGER.info(websocket.textHandlerID() + " has been successfully removed from the system");
			  }
		  }
		  return wsClients;
	  }
	  /**********************************************************************************/
	  /* This is how we re-activate a connection - to do                                */
	  /**********************************************************************************/  
	  private void restartDownedWebsocket()
	  {
	  
	  }
	  
	  /**********************************************************************************/
	  /* Send message to active client where username = 123                             */
	  /**********************************************************************************/  
	  private void spearPhish(String username)
	  {
		  Iterator<WebSocketClient> clients = wsClients.iterator();
		  LOGGER.info("There are currently " + wsClients.size() + " active client connections");
		  while(clients.hasNext())
		  {
			  WebSocketClient hold = clients.next();
			  if(hold.getEndpoint().compareToIgnoreCase(username)==0)
			  {
				  LOGGER.info("Have found a match for username: " + username + " id: "+ hold.getWsConnection().textHandlerID());
				  hold.getWsConnection().writeTextMessage("hello username");
			  }
		  }
	  }
	  /**********************************************************************************/
	  /* Send message to active client where username = 123                             */
	  /**********************************************************************************/  
	  private void updateDashboard(String message)
	  {
		  Iterator<WebSocketClient> clients = dashboardWsClients.iterator();
		  LOGGER.info("There are currently " + dashboardWsClients.size() + " active dashboard connections");
		  while(clients.hasNext())
		  {
			  WebSocketClient hold = clients.next();
			  hold.getWsConnection().writeTextMessage(message);
			  LOGGER.info("Message sent to dashboard: " + message);
		  }
	  }
	  /**********************************************************/
	  /*
	  /* This function creates the web socket SERVER
	  /* 
	  /*********************************************************/
	  private void startWebSocketServer(Vertx vertx)
	  {
		  
		  HttpServer server = vertx.createHttpServer().requestHandler(req -> 
	      {
	    	  if (req.uri().equals("/")) req.response().sendFile("ws.html");
		  });
		  
		  server.webSocketHandler(handler -> 
		  {
			  WebSocketClient wsClient = new WebSocketClient();
			  LOGGER.debug("client connected: "+ handler.textHandlerID());
			  String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
			  
			  wsClient.setConnectionId(handler.textHandlerID());
			  wsClient.setWsConnection(handler);
			  wsClient.setTimestamp(timeStamp);
			  
			  wsClients.add(wsClient);

			  vertx.eventBus().consumer(CHAT_CHANNEL, message -> 
			  {
				  LOGGER.info("Consuming message from clients sent to server:" + (String)message.body());
				  handler.writeTextMessage((String)message.body());
			  });

			  handler.textMessageHandler(message -> 
			  {
				  LOGGER.info("Publishing message from server to clients: " + (String)message); 
				  vertx.eventBus().publish(CHAT_CHANNEL,message);
			  });

			  handler.closeHandler(message ->
			  {
				  LOGGER.info("client disconnected: "+handler.textHandlerID());
				  Iterator<WebSocketClient> clients = wsClients.iterator();
				  if(clients.hasNext())
				  {
					  if(handler.textHandlerID().compareToIgnoreCase(clients.next().getConnectionId())==0)
					  {
						  LOGGER.info("Have found a match and am removing it from the list of active connections");
						  clients.remove();
						  LOGGER.info(handler.textHandlerID() + " has been successfully removed from the system");
					  }
				  }
				  
				  
			  });
          
			  LOGGER.info("Successfully created websocket server and awaiting connections");

		  	}).listen(SocketPort);
		  
	   
	  }
	  /**********************************************************/
	  /*
	  /* This function creates the web socket CLIENT
	  /* 
	  /*********************************************************/
	  	private HttpClient startClient(Vertx vertx) 
	    {
	  		
	  		HttpClientOptions options = new HttpClientOptions();
		  	
	  		HttpClient client = vertx.createHttpClient(options);
	  		
	        
	        	LOGGER.info("Connecting to websocket server");
	        	
	        	client.webSocket(SocketPort, SocketDomain, SocketConnectionId, ar -> 
	        	{
	        		LOGGER.info("Connecting to: "+ SocketDomain + ":" + SocketPort + SocketConnectionId);
	        		if (ar.succeeded()) 
	                {
	                    
	        			wsCtx = ar.result();
	        			wsCtx.headers().add("Connection", "Upgrade");
	                    LOGGER.info("Successfully created the websocket connection");
	                    //ctx.writeTextMessage(message);
	                    
	                    /*ctx.textMessageHandler((msg) -> 
	                    {
	                        System.out.println("Client " + msg);
	                        //ctx.writeTextMessage(message);
	                        client.close();
	                    })*/
	                    wsCtx.exceptionHandler((e) -> 
	                    {
	                        LOGGER.info("Websocket connection error : restarting in 10 seconds");
	                        client.close();
	                        wsCtx = null;
	                        vertx.setTimer(10 * 1000, (__) -> 
	                        {
	                        	LOGGER.info("Attempting a websocket restart");
	                        	startClient(vertx);
	                        });
	                    });
	                } 
	                else 
	                {
	                    LOGGER.error("Unable to connect to the websocket server: " + ar.cause().getMessage());
	                }
	            });
	        	return client;
	    
	    }
	  	/*************************************************************/
	  	/*
	  	/*  This is a simple utility to send a message to a websocket
	  	/*************************************************************/
	  	public void sendMessage(String message)
	    {
	    	if(wsCtx != null)
	    	{
	    		LOGGER.info("Sending message");
	    		wsCtx.writeTextMessage(message);
	    		LOGGER.info("Message sent");
	    	}

	    	else
	    	{
	    		LOGGER.info("Context is null so force starting it");
	    	}
	    }
}
