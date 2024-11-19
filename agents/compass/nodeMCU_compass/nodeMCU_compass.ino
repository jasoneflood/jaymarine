/*********
 Author: Jason Flood
 Codebase: Autopilot_v0.1a 
 The ESP8266 uses the pins gpio0 (D3), gpio1 (TX), gpio2 (D4), gpio3 (RX) and gpio15 (D8) in its boot process, and there is nothing that can be done about
 Components - GY-273
*********/

#include <SPI.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_HMC5883_U.h>
#include <HMC5883L_Simple.h>
#include "DHT.h"
#include <ESP8266WiFi.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <ESP8266HTTPClient.h>
#include <EEPROM.h>
#include <ArduinoJson.h>
#include <WebSocketsClient.h>


WebSocketsClient webSocket;

HMC5883L_Simple Compass;

//#include "webdata.h"

//Set Input and Output Pins
#define JoystickOverRide 16 //D0 (high on boot)
#define TurnA 5 // D01
#define TurnB 4 // D02
//d5 & D6 used as SDA/SDK
#define OnCourse 13 //D07

boolean TurnAState = 0;
boolean TurnBState = 0;

/* BOF LOGO */
#define LOGO_HEIGHT   16
#define LOGO_WIDTH    16
static const unsigned char PROGMEM logo_bmp[] =
{ B00000000, B11000000,
  B00000001, B11000000,
  B00000001, B11000000,
  B00000011, B11100000,
  B11110011, B11100000,
  B11111110, B11111000,
  B01111110, B11111111,
  B00110011, B10011111,
  B00011111, B11111100,
  B00001101, B01110000,
  B00011011, B10100000,
  B00111111, B11100000,
  B00111111, B11110000,
  B01111100, B11110000,
  B01110000, B01110000,
  B00000000, B00110000 };

/* EOF LOGO */

/* BOF Temp pressure sensor */
DHT dht2(2,DHT11);  
float TEMPERATURE = 0.0;
float HUMIDITY = 0.0;
/* Slow the read time for the humidity sensor so set these variables to control the reads*/
unsigned long previousMillis = 0;
unsigned long interval = 5000;
int a = 60;

/* EOF Temp pressure sensor */

/* BOF OLED SCREEN */
#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels
#define OLED_RESET -1 // Reset pin # (or -1 if sharing Arduino reset pin)// Declaration for an SSD1306 display connected to I2C (SDA = D02, SCL = D01 pins)
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

/* EOF OLED SCREEN */

/* BOF MAG Sensor */
Adafruit_HMC5883_Unified mag = Adafruit_HMC5883_Unified(12345);// Assign a unique ID to this sensor at the same time 
/* Define declination of location from where measurement going to be done. 
e.g. here we have added declination from location Pune city, India. 
we can get it from http://www.magnetic-declination.com  - and convert the angle to radians*/
#define Declination       -0.01745
#define hmc5883l_address  0x1E
float XDATA = 0.0;
float YDATA = 0.0;
float ZDATA = 0.0;
float HEADINGDATA = 0.0;
float DESIREDHEADINGDATA= 0.0;
/* EOF MAG Sensor */


/* BOF WIFI/NETWORK variables */
const char* ssid = "suttonzoo";
const char* ss_password = "Happyh1pp0";
String IP_ADDRESS = "0.0.0.0";
AsyncWebServer server(80); // The HTTP Server is run on port 80
#define USE_SERIAL Serial1
/* EOF WIFI/NETWORK variables */


/* BOF Variables for Autopilot Admin */ 
const char* PARAM_INPUT_1 = "setting";
const char* PARAM_INPUT_2 = "svalue";

uint addr = 0;
struct {
  uint val = 0;
  char sensorURL[50] = "";
  char ssid[20] = "";
  char ss_password[20] = "";
  char autopilotOn[2] = "0";
  float x_offset = 0.0;
  float y_offset = 0.0;
  float z_offset = 0.0;
  float heading_offset = 0.0;
  float desired_heading = 0.0;
  
} configData;

char x_offset[8]; // Buffer big enough for 7-character float
char y_offset[8];
char z_offset[8];
char heading_offset[8];
char desired_heading[8];

const char stream_html[]  PROGMEM = R"rawliteral(
{"XValue":%XDATA%,"YValue":%YDATA%,"ZValue":%ZDATA%,"HeadingValue":%HEADINGDATA%, "DesiredHeading": %DESIREDHEADINGDATA% }
)rawliteral";
/*************************************************/

const char script_var[] PROGMEM = R"rawliteral(
  let socket = new WebSocket("ws://%IP_ADDRESS%:81");
  function changeCourse(degree)
  {
    console.log("Changing course by : " + degree);
    var desiredHeading;
    var x;
    var y;
    var z;
    var version;
    var heading;
    
    var versiondata = document.getElementById("version");
    var desiredheadingdata = document.getElementById("desiredHeadingData");
    var xdata = document.getElementById("xdata");
    var ydata = document.getElementById("ydata");
    var zdata = document.getElementById("zdata");
    var headingdata = document.getElementById("headingdata");
    
    //lets just set the desired heading to the current direction as a starting point
    //desiredheadingdata.innerHTML = desiredHeading;
    heading = headingdata.innerHTML;
    x = xdata.innerHTML;
    y = ydata.innerHTML;
    z = zdata.innerHTML;
    version = versiondata.innerHTML;
    desiredHeading = desiredheadingdata.innerHTML;
    console.log("desiredHeading:" + desiredHeading);
    desiredHeading = ((parseInt(desiredHeading) + parseInt((degree)))%360);
    console.log("desiredHeading update:" + desiredHeading);

    /*
     * {"version":"Autopilot_v01a","X":-25.45,"Y":-65.18,"Z": 22.04,"HEADING":200.33,"DesiredHeadingData":  0.00}
     */
    
    
    //console.log("Sending to server: {\"version\":\""+version+"\",\"X\":"+x+",\"Y\":"+y+",\"Z\":"+z+",\"HEADING\":"+heading+",\"DesiredHeadingData\":"+desiredHeading+"}");
    socket.send("{\"version\":\""+version+"\",\"X\":"+x+",\"Y\":"+y+",\"Z\":"+z+",\"HEADING\":"+heading+",\"DesiredHeadingData\":"+desiredHeading+"}");
  }
  
  
  function setCourse()
  {
    
    var desiredHeading;
    var x;
    var y;
    var z;
    var version;
    var heading;
    
    var versiondata = document.getElementById("version");
    var desiredheadingdata = document.getElementById("desiredHeadingData");
    var xdata = document.getElementById("xdata");
    var ydata = document.getElementById("ydata");
    var zdata = document.getElementById("zdata");
    var headingdata = document.getElementById("headingdata");
    
    //lets just set the desired heading to the current direction as a starting point
    //desiredheadingdata.innerHTML = desiredHeading;
    heading = headingdata.innerHTML;
    x = xdata.innerHTML;
    y = ydata.innerHTML;
    z = zdata.innerHTML;
    version = versiondata.innerHTML;
    desiredHeading = headingdata.innerHTML;
    
    /*
     * {"version":"Autopilot_v01a","X":-25.45,"Y":-65.18,"Z": 22.04,"HEADING":200.33,"DesiredHeadingData":  0.00}
     */
    
    //console.log("Sending to server: {\"version\":\""+version+"\",\"X\":"+x+",\"Y\":"+y+",\"Z\":"+z+",\"HEADING\":"+heading+",\"DesiredHeadingData\":"+desiredHeading+"}");
    socket.send("{\"version\":\""+version+"\",\"X\":"+x+",\"Y\":"+y+",\"Z\":"+z+",\"HEADING\":"+heading+",\"DesiredHeadingData\":"+desiredHeading+"}");
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
  socket.onmessage = function(event) {
    //{"version":"Autopilot_v01a","X":-25.45,"Y":-65.18,"Z": 22.04,"HEADING":200.33,"DesiredHeadingData":  0.00}
   // console.log(`${event.data}`);
    const obj = JSON.parse(event.data);
    var heading = obj.HEADING;
    var desiredHeading = obj.DesiredHeadingData;
    //console.log(`Desired Heading Data: ${obj.DesiredHeadingData}`);
    var x = obj.X;
    var y = obj.Y;
    var z = obj.Z;
    var version = obj.version;

    
    //console.log(`heading: ${obj.HEADING}`);
    //console.log(`desiredHeading: ${obj.DesiredHeadingData}`);
    //console.log(`x: ${obj.X}`);
    //console.log(`y: ${obj.Y}`);
    //console.log(`z: ${obj.Z}`);
    //console.log(`version: ${obj.version}`);
  
    var versiondata = document.getElementById("version");
    var desiredheadingdata = document.getElementById("desiredHeadingData");
    var xdata = document.getElementById("xdata");
    var ydata = document.getElementById("ydata");
    var zdata = document.getElementById("zdata");
    var headingdata = document.getElementById("headingdata");

    desiredheadingdata.innerHTML = desiredHeading;
    xdata.innerHTML = x;
    ydata.innerHTML = y;
    zdata.innerHTML = z;
    versiondata.innerHTML = version;
    headingdata.innerHTML = heading;
    
    var currentHeading = document.getElementById("currentHeading");
    currentHeading.setAttribute("transform", "rotate("+heading+", 150, 150)");

    var desiredHeadingSVG = document.getElementById("desiredHeading");
    desiredHeadingSVG.setAttribute("transform", "rotate("+desiredHeading+", 150, 150)");
    
    
  };
  socket.onclose = function(event) 
  {
    if (event.wasClean) 
    {
      console.log(`[close] Connection closed cleanly, code=${event.code} reason=${event.reason}`);
    } 
    else 
    {
      // e.g. server process killed or network down
      // event.code is usually 1006 in this case
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
              <div id=xdata></div>
              <div id=ydata></div>
              <div id=zdata></div>
              <div id=headingdata></div>
              <div id=desiredHeadingData></div>
            </div>
            
                <svg height=300 width=300>
                  <g class="currentHeading" id="currentHeading">
                    <circle cx="150" cy="150" r="130" stroke="black" stroke-width="3" fill="grey"/>
                    <text x="155" y="15" fill=rgb(8, 112, 177)>N</text>
                    <text x="290" y="145" fill=rgb(8, 112, 177)>E</text>
                    <text x="155" y="295" fill=rgb(8, 112, 177)>S</text>
                    <text x="0" y="145" fill=rgb(8, 112, 177)>W</text>
                    
                    
                    <line x1="0" y1="150" x2="300" y2="150" style="stroke:rgb(8, 112, 177);stroke-width:3" />
                    <line x1="150" y1="0" x2="150" y2="300" style="stroke:rgb(8, 112, 177);stroke-width:3" />
                  </g>
                  
                  <g class="desiredHeading" id="desiredHeading">
                    <circle cx="150" cy="150" r="110" stroke="black" stroke-width="3" fill="white" fill-opacity="0.4"/>
                    <text x="155" y="55" fill="red">N</text>
                    <text x="245" y="145" fill="red">E</text>
                    <text x="155" y="255" fill="red">S</text>
                    <text x="45" y="145" fill="red">W</text>
                    
                    
                    <line x1="30" y1="150" x2="270" y2="150" style="stroke:red;stroke-width:3" />
                    <line x1="150" y1="30" x2="150" y2="270" style="stroke:red;stroke-width:3" />
                  </g>
                 </svg>
  
      <div id=controls>
          <button type="button" onclick="changeCourse(1)">Course +1</button>
          <button type="button" onclick="setCourse()">Set Course</button>
          <button type="button" onclick="changeCourse(-1)">Course -1</button>
      </div>
      <div id=GPS></div>
    </div>
  </div>

<script>
 %SCRIPT_VAR%
</script>
</body>
</html>
)rawliteral";
/* EOF Variables for Autopilot Admin */ 






/***********************************************/
/* This is the processor function. This is used
 * as a way to update the HTML at render time. 
 ***********************************************/  
String processor(const String& var)
{
    String gpsData ="";
  //Serial.println(var);
  if(var == "STYLE_VAR")
  {
      Serial.println("integrating the STYLES variable");
      return style_var;
  }
  
  
  if(var == "SCRIPT_VAR")
  {
    Serial.println("integrating the SCRIPT variable");
    return script_var;
  }

  if(var == "IP_ADDRESS")
  {
    return IP_ADDRESS;
  }

  if(var == "ROWPLACEHOLDER_VAR")
  {
    String rows = "";
    dtostrf(configData.x_offset, 6, 2, x_offset);
    dtostrf(configData.y_offset, 6, 2, y_offset);
    dtostrf(configData.z_offset, 6, 2, z_offset);
    dtostrf(configData.heading_offset, 6, 2, heading_offset);
    
    rows += "<tr><td>ssid</td><td><input type=\"text\" id=\"ssid\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.ssid;
    rows +="\"></td></tr>";
    
    rows += "<tr><td>ss_password</td><td><input type=\"text\" id=\"ss_password\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.ss_password;
    rows +="\"></td></tr>";

    rows += "<tr><td>x_offset</td><td><input type=\"text\" id=\"x_offset\" onchange=\"updateSettings(this)\" value=\"";
    rows += x_offset;
    rows +="\"></td></tr>";

    rows += "<tr><td>y_offset</td><td><input type=\"text\" id=\"y_offset\" onchange=\"updateSettings(this)\" value=\"";
    rows += y_offset;
    rows +="\"></td></tr>";

    rows += "<tr><td>z_offset</td><td><input type=\"text\" id=\"z_offset\" onchange=\"updateSettings(this)\" value=\"";
    rows += z_offset;
    rows +="\"></td></tr>";

    rows += "<tr><td>heading_offset</td><td><input type=\"text\" id=\"heading_offset\" onchange=\"updateSettings(this)\" value=\"";
    rows += heading_offset;
    rows +="\"></td></tr>";

    rows += "<tr><td>autopilotOn</td><td><input type=\"text\" id=\"autopilotOn\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.autopilotOn;
    rows +="\"></td></tr>";
    return rows;
  }
  if(var == "GPSPLACEHOLDER")
  {
      gpsData += "<div id=\"GPS\"><a href=\"http://maps.google.com/maps?&z=15&mrt=yp&t=k&q=\">Click here!</a> To check the location in Google maps.</div>";
      return gpsData;
  }
  if(var == "XDATA")
  {
    return String(XDATA,4);
  }
  if(var == "YDATA")
  {
    return String(YDATA,4);
  }
  if(var == "ZDATA")
  {
    return String(ZDATA,4);
  }
  if(var == "HEADINGDATA")
  {
    return String(HEADINGDATA,3);
  }
  if(var == "DESIREDHEADINGDATA")
  {
    return String(DESIREDHEADINGDATA,3);
  }
  
  if(var == "JSONDATA")
  {
    String jsonData = "cat\":\"dog\"";
    Serial.println("jsonData: ");
    Serial.println(jsonData);
    return jsonData;
  }
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
			USE_SERIAL.printf("[WSc] get binary length: %u\n", length);
			hexdump(payload, length);

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
/***********************************************/
/* This code displays the sensor details
 *  
 ***********************************************/
void displaySensorDetails(void)
{
  sensor_t sensor;
  mag.getSensor(&sensor);
  Serial.println("------------------------------------");
  Serial.print  ("Sensor:       "); Serial.println(sensor.name);
  Serial.print  ("Driver Ver:   "); Serial.println(sensor.version);
  Serial.print  ("Unique ID:    "); Serial.println(sensor.sensor_id);
  Serial.print  ("Max Value:    "); Serial.print(sensor.max_value); Serial.println(" uT");
  Serial.print  ("Min Value:    "); Serial.print(sensor.min_value); Serial.println(" uT");
  Serial.print  ("Resolution:   "); Serial.print(sensor.resolution); Serial.println(" uT");  
  Serial.println("------------------------------------");
  Serial.println("");
  delay(500);
}
  

void setup() 
{
  Serial.begin(9600);
  Wire.begin(D6, D5); /* join i2c bus with SDA=D6 and SCL=D5 of NodeMCU */

  Serial.println("Setting up NODE MCU");
  
  
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(configData.ssid);

   EEPROM.begin(512);
  // read bytes (i.e. sizeof(configData) from "EEPROM"),
  // in reality, reads from byte-array cache
  // cast bytes into structure called data
  EEPROM.get(addr,configData);
  //Set the autopilot to off when starting up for the first time - reset its last state back to zero.
  char svalueBuff[2] = "0";
  strncpy(configData.autopilotOn, svalueBuff, 2);
  
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
  IP_ADDRESS = WiFi.localIP().toString();
  //handle the index page
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
    ESP.reset(); 
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
      if(setting == "autopilotOn")
      {
        
        Serial.println("Updating autopilotOn value");
        char svalueBuff[2];
        svalue.toCharArray(svalueBuff, 2);
        Serial.println("autopilotOn value updated");
        strncpy(configData.autopilotOn, svalueBuff, 20);
      }

      if(setting == "x_offset")
      {
        
        Serial.println("Updating x_offset value");
        configData.x_offset = svalue.toFloat();
        Serial.println("x_offset value updated");
      }
      if(setting == "y_offset")
      {
        
        Serial.println("Updating y_offset value");
        configData.y_offset = svalue.toFloat();
        Serial.println("y_offset value updated");
      }
      if(setting == "z_offset")
      {
        
        Serial.println("Updating z_offset value");
        configData.z_offset = svalue.toFloat();
        Serial.println("z_offset value updated");
      }
      if(setting == "heading_offset")
      {
        
        Serial.println("Updating heading_offset value");
        configData.heading_offset = svalue.toFloat();
        Serial.println("heading_offset value updated");
      }
      EEPROM.put(addr,configData);
      EEPROM.commit();
    }
    request->send(200, "text/plain", "OK");
    
  });

  server.begin();
  Serial.println("Web server started");


  webSocket.begin("192.168.10.240", 3200, "/reading/compass");
	webSocket.onEvent(webSocketEvent);
	webSocket.setReconnectInterval(5000);
  Serial.println("Websocket started");
  
  //webSocket.onEvent(webSocketEvent);

  /*Serial.println("HMC5883 Magnetometer Test"); Serial.println("");
  
  if(!mag.begin())
  {
    //There was a problem detecting the HMC5883 ... check your connections
    Serial.println("Ooops, no HMC5883 detected ... Check your wiring!");
    while(1);
  }

  //Display some basic information on this sensor 
  displaySensorDetails();

  // SSD1306_SWITCHCAPVCC = generate display voltage from 3.3V internally
  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) 
  { 
    Serial.println(F("SSD1306 allocation failed"));
    for(;;); // Don't proceed, loop forever
  }

  // Show initial display buffer contents on the screen --
  // the library initializes this with an Adafruit splash screen.
  display.display();
  delay(2000); // Pause for 2 seconds

  // Clear the buffer
  display.clearDisplay();

  // Draw a single pixel in white
  display.drawPixel(10, 10, WHITE);
  // Show the display buffer on the screen. You MUST call display() after
  // drawing commands to make them visible on screen!
  display.display();
  delay(2000);
  // display.display() is NOT necessary after every single drawing command,
  // unless that's what you want...rather, you can batch up a bunch of
  // drawing operations and then update the screen all at once by calling
  // display.display(). These examples demonstrate both approaches...
  */
}

void loop() 
{
  webSocket.loop();
  String jsonData ="{\"version\":\"compass_[builddate]\",\"heading\":";
  jsonData += generateTestHeading();
  jsonData += ",\"worker_ip\":";
  jsonData += "\"";
  jsonData += IP_ADDRESS;
  jsonData += "\"";
  jsonData +="}";
  webSocket.sendTXT(jsonData);
  //webSocket.broadcastTXT(jsonData);
  delay(150);
}

String generateTestHeading()
{
     int random_heading = random(0,361); //returns a number between 0 and 360
     String heading = String(random_heading);
     return heading;
}

String IpAddress2String(IPAddress ipAddress)
{
  return String(ipAddress[0]) + String(".") +\
  String(ipAddress[1]) + String(".") +\
  String(ipAddress[2]) + String(".") +\
  String(ipAddress[3])  ;
}

void sensor_data() 
{
 
}