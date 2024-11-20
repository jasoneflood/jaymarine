/*************************************/
// Jason Flood
// Mark Deegan
// November 2024
//
// script_var code for md_esp_compass
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
/*************************************/