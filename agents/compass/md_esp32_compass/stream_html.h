/*************************************/
// Define stream_html[], script_var[], style_var[] and index_html[] for web server pages
/*************************************/
const char stream_html[]  PROGMEM = R"rawliteral(
{"workerName":%workerName%,"sensorURL":%sensorURL%,"socket_server_ip":%socket_server_ip%,"socket_server_port":%socket_server_port%,"calibration_data":%calibration_data%}
)rawliteral";
/*************************************/
