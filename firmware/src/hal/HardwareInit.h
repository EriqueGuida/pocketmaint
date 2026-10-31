#pragma once

#ifndef POCKETMAINT_HARDWARE_INIT_H
#define POCKETMAINT_HARDWARE_INIT_H

#include <Arduino.h>
#include <M5Cardputer.h>

class HardwareInit {
public:
    static void begin();
    static int getBatteryLevel();
    static bool isCharging();
    static void setBrightness(uint8_t brightness);

    // Feedback sonoro
    static void beepConfirm();
    static void beepError();
    static void beepKey();
    static void tone(int frequency, int durationMs);
};

#endif // POCKETMAINT_HARDWARE_INIT_H
