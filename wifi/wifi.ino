fireb#include <FirebaseESP32.h>
#include <WiFi.h>
#include <time.h>

// Configurações WiFi
#define WIFI_SSID "Eric"
#define WIFI_PASSWORD "pedrolucas01"

// Configurações Firebase
#define FIREBASE_HOST "https://sensor-saude-tcc-default-rtdb.firebaseio.com/"
#define FIREBASE_AUTH "suZUvaRdamGQiVHhXYMqtAJa66l1DMoJDHxiHGx3"

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

// ID do idoso (deve ser único para cada dispositivo)
#define ID_IDOSO "fa848220-df80-4ddc-a861-5fefc8128d7d"
#define NOME_IDOSO "pedro"

void setup() {
  Serial.begin(115200);

  // Conectar ao WiFi
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Conectando ao Wi-Fi...");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println(" Conectado!");

  // Configurar NTP para obter timestamp
  configTime(0, 0, "pool.ntp.org");
  
  // Configurar Firebase
  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Configurar informações básicas do idoso (apenas na primeira vez)
  if(Firebase.getString(firebaseData, "/idosos/" + String(ID_IDOSO) + "/nome") && firebaseData.stringData() != NOME_IDOSO) {
    Firebase.setString(firebaseData, "/idosos/" + String(ID_IDOSO) + "/nome", NOME_IDOSO);
  }
}

String getCurrentTimestamp() {
  struct tm timeinfo;
  if(!getLocalTime(&timeinfo)){
    Serial.println("Falha ao obter tempo");
    return "2024-05-20T14:30:00Z"; // Fallback
  }
  timeinfo.tm_hour -= 3;
  
  char timestamp[25];
  strftime(timestamp, sizeof(timestamp), "%Y-%m-%dT%H:%M:%SZ", &timeinfo);
  return String(timestamp);
}

String getCurrentDate() {
  struct tm timeinfo;
  if(!getLocalTime(&timeinfo)){
    Serial.println("Falha ao obter data");
    return "2024-05-20"; // Fallback
  }
  
  char date[11];
  strftime(date, sizeof(date), "%Y-%m-%d", &timeinfo);
  return String(date);
}

String getCurrentTime() {
  struct tm timeinfo;
  if(!getLocalTime(&timeinfo)){
    Serial.println("Falha ao obter hora");
    return "14:30"; // Fallback
  }
  
  char time[6];
  strftime(time, sizeof(time), "%H:%M", &timeinfo);
  return String(time);
}

String generateUniqueID() {
  return String(millis()) + String(random(1000, 9999));
}

void sendToHistorico(int bpm) {
  String datePath = "/idosos/" + String(ID_IDOSO) + "/historico/" + getCurrentDate() + "/";
  String timeEntryPath = datePath + generateUniqueID();
  
  FirebaseJson json;
  json.set("hora", getCurrentTime());
  json.set("bpm", bpm);
  
  Firebase.set(firebaseData, timeEntryPath, json);
}

void loop() {
  // Simular dados dos sensores
  int bpm = random(70, 80);
  int oxigenacao = random(95, 100);
  float temperatura = 36.0 + (random(0, 15)) / 10.0;
  bool queda = false; // Simular sem queda
  
  // Criar JSON para a leitura atual
  FirebaseJson leituraJson;
  leituraJson.set("bpm", bpm);
  leituraJson.set("oxigenacao", oxigenacao);
  leituraJson.set("temperatura", temperatura);
  leituraJson.set("queda", queda);
  leituraJson.set("timestamp", getCurrentTimestamp());
  
  // Enviar leitura atual
  String leituraPath = "/idosos/" + String(ID_IDOSO) + "/leitura/";
  if (Firebase.set(firebaseData, leituraPath, leituraJson)) {
    Serial.println("Leitura enviada com sucesso!");
  } else {
    Serial.println("Erro ao enviar leitura: " + firebaseData.errorReason());
  }
  
  // Adicionar ao histórico
  sendToHistorico(bpm);
  
  delay(30000); // Enviar a cada 30 segundos
}
