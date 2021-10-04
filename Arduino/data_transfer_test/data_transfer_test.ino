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
#include <TroykaDHT.h> //Third Party Library for reading the temperature/humidity sensor
#include "Adafruit_SGP30.h" //Adafruit's library for reading their TVOC/eCO2 sensor
#include <SoftwareSerial.h> //Standard library to create a serial port on any two pins

Adafruit_SGP30 sgp; // create a gas sensor object
DHT dht(4, DHT11);  // create a Temp/Humidity sensor object
SoftwareSerial BTSerial(11, 12); //RX|TX for serial port to the bluetooth module

int counter = 0; //counter to update CO2 baseline values
float temperature = 22.1; // [°C]
float humidity = 45.2; // [%RH]

unsigned long acetoneRead;
unsigned long counterTime; //time of last counter update
const unsigned long heaterTimeOut = 5*60*1000;
unsigned long lastMessage;

String received = ""; //holds the value of any returned data from the bluetooth module

const int LED = 13; //The on board LED pin
bool LED_ON = false;

/* return absolute humidity [mg/m^3] with approximation formula
  @param temperature [°C]
  @param humidity [%RH]
*/
uint32_t getAbsoluteHumidity(float temperature, float humidity) {
  // approximation formula from Sensirion SGP30 Driver Integration chapter 3.15
  const float absoluteHumidity = 216.7f * ((humidity / 100.0f) * 6.112f * exp((17.62f * temperature) / (243.12f + temperature)) / (273.15f + temperature)); // [g/m^3]
  const uint32_t absoluteHumidityScaled = static_cast<uint32_t>(1000.0f * absoluteHumidity); // [mg/m^3]
  return absoluteHumidityScaled;
}

void(* reset) (void) = 0;//declare reset function at address 0

void setup() {
  //Start Serial ports
  Serial.begin(115200);
  BTSerial.begin(115200);
  
  //initialise and connect to the gas sensor
  Serial.println("SGP30 init");
  dht.begin();
  if (! sgp.begin()) {
    Serial.println("Sensor not found :(");
    while (1);
  }
  Serial.print("Found SGP30 serial #");
  Serial.print(sgp.serialnumber[0], HEX);
  Serial.print(sgp.serialnumber[1], HEX);
  Serial.println(sgp.serialnumber[2], HEX);

  //initialise the LED and set to off
  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);

  //Let phone know that the device has re/started
  String title = "START";
  char tmp[title.length() + 1];
  title.toCharArray(tmp, title.length() + 1);
  BTSerial.write(tmp);
  BTSerial.write("STARTED");

  //Start the counter timer
  counterTime = millis();
  // If you have a baseline measurement from before you can assign it to start, to 'self-calibrate'
  //sgp.setIAQBaseline(0x8E68, 0x8F41);  // Will vary for each sensor!
}


void loop() {
  //recieve data if there is anything in the buffer
  if (receivedData()) {
    if (received == "HEAT") { //currently toggles the LED and enables Heating Regulator
      LED_ON = true;
      digitalWrite(LED, HIGH);
    } else if (received == "COOL") { //currently toggles the LED and enables Heating Regulator
      LED_ON = false;
      digitalWrite(LED, LOW);
    } else if (received == "RESET"){
      reset();
    } else{
      BTSerial.flush();
    }
    lastMessage = millis();
  }

  if((millis()-lastMessage>heaterTimeOut) && LED_ON){
    LED_ON = false;
    digitalWrite(LED, LOW);
  }

  //reads and transmits the Temp/ humidity
  dht.read();
  if (dht.getState() == DHT_OK) {
    sendData("Temp", uint16_t(dht.getTemperatureC() + 0.5));
    delay(7);
    sendData("Humi", uint16_t(dht.getHumidity() + 0.5));
  }

  if (millis() - counterTime > 1000) {//activates at most once every second
    // If you have a temperature / humidity sensor, you can set the absolute humidity to enable the humditiy compensation for the air quality signals
    sgp.setHumidity(getAbsoluteHumidity(temperature, humidity));

    //try to read CO2
    if (! sgp.IAQmeasure()) {
      Serial.println("Measurement failed");
      return;
    }
    sendData("TVOC",  uint16_t(sgp.TVOC));
    delay(7);
    sendData("eCO2", uint16_t(sgp.eCO2));
    
    //try to read H2 and ethanol
    if (! sgp.IAQmeasureRaw()) {
      Serial.println("Raw Measurement failed");
      return;
    }
    sendData("RaH2", uint16_t(sgp.rawH2));
    delay(7);
    sendData("etha", uint16_t(sgp.rawEthanol));

    counter++; //increment counter. After 30 readings update baseline
    if (counter == 30) {
      counter = 0;

      uint16_t TVOC_base, eCO2_base;
      if (! sgp.getIAQBaseline(&eCO2_base, &TVOC_base)) {
        Serial.println("Failed to get baseline readings");
        return;
      }
      Serial.print("****Baseline values: eCO2: 0x"); Serial.print(eCO2_base, HEX);
      Serial.print(" & TVOC: 0x"); Serial.println(TVOC_base, HEX);
    }
    counterTime = millis();//reset timer
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
