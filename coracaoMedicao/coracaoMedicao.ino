#include <spo2_algorithm.h>
#include <heartRate.h>
#include <MAX30105.h>

MAX30105 particleSensor;

// Configurações
const int WINDOW_SIZE = 5;
int redValues[WINDOW_SIZE];
int dataIndex = 0;
long lastBeatTime = 0;
int beatCount = 0;
int bpm = 0;

void setup() {
  Serial.begin(115200);
  
  if (!particleSensor.begin(Wire, I2C_SPEED_FAST)) {
    Serial.println("Sensor não encontrado!");
    while(1);
  }

  particleSensor.setup();
  particleSensor.setPulseAmplitudeRed(0x0F);
  particleSensor.setADCRange(2048);
}

void loop() {
  int rawRed = particleSensor.getRed();
  
  // Filtro de média móvel
  redValues[dataIndex] = rawRed;
  dataIndex = (dataIndex + 1) % WINDOW_SIZE;
  
  int avgRed = 0;
  for(int i=0; i<WINDOW_SIZE; i++) avgRed += redValues[i];
  avgRed /= WINDOW_SIZE;

  // Detecção dinâmica de threshold
  static int threshold = 2000;
  static int maxValue = 0;
  
  if(avgRed > threshold + 100 && avgRed > maxValue) {
    maxValue = avgRed;
  } 
  else if(maxValue > 0 && avgRed < maxValue - 50) {
    // Pico detectado!
    if(millis() - lastBeatTime > 300) { // Evita detecções muito rápidas
      bpm = 60000 / (millis() - lastBeatTime);
      bpm = constrain(bpm, 40, 180);
      lastBeatTime = millis();
      
      Serial.print("BPM: ");
      Serial.print(bpm);
      Serial.print(" | Sinal: ");
      Serial.println(avgRed);
    }
    maxValue = 0;
    threshold = avgRed * 0.7; // Threshold dinâmico
  }

  delay(10);
}
