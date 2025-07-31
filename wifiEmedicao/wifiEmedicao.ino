#include <Wire.h>
#include "MAX30105.h"
#include "heartRate.h"
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_MLX90614.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <BluetoothSerial.h>
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <time.h>

#define WIFI_SSID "iPhonepedroanicio"
#define WIFI_PASSWORD "pedroanicio"
#define FIREBASE_HOST "https://sensor-saude-tcc-default-rtdb.firebaseio.com/"
#define FIREBASE_AUTH "suZUvaRdamGQiVHhXYMqtAJa66l1DMoJDHxiHGx3"

// UUIDs para o serviço e característica BLE
#define SERVICE_UUID "0000ffe0-0000-1000-8000-00805f9b34fb"
#define CHARACTERISTIC_UUID "0000ffe1-0000-1000-8000-00805f9b34fb"

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

#define ID_IDOSO "fa848220-df80-4ddc-a861-5fefc8128d7d"
#define NOME_IDOSO "pedro"

MAX30105 particleSensor;
Adafruit_MPU6050 mpu;
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

BLEServer* pServer = NULL;
BLECharacteristic* pCharacteristic = NULL;
bool deviceConnected = false;
bool wifiConfigured = false;

bool quedaDetectada = false;

BluetoothSerial SerialBT;

// Callbacks para o servidor BLE
class MyServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) {
      deviceConnected = true;
      Serial.println("Dispositivo conectado via BLE");
    }
    void onDisconnect(BLEServer* pServer) {
      deviceConnected = false;
      Serial.println("Dispositivo desconectado, reiniciando anúncio BLE");
      BLEDevice::startAdvertising();
    }
};

// Callbacks para a característica BLE
class MyCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* pCharacteristic) {

      Serial.println("Callback onWrite foi chamado!");

      String command = pCharacteristic->getValue().c_str(); // Converter std::string para String
      String value = pCharacteristic->getValue();
      Serial.print("Valor recebido: ");
      Serial.println(value);
      Serial.println("Comando recebido via BLE: " + command);

      if (command.length() == 0) {
        Serial.println("Comando vazio.");
        return;
      } 

      // Parâmetros esperados: SSID=...;PASS=...
      int ssidIndex = command.indexOf("SSID=");
      int passIndex = command.indexOf("PASS=");

      // Parsear comando: SSID=xxx;PASS=yyy;ID=zzz
      if (command.startsWith("SSID=")) {
        int ssidStart = command.indexOf("SSID=") + 5;
        int ssidEnd = command.indexOf(";", ssidStart);
        int passStart = command.indexOf("PASS=") + 5;
        int passEnd = command.indexOf(";", passStart);

        String newSsid = command.substring(ssidStart, ssidEnd);
        String newPassword = command.substring(passStart, passEnd);
        
        Serial.println(String("SSID: ") + newSsid);
        Serial.println(String("Senha: ") + newPassword);
             
        // Reconectar ao Wi-Fi com novas credenciais
        WiFi.disconnect();
        WiFi.begin(newSsid.c_str(), newPassword.c_str());
        Serial.print("Conectando ao Wi-Fi...");
        unsigned long startTime = millis();
        while (WiFi.status() != WL_CONNECTED && millis() - startTime < 10000) {
          delay(500);
          Serial.print(".");
        }
        if (WiFi.status() == WL_CONNECTED) {
          Serial.println(" WiFi conectado!");
          wifiConfigured = true;
          // Atualizar Firebase com novo ID
          //if (Firebase.getString(firebaseData, "/idosos/" + newIdIdoso + "/nome") && firebaseData.stringData() != NOME_IDOSO) {
          //  Firebase.setString(firebaseData, "/idosos/" + newIdIdoso + "/nome", NOME_IDOSO);
          //}
        } else {
          Serial.println(" Falha ao conectar ao Wi-Fi");
        }
      }
    }
};

void setup() {
  Serial.begin(115200);
  Wire.begin();

  // Inicializar BLE
  BLEDevice::init("ESP32Monitor");
  pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());
  BLEService* pService = pServer->createService(SERVICE_UUID);
  pCharacteristic = pService->createCharacteristic(
      CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
      BLECharacteristic::PROPERTY_WRITE
  );
  pCharacteristic->addDescriptor(new BLE2902()); 
  pCharacteristic->setAccessPermissions(ESP_GATT_PERM_READ | ESP_GATT_PERM_WRITE);
  pCharacteristic->setCallbacks(new MyCharacteristicCallbacks());
  pService->start();
  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->setMinPreferred(0x06); // Intervalo de anúncio mais rápido
  pAdvertising->setMaxPreferred(0x12);
  BLEDevice::startAdvertising();
  Serial.println("BLE iniciado como ESP32Monitor");

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

  if (!mpu.begin()) {
    Serial.println("MPU6050 não encontrado. Verifique conexões.");
    while (1) delay(10);
  }
  Serial.println("MPU6050 inicializado!");

  mpu.setAccelerometerRange(MPU6050_RANGE_8_G);
  mpu.setGyroRange(MPU6050_RANGE_500_DEG);
  mpu.setFilterBandwidth(MPU6050_BAND_21_HZ);

  if (!mlx.begin()) {
    Serial.println("MLX90614 não encontrado. Verifique conexões.");
    while (1) delay(10);
  }
  Serial.println("MLX90614 inicializado!");

  SerialBT.begin("ESP32Monitor"); // Nome do dispositivo Bluetooth
  Serial.println("Bluetooth iniciado como ESP32Monitor");
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
  long redMean = 0, irMean = 0;
  long redAC = 0, irAC = 0;
  
  for (int i = 0; i < SAMPLES; i++) {
    redBuffer[i] = particleSensor.getRed();
    irBuffer[i] = particleSensor.getIR();
    redMean += redBuffer[i];
    irMean += irBuffer[i];
    delay(10);
  }
  redMean /= SAMPLES;
  irMean /= SAMPLES;

  for (int i = 0; i < SAMPLES; i++) {
    redAC += abs(redBuffer[i] - redMean);
    irAC += abs(irBuffer[i] - irMean);
  }
  redAC /= SAMPLES;
  irAC /= SAMPLES;

  float R = ((float)redAC / redMean) / ((float)irAC / irMean);
  float SpO2 = 110.0 - 25.0 * R;
  if(SpO2 > 100) SpO2 = 100;
  if(SpO2 < 90) SpO2 = 96; // evitar valores irreais
  return SpO2;
}

bool verificarQueda() {
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  if (abs(a.acceleration.x) > 1.5 || abs(a.acceleration.y) > 1.5 || abs(a.acceleration.z - 9.8) > 2.0) {
    return true;
  }
  return false;
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
        bool queda = verificarQueda();
        
        Serial.print("Queda: "); Serial.print(queda);
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
          leituraJson.set("queda", queda);
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
