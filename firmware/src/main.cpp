/**
 * PocketMaint — Milestone 7: Sketch de Diagnóstico e Validação Física do Hardware
 * 
 * Validação dos 3 periféricos essenciais do M5Stack CardPuter ADV:
 * 1. Display TFT LCD 1.14" (240x135 pixels, ST7789V2)
 * 2. Leitura de nível e status da bateria (Power Management)
 * 3. Matriz do teclado físico de 56 teclas (detecção de teclas em tempo real)
 */

#include <Arduino.h>
#include <M5Cardputer.h>
#include <string>

// Buffer para armazenar os caracteres digitados no teste
static std::string typedText = "";
static char lastChar = '-';
static int lastBattery = -1;

void drawHeader(int batteryLevel) {
    // Barra de topo azul escuro (240 x 20 px)
    M5Cardputer.Display.fillRect(0, 0, 240, 20, 0x0012); // Navy escuro
    M5Cardputer.Display.setTextSize(1);
    M5Cardputer.Display.setTextColor(TFT_CYAN, 0x0012);
    M5Cardputer.Display.drawString("POCKETMAINT", 8, 5);

    // Indicador de bateria
    char batStr[16];
    if (batteryLevel >= 0) {
        snprintf(batStr, sizeof(batStr), "BAT: %d%%", batteryLevel);
    } else {
        snprintf(batStr, sizeof(batStr), "BAT: N/A");
    }
    uint16_t batColor = (batteryLevel > 20) ? TFT_GREEN : TFT_RED;
    M5Cardputer.Display.setTextColor(batColor, 0x0012);
    M5Cardputer.Display.drawRightString(batStr, 232, 5);
}

void drawStaticUI() {
    // Linha separadora do cabeçalho
    M5Cardputer.Display.drawFastHLine(0, 20, 240, TFT_DARKGRAY);

    // Título e identificação do teste
    M5Cardputer.Display.setTextSize(1);
    M5Cardputer.Display.setTextColor(TFT_YELLOW, TFT_BLACK);
    M5Cardputer.Display.drawString("TESTE DE HARDWARE (VAL-01)", 8, 25);

    M5Cardputer.Display.setTextColor(TFT_LIGHTGRAY, TFT_BLACK);
    M5Cardputer.Display.drawString("Display: OK (240x135) | ESP32-S3", 8, 38);
    M5Cardputer.Display.drawString("Digite no teclado de 56 teclas:", 8, 51);

    // Moldura da caixa de texto interativa
    M5Cardputer.Display.drawRect(6, 64, 228, 48, TFT_GREEN);
    M5Cardputer.Display.fillRect(7, 65, 226, 46, 0x0841); // Fundo cinza bem escuro

    // Barra de rodapé com atalhos
    M5Cardputer.Display.fillRect(0, 118, 240, 17, 0x18C3);
    M5Cardputer.Display.setTextColor(TFT_WHITE, 0x18C3);
    M5Cardputer.Display.drawString("[DEL] Apagar   [ENTER] Limpar", 8, 122);
}

void updateInputDisplay() {
    // Limpa a área interna da caixa de texto
    M5Cardputer.Display.fillRect(8, 66, 224, 44, 0x0841);

    // Exibe a última tecla pressionada em destaque
    M5Cardputer.Display.setTextSize(1);
    M5Cardputer.Display.setTextColor(TFT_ORANGE, 0x0841);
    char lastKeyStr[32];
    snprintf(lastKeyStr, sizeof(lastKeyStr), "Ultima tecla: [%c]", lastChar);
    M5Cardputer.Display.drawString(lastKeyStr, 12, 70);

    // Exibe o texto digitado até o momento
    M5Cardputer.Display.setTextColor(TFT_WHITE, 0x0841);
    std::string displayStr = "> " + typedText;
    
    // Se passar da largura da tela, exibe apenas os caracteres finais
    if (displayStr.length() > 24) {
        displayStr = "..." + displayStr.substr(displayStr.length() - 21);
    }
    M5Cardputer.Display.drawString(displayStr.c_str(), 12, 88);
}

void setup() {
    auto cfg = M5.config();
    M5Cardputer.begin(cfg, true);

    // Configuração de orientação horizontal e inicialização da tela
    M5Cardputer.Display.setRotation(1);
    M5Cardputer.Display.fillScreen(TFT_BLACK);

    // Log na porta serial USB CDC
    Serial.begin(115200);
    Serial.println("\n=================================");
    Serial.println("PocketMaint - Diagnostic Bring-up");
    Serial.println("Hardware: M5Stack CardPuter ADV");
    Serial.println("=================================\n");

    // Som de bip curto de inicialização (500Hz por 80ms)
    M5Cardputer.Speaker.tone(1000, 80);

    lastBattery = M5Cardputer.Power.getBatteryLevel();
    drawHeader(lastBattery);
    drawStaticUI();
    updateInputDisplay();
}

void loop() {
    M5Cardputer.update();

    // Atualiza leitura de bateria periodicamente
    static unsigned long lastBatteryCheck = 0;
    if (millis() - lastBatteryCheck > 3000) {
        lastBatteryCheck = millis();
        int currentBattery = M5Cardputer.Power.getBatteryLevel();
        if (currentBattery != lastBattery) {
            lastBattery = currentBattery;
            drawHeader(lastBattery);
        }
    }

    // Leitura do teclado físico de 56 teclas
    if (M5Cardputer.Keyboard.isChange()) {
        if (M5Cardputer.Keyboard.isPressed()) {
            Keyboard_Class::KeysState status = M5Cardputer.Keyboard.keysState();

            // Processa caracteres alfanuméricos digitados
            for (auto c : status.word) {
                lastChar = c;
                if (typedText.length() < 60) {
                    typedText += c;
                }
                // Pequeno feedback de clique sonoro
                M5Cardputer.Speaker.tone(2000, 15);
                Serial.printf("Key pressed: '%c' (ASCII %d)\n", c, (int)c);
            }

            // Tecla Delete / Backspace
            if (status.del) {
                lastChar = '<';
                if (!typedText.empty()) {
                    typedText.pop_back();
                }
                M5Cardputer.Speaker.tone(1500, 20);
                Serial.println("Key: BACKSPACE");
            }

            // Tecla Enter
            if (status.enter) {
                lastChar = 'E';
                typedText.clear();
                M5Cardputer.Speaker.tone(800, 40);
                Serial.println("Key: ENTER (Cleared)");
            }

            updateInputDisplay();
        }
    }

    delay(10);
}
