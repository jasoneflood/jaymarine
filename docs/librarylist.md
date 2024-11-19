1 Removed ALL libraries from Arduino IDE
The following did not report any issues, so may be installed by default
#include <SPI.h>                // default library?
#include <Wire.h>               // default library?
#include <WiFi.h>               // default library?


The following reported errors (No such file or directory) and were addressed by installing the indicted libraries\
#include <AsyncTCP.h>         // dvarell, https://github.com/dvarrel/AsyncTCP\
#include <AsyncTCP.h>           // mathieucarbou, https://github.com/mathieucarbou/AsyncTCP\
#include <ESPAsyncWebServer.h>  // mathieucarbou,, https://github.com/mathieucarbou/ESPAsyncWebServer\
#include <HTTPClient.h>         // ?? included with above libraries?\
// #include <EEPROM.h>          // replaced by the following\
#include <Preferences.h>        // Volodymyr Shymanskyy, https://github.com/vshymanskyy/Preferences\
#include <ArduinoJson.h>        // https://arduinojson.org/?utm_source=meta&utm_medium=library.properties\
#include <WebSocketsClient.h>   // Markus Sattler, https://github.com/Links2004/arduinoWebSockets\

#include <Adafruit_GFX.h>       // Adafruit, https://github.com/adafruit/Adafruit-GFX-Library\
#include <Adafruit_SSD1306.h>   // Adafruit, https://github.com/adafruit/Adafruit_SSD1306\
#include <esp32-hal-ledc.h>     // Appears to be built-in or carried by one of the other libraries\

/*BOF COMPASS*/
#include <QMC5883LCompass.h>    // MPrograms, https://github.com/mprograms/QMC5883LCompass\
