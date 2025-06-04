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

#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>

/* BOF LED */
const int redPin = 13;   
const int greenPin = 12; 
const int bluePin = 14;  

/* BOF Reset Pin */
const int resetPin = 26;


/*BOF COMPASS*/
#include <QMC5883LCompass.h>

WebSocketsClient webSocket;

// Mode Control (MODE)
const byte qmc5883l_mode_stby = 0x00;
const byte qmc5883l_mode_cont = 0x01;
const byte qmc5883l_odr_10hz  = 0x00;
const byte qmc5883l_odr_50hz  = 0x04;
const byte qmc5883l_odr_100hz = 0x08;
const byte qmc5883l_odr_200hz = 0x0C;
const byte qmc5883l_rng_2g    = 0x00;
const byte qmc5883l_rng_8g    = 0x10;
const byte qmc5883l_osr_512   = 0x00;
const byte qmc5883l_osr_256   = 0x40;
const byte qmc5883l_osr_128   = 0x80;
const byte qmc5883l_osr_64    = 0xC0;

QMC5883LCompass compass;

#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels

#define OLED_RESET     -1 // Reset pin # (or -1 if sharing Arduino reset pin)
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

#define NUMFLAKES     10 // Number of snowflakes in the animation example

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
  B00000000, B00110000 
};

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
  char calibration_offset[100]="";
  char calibration_scale[100]="";
} configData;


const char stream_html[]  PROGMEM = R"rawliteral(
{"workerName":%workerName%,"sensorURL":%sensorURL%,"socket_server_ip":%socket_server_ip%,"socket_server_port":%socket_server_port%,"calibration_data":%calibration_data%}
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
              <button type="button" onclick="testScript(1)">test socket</button>
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
    rows +="\"></td> (EG: SSID)</tr>";
    
    rows += "<tr><td>ss_password</td><td><input type=\"text\" id=\"ss_password\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.ss_password;
    rows +="\"></td> (EG: PASSWORD)</tr>";

    rows += "<tr><td>workerName</td><td><input type=\"text\" id=\"workerName\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.workerName;
    rows +="\"></td> (EG: compass)</tr>";

    rows += "<tr><td>socket_server_ip</td><td><input type=\"text\" id=\"socket_server_ip\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.socket_server_ip;
    rows +="\"></td> (EG: 192.168.10.239)</tr>";

    rows += "<tr><td>socket_server_port</td><td><input type=\"text\" id=\"socket_server_port\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.socket_server_port;
    rows +="\"></td> (EG: 3200)</tr>";

    rows += "<tr><td>sensorURL</td><td><input type=\"text\" id=\"sensorURL\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.sensorURL;
    rows +="\"></td> (EG: /reading/compass)</tr>";

    rows += "<tr><td>calibration_offset</td><td><input type=\"text\" id=\"calibration_offset\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.calibration_offset;
    rows +="\"></td> (EG: -729.00,-445.00,1045.00) </tr>";

    rows += "<tr><td>calibration_scale</td><td><input type=\"text\" id=\"calibration_scale\" onchange=\"updateSettings(this)\" value=\"";
    rows += configData.calibration_scale;
    rows +="\"></td> (EG: 0.94, 0.87, 1.27) </tr>";
    

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
/*************************************************************************/
// Function to set the color of the RGB LED
void setColor(int red, int green, int blue) {
  // Use the ledcWrite function to set the PWM duty cycle
  ledcWrite(0, red);
  ledcWrite(1, green);
  ledcWrite(2, blue);
}
/***********************************************/
void setDisplay(boolean invert, int textSize) 
{
  display.invertDisplay(invert);
  display.clearDisplay();
  display.setTextSize(textSize);   
  display.setTextColor(WHITE);        // Draw white text
  display.setCursor(0, 10);           // Start at top-left corner
  display.display();                  //call to display
}
/*******************************************/
void oledDisplayCenter(String text) 
{
  int16_t x1;
  int16_t y1;
  uint16_t width;
  uint16_t height;
  display.getTextBounds(text, 0, 0, &x1, &y1, &width, &height);
  display.clearDisplay(); // clear display
  display.setCursor((SCREEN_WIDTH - width) / 2, (SCREEN_HEIGHT - height) / 2);
  display.println(text); // text to display
  display.display();
}
/*******************************************/
void oledDisplayText(String text, boolean invert, int textSize) 
{
  
  setDisplay(invert, textSize);
  display.println(text); // text to display
  display.display();
}
/*************************************************/
void parseCSV(char *data, float *values) {
  char *token; // Pointer to store the extracted token
  int index = 0; // Counter for values

  // Extract the first token
  token = strtok(data, ",");
  
  // Keep extracting tokens until no more tokens are found
  while (token != NULL) {
    // Print the extracted value
    Serial.print("Value ");
    Serial.print(index);
    Serial.print(": ");
    Serial.println(token);
    values[index] = atof(token);
    // Move to the next token
    token = strtok(NULL, ",");
    index++;
  }
}
/***********************************************/
void setup() 
{
  Serial.begin(9600);
  //Wire.begin(D6, D5); /* join i2c bus with SDA=D6 and SCL=D5 of NodeMCU */

  /**/
  // Initialize the reset pin as an input
  pinMode(resetPin, INPUT_PULLDOWN); // Use the internal pull-down resistor
  Serial.println("ESP32 Reset on High");



  /* BOFLED */
/*
  ledcSetup(0, 5000, 8); // Channel 0, 5000 Hz, 8-bit resolution
  ledcSetup(1, 5000, 8); // Channel 1, 5000 Hz, 8-bit resolution
  ledcSetup(2, 5000, 8); // Channel 2, 5000 Hz, 8-bit resolution

  ledcAttachPin(redPin, 0);   // Attach red pin to channel 0
  ledcAttachPin(greenPin, 1); // Attach green pin to channel 1
  ledcAttachPin(bluePin, 2);  // Attach blue pin to channel 2

*/
  /* EOF LED */
  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) 
  { 
    Serial.println(F("SSD1306 allocation failed"));
    for(;;); // Don't proceed, loop forever
  }
  // Clear the buffer
  display.clearDisplay();
  setDisplay(false, 2);
  oledDisplayCenter("Jay Marine");
  delay(1500);

  Serial.println("Setting up ESP 32 WROOM");
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(configData.ssid);

  EEPROM.begin(512);
  // read bytes (i.e. sizeof(configData) from "EEPROM"),
  // in reality, reads from byte-array cache
  // cast bytes into structure called data
  EEPROM.get(addr,configData);
  
 
  float x_calibration_offset = 0.00;
  float y_calibration_offset = 0.00;
  float z_calibration_offset = 0.00;
  /*
  float values[3];
  parseCSV(configData.calibration_offset, values);
  int numValues = sizeof(values) / sizeof(values[0]);
  
  
  for(int i = 0; i < numValues; i++)
  {
    Serial.print("EPORM Value ");
    Serial.print(i);
    Serial.print(": ");
    Serial.println(values[i], 2); // Print each value with 2 decimal places
    if(i == 0)
    {
      x_calibration_offset = values[i];
    }
    if(i == 1)
    {
      y_calibration_offset = values[i];
    }
    if(i == 2)
    {
      z_calibration_offset = values[i];
    }
  
  }
  */
  compass.init();
   //compass.setADDR(byte b);
   //compass.setMode(byte mode,          byte odr,           byte rng,        byte osr        );
   //compass.setMode(qmc5883l_mode_cont, qmc5883l_odr_200hz, qmc5883l_rng_8g, qmc5883l_osr_512);
   //compass.setSmoothing(byte steps, bool adv);
   //compass.setCalibration(int x_min, int x_max, int y_min, int y_max, int z_min, int z_max);
  compass.setCalibrationOffsets(x_calibration_offset, y_calibration_offset, z_calibration_offset);
  compass.setCalibrationScales(0.94, 0.87, 1.26);

  Serial.println("EPROM values are: "+String(configData.ssid)+","+String(configData.ss_password));

  oledDisplayText("Connecting: " + String(configData.ssid), false, 2);
  delay(500);
  WiFi.begin(String(configData.ssid), String(configData.ss_password));

 
  
  int allowedConnectTime = 24;
  int j = 0;
  
  while (WiFi.status() != WL_CONNECTED && j <= allowedConnectTime)
  {
    setColor(255, 0, 0); // Red
    delay(500);
    setColor(0, 0, 0); // Green
    Serial.print(".");
    j = j+1;
    if(j == allowedConnectTime)
    {
      /*basically at this point the wifi is not connecting, so moving towards a default hotspot*/
      Serial.println("Unable to connect to configured wifi - moving to reserve connectivity");
      Serial.println("Will try to connect to SSID: ");
      Serial.print(ssid); 
      oledDisplayText("Connecting BKUP: " + String(ssid), false, 2);
      delay(500);
      Serial.print(", PASSWORD:");
      Serial.print(ss_password);
      WiFi.begin(ssid, ss_password);
      while (WiFi.status() != WL_CONNECTED)
      {
          setColor(0, 0, 255); // Blue
          delay(500);
          setColor(0, 0, 0); // Green
         Serial.print(".");
      }
    }
  }
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println(WiFi.localIP()); 

  WORKER_IP_ADDRESS = WiFi.localIP().toString();

  oledDisplayText(WORKER_IP_ADDRESS, false, 2);
  delay(1000);

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
      if(setting == "calibration_offset")
      {
        Serial.println("Updating calibration_offset value");
        char svalueBuff[100];
        svalue.toCharArray(svalueBuff, 100);
        Serial.println("calibration_offset value updated");
        strncpy(configData.calibration_offset, svalueBuff, 100);
      }
      if(setting == "calibration_scale")
      {
        Serial.println("Updating calibration_scale value");
        char svalueBuff[100];
        svalue.toCharArray(svalueBuff, 100);
        Serial.println("calibration_scale value updated");
        strncpy(configData.calibration_scale, svalueBuff, 100);
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
void calibrateCompass()
{
  Serial.println("Calibration will begin in 5 seconds.");
  delay(5000);

  Serial.println("CALIBRATING. Keep moving your sensor...");
  compass.calibrate();

  Serial.println("DONE. Copy the lines below and paste it into your projects sketch.);");
  Serial.println();
  Serial.print("compass.setCalibrationOffsets(");
  Serial.print(compass.getCalibrationOffset(0));
  Serial.print(", ");
  Serial.print(compass.getCalibrationOffset(1));
  Serial.print(", ");
  Serial.print(compass.getCalibrationOffset(2));
  Serial.println(");");
  Serial.print("compass.setCalibrationScales(");
  Serial.print(compass.getCalibrationScale(0));
  Serial.print(", ");
  Serial.print(compass.getCalibrationScale(1));
  Serial.print(", ");
  Serial.print(compass.getCalibrationScale(2));
  Serial.println(");");
}
/**************************************/
void sensor_data() 
{
 
}
/**************************************/
int getCompassReading()
{
    int x_value;
   int y_value;
   int z_value;
   int azimuth;  // 0° - 359°
   byte bearing; // 0 - 15 (N, NNE, NE, ENE, E, ...)
   char direction[strlen("NNE") + 1];
   char buffer[strlen("X=-99999 | Y=-99999 | Z=-99999 | A=259° | B=15 | D=NNE") + 1]; 
   
   compass.read(); // Read compass values via I2C

   x_value   = compass.getX();
   y_value   = compass.getY();
   z_value   = compass.getZ();
   azimuth   = compass.getAzimuth(); // Calculated from X and Y value 
   bearing   = compass.getBearing(azimuth);
   
   compass.getDirection(direction, azimuth);
   direction[3] = '\0';
/*
   sprintf(buffer,
           "X=%6d | Y=%6d | Z=%6d | A=%3d° | B=%02hu | %s",
           x_value,
           y_value,
           z_value,
           azimuth,
           bearing,
           direction                                           );
   Serial.println(buffer);
*/
  
   return azimuth;
}
/************************************************/
void loop() 
{
  webSocket.loop();

  int pinState = digitalRead(resetPin);

  // Check if the pin is HIGH
  if (pinState == HIGH) {
    Serial.println("Reset pin is HIGH, resetting ESP32...");
    delay(100); // Small delay to allow serial message to be sent

    // Perform a software reset
    esp_restart();
  }


  /*
  setColor(255, 0, 0); // Red
  delay(1000);
  setColor(0, 255, 0); // Green
  delay(1000);
  setColor(0, 0, 255); // Blue
  delay(1000);
  setColor(255, 255, 0); // Yellow
  delay(1000);
  setColor(0, 255, 255); // Cyan
  delay(1000);
  setColor(255, 0, 255); // Magenta
  delay(1000);
  setColor(255, 255, 255); // White
  delay(1000);
  setColor(0, 0, 0); // Off
  delay(1000);
  */

  setDisplay(false, 4);
  
   //int randNumber = random(0, 359);
  
  int azimuth = getCompassReading();
  azimuth = (azimuth + 180) % 360;

  String str = String(azimuth);
  oledDisplayCenter(str);

 
  String jsonData ="{\"version\":";
  jsonData += "\"1\"";
  jsonData += ",\"data\":";
  jsonData += str; //generateTestData_intAsString();
  jsonData += ",\"worker_ip\":";
  jsonData += "\"";
  jsonData += WORKER_IP_ADDRESS;
  jsonData += "\"";
  jsonData +="}";

  setColor(0, 0, 255); // Green
  bool sent = webSocket.sendTXT(jsonData);
  Serial.print("message successfully sent: " + sent);
  
  if(sent != true)
  {
        oledDisplayText("Socket Error: Retrying", false, 2);
        setColor(255, 0, 0); // Green
  }
  delay(500);
  setColor(0, 0, 0); // Green
  //setDisplay(false, 2);
  //oledDisplayCenter(WORKER_IP_ADDRESS);
  
  //delay(500);
 
}

