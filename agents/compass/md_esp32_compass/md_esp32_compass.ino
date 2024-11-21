/*****************************************/

/*********
 Author: Jason Flood
 Codebase: esp32_workerTemplate_v1
 Contributor: Mark Deegan
 *********/

#include <SPI.h>                // default library?
#include <Wire.h>               // default library?
#include <WiFi.h>               // default library?
#include <String.h>

#include <AsyncTCP.h>           // mathieucarbou, https://github.com/mathieucarbou/AsyncTCP 
#include <ESPAsyncWebServer.h>  // mathieucarbou,, https://github.com/mathieucarbou/ESPAsyncWebServer
#include <HTTPClient.h>         // ?? included with above libraries?
#include <EEPROM.h>          // replaced by the following
// #include <Preferences.h>        // Volodymyr Shymanskyy, https://github.com/vshymanskyy/Preferences
#include <ArduinoJson.h>        // https://arduinojson.org/?utm_source=meta&utm_medium=library.properties
#include <WebSocketsClient.h>   // Markus Sattler, https://github.com/Links2004/arduinoWebSockets

#include <Adafruit_GFX.h>       // Adafruit, https://github.com/adafruit/Adafruit-GFX-Library
#include <Adafruit_SSD1306.h>   // Adafruit, https://github.com/adafruit/Adafruit_SSD1306
#include <esp32-hal-ledc.h>     // Appears to be built-in or carried by one of the other libraries
// that appeared problematic to the compile

/*BOF COMPASS*/
#include <QMC5883LCompass.h>    // MPrograms, https://github.com/mprograms/QMC5883LCompass

//////////////////////////////////////////////////////////////////////////////////////////
// The following are local includes that remove some settings from being specified in the
// sketch, and place them in external include files within the sketch/src/include folder
////////////////////////////////////////////////////////////////////////////////////////// 
#include "src/include/QMC5833LCompassModes.h" // some defined strings for the compass 
#include "src/include/logo_bmp.h"     // snowflake logo
#include "src/include/script_var.h"   // script variable
#include "src/include/stream_html.h"  // stream variable
#include "src/include/style_var.h"    // style variable
#include "src/include/index_html.h"   // index.html
#include "src/include/WiFiDetails.h"  // default WiFi logon details
#include "src/include/ConfigData.h"


#define DEBUG


#define USE_SERIAL Serial1

// declare myConfigData as an instance of typedef CD
ConfigData  myConfigData;

/* BOF LED */
const int redPin = 13;   
const int greenPin = 12; 
const int bluePin = 14;  

/* BOF Reset Pin */
const int resetPin = 26;

// Colour table gives 
// Black, Blue, Green, Cyan,Red,Magenta, Yellow, White
// values are taken as triples 0,0,0 0,0,255 etc.
char colourTable[24] = 
{0,0,0,0,0,255,0,255,0,0,255,255,255,0,0,255,0,255,255,255,0,255,255,255};
// index into the above colur table.
char colour_index = 0;

// declare w WebSocketClient
WebSocketsClient webSocket;

// declare a compass object
QMC5883LCompass compass;

#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels

#define OLED_RESET     -1 // Reset pin # (or -1 if sharing Arduino reset pin)
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

#define NUMFLAKES     10 // Number of snowflakes in the animation example

String WORKER_IP_ADDRESS = "0.0.0.0";

String SOCKET_SERVER_IP_ADDRESS = "0.0.0.0";
String SOCKET_SERVER_PORT = "3200";

AsyncWebServer server(80); // The HTTP Server is run on port 80


const char* PARAM_INPUT_1 = "setting";
const char* PARAM_INPUT_2 = "svalue";

int resetPinState;

uint addr = 0;


#include "src/include/processor.h"

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
 
  } // end switch
} // end webSocketEvent
/*************************************************************************/

/*************************************************************************/
// Function to set the color of the RGB LED
void setColor(int red, int green, int blue) 
{ // begin method setColor for the RGB LED
  // Use the ledcWrite function to set the PWM duty cycle
  ledcWrite(0, red);
  ledcWrite(1, green);
  ledcWrite(2, blue);
} // end method setColor for the RGB LED
/***********************************************/

/*************************************************************************/
void setDisplay(boolean invert, int textSize) 
{ // begin method to set configuration for the display device
  display.invertDisplay(invert);
  display.clearDisplay();
  display.setTextSize(textSize);   
  display.setTextColor(WHITE);        // Draw white text
  display.setCursor(0, 10);           // Start at top-left corner
  display.display();                  //call to display
} // end method to set configuration for the display device
/*************************************************************************/

/*******************************************/
void oledDisplayCenter(String text) 
{ // begin method to centre display text on the OLED
  int16_t x1;
  int16_t y1;
  uint16_t width;
  uint16_t height;
  display.getTextBounds(text, 0, 0, &x1, &y1, &width, &height);
  display.clearDisplay(); // clear display
  display.setCursor((SCREEN_WIDTH - width) / 2, (SCREEN_HEIGHT - height) / 2);
  display.println(text); // text to display
  display.display();
} // end method to centre display text on the OLED
/*************************************************************************/

/*******************************************/
void oledDisplayText(String text, boolean invert, int textSize) 
{ // begin method to display text on the OLED
  setDisplay(invert, textSize);
  display.println(text); // text to display
  display.display();
}// begin method to display text on the OLED
/*************************************************/

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

/*************************************************/
void setup() 
{ // typical Arduino setup mothod
  Serial.begin(9600);
  //Wire.begin(D6, D5); /* join i2c bus with SDA=D6 and SCL=D5 of NodeMCU */

  /**/
  // Initialize the reset pin as an input
  pinMode(resetPin, INPUT_PULLDOWN); // Use the internal pull-down resistor
  Serial.println("ESP32 Reset on High");



  /* BOFLED */
// MD20241118 Commented out for now, as the hardware setup I am using does not have any display device.
// and there is an issue when building with ledcSetup not defined in the current context
  // ledcSetup(0, 5000, 8); // Channel 0, 5000 Hz, 8-bit resolution
  // ledcSetup(1, 5000, 8); // Channel 1, 5000 Hz, 8-bit resolution
  // ledcSetup(2, 5000, 8); // Channel 2, 5000 Hz, 8-bit resolution

//  ledcAttachPin(redPin, 0);   // Attach red pin to channel 0
//  ledcAttachPin(greenPin, 1); // Attach green pin to channel 1
//  ledcAttachPin(bluePin, 2);  // Attach blue pin to channel 2

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
  Serial.println(myConfigData.ssid);

  EEPROM.begin(512);
  // read bytes (i.e. sizeof(myConfigData) from "EEPROM"),
  // in reality, reads from byte-array cache
  // cast bytes into structure called data
  EEPROM.get(addr,myConfigData);
  
 
  float x_calibration_offset = 0.00;
  float y_calibration_offset = 0.00;
  float z_calibration_offset = 0.00;
  /*
  float values[3];
  parseCSV(myConfigData.calibration_offset, values);
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

  Serial.println("EPROM values are: "+String(myConfigData.ssid)+","+String(myConfigData.ss_password));

  oledDisplayText("Connecting: " + String(myConfigData.ssid), false, 2);
  delay(500);
  WiFi.begin(String(myConfigData.ssid), String(myConfigData.ss_password));

 
  
  int allowedConnectTime = 24;
  int j = 0;
  
  while (WiFi.status() != WL_CONNECTED && j <= allowedConnectTime)
  { // being while loop for WiFi.status != WL_CONNECTED and j <= allowedCOnnectedTime
    setColor(255, 0, 0); // Red
    delay(500);
    setColor(0, 0, 0); // Green
    Serial.print(".");
    j = j+1;
    if(j >= allowedConnectTime)
    { // begin if block for j >= allowedConnectTime
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
      { // start while loop for WiFi.status != WL_CONNECTED
          setColor(0, 0, 255); // Blue
          delay(500);
          setColor(0, 0, 0); // Green
         Serial.print(".");
      } // end while loop for WiFi.status != WL_CONNECTED
    } // end if block for j == allowedConnectTime
  } // being while loop for WiFi.status != WL_CONNECTED and j <= allowedCOnnectedTime
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
        strncpy(myConfigData.ssid, svalueBuff, 20);
      }
      if(setting == "ss_password")
      {
        Serial.println("Updating password value");
        char svalueBuff[20];
        svalue.toCharArray(svalueBuff, 20);
        Serial.println("password value updated");
        strncpy(myConfigData.ss_password, svalueBuff, 20);
      }
      if(setting == "socket_server_ip")
      {
        Serial.println("Updating socket_server_ip value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("socket_server_ip value updated");
        strncpy(myConfigData.socket_server_ip, svalueBuff, 50);
      }
      if(setting == "socket_server_port")
      {
        Serial.println("Updating socket_server_port value");
        char svalueBuff[8];
        svalue.toCharArray(svalueBuff, 8);
        Serial.println("socket_server_port value updated");
        strncpy(myConfigData.socket_server_port, svalueBuff, 8);
      }
      if(setting == "sensorURL")
      {
        Serial.println("Updating sensorURL value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("sensorURL value updated");
        strncpy(myConfigData.sensorURL, svalueBuff, 50);
      }
      if(setting == "workerName")
      {
        Serial.println("Updating workerName value");
        char svalueBuff[50];
        svalue.toCharArray(svalueBuff, 50);
        Serial.println("workerName value updated");
        strncpy(myConfigData.workerName, svalueBuff, 50);
      }
      if(setting == "calibration_offset")
      {
        Serial.println("Updating calibration_offset value");
        char svalueBuff[100];
        svalue.toCharArray(svalueBuff, 100);
        Serial.println("calibration_offset value updated");
        strncpy(myConfigData.calibration_offset, svalueBuff, 100);
      }
      if(setting == "calibration_scale")
      {
        Serial.println("Updating calibration_scale value");
        char svalueBuff[100];
        svalue.toCharArray(svalueBuff, 100);
        Serial.println("calibration_scale value updated");
        strncpy(myConfigData.calibration_scale, svalueBuff, 100);
      }

      
      EEPROM.put(addr,myConfigData);
      EEPROM.commit();
    }
    request->send(200, "text/plain", "OK");
  });
  /******************************************/
  server.begin();
  Serial.println("Web server started");
  webSocket.begin(myConfigData.socket_server_ip, 3200, myConfigData.sensorURL);
  webSocket.onEvent(webSocketEvent);
  webSocket.setReconnectInterval(5000);
  Serial.println("Websocket started");



} // end Arduino setup method
/************************************************/

/************************************************/
String generateTestData_intAsString()
{
     int random_int = random(0,361); //returns a number between 0 and 360
     String random_string = String(random_int);
     return random_string;
}
/************************************************/

/************************************************/
String IpAddress2String(IPAddress ipAddress)
{
  return String(ipAddress[0]) + String(".") +\
  String(ipAddress[1]) + String(".") +\
  String(ipAddress[2]) + String(".") +\
  String(ipAddress[3])  ;
}
/************************************************/

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

/************************************************/
void sensor_data() 
{
 
}
/**************************************/

/************************************************/
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
  
  #ifdef DEBUG
    bearing   = compass.getBearing(azimuth);
    compass.getDirection(direction, azimuth);
    direction[3] = '\0';
    sprintf(buffer,
           "X=%6d | Y=%6d | Z=%6d | A=%3d° | B=%02hu | %s",
           x_value,
           y_value,
           z_value,
           azimuth,
           bearing,
           direction);
    Serial.println(buffer);
  #endif

  return azimuth;
}
/************************************************/

/************************************************/
void loop() 
{ // begin Arduino loop 
  webSocket.loop();

  resetPinState = digitalRead(resetPin);
  // loop through colour_index 0-7;
  colour_index = (colour_index + 1)%8;


  // Check if the pin is HIGH
  if (resetPinState == HIGH) {
    Serial.println("Reset pin is HIGH, resetting ESP32...");
    delay(100); // Small delay to allow serial message to be sent

    // Perform a software reset
    esp_restart();
  }

  #ifdef DEBUG
  setColor(colourTable[colour_index*3], colourTable[(colour_index*3)+1], colourTable[(colour_index*3)+2]);
  delay(1000);
  #endif

  setDisplay(false, 4);
  
   //int randNumber = random(0, 359);
  
  int azimuth = getCompassReading();
  azimuth = (azimuth + 180) % 360;

  String azimuth_str = String(azimuth);
  oledDisplayCenter(azimuth_str);

  // construct the string to be sent to the webSocket below
  String jsonData ="{\"version\":";
  jsonData += "\"1\"";
  jsonData += ",\"data\":";
  jsonData += azimuth_str; //generateTestData_intAsString();
  jsonData += ",\"worker_ip\":";
  jsonData += "\"";
  jsonData += WORKER_IP_ADDRESS;
  jsonData += "\"";
  jsonData +="}";

  setColor(0, 255, 255); // Green

  bool sent = webSocket.sendTXT(jsonData);
  if(sent)
  {
    Serial.print("message successfully sent: " + sent);
    setColor(0,255, 0); // Green
  }

  if(sent != true)
  {
        oledDisplayText("Socket Error: Retrying", false, 2);    
        Serial.print("message successfully sent: " + sent);
        setColor(255, 0, 0); // Red
  }
  delay(1000);
  setColor(0, 0, 0); // OFF, Black
  //setDisplay(false, 2);
  //oledDisplayCenter(WORKER_IP_ADDRESS);
  
  //delay(500);
 
} // end Arduino loop 
