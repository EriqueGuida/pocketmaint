#include "UiRenderer.h"

UiRenderer uiRenderer;

UiRenderer::UiRenderer()
    : _canvas(&M5Cardputer.Display)
{
}

void UiRenderer::begin() {
    _canvas.createSprite(240, 135);
    _canvas.setTextWrap(false);
}

void UiRenderer::clear(uint16_t bgColor) {
    _canvas.fillScreen(bgColor);
}

void UiRenderer::drawHeader(const char* screenTitle, bool wifiConnected, int batteryPercent, const char* username) {
    // Barra de topo azul escuro elegante (240 x 18 px)
    _canvas.fillRect(0, 0, 240, 18, 0x0012);

    // Titulo ou modulo a esquerda
    _canvas.setTextSize(1);
    _canvas.setTextColor(TFT_CYAN, 0x0012);
    _canvas.drawString(screenTitle != nullptr ? screenTitle : "POCKETMAINT", 6, 4);

    // Indicador central de Wi-Fi ou usuario
    if (username != nullptr && strlen(username) > 0) {
        _canvas.setTextColor(TFT_WHITE, 0x0012);
        char userStr[32];
        snprintf(userStr, sizeof(userStr), "[%s]", username);
        _canvas.drawString(userStr, 100, 4);
    } else {
        if (wifiConnected) {
            _canvas.setTextColor(TFT_GREEN, 0x0012);
            _canvas.drawString("WiFi:OK", 104, 4);
        } else {
            _canvas.setTextColor(TFT_YELLOW, 0x0012);
            _canvas.drawString("WiFi:--", 104, 4);
        }
    }

    // Indicador de bateria a direita
    char batStr[16];
    if (batteryPercent >= 0) {
        snprintf(batStr, sizeof(batStr), "%d%%", batteryPercent);
    } else {
        snprintf(batStr, sizeof(batStr), "--%%");
    }
    uint16_t batColor = (batteryPercent > 20) ? TFT_GREEN : TFT_RED;
    _canvas.setTextColor(batColor, 0x0012);
    _canvas.drawRightString(batStr, 234, 4);

    // Linha divisoria sutil
    _canvas.drawFastHLine(0, 18, 240, TFT_DARKGRAY);
}

void UiRenderer::drawFooter(const char* shortcutsText) {
    // Linha divisoria superior do rodape
    _canvas.drawFastHLine(0, 115, 240, TFT_DARKGRAY);

    // Barra de rodape com fundo azul marinho invertido (240 x 20 px)
    _canvas.fillRect(0, 116, 240, 19, 0x18C3);

    _canvas.setTextSize(1);
    _canvas.setTextColor(TFT_WHITE, 0x18C3);
    _canvas.drawString(shortcutsText != nullptr ? shortcutsText : "", 6, 120);
}

void UiRenderer::drawText(const char* text, int x, int y, uint16_t color, uint8_t size) {
    _canvas.setTextSize(size);
    _canvas.setTextColor(color);
    _canvas.drawString(text, x, y);
}

void UiRenderer::drawTextRight(const char* text, int x, int y, uint16_t color, uint8_t size) {
    _canvas.setTextSize(size);
    _canvas.setTextColor(color);
    _canvas.drawRightString(text, x, y);
}

void UiRenderer::drawTextCenter(const char* text, int y, uint16_t color, uint8_t size) {
    _canvas.setTextSize(size);
    _canvas.setTextColor(color);
    _canvas.drawCenterString(text, 120, y);
}

void UiRenderer::drawRect(int x, int y, int w, int h, uint16_t color) {
    _canvas.drawRect(x, y, w, h, color);
}

void UiRenderer::fillRect(int x, int y, int w, int h, uint16_t color) {
    _canvas.fillRect(x, y, w, h, color);
}

void UiRenderer::drawFastHLine(int x, int y, int w, uint16_t color) {
    _canvas.drawFastHLine(x, y, w, color);
}

void UiRenderer::flush() {
    _canvas.pushSprite(0, 0);
}
