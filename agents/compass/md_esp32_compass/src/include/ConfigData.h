// define a struct type called CD to be used for config data

typedef struct {
  uint val = 0;
  char workerName[50] = "";
  char sensorURL[50] = "";
  char ssid[20] = "";
  char ss_password[20] = "";
  char socket_server_ip[50]= "";
  char socket_server_port[8]="";
  char calibration_offset[100]="";  // If we are to generalise this, are these fields general or
  char calibration_scale[100]="";   // are they sorcific to the QMC5883LCompass
} ConfigData;