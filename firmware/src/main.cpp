/**
 * PocketMaint — Milestone 9: Motor Gráfico e Navegação de Telas
 * 
 * Integração dos módulos de interface:
 * - UiRenderer: Motor gráfico com double-buffering via M5Canvas (RAM) com ZERO flicker.
 * - AppController: Máquina de estados de navegação de telas (SCR-001 a SCR-005).
 * - Keyboard: Navegação com as setas físicas do CardPuter (';' = Cima, '.' = Baixo).
 */

#include <Arduino.h>
#include <M5Cardputer.h>
#include "secrets.h"
#include "hal/HardwareInit.h"
#include "net/NetworkManager.h"
#include "session/SessionManager.h"
#include "ui/UiRenderer.h"
#include "app/AppController.h"

void setup() {
    // 1. Inicializa periféricos via HardwareInit
    HardwareInit::begin();

    // 2. Inicializa porta serial USB CDC
    Serial.begin(115200);
    Serial.println("\n================================================");
    Serial.println("PocketMaint — Milestone 9: Motor Grafico e Telas");
    Serial.println("================================================\n");

    // 3. Inicializa rádio Wi-Fi
    pocketNetworkManager.begin(WIFI_SSID, WIFI_PASSWORD);

    // 4. Inicializa o controlador de telas e dados mockados
    appController.begin();
}

void loop() {
    M5Cardputer.update();

    // Atualiza rotinas de rede
    pocketNetworkManager.update();

    // Atualiza máquina de estados de interface
    appController.update();

    // Tratamento de teclas físicas do teclado matricial de 56 teclas
    if (M5Cardputer.Keyboard.isChange() && M5Cardputer.Keyboard.isPressed()) {
        Keyboard_Class::KeysState keys = M5Cardputer.Keyboard.keysState();

        // 1. Tecla Enter
        if (keys.enter) {
            appController.handleKeyPress('\n', false, true, keys.fn);
        }
        // 2. Tecla Delete / Backspace / Esc
        else if (keys.del) {
            appController.handleKeyPress('\b', true, false, keys.fn);
        }
        // 3. Caracteres alfanuméricos e setas de navegação (';' cima, '.' baixo, ',' esquerda, '/' direita)
        else {
            for (auto c : keys.word) {
                appController.handleKeyPress(c, false, false, keys.fn);
            }
        }
    }

    delay(15);
}
