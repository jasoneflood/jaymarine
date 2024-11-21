#include <QMC5883LCompass.h>

/*********
 Author: Jason Flood
 Codebase: esp32_workerTemplate_v1
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

const char* ssid = "jaymarine";
const char* ss_password = "passpass";

String WORKER_IP_ADDRESS = "0.0.0.0";

String SOCKET_SERVER_IP_ADDRESS = "0.0.0.0";
String SOCKET_SERVER_PORT = "3200";

AsyncWebServer server(80); // The HTTP Server is run on port 80
#define USE_SERIAL Serial1

const char* PARAM_INPUT_1 = "setting";
const char* PARAM_INPUT_2 = "svalue";

uint addr = 0;
struct {
  uint val = 0;
  char workerName[50] = "";
  char sensorURL[50] = "";
  char ssid[20] = "";
  char ss_password[20] = "";
  char socket_server_ip[50]= "";
  char socket_server_port[8]="";
} configData;


const char stream_html[]  PROGMEM = R"rawliteral(
{"workerName":%workerName%,"sensorURL":%sensorURL%,"socket_server_ip":%socket_server_ip%,"socket_server_port":%socket_server_port%}
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
  <title>Auto Pilot Manager</title>
  <style>
   
  </style>
  </head>
  <body>
  <div id=pageContainer>
    <div id=pageContent>
        <div id=pageTitle>Auto Pilot Manager
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
    return SOCKET_SERVER_IP_ADDRESS;
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

    rows += "<tr><td>workerName</td><td><input type=\"text\" id=\"workerName\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.workerName;
    rows +="\"></td></tr>";

    rows += "<tr><td>socket_server_ip</td><td><input type=\"text\" id=\"socket_server_ip\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.socket_server_ip;
    rows +="\"></td></tr>";

    rows += "<tr><td>socket_server_port</td><td><input type=\"text\" id=\"socket_server_port\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.socket_server_port;
    rows +="\"></td></tr>";

    rows += "<tr><td>sensorURL</td><td><input type=\"text\" id=\"sensorURL\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.sensorURL;
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
			break;
		case WStype_CONNECTED:
			USE_SERIAL.printf("[WSc] Connected to url: %s\n", payload);

			// send message to server when Connected
			webSocket.sendTXT("Connected");
			break;
		case WStype_TEXT:
			USE_SERIAL.printf("[WSc] get text: %s\n", payload);

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

  Serial.println("Setting up ESP 32 WROOM");
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(configData.ssid);

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
      }
    }
  }
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println(WiFi.localIP()); 

  WORKER_IP_ADDRESS = WiFi.localIP().toString();
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
      if(setting == "sensorURL")
      {
        Serial.println("Updating sensorURL value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("sensorURL value updated");
        strncpy(configData.sensorURL, svalueBuff, 50);
      }
      if(setting == "workerName")
      {
        Serial.println("Updating workerName value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("workerName value updated");
        strncpy(configData.workerName, svalueBuff, 50);
      }

      
      EEPROM.put(addr,configData);
      EEPROM.commit();
    }
    request->send(200, "text/plain", "OK");
  });
  /******************************************/
  server.begin();
  Serial.println("Web server started");
  webSocket.begin(configData.socket_server_ip, 3200, configData.sensorURL);
	webSocket.onEvent(webSocketEvent);
	webSocket.setReconnectInterval(5000);
  Serial.println("Websocket started");



}
/************************************************/
String generateTestData_intAsString()
{
     int random_int = random(0,361); //returns a number between 0 and 360
     String random_string = String(random_int);
     return random_string;
}
/************************************************/
String IpAddress2String(IPAddress ipAddress)
{
  return String(ipAddress[0]) + String(".") +\
  String(ipAddress[1]) + String(".") +\
  String(ipAddress[2]) + String(".") +\
  String(ipAddress[3])  ;
}
/************************************************/
void sensor_data() 
{
 
}
/************************************************/
void loop() 
{
  webSocket.loop();
  String jsonData ="{\"version\":";
  jsonData += "\"template\"";
  jsonData += ",\"template_data\":";
  jsonData += generateTestData_intAsString();
  jsonData += ",\"worker_ip\":";
  jsonData += "\"";
  jsonData += WORKER_IP_ADDRESS;
  jsonData += "\"";
  jsonData +="}";
  webSocket.sendTXT(jsonData);
  delay(150);
}
