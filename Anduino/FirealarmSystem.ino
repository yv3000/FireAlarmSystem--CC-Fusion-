// ==========================================
// 3-FLOOR FIRE + SMOKE DETECTION SYSTEM
// FLAME â†’ BUZZER + LED
// SMOKE â†’ SERIAL ONLY
// FINAL STABLE VERSION
// ==========================================

// -------- FLOOR 1 --------
const int FLAME1_PIN = 27;
const int SMOKE1_PIN = 32;
const int PIR1_PIN   = 35;

// -------- FLOOR 2 --------
const int FLAME2_PIN = 25;
const int SMOKE2_PIN = 33;
const int PIR2_PIN   = 26;

// -------- FLOOR 3 --------
const int FLAME3_PIN = 14;
const int SMOKE3_PIN = 12;
const int PIR3_PIN   = 13;

// -------- OUTPUTS --------
const int RELAY_PIN  = 2;
const int BUZZER_PIN = 5;
const int LED1_PIN   = 18;
const int LED2_PIN   = 19;
const int LED3_PIN   = 21;

// -------- LOGIC --------
const int FIRE_LEVEL = LOW;
const unsigned long FIRE_CONFIRM_TIME = 300;   // ms
const unsigned long FIRE_DURATION = 10000;
const unsigned long PRINT_INTERVAL = 15000;

// -------- GLOBALS --------
int flame[3], smoke[3], pir[3];

bool fireActive = false;
unsigned long fireStartTime = 0;
int activeFloor = -1;

unsigned long flameLowStart[3] = {0, 0, 0};
unsigned long lastPrint[3];

// -------- LED HANDLER --------
void updateFloorLEDs(int floor, bool blink) {
  digitalWrite(LED1_PIN, (floor == 0 && blink) ? HIGH : LOW);
  digitalWrite(LED2_PIN, (floor == 1 && blink) ? HIGH : LOW);
  digitalWrite(LED3_PIN, (floor == 2 && blink) ? HIGH : LOW);
}

void printFloor(int f) {
  Serial.println("\n==============================================");
  Serial.printf("ï¿½ï¿½ FLOOR %d STATUS í´¥\n", f + 1);
  Serial.println("==============================================");
  Serial.printf("Flame: %s\n", flame[f] == LOW ? "í´¥ FIRE" : "âœ… SAFE");
  Serial.printf("Smoke: %s\n", smoke[f] == LOW ? "í²¨ SMOKE" : "âœ… CLEAN");
  Serial.printf("PIR:   %s\n", pir[f] ? "í±¤ MOTION" : "í±» NO MOTION");
  Serial.printf("Alert: %s\n", activeFloor == f ? "íº¨ ACTIVE" : "í¿¢ IDLE");
}

void setup() {
  Serial.begin(115200);
  delay(2000);

  pinMode(FLAME1_PIN, INPUT_PULLUP);
  pinMode(FLAME2_PIN, INPUT_PULLUP);
  pinMode(FLAME3_PIN, INPUT_PULLUP);

  pinMode(SMOKE1_PIN, INPUT);
  pinMode(PIR1_PIN, INPUT);
  pinMode(SMOKE2_PIN, INPUT);
  pinMode(PIR2_PIN, INPUT);
  pinMode(SMOKE3_PIN, INPUT);
  pinMode(PIR3_PIN, INPUT);

  pinMode(RELAY_PIN, OUTPUT);  digitalWrite(RELAY_PIN, HIGH);
  pinMode(BUZZER_PIN, OUTPUT); digitalWrite(BUZZER_PIN, LOW);
  pinMode(LED1_PIN, OUTPUT);
  pinMode(LED2_PIN, OUTPUT);
  pinMode(LED3_PIN, OUTPUT);

  unsigned long now = millis();
  for (int i = 0; i < 3; i++) lastPrint[i] = now;

  Serial.println("\ní´¥ FIRE SYSTEM READY (STABLE)");
}

void loop() {
  unsigned long now = millis();

  // -------- READ SENSORS --------
  flame[0] = digitalRead(FLAME1_PIN);
  smoke[0] = digitalRead(SMOKE1_PIN);
  pir[0]   = digitalRead(PIR1_PIN);

  flame[1] = digitalRead(FLAME2_PIN);
  smoke[1] = digitalRead(SMOKE2_PIN);
  pir[1]   = digitalRead(PIR2_PIN);

  flame[2] = digitalRead(FLAME3_PIN);
  smoke[2] = digitalRead(SMOKE3_PIN);
  pir[2]   = digitalRead(PIR3_PIN);

  // -------- FLAME CONFIRMATION --------
  if (!fireActive) {
    for (int i = 0; i < 3; i++) {

      if (flame[i] == FIRE_LEVEL) {
        if (flameLowStart[i] == 0) {
          flameLowStart[i] = now;
        }
        else if (now - flameLowStart[i] >= FIRE_CONFIRM_TIME) {
          fireActive = true;
          fireStartTime = now;
          activeFloor = i;

          Serial.printf("\níº¨íº¨íº¨ FIRE CONFIRMED ON FLOOR %d íº¨íº¨íº¨\n", i + 1);
          printFloor(i);
          break;
        }
      } 
      else {
        flameLowStart[i] = 0;
      }
    }
  }

  // -------- OUTPUT CONTROL --------
  if (fireActive && now - fireStartTime < FIRE_DURATION) {
    digitalWrite(BUZZER_PIN, HIGH);
    digitalWrite(RELAY_PIN, LOW);

    bool blink = (now % 400 < 200);
    updateFloorLEDs(activeFloor, blink);
  }
  else if (fireActive) {
    fireActive = false;
    activeFloor = -1;

    digitalWrite(BUZZER_PIN, LOW);
    digitalWrite(RELAY_PIN, HIGH);
    digitalWrite(LED1_PIN, LOW);
    digitalWrite(LED2_PIN, LOW);
    digitalWrite(LED3_PIN, LOW);

    Serial.println("\nâœ… FIRE CLEARED â€” SYSTEM NORMAL\n");
  }

  // -------- SERIAL MONITOR --------
  if (!fireActive) {
    for (int i = 0; i < 3; i++) {
      if (now - lastPrint[i] >= PRINT_INTERVAL) {
        printFloor(i);
        lastPrint[i] = now;
      }
    }
  }
}
