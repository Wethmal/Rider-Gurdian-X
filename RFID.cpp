#include <SPI.h>
#include <MFRC522.h>

// RFID Pins
#define SS_PIN D4
#define RST_PIN D3

MFRC522 rfid(SS_PIN, RST_PIN);

void setup() {
  Serial.begin(115200);
  SPI.begin();
  rfid.PCD_Init();

  Serial.println("Scan RFID Card...");
}

void loop() {
  // Check for new RFID card
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial())
    return;

  Serial.print("RFID UID: ");
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    Serial.print(rfid.uid.uidByte[i], HEX);
    Serial.print(" ");
    uid += String(rfid.uid.uidByte[i], HEX) + " ";
  }
  Serial.println();
  Serial.println("UID as String: " + uid);
  Serial.println("---------------------------");

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  delay(1000);
}