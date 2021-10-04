/*
    Author: Alexander Cavalli
    Date: August 2019

    This code currently reads sensor data then publishes the data via a BLuetoothLE module
    as ASCII characters in the format Name:value:time.
    The data needs too be unsigned integers to work properly so we send the time in
    deciseconds to get more accuracy and any decimal values can either by multiplied by a
    factor of 10/100 or rounded.

    WARNING! The pins on the bluetooth module are 3.3v tolerant so the TX pin of the
    arduino should be passed through a volatage divider or logic level shifter(overkill)
    before going to the bluetooth modules RX pin.
             (5V)          x ohms           BluetoothRX(3.3V)           2x ohms
          ArduinoTX_______/\/\/\/\___________________|_________________/\/\/\/\________GND

    Obviously the higher the resistances, the more accurate the output voltage will be
*/
#include <Wire.h>  //I2C library
#include <SoftwareSerial.h> //Standard library to create a serial port on any two pins
#include <SparkFun_SCD30_Arduino_Library.h> //Sparkfun library to read SDC30 

#define acetonePin A0

SoftwareSerial BTSerial(11, 12); //RX|TX for serial port to the bluetooth module
SCD30 airSensor;

unsigned long acetoneRead;
const unsigned long heaterTimeOut = 5*60*1000;
unsigned long lastMessage;

const float Iset = 180.87;//microAmps
const float Rsafe = 25380.0;//Ohms
char* response;

String received = ""; //holds the value of any returned data from the bluetooth module

const int HEAT_PIN = 13; //The on board LED pin
bool heating = false;

/* return absolute humidity [mg/m^3] with approximation formula
  @param temperature [°C]
  @param humidity [%RH]
*/

void(* reset) (void) = 0;//declare reset function at address 0

void setup() {
  //Start Serial ports
  Serial.begin(115200);
  BTSerial.begin(115200);
  Wire.begin();
  
  //initialise and connect to the gas sensor
  //initialise the Heater and set to off
  Serial.println("HEATER OFF");
  pinMode(HEAT_PIN, OUTPUT);
  digitalWrite(HEAT_PIN, LOW);

  airSensor.begin();

  //Let phone know that the device has re/started
  String title = "STARTED";
  char tmp[title.length() + 1];
  title.toCharArray(tmp, title.length() + 1);
  BTSerial.write(tmp);

  title = "RECEIVED";
  char r[title.length() + 1];
  title.toCharArray(r, title.length() + 1);
  response = *r;
}


void loop() {
  //recieve data if there is anything in the buffer
  if (receivedData()) {
    if (received == "HEAT") { //currently toggles the LED and enables Heating Regulator
      heating = true;
      digitalWrite(HEAT_PIN, HIGH);
      sendResponse();
    } else if (received == "COOL") { //currently toggles the LED and enables Heating Regulator
      heating = false;
      digitalWrite(HEAT_PIN, LOW);
      sendResponse();
    } else if (received == "RESET"){
      reset();
    } else{
      BTSerial.flush();
    }
    lastMessage = millis();
  }

  if(heating && (millis()-lastMessage>heaterTimeOut)){
    heating = false;
    digitalWrite(HEAT_PIN, LOW);
  }

  if (airSensor.dataAvailable()){
    sendData("Humi", uint16_t(airSensor.getHumidity()+0.5));
    delay(10);
    sendData("Temp", uint16_t(airSensor.getTemperature()+0.5));
    delay(10);
    sendData("CO2", airSensor.getCO2());
    delay(10);
  }

  if(millis()-acetoneRead>700){
    float V = analogRead(acetonePin)*5.0/1024.0;
    uint16_t res = ((V*Rsafe)/((Iset*Rsafe)/1000000.0-V));
    Serial.print(V);
    Serial.print("\t");
    Serial.print(Iset);
    Serial.print("\t");
    Serial.println(res);
    sendData("Acet", res);
    acetoneRead = millis();
    delay(10);
  }

  
}


/**
 * writes the data to the bluetooth module in the format given above.
 * @Param title - The title/label for the data so the receiver knows what's being sent
 * @Param data  - The value to be associated with the title
 */
void sendData(String title, uint16_t data) {
  title += ":";
  title += data;
  title += ":";
  title += (unsigned long)(millis() / 100);
  char tmp[title.length() + 1];
  title.toCharArray(tmp, title.length() + 1);
  BTSerial.write(tmp);
}

void sendResponse(){
  BTSerial.write(&response);
}


/**
 * Reads in any data stored in the buffer and saves the ASCII interpretation to the 
 * received variable
 * 
 * @returns  true if there was data to read, false otherwise.
 */
bool receivedData() {
  if (BTSerial.available()) {
    received = "";
    while (BTSerial.available()) {
      received.concat(char(BTSerial.read()));
    }
    Serial.println(received);
    return true;
  }
  return false;
}
