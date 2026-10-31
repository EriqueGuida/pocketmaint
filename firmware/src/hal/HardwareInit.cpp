#include "HardwareInit.h"

void HardwareInit::begin() {
    auto cfg = M5.config();
    M5Cardputer.begin(cfg, true);

    // Orientacao horizontal padrao para tela de 240x135
    M5Cardputer.Display.setRotation(1);
    M5Cardputer.Display.fillScreen(TFT_BLACK);

    // Brilho inicial confortavel (180 de 255)
    setBrightness(180);

    // Som curto de confirmacao de inicializacao
    tone(1200, 60);
}

int HardwareInit::getBatteryLevel() {
    int level = M5Cardputer.Power.getBatteryLevel();
    if (level < 0) return 0;
    if (level > 100) return 100;
    return level;
}

bool HardwareInit::isCharging() {
    return M5Cardputer.Power.isCharging();
}

void HardwareInit::setBrightness(uint8_t brightness) {
    M5Cardputer.Display.setBrightness(brightness);
}

void HardwareInit::beepConfirm() {
    M5Cardputer.Speaker.tone(1800, 40);
    delay(50);
    M5Cardputer.Speaker.tone(2400, 60);
}

void HardwareInit::beepError() {
    M5Cardputer.Speaker.tone(450, 150);
}

void HardwareInit::beepKey() {
    M5Cardputer.Speaker.tone(2200, 12);
}

void HardwareInit::tone(int frequency, int durationMs) {
    M5Cardputer.Speaker.tone(frequency, durationMs);
}
