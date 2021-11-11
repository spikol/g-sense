//include libraries
#include "rpcWiFi.h" //Wi-Fi library 
#include "DHT.h" //DHT library
#include "TFT_eSPI.h" //TFT LCD library 
#include "Free_Fonts.h" //free fonts library 

#include <DNSServer.h>
#include <WebServer.h>
#include <WiFiManager.h>  

TFT_eSPI tft; //initialize TFT LCD
TFT_eSprite spr = TFT_eSprite(&tft); //initialize sprite buffer

#define DHTPIN 0 //define Signal Pin of DHT
#define DHTTYPE DHT11 //define DHT Sensor Type
DHT dht(DHTPIN, DHTTYPE); //initialize DHT sensor
 
const char* serverName = "io.adafruit.com"; //Domain Name for HTTP POST Request
String AIOKey = "aio_bRhZ80BGWvA1rqRB5SIyv8Kgdrys"; //

//tcp connections
WiFiClient client;
 
void setup() {
  Serial.begin(115200); //start serial communication
  while (!Serial); //wait for Serial to be ready
  delay(100);

  // Use WiFiClient class to create TCP connections
  WiFiManager wifiManager;
  wifiManager.resetSettings(); //to reconnect
  wifiManager.autoConnect("AutoConnectAP");
  WiFi.begin();
 

    
  Serial.println("Connected to Wifi");
  Serial.print("IP Address: ");
  Serial.println(WiFi.localIP()); //print Wio Terminal's IP address
  Serial.println(""); //print empty line

  dht.begin(); //Start DHT sensor

//  ----------------LCD------------------------

  //LCD setup
  tft.begin(); //start TFT LCD
  tft.setRotation(3); //set TFT LCD rotation
  tft.fillScreen(TFT_BLACK); //fill background

  //header title 
  tft.fillRect(0,0,320,50,TFT_YELLOW); //fill rectangle 
  tft.setFreeFont(FSB12); //set font type 
  tft.setTextColor(TFT_BLACK); //set text color
  tft.drawString("WEATHER STATION", 40, 15); //draw string 
    
  //text strings 
  tft.setTextColor(TFT_WHITE); 
  tft.setFreeFont(FS18); 
  tft.drawString("Temperature:", 10,75);
  tft.drawString("Humidity:", 10,175);
  
  tft.setTextColor(TFT_YELLOW); 
  tft.setFreeFont(FMB24);  
  tft.drawString("C",260,70);
  tft.drawString("%RH", 215,170);
  
  tft.drawFastHLine(0,140,320,TFT_YELLOW); //draw horizontal line

}
 
void loop() {
  Serial.print("Connecting to ");
  Serial.println(serverName); //print server IP address

  //check whether Wio Terminal is connected to server
  if (!client.connect(serverName, 80)) {  //80 is default port of HTTP
      Serial.println("Connection failed!");
  } else {
      Serial.println("Connected to server!");

  int t = dht.readTemperature(); //Assign variable to store temperature
  int h = dht.readHumidity(); //Assign variable to store humidity

  //data needed to send 
  String tempStr;
  tempStr +="value=";
  tempStr +=String(t);

  String humiStr;
  humiStr +="value=";
  humiStr +=String(h);
   

  //make an HTTP POST request
  client.print("POST /api/v2/spikol/feeds/temperature/data HTTP/1.1\n");
  client.print("Host: io.adafruit.com\n");
  client.print("Connection: keep-alive\n");
  client.print("X-AIO-Key: aio_bRhZ80BGWvA1rqRB5SIyv8Kgdrys\n");
  client.print("Content-Type: application/x-www-form-urlencoded\n");
  client.print("Content-Length: ");
  client.print(tempStr.length());
  client.print("\n\n");
  client.print(tempStr);

  client.print("POST /api/v2/spikol/feeds/humidity/data HTTP/1.1\n");
  client.print("Host: io.adafruit.com\n");
  client.print("Connection: close\n");
  client.print("X-AIO-Key: aio_bRhZ80BGWvA1rqRB5SIyv8Kgdrys\n");
  client.print("Content-Type: application/x-www-form-urlencoded\n");
  client.print("Content-Length: ");
  client.print(humiStr.length());
  client.print("\n\n");
  client.print(humiStr);

  //display sensor data on serial monitor 
  Serial.print("temperature: "); //print string
  Serial.println(t); //print data stored in t
  Serial.print("Humidity:");
  Serial.println(h);
                             
  Serial.println("Sent to AdaFruit.IO.");

      
  //sprite buffer for temperature
  spr.createSprite(55, 40); //create buffer
  spr.fillSprite(TFT_BLACK); //fill background color of buffer
  spr.setFreeFont(FMB24); //set font type 
  spr.setTextColor(TFT_YELLOW); //set text color
  spr.drawNumber(t, 0, 0); //display number 
  spr.pushSprite(200, 70); //push to LCD 
  spr.deleteSprite(); //clear buffer

  //sprite buffer for humidity
  spr.createSprite(55, 40);
  spr.fillSprite(TFT_BLACK);
  spr.setFreeFont(FMB24);
  spr.setTextColor(TFT_YELLOW);
  spr.drawNumber(h, 0, 0); 
  spr.pushSprite(155, 170);
  spr.deleteSprite();
  
  }
  
  client.stop(); //disconnect from server
  //wait 10 seconds to send the next POST request
  Serial.println("Waiting...");
  delay(15000);
}
