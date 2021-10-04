#include <SoftwareSerial.h>

SoftwareSerial BTSerial(11, 12); //RX|TX
unsigned long time_last;
const unsigned long interval = 3000;
unsigned int count = 0;

void setup(){
  Serial.begin(115200);
  BTSerial.begin(115200); // default baud rate
  while(!Serial); //if it is an Arduino Micro
  time_last = millis();
  Serial.println("testing");
}

void loop(){
  //read from the HM-10 and print in the Serial
  if(BTSerial.available()){
    String s = "";
    while (BTSerial.available()){
      s.concat(char(BTSerial.read()));
    }
    Serial.print("<\t");
    Serial.println(s);
  }

  //read from the serial and send via bluetooth and print back to serial
  if(Serial.available()){
    String s = "";
    while (Serial.available()){
      s.concat(Serial.readString());
    }
    unsigned int len = s.length();
    char c [len];
    s.toCharArray(c, len);
    Serial.print(">\t");
    Serial.println(c);
    BTSerial.write(c);
  }

  //send and print a count;
  if(millis()-time_last > 1500){
    count += 1;
    String s = String(count);
    unsigned int len = s.length()+1;
    char c [len];
    s.toCharArray(c, len);
    BTSerial.write(c);
    Serial.print(">\t");
    Serial.println(c);
    time_last = millis();
  }
}

//void loop(){
//  if(BTSerial.available()){
//    Serial.print("<\t");
//    Serial.println(BTSerial.read());
//  }
//
//  if(millis()-time_last > interval){
//    count += 1;
//    String s = String(count);
//    unsigned int len = s.length()+1;
//    char c [len];
//    s.toCharArray(c, len);
//    BTSerial.write(c);
//    Serial.print(">\t");
//    Serial.println(c);
//    time_last = millis();
//  }
//}

//void loop(){
//  //read from the HM-10 and print in the Serial
//  if(BTSerial.available()){
//    String s = "";
//    while (BTSerial.available()){
//      s.concat(char(BTSerial.read()));
//    }
//    Serial.print("<\t");
//    Serial.println(s);
//  }
//
//  if(Serial.available()){
//    String s = "";
//    while (Serial.available()){
//      s.concat(Serial.readString());
//    }
//    unsigned int len = s.length();
//    char c [len];
//    s.toCharArray(c, len);
//    Serial.print(">\t");
//    Serial.println(c);
//    BTSerial.write(c);
//  }
//}
