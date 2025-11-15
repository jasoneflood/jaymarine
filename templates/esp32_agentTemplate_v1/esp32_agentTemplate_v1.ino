/*********
 Author: Jason Flood
 Codebase: esp32_agentTemplate_v1

This is a template file upon which all v1 agents should be built. If the template is no longer suitable - please do not update it, instead create a new version template.
This is to support API requests and handling from potential worker evolutions.


 *********/

#include <SPI.h>
#include <Wire.h>
#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <HTTPClient.h>
#include <EEPROM.h>
#include <ArduinoJson.h>
#include <WebSocketsClient.h>

WebSocketsClient webSocket;

//const char* ssid = "jasonmarine-live";
//const char* ss_password = "passpass";

const char* ssid = "suttonzoo";
const char* ss_password = "Happyh1pp0";


String WORKER_IP_ADDRESS = "0.0.0.0";

String jsonSensorString ="";

String SOCKET_SERVER_IP_ADDRESS = "0.0.0.0";
String SOCKET_SERVER_PORT = "3200";

AsyncWebServer server(80); // The HTTP Server is run on port 80
#define USE_SERIAL Serial1

#define PIN_RED    25 // GPIO23
#define PIN_GREEN  26 // GPIO22
#define PIN_BLUE   27 // GPIO21

void flashRGB(int, int, int);
int rest_red;
int rest_blue;
int rest_green;

const char* PARAM_INPUT_1 = "setting";
const char* PARAM_INPUT_2 = "svalue";

unsigned long lastSendTime = 0;  // keeps track of last message send time

uint addr = 0;
struct {
  uint val = 0;
  char endpointName[50];
  char endpointUrl[50];
  char ssid[20];
  char ss_password[20];
  char domain[50];
  char protocol[10];
  char socket_server_ip[50];
  char socket_server_port[8];
  char poll_frequency[3];
} configData = {
  1,                        // val
  "endpoint_x",        // endpointName
  "localhost",// endpointUrl
  "jaymarine",             // ssid
  "passpass",         // ss_password
  "jaysboat.com",            // domain
  "http",                  // protocol
  "192.168.1.100",          // socket_server_ip
  "80",                   // socket_server_port
  "10"                      // poll_frequency
};;


const size_t capacity = JSON_OBJECT_SIZE(500); // Adjust size based on elements
DynamicJsonDocument doc(capacity);


const char stream_html[]  PROGMEM = R"rawliteral(
{"endpointName":%endpointName%,"endpointUrl":%endpointUrl%,"domain":%domain%,"protocol":%protocol%, "socket_server_ip":%socket_server_ip%,"socket_server_port":%socket_server_port%, "poll_frequency":%poll_frequency%}
)rawliteral";
/*************************************/
const char script_var[] PROGMEM = R"rawliteral(
  let socket = new WebSocket("ws://%SOCKET_SERVER_IP_ADDRESS%:%SOCKET_SERVER_PORT%");
  function testScript()
  {
    socket.send("{\"version\":\""+version+"\"}");
  }
  function updateSettings(element) 
  {
    var xhr = new XMLHttpRequest();
    var elementID = element.id;
    var elementValue = element.value;
    console.log(elementID, elementValue);
    xhr.open("GET", "/update?setting="+element.id+"&svalue="+element.value, true);
    xhr.send();
  }
  socket.onopen = function(e) 
  {
    console.log("[open] Connection established");
    socket.send("{\"version\":\"1\"}");
  };
  socket.onmessage = function(event) 
  {
    console.log(`${event.data}`);
  };
  socket.onclose = function(event) 
  {
    if (event.wasClean) 
    {
      console.log(`[close] Connection closed cleanly, code=${event.code} reason=${event.reason}`);
    } 
    else 
    {
      console.log('[close] Connection died');
    }
  };
  socket.onerror = function(error) 
  {
    console.log(`[error] ${error.message}`);
  };
)rawliteral";
/*************************************************/
const char style_var[] PROGMEM = R"rawliteral(
  body
  {
    background-color: #0c0a3e;
    margin: 0; height: 100%; overflow: hidden
  }
  a
  {
    color: #f9564f;
  }
    #pageContainer
    {
      height: 100vh;
      width: 100%;
      background-color: #0c0a3e;
      color: #eee;
      margin: 0 auto;
    }
    #pageContent
    {
      height: 100vh;
      width: 80%;
      background-color: #b33f62;
      color: #eee;
      margin: 0 auto;
    }
    #pageTitle
    {
      background-color:#7b1e7a;
      height: 5vh;
      width: 100%;
      margin: 0 auto;
      text-align:center;
      padding-top: 2vh;
      color:#f3c677;
    }
  #pageLinks
    {
      
    }
  #settingsDetails
    {
    
    }
  #splitContent
  {
    height:400px;
    width: 100%;
  }
  #settings
  {
    height:99%;
    width: 49%;
    border-style: solid;
  }
  #headings
  {
    height:99%;
    width: 49.5%;
    margin-left: 50%;
    margin-top: -400px;
    border-style: solid;
  }
  #xdata
  {
    height: 30px;
    width:32%;
    text-align: center;
    border-style: solid;
    margin-left:-.25%;
  }
  #ydata
  {
    height: 30px;
    margin-top:-34px;
    margin-left:33%;
    width:32%;
    text-align: center;
    border-style: solid;
  }
  #zdata
  {
    height: 30px;
    margin-top:-34px;
    margin-left:66.5%;
    width:33%;
    text-align: center;
    border-style: solid;
  }
  #headingdata
  {
    height: 50px;
    width:99.25%;
    text-align: center;
    font-size:32px;
    border-style: solid;
  }
  table
  {
     text-align: left;
  }
  #GPS
  {
      
  }
)rawliteral";
/************************************************/
const char index_html[] PROGMEM = R"rawliteral(
<!DOCTYPE HTML>
<html>
<head>
  <title>jaymarine agent manager</title>
  <style>
   
  </style>
  </head>
  <body>
  <div id=pageContainer>
    <div id=pageContent>
        <div id=pageTitle>Agent Manager
          <div id=version></div>
        </div>
        <div id=pageLinks><a href=\reset>reset</a> | <a href=\about>about</a> </div>
          <div id=settingsDetails>Settings update on change. </div>
          <div id=splitContent>
            <div id=settings>
              <table>
               %ROWPLACEHOLDER_VAR%
              </table>
            </div>
            <div id=headings>
            </div>
            <div id=controls>
              <button type="button" onclick="testScript(1)">testScript</button>
            </div>
    </div>
  </div>

<script>
 %SCRIPT_VAR%
</script>
</body>
</html>
)rawliteral";
/******************************************************/
String processor(const String& var)
{
    String gpsData ="";
  //Serial.println(var);
  if(var == "STYLE_VAR")
  {
      Serial.println("integrating the STYLES variable");
      return style_var;
  }
  /******************************************************/
  if(var == "SCRIPT_VAR")
  {
    Serial.println("integrating the SCRIPT variable");
    return script_var;
  }
  /******************************************************/
  if(var == "SOCKET_SERVER_IP_ADDRESS")
  {
    return configData.domain;
  }
  if(var == "SOCKET_SERVER_PORT")
  {
    return configData.socket_server_port;
  }

  /******************************************************/
  if(var == "ROWPLACEHOLDER_VAR")
  {
    String rows = "";
    rows += "<tr><td>ssid</td><td><input type=\"text\" id=\"ssid\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.ssid;
    rows +="\"></td></tr>";
    
    rows += "<tr><td>ss_password</td><td><input type=\"text\" id=\"ss_password\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.ss_password;
    rows +="\"></td></tr>";

    rows += "<tr><td>endpointName</td><td><input type=\"text\" id=\"endpointName\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.endpointName;
    rows +="\"></td></tr>";

    rows += "<tr><td>endpointUrl</td><td><input type=\"text\" id=\"endpointUrl\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.endpointUrl;
    rows +="\"></td></tr>";

    rows += "<tr><td>domain</td><td><input type=\"text\" id=\"domain\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.domain;
    rows +="\"></td></tr>";

    rows += "<tr><td>protocol</td><td><input type=\"text\" id=\"protocol\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.protocol;
    rows +="\"></td></tr>";

    rows += "<tr><td>socket_server_ip</td><td><input type=\"text\" id=\"socket_server_ip\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.socket_server_ip;
    rows +="\"></td></tr>";

    rows += "<tr><td>socket_server_port</td><td><input type=\"text\" id=\"socket_server_port\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.socket_server_port;
    rows +="\"></td></tr>";

    rows += "<tr><td>poll_frequency</td><td><input type=\"text\" id=\"poll_frequency\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.poll_frequency;
    rows +="\"></td></tr>";
    return rows;
  }
  /******************************************************/
  if(var == "JSONDATA")
  {
    String jsonData = "cat\":\"dog\"";
    Serial.println("jsonData: ");
    Serial.println(jsonData);
    return jsonData;
  }
  /******************************************************/
  return String();
}
/*******************************************************/
/**************************************************************************************************************************************/
String IpAddress2String(IPAddress ipAddress)
{
  return String(ipAddress[0]) + String(".") +\
  String(ipAddress[1]) + String(".") +\
  String(ipAddress[2]) + String(".") +\
  String(ipAddress[3])  ;
}
/************************************************/
String generateTestData_intAsString()
{
     int random_int = random(0,361); //returns a number between 0 and 360
     String random_string = String(random_int);
     return random_string;
}
/************************************************/
String getAgentVersion()
{
  String version = "1";
  return version;
}
/************************************************/
String getAgentIP()
{
  String ip = WORKER_IP_ADDRESS;
  return ip;
}
/************************************************/
String getAgentData()
{
  String data = generateTestData_intAsString();
  return data;
}
/************************************************/
String getAgentEndpoint()
{
  String endpoint = "configData.workerName";
  return endpoint;
}
/**************************************************************************************************************************************/


void takeAction(char * myPayload)
{
     DeserializationError error = deserializeJson(doc, myPayload);

    // Check for errors in parsing
    if (error) 
    {
      Serial.print("Failed to parse JSON: ");
      Serial.println(error.c_str());
    }
    else
    {
      Serial.print("Successfully deserialized json data");
      Serial.println(myPayload); 
      const char* version = doc["version"];      
      const char* data = doc["data"];      
      const char* epoch = doc["epoch"];   

      Serial.println("Parsed JSON:");
      Serial.print("version: ");
      Serial.println(version);
      Serial.print("data: ");
      Serial.println(data);
      Serial.print("epoch: ");
      Serial.println(epoch);



      if(data == "heartbeat")
      {
        String version = getAgentVersion();
        String data = getAgentData();
        String ip = getAgentIP();
        String endpoint = getAgentEndpoint();
         
        String hearbeatjsonData ="{\"version\":";
        hearbeatjsonData += "\""+version+"\"";
        hearbeatjsonData += ",\"data\":\"";
        hearbeatjsonData += data;
        hearbeatjsonData += "\",\"ip\":";
        hearbeatjsonData += "\"";
        hearbeatjsonData += ip;
        hearbeatjsonData += "\",\"endpoint\":";
        hearbeatjsonData += "\"";
        hearbeatjsonData += endpoint;
        hearbeatjsonData +="\"";
        hearbeatjsonData += "\",\"epoch\":";
        hearbeatjsonData += "\"";
        hearbeatjsonData += epoch;
        hearbeatjsonData +="\"}";

        webSocket.sendTXT(hearbeatjsonData);
      }
    }
}

/***********************************************/
/* This is a websocket event manager. This 
 * code executes when client connects. Or  
 * a message is to be sent.
 ***********************************************/
void webSocketEvent(WStype_t type, uint8_t * payload, size_t length) 
{
	switch(type) 
  {
		case WStype_DISCONNECTED:
			USE_SERIAL.printf("[WSc] Disconnected!\n");
      rest_red = 255;
      rest_green = 0;
      rest_blue = 0;
      
      flashRGB(255,0,0);
      
			break;
		case WStype_CONNECTED:
			USE_SERIAL.printf("[WSc] Connected to url: %s\n", payload);
      rest_red = 150;                                                                                             
      rest_green = 150;
      rest_blue = 150;

      flashRGB(0,150,150);
			// send message to server when Connected
			webSocket.sendTXT("{\"status\":\"Connected\"}");
			break;
		case WStype_TEXT:
			USE_SERIAL.printf("[WSc] get text: %s\n", payload);
      flashRGB(255,255,255);
      
      /*************************** AGENT HAS RECIEVED A MESSAGE - THIS SHOULD BE A HEARTBEAT ****************************************/
      char myPayload[1000]; // Ensure the array is large enough to hold the final string
      myPayload[0] = '\0';
      for (size_t i = 0; i < length; i++) 
      {
          char temp[2] = {(char)payload[i], '\0'};
          strcat(myPayload, temp);
      }
      //Serial.println(myPayload); 
      takeAction(myPayload);

      myPayload[0] = '\0';

      //Serial.print(payload);
			// send message to server
			// webSocket.sendTXT("message here");
			break;
		case WStype_BIN:
			//USE_SERIAL.printf("[WSc] get binary length: %u\n", length);
			//hexdump(payload, length);

			// send data to server
			// webSocket.sendBIN(payload, length);
			break;
		case WStype_ERROR:			
		case WStype_FRAGMENT_TEXT_START:
		case WStype_FRAGMENT_BIN_START:
		case WStype_FRAGMENT:
		case WStype_FRAGMENT_FIN:
			break;
	}
}



void setup() 
{
  Serial.begin(9600);
  //Wire.begin(D6, D5); /* join i2c bus with SDA=D6 and SCL=D5 of NodeMCU */

  rest_red = 0;
  rest_blue = 0;
  rest_green = 0;
  

  Serial.println("Setting up ESP 32 WROOM");
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(configData.ssid);

  Serial.println("Setting up LED");
  pinMode(PIN_RED,   OUTPUT);
  pinMode(PIN_GREEN, OUTPUT);
  pinMode(PIN_BLUE,  OUTPUT);

  analogWrite(PIN_RED,   0);
  analogWrite(PIN_GREEN, 0);
  analogWrite(PIN_BLUE,  0);

  analogWrite(PIN_RED,   255);
  analogWrite(PIN_GREEN, 0);
  analogWrite(PIN_BLUE,  0);

  delay(1000);

  analogWrite(PIN_RED,   0);
  analogWrite(PIN_GREEN, 255);
  analogWrite(PIN_BLUE,  0);

  delay(1000);

  analogWrite(PIN_RED,   0);
  analogWrite(PIN_GREEN, 0);
  analogWrite(PIN_BLUE,  255);

  delay(1000);



  EEPROM.begin(512);
  // read bytes (i.e. sizeof(configData) from "EEPROM"),
  // in reality, reads from byte-array cache
  // cast bytes into structure called data
  EEPROM.get(addr,configData);
  
  Serial.println("EPROM values are: "+String(configData.ssid)+","+String(configData.ss_password));
  WiFi.begin(String(configData.ssid), String(configData.ss_password));
  
  int allowedConnectTime = 24;
  int j = 0;
  
  while (WiFi.status() != WL_CONNECTED && j <= allowedConnectTime)
  {
    delay(500);
    Serial.print(".");
    flashRGB(255,255,255);
    j = j+1;

    

    if(j == allowedConnectTime)
    {
      

      /*basically at this point the wifi is not connecting, so moving towards a default hotspot*/
      Serial.println("Unable to connect to configured wifi - moving to reserve connectivity");
      Serial.println("Will try to connect to SSID: ");
      Serial.print(ssid); 
      Serial.print(", PASSWORD:");
      Serial.print(ss_password);
      WiFi.begin(ssid, ss_password);
      while (WiFi.status() != WL_CONNECTED)
      {
         delay(500);
         Serial.print(".");
         flashRGB(100,100,100);   
      }
    }
  }
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println(WiFi.localIP()); 

  WORKER_IP_ADDRESS = WiFi.localIP().toString();
  
  //Default light state
  rest_red = 0;
  rest_green = 255;                                                                               // When a network connection is made the GREEN LED stays on.
  rest_blue = 0;

  server.on("/", HTTP_GET, [](AsyncWebServerRequest *request)
  {
    request->send_P(200, "text/html", index_html, processor);
  });
  //serve up the latest data as a JSON object
  server.on("/data", HTTP_GET, [] (AsyncWebServerRequest *request) 
  {
    request->send_P(200, "application/json", stream_html, processor);
  });
  //restart the NODEMCU
  server.on("/reset", HTTP_GET, [](AsyncWebServerRequest *request)
  {
    //ESP.reset(); 
  });
  // Send a GET request to <ESP_IP>/update?setting=<setting>&svalue=<svalue>
  server.on("/update", HTTP_GET, [] (AsyncWebServerRequest *request) 
  {
    String setting;
    String svalue;
    // GET input1 value on <ESP_IP>/update?setting=<setting>&svalue=<svalue>
    if (request->hasParam(PARAM_INPUT_1) && request->hasParam(PARAM_INPUT_2)) 
    {
      setting = request->getParam(PARAM_INPUT_1)->value();
      svalue = request->getParam(PARAM_INPUT_2)->value();
      Serial.print("Recieved setting :");
      Serial.print(setting);
      Serial.print("Recieved svalue: ");
      Serial.println(svalue);
      
      if(setting == "ssid")
      {
        Serial.println("Updating ssid value");
        char svalueBuff[20];
        svalue.toCharArray(svalueBuff, 20);
        Serial.println("ssid value updated");
        strncpy(configData.ssid, svalueBuff, 20);
      }
      if(setting == "ss_password")
      {
        Serial.println("Updating password value");
        char svalueBuff[20];
        svalue.toCharArray(svalueBuff, 20);
        Serial.println("password value updated");
        strncpy(configData.ss_password, svalueBuff, 20);
      }
      if(setting == "endpointName")
      {
        Serial.println("Updating endpointName value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("endpointName value updated");
        strncpy(configData.endpointName, svalueBuff, 50);
      }
      if(setting == "domain")
      {
        Serial.println("Updating domain value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("domain value updated");
        strncpy(configData.domain, svalueBuff, 50);
      }
      if(setting == "protocol")
      {
        Serial.println("Updating protocol value");
        char svalueBuff[10];
        svalue.toCharArray(svalueBuff, 10);
        Serial.println("protocol value updated");
        strncpy(configData.protocol, svalueBuff, 10);
      }
      if(setting == "socket_server_ip")
      {
        Serial.println("Updating socket_server_ip value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("socket_server_ip value updated");
        strncpy(configData.socket_server_ip, svalueBuff, 50);
      }
      if(setting == "socket_server_port")
      {
        Serial.println("Updating socket_server_port value");
        char svalueBuff[8];
        svalue.toCharArray(svalueBuff, 8);
        Serial.println("socket_server_port value updated");
        strncpy(configData.socket_server_port, svalueBuff, 8);
      }
      if(setting == "endpointUrl")
      {
        Serial.println("Updating endpointUrl value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("endpointUrl value updated");
        strncpy(configData.endpointUrl, svalueBuff, 50);
      }
      if(setting == "poll_frequency")
      {
        Serial.println("Updating poll_frequency value");
        char svalueBuff[3];
        svalue.toCharArray(svalueBuff, 3);
        Serial.println("poll_frequency value updated");
        strncpy(configData.poll_frequency, svalueBuff, 3);
      }
      
      EEPROM.put(addr,configData);
      EEPROM.commit();
    }
    request->send(200, "text/plain", "OK");
  });
  /******************************************/
  server.begin();
  Serial.println("Web server started");

 // "www.jaysboat.com/websocket"
 // "type/access/endpoint"

  //webSocket.begin(configData.socket_server_ip,  atoi(configData.socket_server_port), configData.endpointName);
  String fullEndpoint = strcat(configData.endpointUrl, configData.endpointName);
  Serial.println("fullEndpoint: ");
  Serial.println(fullEndpoint);
  //webSocket.begin(configData.domain, atoi(configData.socket_server_port), fullEndpoint);
  webSocket.begin("www.jaysboat.com", 80, "/websocket/type/access/endpoint");

	webSocket.onEvent(webSocketEvent);
	webSocket.setReconnectInterval(5000);
  Serial.println("Websocket started");
}



//////////////////

String makeTempString(const char* part1, const char* part2) {
  return String(part1) + String(part2);
}

/**************************************************************************************************************************************/
/*
/* Change the code below these lines to suit your sensor.
/*
/***************************************************************************************************************************************/


String generateSensorData()
{
  
  String version = getAgentVersion();
  String data = getAgentData();
  String ip = getAgentIP();
  String endpoint = getAgentEndpoint();

  String jsonData ="{\"version\":";
  jsonData += "\""+version+"\"";
  jsonData += ",\"data\":\"";
  jsonData += data;
  jsonData += "\",\"ip\":";
  jsonData += "\"";
  jsonData += ip;
  jsonData += "\",\"endpoint\":";
  jsonData += "\"";
  jsonData += endpoint;
  jsonData +="\"}";
  

  Serial.println(jsonData);

  
  return jsonData;

}
/************************************************/
void loop() 
{
  webSocket.loop();

  if (webSocket.isConnected()) 
  {
    int pollInterval = atoi(configData.poll_frequency) * 1000;  // e.g. "10" → 10 seconds
    // If time since last send >= poll_interval → send a new message
    if (millis() - lastSendTime >= pollInterval) 
    {
      String hold = generateSensorData();
      
      webSocket.sendTXT(hold);
      flashRGB(0,0,255);

      lastSendTime = millis();  // reset timer
    }
  
  } 
  else 
  {
    Serial.println("websocket not connected yet...");
    flashRGB(255,0,0);
  }
  
  
}

void flashRGB(int red, int green, int blue)
{
    analogWrite(PIN_RED,   red);
    analogWrite(PIN_GREEN, green);
    analogWrite(PIN_BLUE,  blue);

    delay(100);  

    analogWrite(PIN_RED,   rest_red);
    analogWrite(PIN_GREEN, rest_green);
    analogWrite(PIN_BLUE,  rest_blue);
}


