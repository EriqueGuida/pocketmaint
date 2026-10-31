/**
 * PocketMaint — Milestone 8: Módulos de Infraestrutura (Wi-Fi, Bateria e Sessão em RAM)
 * 
 * Versão com Canvas/Sprite em memória RAM (Double-Buffering):
 * - ZERO flicker / cintilação: todo o desenho ocorre em RAM e é transferido via DMA.
 * - Filtro passa-baixa e histerese na bateria: elimina o recálculo contínuo do ADC.
 * - Atualizações estáveis a cada 1 segundo sem apagar a tela fisicamente.
 */

#include <Arduino.h>
#include <M5Cardputer.h>
#include "secrets.h"
#include "hal/HardwareInit.h"
#include "net/NetworkManager.h"
#include "session/SessionManager.h"

// Sprite de tela cheia (240 x 135) para double-buffering em RAM (elimina 100% do flicker)
static M5Canvas canvas(&M5Cardputer.Display);

// Variáveis com filtro para exibição estável
static int filteredBattery = -1;
static int filteredRssi = -100;
static unsigned long lastBatterySampleMs = 0;
static unsigned long lastRssiSampleMs = 0;
static unsigned long lastUiRenderMs = 0;

int readFilteredBattery() {
    int raw = HardwareInit::getBatteryLevel();
    if (filteredBattery < 0) {
        filteredBattery = raw;
    } else {
        // Filtro passa-baixa: 80% do valor anterior + 20% do novo (suaviza ruído do ADC)
        filteredBattery = (int)(filteredBattery * 0.8f + raw * 0.2f);
    }
    return filteredBattery;
}

void renderUI() {
    // 1. Limpa o buffer em RAM (não na tela LCD física)
    canvas.fillScreen(TFT_BLACK);

    // 2. Cabeçalho (Barra azul escuro 240x20)
    canvas.fillRect(0, 0, 240, 20, 0x0012);
    canvas.setTextSize(1);
    canvas.setTextColor(TFT_CYAN, 0x0012);
    canvas.drawString("POCKETMAINT", 8, 5);

    // Indicador central de Wi-Fi
    if (pocketNetworkManager.isConnected()) {
        canvas.setTextColor(TFT_GREEN, 0x0012);
        canvas.drawString("WiFi: OK", 100, 5);
    } else {
        canvas.setTextColor(TFT_YELLOW, 0x0012);
        canvas.drawString("WiFi: ...", 100, 5);
    }

    // Indicador direito de bateria estável
    char batStr[16];
    snprintf(batStr, sizeof(batStr), "BAT: %d%%", filteredBattery);
    uint16_t batColor = (filteredBattery > 20) ? TFT_GREEN : TFT_RED;
    canvas.setTextColor(batColor, 0x0012);
    canvas.drawRightString(batStr, 232, 5);

    canvas.drawFastHLine(0, 20, 240, TFT_DARKGRAY);

    // 3. Corpo da tela
    canvas.setTextColor(TFT_YELLOW, TFT_BLACK);
    canvas.drawString("INFRAESTRUTURA & REDE (M-08)", 8, 26);

    canvas.setTextColor(TFT_LIGHTGRAY, TFT_BLACK);
    char ssidBuf[64];
    snprintf(ssidBuf, sizeof(ssidBuf), "SSID : %s", pocketNetworkManager.getSsid().c_str());
    canvas.drawString(ssidBuf, 8, 42);

    if (pocketNetworkManager.isConnected()) {
        char ipBuf[64];
        snprintf(ipBuf, sizeof(ipBuf), "IP   : %s", pocketNetworkManager.getIpAddress().c_str());
        canvas.setTextColor(TFT_GREEN, TFT_BLACK);
        canvas.drawString(ipBuf, 8, 56);

        char rssiBuf[64];
        snprintf(rssiBuf, sizeof(rssiBuf), "Sinal: %s (%d dBm | %d%%)",
                 pocketNetworkManager.getSignalQualityText().c_str(),
                 filteredRssi,
                 pocketNetworkManager.getSignalQualityPercent());
        canvas.setTextColor(TFT_WHITE, TFT_BLACK);
        canvas.drawString(rssiBuf, 8, 70);

        char gwBuf[64];
        snprintf(gwBuf, sizeof(gwBuf), "GW   : %s", pocketNetworkManager.getGatewayIp().c_str());
        canvas.setTextColor(TFT_DARKGRAY, TFT_BLACK);
        canvas.drawString(gwBuf, 8, 84);
    } else {
        canvas.setTextColor(TFT_ORANGE, TFT_BLACK);
        canvas.drawString("IP   : Conectando a rede...", 8, 56);
        canvas.setTextColor(TFT_DARKGRAY, TFT_BLACK);
        canvas.drawString("Sinal: Aguardando handshake...", 8, 70);
        canvas.drawString("Aguarde a associacao 2.4 GHz", 8, 84);
    }

    // Status da Sessão em RAM
    canvas.setTextColor(TFT_CYAN, TFT_BLACK);
    if (sessionManager.isAuthenticated()) {
        canvas.drawString("Sessao RAM: Autenticado", 8, 98);
    } else {
        canvas.drawString("Sessao RAM: Pronta (Nao autenticado)", 8, 98);
    }

    // 4. Rodapé de atalhos
    canvas.fillRect(0, 118, 240, 17, 0x18C3);
    canvas.setTextColor(TFT_WHITE, 0x18C3);
    canvas.drawString("[R] Reconectar      [B] Beep Som", 8, 122);

    // 5. Transfere todo o quadro pronto de uma só vez para o display LCD físico
    canvas.pushSprite(0, 0);
}

void setup() {
    // 1. Inicializa periféricos via HardwareInit
    HardwareInit::begin();

    // 2. Aloca o buffer Sprite na memória RAM (240x135 pixels em 16-bit = 64KB)
    canvas.createSprite(240, 135);

    // 3. Inicializa leitura estável inicial de bateria
    filteredBattery = HardwareInit::getBatteryLevel();

    // 4. Inicializa porta serial USB CDC
    Serial.begin(115200);
    Serial.println("\n===========================================");
    Serial.println("PocketMaint — Milestone 8: Infraestrutura");
    Serial.println("===========================================\n");

    // 5. Inicia o rádio Wi-Fi
    pocketNetworkManager.begin(WIFI_SSID, WIFI_PASSWORD);

    // 6. Desenha primeiro quadro estável
    renderUI();
}

void loop() {
    M5Cardputer.update();

    // Atualiza rotinas de rede
    pocketNetworkManager.update();

    unsigned long now = millis();

    // Amostragem de bateria a cada 5 segundos (elimina o jitter do ADC)
    if (now - lastBatterySampleMs > 5000) {
        lastBatterySampleMs = now;
        readFilteredBattery();
    }

    // Amostragem de sinal RSSI a cada 3 segundos
    if (now - lastRssiSampleMs > 3000) {
        lastRssiSampleMs = now;
        filteredRssi = pocketNetworkManager.getRssi();
    }

    // Atualiza a tela a cada 1 segundo sem qualquer flicker
    if (now - lastUiRenderMs > 1000) {
        lastUiRenderMs = now;
        renderUI();
    }

    // Tratamento de teclas físicas de atalho
    if (M5Cardputer.Keyboard.isChange() && M5Cardputer.Keyboard.isPressed()) {
        Keyboard_Class::KeysState keys = M5Cardputer.Keyboard.keysState();

        for (auto c : keys.word) {
            if (c == 'r' || c == 'R') {
                HardwareInit::beepKey();
                Serial.println("[Teclado] Solicitada reconexao Wi-Fi manual.");
                pocketNetworkManager.reconnect();
                renderUI();
            } else if (c == 'b' || c == 'B') {
                Serial.println("[Teclado] Teste de audio de confirmacao.");
                HardwareInit::beepConfirm();
            }
        }
    }

    delay(20);
}
