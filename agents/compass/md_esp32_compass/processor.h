/******************************************************/
// processor returns one of the defined cnaracter arrays above
// stream_html[], script_var[], style_var[] and index_html[]
/******************************************************/
String processor(const String& var)
{
    String gpsData ="";
  //Serial.println(var);
  /******************************************************/
  
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
    rows += myConfigData.ssid;
    rows +="\"></td> (EG: SSID)</tr>";
    
    rows += "<tr><td>ss_password</td><td><input type=\"text\" id=\"ss_password\" onchange=\"updateSettings(this)\" value=\"";
    rows += myConfigData.ss_password;
    rows +="\"></td> (EG: PASSWORD)</tr>";

    rows += "<tr><td>workerName</td><td><input type=\"text\" id=\"workerName\" onchange=\"updateSettings(this)\" value=\"";
    rows += myConfigData.workerName;
    rows +="\"></td> (EG: compass)</tr>";

    rows += "<tr><td>socket_server_ip</td><td><input type=\"text\" id=\"socket_server_ip\" onchange=\"updateSettings(this)\" value=\"";
    rows += myConfigData.socket_server_ip;
    rows +="\"></td> (EG: 192.168.10.239)</tr>";

    rows += "<tr><td>socket_server_port</td><td><input type=\"text\" id=\"socket_server_port\" onchange=\"updateSettings(this)\" value=\"";
    rows += myConfigData.socket_server_port;
    rows +="\"></td> (EG: 3200)</tr>";

    rows += "<tr><td>sensorURL</td><td><input type=\"text\" id=\"sensorURL\" onchange=\"updateSettings(this)\" value=\"";
    rows += myConfigData.sensorURL;
    rows +="\"></td> (EG: /reading/compass)</tr>";

    rows += "<tr><td>calibration_offset</td><td><input type=\"text\" id=\"calibration_offset\" onchange=\"updateSettings(this)\" value=\"";
    rows += myConfigData.calibration_offset;
    rows +="\"></td> (EG: -729.00,-445.00,1045.00) </tr>";

    rows += "<tr><td>calibration_scale</td><td><input type=\"text\" id=\"calibration_scale\" onchange=\"updateSettings(this)\" value=\"";
    rows += myConfigData.calibration_scale;
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