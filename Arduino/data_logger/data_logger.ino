#include <SPI.h>
#include <SD.h>

#define ANALOG1 A0
#define ANALOG2 A5

const int chipSelect = 4;
const unsigned long knownCurrent = 250;//microAmps

unsigned long start;
unsigned long lastRead;
bool file1 = true;
bool success;

void setup() {
  //  Serial.begin(115200);
  analogReference(EXTERNAL);
  pinMode(13, OUTPUT);
  pinMode(8, INPUT);
  digitalWrite(LED_BUILTIN, HIGH);
  //see if the card is present and can be initialized:
  if (!SD.begin(chipSelect)) {
    SPI.end();
    pinMode(13, OUTPUT);
    while (true) {
      for (int i = 0; i < 3; i++) {
        digitalWrite(13, HIGH);
        delay(500);
        digitalWrite(13, LOW);
        delay(500);
      }
      for (int i = 0; i < 3; i++) {
        digitalWrite(LED_BUILTIN, HIGH);
        delay(1000);
        digitalWrite(LED_BUILTIN, LOW);
        delay(500);
      }
      for (int i = 0; i < 3; i++) {
        digitalWrite(LED_BUILTIN, HIGH);
        delay(500);
        digitalWrite(LED_BUILTIN, LOW);
        delay(500);
      }
      delay(1000);
    }
  }
  SD.remove("Sensor.txt");
  start = millis();
  lastRead = millis();

}

void loop() {
  unsigned long now = millis();
  //  if (now - lastRead > 500) {
  //    String line = "Time: ";
  String line = "";
  File dataFile;
  long data = analogRead(ANALOG1);
  line += String(now - start);
  //    line += ",Raw: ";
  line += ",";
  line += String(data);
  //    line += ",SensorV: ";
  line += ",";
  float dataV = data * 5.0 / 1024.0;
  line += String(dataV, 3);
  unsigned long SensorR = dataV / knownCurrent * 1000000;
  //    line += ",SensorR: ";
  line += ",";
  line += String(SensorR);

  //    line += ",SensorV: ";
  //    line += String(5 - dataV);
  //    line += ",Approx Current: ";
  //    line += String(dataV / knownRes1);
  //    line += ",Approx Res: ";
  //    line += String(((5 / dataV) - 1) * knownRes1);
  //    line += ",Known Res: ";
  //    line += String(knownRes1);

  if (digitalRead(8) == HIGH) {
    //    Serial.println(line);
    dataFile = SD.open("Sensor.txt", FILE_WRITE);
    if (dataFile) {
      dataFile.println(line);
      dataFile.close();
    }
  }
  //    lastRead = millis();
  //  }
}
