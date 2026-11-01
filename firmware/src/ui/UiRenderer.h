#pragma once

#ifndef POCKETMAINT_UI_RENDERER_H
#define POCKETMAINT_UI_RENDERER_H

#include <Arduino.h>
#include <M5Cardputer.h>

/**
 * PocketMaint — UiRenderer
 * 
 * Gerencia a renderizacao grafica no display de 240x135 px:
 * - Double-buffering via M5Canvas (RAM) para ZERO flicker.
 * - Barra de topo padronizada (18 px): App, Wi-Fi, Usuario, Bateria.
 * - Area central util (97 px): Conteudo denso e legivel.
 * - Barra de rodape padronizada (20 px): Fundo invertido com legenda de atalhos.
 */
class UiRenderer {
public:
    UiRenderer();

    // Inicializa o sprite em RAM com a resolucao nativa de 240x135
    void begin();

    // Limpa o buffer de tela em RAM
    void clear(uint16_t bgColor = TFT_BLACK);

    // Barra de topo (18 px)
    void drawHeader(const char* screenTitle, bool wifiConnected, int batteryPercent, const char* username = nullptr);

    // Barra de rodape (20 px)
    void drawFooter(const char* shortcutsText);

    // Metodos auxiliares de desenho
    void drawText(const char* text, int x, int y, uint16_t color = TFT_WHITE, uint8_t size = 1);
    void drawTextRight(const char* text, int x, int y, uint16_t color = TFT_WHITE, uint8_t size = 1);
    void drawTextCenter(const char* text, int y, uint16_t color = TFT_WHITE, uint8_t size = 1);

    // Overloads para Arduino String / StringSumHelper
    void drawText(const String& text, int x, int y, uint16_t color = TFT_WHITE, uint8_t size = 1) { drawText(text.c_str(), x, y, color, size); }
    void drawTextRight(const String& text, int x, int y, uint16_t color = TFT_WHITE, uint8_t size = 1) { drawTextRight(text.c_str(), x, y, color, size); }
    void drawTextCenter(const String& text, int y, uint16_t color = TFT_WHITE, uint8_t size = 1) { drawTextCenter(text.c_str(), y, color, size); }

    void drawRect(int x, int y, int w, int h, uint16_t color);
    void fillRect(int x, int y, int w, int h, uint16_t color);
    void drawFastHLine(int x, int y, int w, uint16_t color);

    // Transfere o buffer completo de RAM para o display LCD fisico
    void flush();

    M5Canvas& getCanvas() { return _canvas; }

private:
    M5Canvas _canvas;
};

extern UiRenderer uiRenderer;

#endif // POCKETMAINT_UI_RENDERER_H
