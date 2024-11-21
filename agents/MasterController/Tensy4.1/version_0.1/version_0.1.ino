/*
 
*/

#include <NativeEthernet.h>
#include <ArduinoWebsockets.h>
#include <TeensyID.h>
#include <EEPROM.h>

using namespace websockets;

byte mac[6];

const uint16_t websocketsPort = 3000;

const byte maxSocketClients = 4;

WebsocketsClient socketClients[maxSocketClients];
WebsocketsServer socketServer;
EthernetServer httpServer;
boolean networkConnection;

struct configurationData{
  uint val;
  char id[20];
  char pollUrl[50];
  char hostname[50];
  char wsPort[5];
  char wsPath[50];
  uint8_t padding[2];
};

configurationData configData;

struct readConfigurationData{
  uint val;
  char id[20];
  char pollUrl[50];
  char hostname[50];
  char wsPort[5];
  char wsPath[50];
  uint8_t padding[2];
};

readConfigurationData readConfigData;

constexpr char kHostname[]{"MyHostname"};

int looper;

void setup() {
  looper = 0;

  // Set the MAC address.
  networkConnection = false;
  
  memcpy(&configData.hostname, "maryj", sizeof configData.hostname);
  
  //configData.wifipassword = "passpass";
  EEPROM.put(0x0100, configData);  //storing struct into EEPROM
  EEPROM.get(0x0100, readConfigData); //retrieving the struct from EEPROM
  Serial.println(readConfigData.hostname); 
  
  teensyMAC(mac);

  // Start Serial and wait until it is ready.
  Serial.begin(9600);
  while (!Serial) {}
  Serial.println("Setting up");
  // Connect to ethernet.
  

  // start the Ethernet connection:
  Serial.println("Initialize Ethernet with DHCP:");
  if (Ethernet.begin(mac) == 0) 
  {
    Serial.println("Failed to configure Ethernet using DHCP");
    if (Ethernet.hardwareStatus() == EthernetNoHardware) 
    {
      Serial.println("Ethernet shield was not found.  Sorry, can't run without hardware. :(");
    } 
    else if (Ethernet.linkStatus() == LinkOFF) 
    {
      Serial.println("Ethernet cable is not connected.");
    }
    networkConnection = false;
  }
  else
  {
    networkConnection = true;
  }
  Serial.print("My IP address: ");
  Serial.println(Ethernet.localIP());
  if(networkConnection)
  {
    
    Serial.println("Attempting to create websocket service");
    
    // Start websockets server.
    socketServer.listen(websocketsPort);
    if (!socketServer.available()) 
    {
      Serial.println("Websockets Server not available!");
    }

    // Start http server.
    httpServer.begin(80);
    Serial.print("Visit http://");
    Serial.print(Ethernet.localIP());
    Serial.println(" in the browser to connect.");
  }
  else
  {
    Serial.println("Websocket service unavailable");
  }
  
}

int8_t getFreeSocketClientIndex() 
{
  // If a client in our list is not available, it's connection is closed and we
  // can use it for a new client.  
  for (byte i = 0; i < maxSocketClients; i++) 
  {
    if (!socketClients[i].available()) return i;
  }
  return -1;
}

void handleMessage(WebsocketsClient &client, WebsocketsMessage message) 
{
  auto data = message.data();

  // Log message
  Serial.print("Got Message: ");
  Serial.println(data);

  // Echo message
  client.send("Echo: " + data);
}

void handleEvent(WebsocketsClient &client, WebsocketsEvent event, String data) 
{
  if (event == WebsocketsEvent::ConnectionClosed) 
  {
    Serial.println("Connection closed");
    client.close();
  }
}

void listenForSocketClients() 
{
  if (socketServer.poll()) 
  {
    int8_t freeIndex = getFreeSocketClientIndex();
    if (freeIndex >= 0) 
    {
      WebsocketsClient newClient = socketServer.accept();
      Serial.printf("Accepted new websockets client at index %d\n", freeIndex);
      newClient.onMessage(handleMessage);
      newClient.onEvent(handleEvent);
      newClient.send("Hello from Teensy");
      socketClients[freeIndex] = newClient;
    }
  }
}

void pollSocketClients() 
{
  Serial.println("------ Connected Socket Clients ------");
  
  for (byte i = 0; i < maxSocketClients; i++) 
  {
    socketClients[i].poll();
    Serial.println("ClientID:");
  }
   Serial.println("----- ----------------------- ------");
}

void sendHttpReply(EthernetClient &client) 
{
  // Send a website that connects to the websocket server and allows to
  // communicate with the teensy.

  const char* header = 
    "HTTP/1.1 200 OK\r\n"
    "Content-Type: text/html\r\n"
    "Connection: close\r\n"
    "\r\n";

  const char* document = 
    "<!DOCTYPE html>\n"
    "<title>Teensy 4.1 Websockets</title>\n"
    "<meta charset='UTF-8'>\n"
    "<style>\n"
    "  body {\n"
    "    display: grid;\n"
    "    grid-template: min-content auto / auto min-content;\n"
    "    grid-gap: 1em;\n"
    "    margin: 0;\n"
    "    padding: 1em;\n"
    "    height: 100vh;\n"
    "    box-sizing: border-box;\n"
    "  }\n"
    "  #output {\n"
    "    grid-column-start: span 2;\n"
    "    overflow-y: scroll;\n"
    "    padding: 0.1em;\n"
    "    border: 1px solid;\n"
    "    font-family: monospace;\n"
    "  }\n"
    "</style>\n"
    "<input type='text' id='message' placeholder='Send a message and Teensy will echo it back!'>\n"
    "<button id='send-message'>send</button>\n"
    "<div id='output'></div>\n"
    "<script>\n"
    "  const url = `ws://${window.location.host}:3000`\n"
    "  const ws = new WebSocket(url)\n"
    "  let connected = false\n"
    "  const sendMessage = document.querySelector('#send-message')\n"
    "  const message = document.querySelector('#message')\n"
    "  const output = document.querySelector('#output')\n"
    "  function log(message, color = 'black') {\n"
    "    const el = document.createElement('div')\n"
    "    el.innerHTML = message\n"
    "    el.style.color = color\n"
    "    output.append(el)\n"
    "    output.scrollTop = output.scrollHeight\n"
    "  }\n"
    "  ws.addEventListener('open', () => {\n"
    "    connected = true\n"
    "    log('(✔️) Open', 'green')\n"
    "  })\n"
    "  ws.addEventListener('close', () => {\n"
    "    connected = false\n"
    "    log('(❌) Close', 'red')\n"
    "  })\n"
    "  ws.addEventListener('message', ({ data }) =>\n"
    "    log(`(💌) ${data}`)\n"
    "  )\n"
    "  sendMessage.addEventListener('click', () => {\n"
    "    connected && ws.send(message.value)\n"
    "  })\n"
    "  message.addEventListener('keyup', ({ keyCode }) => {\n"
    "     connected && keyCode === 13 && ws.send(message.value)\n"
    "  })\n"
    "  log(`(📡) Connecting to ${url} ...`, 'blue')\n"
    "</script>\n";

  client.write(header);
  client.write(document);  
}

void listenForHttpClients() 
{
  // Listen for incoming http clients.
  EthernetClient client = httpServer.available();

  if (client) 
  {
    Serial.println("Http client connected!");

    // An http request ends with a blank line.
    bool currentLineIsBlank = true;

    while (client.connected()) 
    {
      if (client.available()) 
      {
        char c = client.read();

        if (c == '\n' && currentLineIsBlank) 
        {
          // If we've gotten to the end of the line (received a newline
          // character) and the line is blank, the http request has ended,
          // so we can send a reply.
          sendHttpReply(client);
          break;
        } else if (c == '\n') 
        {
          // Starting a new line.
          currentLineIsBlank = true;
        } else if (c != '\r') 
        {
          // Read a character on the current line.
          currentLineIsBlank = false;
        }
      }
    }

    // The NativeEthernet's WebServer example adds a small delay here. For me it
    // seems to work without the delay. Uncomment to following line if you have
    // issues connecting to the website in the browser.
    // delay(1);

    // Close the connection.
    client.stop();
  }
}

void loop() 
{
  
  

  switch (Ethernet.maintain()) 
  {
    case 1:
      //renewed fail
      Serial.println("Error: renewed fail");
      break;

    case 2:
      //renewed success
      Serial.println("Renewed success");
      //print your local IP address:
      Serial.print("My IP address: ");
      Serial.println(Ethernet.localIP());
      break;

    case 3:
      //rebind fail
      Serial.println("Error: rebind fail");
      break;

    case 4:
      //rebind success
      Serial.println("Rebind success");
      //print your local IP address:
      Serial.print("My IP address: ");
      Serial.println(Ethernet.localIP());
      break;

    default:
      //nothing happened
      if(looper == 100000)
      {
        //Serial.println(looper);
        for (byte i = 0; i < maxSocketClients; i++) 
        {
          socketClients[i].send("test");
        }
        looper = 0;
      }
      looper = looper+1;
      //Serial.println(looper);
    break;
  }
  listenForSocketClients();
  pollSocketClients();
  listenForHttpClients();
  
}