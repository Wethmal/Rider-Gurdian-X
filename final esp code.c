#include "esp_camera.h"
#include "SD_MMC.h"
#include <WiFi.h>
#include <FS.h>


const char* ssid = "Dialog 4G 049";
const char* password = "8fb279CF";

WiFiServer server(80);

#define FLASH_PIN 4   // Flash LED pin

// Recording Settings
const unsigned long recordingTime = 60000;    // 1 minute
const unsigned long snapshotInterval = 1000;  // 1 image per second
unsigned long lastSnap = 0;
unsigned long recordStart = 0;
String folderName = "";

// AI Thinker Pin Config
#define PWDN_GPIO_NUM     32
#define RESET_GPIO_NUM    -1
#define XCLK_GPIO_NUM     0
#define SIOD_GPIO_NUM     26
#define SIOC_GPIO_NUM     27
#define Y9_GPIO_NUM       35
#define Y8_GPIO_NUM       34
#define Y7_GPIO_NUM       39
#define Y6_GPIO_NUM       36
#define Y5_GPIO_NUM       21
#define Y4_GPIO_NUM       19
#define Y3_GPIO_NUM       18
#define Y2_GPIO_NUM       5
#define VSYNC_GPIO_NUM    25
#define HREF_GPIO_NUM     23
#define PCLK_GPIO_NUM     22


void setupCamera() {
  camera_config_t config;
  config.ledc_channel = LEDC_CHANNEL_0;
  config.ledc_timer = LEDC_TIMER_0;
  config.pin_d0 = Y2_GPIO_NUM;
  config.pin_d1 = Y3_GPIO_NUM;
  config.pin_d2 = Y4_GPIO_NUM;
  config.pin_d3 = Y5_GPIO_NUM;
  config.pin_d4 = Y6_GPIO_NUM;
  config.pin_d5 = Y7_GPIO_NUM;
  config.pin_d6 = Y8_GPIO_NUM;
  config.pin_d7 = Y9_GPIO_NUM;
  config.pin_xclk = XCLK_GPIO_NUM;
  config.pin_pclk = PCLK_GPIO_NUM;
  config.pin_vsync = VSYNC_GPIO_NUM;
  config.pin_href = HREF_GPIO_NUM;
  config.pin_sscb_sda = SIOD_GPIO_NUM;
  config.pin_sscb_scl = SIOC_GPIO_NUM;
  config.pin_pwdn = PWDN_GPIO_NUM;
  config.pin_reset = RESET_GPIO_NUM;
  config.xclk_freq_hz = 20000000;
  config.pixel_format = PIXFORMAT_JPEG;
  config.frame_size = FRAMESIZE_VGA;
  config.jpeg_quality = 12;
  config.fb_count = 1;

  if (esp_camera_init(&config) != ESP_OK) {
    Serial.println("Camera init failed!");
    while (true);
  }
}

// ==========================
// Delete Oldest Folder
// ==========================
void deleteOldestFolder() {
  File root = SD_MMC.open("/");
  File oldest;
  unsigned long oldestTime = ULONG_MAX;

  while (true) {
    File entry = root.openNextFile();
    if (!entry) break;
    if (entry.isDirectory()) {
      if (entry.getLastWrite() < oldestTime) {
        oldestTime = entry.getLastWrite();
        oldest = entry;
      }
    }
  }

  if (oldest) {
    Serial.print("Deleting old folder: ");
    Serial.println(oldest.name());
    SD_MMC.rmdir(oldest.name());
  }
}


void startNewRecording() {
  folderName = "/REC_" + String(millis());
  SD_MMC.mkdir(folderName);
  Serial.println("🎬 New Recording: " + folderName);
  recordStart = millis();
  lastSnap = 0;
}

void captureSnapshot(bool flashOn) {
  if(flashOn) digitalWrite(FLASH_PIN, HIGH);  // Turn flash ON for snapshot
  delay(50); // small delay for flash

  camera_fb_t *fb = esp_camera_fb_get();
  if (!fb) {
    if(flashOn) digitalWrite(FLASH_PIN, LOW);
    return;
  }

  String filename = folderName + "/IMG_" + String(millis()) + ".jpg";
  File file = SD_MMC.open(filename, FILE_WRITE);

  if (file) {
    file.write(fb->buf, fb->len);
    file.close();
    Serial.println("📸 Saved: " + filename);
  }

  esp_camera_fb_return(fb);

  if(flashOn) digitalWrite(FLASH_PIN, LOW); // Turn flash OFF after snapshot
}


void setup() {
  Serial.begin(115200);

  pinMode(FLASH_PIN, OUTPUT);
  digitalWrite(FLASH_PIN, LOW);

  if (!SD_MMC.begin("/sdcard", true)) {
    Serial.println("SD Card Mount Failed!");
    while (true);
  }

  setupCamera();
  startNewRecording();

  WiFi.begin(ssid, password);
  Serial.print("Connecting WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi Connected!");
  Serial.print("Live stream: http://");
  Serial.println(WiFi.localIP());

  server.begin();
}

void loop() {
  unsigned long now = millis();

  // Handle 1-minute recording snapshots
  if (now - recordStart >= recordingTime) {
    Serial.println("🎥 Recording Finished");
    deleteOldestFolder();
    delay(500);
    startNewRecording();
  }

  // Snapshot every 1s
  if (now - lastSnap >= snapshotInterval) {
    lastSnap = now;
    captureSnapshot(true);  // Flash ON for snapshot
  }

  // Live streaming
  WiFiClient client = server.available();
  if (client) {
    digitalWrite(FLASH_PIN, HIGH); // Flash continuously ON while client connected
    if (client.connected()) {
      camera_fb_t *fb = esp_camera_fb_get();
      if (fb) {
        client.println("HTTP/1.1 200 OK");
        client.println("Content-Type: image/jpeg");
        client.println("Content-Length: " + String(fb->len));
        client.println();
        client.write(fb->buf, fb->len);
        esp_camera_fb_return(fb);
      }
    } else {
      client.stop();
      digitalWrite(FLASH_PIN, LOW); // Turn off flash when client leaves
    }
  } else {
    // No client, flash handled by snapshot logic (blinking every 1s)
  }
}