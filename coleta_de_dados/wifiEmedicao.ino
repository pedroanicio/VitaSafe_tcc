#include <Wire.h>
#include "MAX30105.h"
#include "heartRate.h"
#include <Adafruit_Sensor.h>
#include <Adafruit_MLX90614.h>
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <time.h>

#define WIFI_SSID "test"
#define WIFI_PASSWORD "123"
#define FIREBASE_HOST "https..."
#define FIREBASE_AUTH "456"

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

#define ID_IDOSO "fa848220-df80-4ddc-a861-5fefc8128d7d"
#define NOME_IDOSO "pedro"

MAX30105 particleSensor;
Adafruit_MLX90614 mlx = Adafruit_MLX90614();

const byte RATE_SIZE = 4;
byte rates[RATE_SIZE];
byte rateSpot = 0;
long lastBeat = 0;
float beatsPerMinute;
int beatAvg;

#define SAMPLES 50
long redBuffer[SAMPLES];
long irBuffer[SAMPLES];

void setup() {
  Serial.begin(115200);
  Wire.begin();

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" WiFi conectado!");

  configTime(0, 0, "pool.ntp.org");

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  if(Firebase.getString(firebaseData, "/idosos/" + String(ID_IDOSO) + "/nome") && firebaseData.stringData() != NOME_IDOSO) {
    Firebase.setString(firebaseData, "/idosos/" + String(ID_IDOSO) + "/nome", NOME_IDOSO);
  }

  if (!particleSensor.begin(Wire, I2C_SPEED_STANDARD)) {
    Serial.println("MAX30102 não encontrado. Verifique conexões.");
    while (1);
  }

  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x1F);
  particleSensor.setPulseAmplitudeIR(0x1F);

  if (!mlx.begin()) {
    Serial.println("MLX90614 não encontrado. Verifique conexões.");
    while (1) delay(10);
  }
  Serial.println("MLX90614 inicializado!");

}

String getCurrentTimestamp() {
  struct tm timeinfo;
  if(!getLocalTime(&timeinfo)){
    return "2025-07-13T00:00:00Z";
  }
  timeinfo.tm_hour -= 3;
  char timestamp[25];
  strftime(timestamp, sizeof(timestamp), "%Y-%m-%dT%H:%M:%SZ", &timeinfo);
  return String(timestamp);
}

String getCurrentDate() {
  struct tm timeinfo;
  if(!getLocalTime(&timeinfo)){
    return "2025-07-13";
  }
  char date[11];
  strftime(date, sizeof(date), "%Y-%m-%d", &timeinfo);
  return String(date);
}

String getCurrentTime() {
  struct tm timeinfo;
  if(!getLocalTime(&timeinfo)){
    return "00:00";
  }
  char time[6];
  strftime(time, sizeof(time), "%H:%M", &timeinfo);
  return String(time);
}

String generateUniqueID() {
  return String(millis()) + String(random(1000, 9999));
}

void sendToHistorico(int bpm, float temperatura) {
  String datePath = "/idosos/" + String(ID_IDOSO) + "/historico/" + getCurrentDate() + "/";
  String timeEntryPath = datePath + generateUniqueID();

  FirebaseJson json;
  json.set("hora", getCurrentTime());
  json.set("bpm", bpm);
  json.set("temperatura", temperatura);

  Firebase.set(firebaseData, timeEntryPath, json);
}

float calculateSpO2() {
  double redMean = 0;
  double irMean = 0;
  double redAC = 0;
  double irAC = 0;

  for (int i = 0; i < SAMPLES; i++) {
    redBuffer[i] = particleSensor.getRed();
    irBuffer[i] = particleSensor.getIR();

    redMean += redBuffer[i];
    irMean += irBuffer[i];

    delay(10);
  }

  redMean /= SAMPLES;
  irMean /= SAMPLES;

  if (redMean <= 0 || irMean <= 0) {
    return NAN;
  }

  for (int i = 0; i < SAMPLES; i++) {
    redAC += pow(redBuffer[i] - redMean, 2);
    irAC += pow(irBuffer[i] - irMean, 2);
  }

  redAC = sqrt(redAC / SAMPLES);
  irAC = sqrt(irAC / SAMPLES);

  if (redAC <= 0 || irAC <= 0) {
    return NAN;
  }

  float R = (redAC / redMean) / (irAC / irMean);

  float SpO2 = 104.0 - 17.0 * R;

  if (SpO2 > 100) SpO2 = 100;

  if (SpO2 < 70 || isnan(SpO2) || !isfinite(SpO2)) {
    return NAN;
  }

  return SpO2;
}

void loop() {
  long irValue = particleSensor.getIR();
  if (irValue > 7000) {
    if (checkForBeat(irValue)) {
      long delta = millis() - lastBeat;
      lastBeat = millis();

      beatsPerMinute = 60 / (delta / 1000.0);
      if (beatsPerMinute < 130 && beatsPerMinute > 50) {
        rates[rateSpot++] = (byte)beatsPerMinute;
        rateSpot %= RATE_SIZE;

        beatAvg = 0;
        for (byte x = 0 ; x < RATE_SIZE ; x++) beatAvg += rates[x];
        beatAvg /= RATE_SIZE;

        float oxigenacao = calculateSpO2();
        float temperatura = mlx.readObjectTempC();
        
        Serial.print(" | BPM: "); Serial.print(beatAvg);
        Serial.print(" | SpO2: "); Serial.print(oxigenacao);
        Serial.print(" | Temp: "); Serial.println(temperatura);

        if (beatAvg < 65 || beatAvg > 110 ){ //|| temperatura < 25.0 || temperatura > 42.0) {
          Serial.println("Configurando sensores ou valores fora da faixa normal.");
        } else {
          FirebaseJson leituraJson;
          leituraJson.set("bpm", beatAvg);
          leituraJson.set("oxigenacao", oxigenacao);
          leituraJson.set("temperatura", temperatura);
          leituraJson.set("timestamp", getCurrentTimestamp());

          String leituraPath = "/idosos/" + String(ID_IDOSO) + "/leitura/";
          if (Firebase.set(firebaseData, leituraPath, leituraJson)) {
            Serial.println("Leitura enviada ao Firebase!");
            sendToHistorico(beatAvg, temperatura);
          } else {
            Serial.println("Erro Firebase: " + firebaseData.errorReason());
          }  
        }
        delay(500);
      }
    }
  } else {
    Serial.println("Aguardando dedo no sensor...");
    delay(500);
  }
}
