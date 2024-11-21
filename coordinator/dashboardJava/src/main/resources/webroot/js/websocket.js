var socket = new WebSocket("ws://127.0.0.1:3200/reading/dashboard");

// WebSocket open
socket.onopen = function(event) 
{
	console.log("WebSocket connection established.");
};

// WebSocket message received
socket.onmessage = function(event) 
{
	//console.log("Message received: " + event.data);
	//processUpdateFromSocket(event.data);
	processUpdateFromSocket(event.data);
};

// WebSocket error
socket.onerror = function(error) 
{
	console.error("WebSocket error: " + error);
};

// WebSocket closed
socket.onclose = function(event) 
{
	console.log("WebSocket connection closed.");
};

// Function to send a message through WebSocket
function sendMessage() 
{
    var message = document.getElementById("messageInput").value;
	socket.send(message);
}
 