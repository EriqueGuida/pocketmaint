#include "AppController.h"
#include "../ui/UiRenderer.h"
#include "../hal/HardwareInit.h"
#include "../net/NetworkManager.h"
#include "../session/SessionManager.h"
#include "secrets.h"

AppController appController;

AppController::AppController()
    : _currentScreen(SCR_BOOT)
    , _screenEnterTimeMs(0)
    , _screenNeedsRedraw(true)
    , _loginActiveField(0)
    , _loginUser("tec.carlos")
    , _loginPass("password")
    , _selectedOrderIndex(0)
    , _scrollOffset(0)
    , _detailScrollLine(0)
{
}

void AppController::begin() {
    uiRenderer.begin();
    initMockData();
    setScreen(SCR_BOOT);
}

void AppController::initMockData() {
    _mockOrders.clear();

    _mockOrders.push_back({
        1L,
        "OS-2026-0001",
        "Substituir rolamento motor",
        "ASSIGNED",
        "ALTA",
        "Compressor Parafuso (EQ-100)",
        "Setor Industrial (SEC-01)",
        "Equipamento apresentando vibracao excessiva no mancal do motor principal. Substituir conjunto de rolamentos e filtro de oleo lubrificante conforme manual."
    });

    _mockOrders.push_back({
        2L,
        "OS-2026-0002",
        "Calibracao sensor pressao",
        "IN_PROGRESS",
        "CRITICA",
        "Torno CNC 02 (EQ-104)",
        "Usinagem de Precisao (SEC-02)",
        "Pressostato de seguranca indicando corte indevido da bomba refrigerante. Necessario calibrar ponto de desarme e inspecionar fiacao do sensor."
    });

    _mockOrders.push_back({
        3L,
        "OS-2026-0003",
        "Troca retentor hidraulico",
        "ASSIGNED",
        "MEDIA",
        "Prensa Hidraulica 50T (EQ-108)",
        "Estamparia Pesada (SEC-03)",
        "Vazamento visivel de fluido no cilindro de compressao. Substituir gaxetas de vedacao e verificar nivel do reservatorio ISO VG 68."
    });

    _mockOrders.push_back({
        4L,
        "OS-2026-0004",
        "Inspecao preventiva geral",
        "ASSIGNED",
        "BAIXA",
        "Furadeira Radial (EQ-112)",
        "Manutencao Geral (SEC-01)",
        "Lubrificacao das guias lineares e verificacao da folga da arvore de furacao. Intervencao semestral programada."
    });
}

void AppController::setScreen(ScreenId screen) {
    _currentScreen = screen;
    _screenEnterTimeMs = millis();
    _screenNeedsRedraw = true;
    _detailScrollLine = 0;

    Serial.printf("[AppController] Mudanca de tela -> SCR-%03d\n", (int)screen);
}

void AppController::update() {
    unsigned long now = millis();

    // Transicao automatica da tela de boot apos 2.5 segundos
    if (_currentScreen == SCR_BOOT && (now - _screenEnterTimeMs > 2500)) {
        setScreen(SCR_CONN_STATUS);
        return;
    }

    // Redesenha tela quando necessario ou a cada 1 segundo para atualizar status
    static unsigned long lastTick = 0;
    if (_screenNeedsRedraw || (now - lastTick > 1000)) {
        lastTick = now;
        _screenNeedsRedraw = false;
        renderScreen();
    }
}

void AppController::renderScreen() {
    switch (_currentScreen) {
        case SCR_BOOT:
            renderBootScreen();
            break;
        case SCR_CONN_STATUS:
            renderConnStatusScreen();
            break;
        case SCR_LOGIN:
            renderLoginScreen();
            break;
        case SCR_ORDER_LIST:
            renderOrderListScreen();
            break;
        case SCR_ORDER_DETAIL:
            renderOrderDetailScreen();
            break;
    }
    uiRenderer.flush();
}

// =============================================================================
// SCR-001: Boot / Splash Screen
// =============================================================================
void AppController::renderBootScreen() {
    uiRenderer.clear(TFT_BLACK);

    uiRenderer.drawHeader("POCKETMAINT", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.drawTextCenter("POCKETMAINT", 34, TFT_CYAN, 2);
    uiRenderer.drawTextCenter("Terminal Portatil de Manutencao", 58, TFT_WHITE, 1);
    uiRenderer.drawTextCenter("Versao v1.1.0-mvp | ESP32-S3", 74, TFT_GREEN, 1);
    uiRenderer.drawTextCenter("Inicializando modulos...", 92, TFT_LIGHTGRAY, 1);

    uiRenderer.drawFooter("[ENTER] Avancar");
}

// =============================================================================
// SCR-002: Status de Conexao
// =============================================================================
void AppController::renderConnStatusScreen() {
    uiRenderer.clear(TFT_BLACK);

    uiRenderer.drawHeader("CONEXAO", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.drawText("STATUS DE REDE (SCR-002)", 8, 24, TFT_YELLOW, 1);

    char buf[80];
    snprintf(buf, sizeof(buf), "SSID  : %s", pocketNetworkManager.getSsid().c_str());
    uiRenderer.drawText(buf, 8, 38, TFT_WHITE, 1);

    if (pocketNetworkManager.isConnected()) {
        snprintf(buf, sizeof(buf), "IP    : %s", pocketNetworkManager.getIpAddress().c_str());
        uiRenderer.drawText(buf, 8, 52, TFT_GREEN, 1);

        snprintf(buf, sizeof(buf), "Sinal : %s (%d dBm | %d%%)",
                 pocketNetworkManager.getSignalQualityText().c_str(),
                 pocketNetworkManager.getRssi(),
                 pocketNetworkManager.getSignalQualityPercent());
        uiRenderer.drawText(buf, 8, 66, TFT_WHITE, 1);

        snprintf(buf, sizeof(buf), "API   : %s", API_BASE_URL);
        uiRenderer.drawText(buf, 8, 80, TFT_CYAN, 1);

        uiRenderer.drawText("Link  : ATIVO (Pronto para login)", 8, 96, TFT_GREEN, 1);
    } else {
        uiRenderer.drawText("IP    : Conectando a rede...", 8, 52, TFT_ORANGE, 1);
        uiRenderer.drawText("Sinal : Buscando sinal 2.4 GHz...", 8, 66, TFT_DARKGRAY, 1);
        uiRenderer.drawText("Aguarde a associacao Wi-Fi...", 8, 84, TFT_YELLOW, 1);
    }

    uiRenderer.drawFooter("[ENTER] Login    [R] Reconectar");
}

// =============================================================================
// SCR-003: Formulario de Login
// =============================================================================
void AppController::renderLoginScreen() {
    uiRenderer.clear(TFT_BLACK);

    uiRenderer.drawHeader("LOGIN", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.drawText("AUTENTICACAO DO TECNICO (SCR-003)", 8, 24, TFT_YELLOW, 1);

    // Campo 1: Usuario
    bool userActive = (_loginActiveField == 0);
    uint16_t userBoxColor = userActive ? TFT_GREEN : TFT_DARKGRAY;
    uiRenderer.drawText("Usuario:", 8, 42, userActive ? TFT_GREEN : TFT_WHITE, 1);
    uiRenderer.drawRect(65, 38, 168, 18, userBoxColor);
    uiRenderer.fillRect(66, 39, 166, 16, 0x0841);
    uiRenderer.drawText(_loginUser.c_str(), 70, 43, TFT_WHITE, 1);
    if (userActive) {
        int cursorX = 70 + _loginUser.length() * 6;
        uiRenderer.drawFastHLine(cursorX, 51, 6, TFT_GREEN);
    }

    // Campo 2: Senha (com mascara de asteriscos)
    bool passActive = (_loginActiveField == 1);
    uint16_t passBoxColor = passActive ? TFT_GREEN : TFT_DARKGRAY;
    uiRenderer.drawText("Senha  :", 8, 66, passActive ? TFT_GREEN : TFT_WHITE, 1);
    uiRenderer.drawRect(65, 62, 168, 18, passBoxColor);
    uiRenderer.fillRect(66, 63, 166, 16, 0x0841);

    String maskedPass = "";
    for (size_t i = 0; i < _loginPass.length(); i++) maskedPass += "*";
    uiRenderer.drawText(maskedPass.c_str(), 70, 67, TFT_WHITE, 1);
    if (passActive) {
        int cursorX = 70 + _loginPass.length() * 6;
        uiRenderer.drawFastHLine(cursorX, 75, 6, TFT_GREEN);
    }

    uiRenderer.drawText("Dica: tec.carlos / password", 8, 92, TFT_DARKGRAY, 1);

    uiRenderer.drawFooter("[;/.] Campo  [ENTER] Entrar");
}

// =============================================================================
// SCR-004: Lista de Ordens de Servico
// =============================================================================
void AppController::renderOrderListScreen() {
    uiRenderer.clear(TFT_BLACK);

    const char* user = sessionManager.isAuthenticated() ? sessionManager.getUsername().c_str() : "tec.carlos";
    uiRenderer.drawHeader("MINHAS ORDENS", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel(), user);

    // Ajusta o offset para rolar quando a lista for maior que o espaco da tela
    int maxVisible = 3;
    if (_selectedOrderIndex < _scrollOffset) {
        _scrollOffset = _selectedOrderIndex;
    }
    if (_selectedOrderIndex >= _scrollOffset + maxVisible) {
        _scrollOffset = _selectedOrderIndex - maxVisible + 1;
    }

    int startY = 22;
    for (int i = 0; i < maxVisible; i++) {
        int orderIdx = _scrollOffset + i;
        if (orderIdx >= (int)_mockOrders.size()) break;

        const MockOrder& order = _mockOrders[orderIdx];
        bool isSelected = (orderIdx == _selectedOrderIndex);

        int boxY = startY + (i * 30);

        // Fundo do item selecionado
        if (isSelected) {
            uiRenderer.fillRect(4, boxY, 232, 28, 0x01E0); // Verde escuro destacado
            uiRenderer.drawRect(4, boxY, 232, 28, TFT_GREEN);
            uiRenderer.drawText(">", 7, boxY + 4, TFT_YELLOW, 1);
        } else {
            uiRenderer.fillRect(4, boxY, 232, 28, 0x10A2); // Cinza azulado suave
            uiRenderer.drawRect(4, boxY, 232, 28, TFT_DARKGRAY);
        }

        // Linha 1: Numero da OS e Titulo
        char line1[64];
        snprintf(line1, sizeof(line1), "%s: %s", order.orderNumber.c_str(), order.title.c_str());
        line1[26] = '\0'; // Limita largura
        uiRenderer.drawText(line1, isSelected ? 16 : 8, boxY + 3, isSelected ? TFT_WHITE : TFT_LIGHTGRAY, 1);

        // Linha 2: Status formatado e prioridade
        uint16_t statusColor = TFT_YELLOW;
        if (order.status == "IN_PROGRESS") statusColor = TFT_CYAN;
        if (order.status == "COMPLETED") statusColor = TFT_GREEN;

        char statusTag[32];
        snprintf(statusTag, sizeof(statusTag), "[%s]", order.status.c_str());
        uiRenderer.drawText(statusTag, isSelected ? 16 : 8, boxY + 15, statusColor, 1);

        char priorityTag[32];
        snprintf(priorityTag, sizeof(priorityTag), "Prio: %s", order.priority.c_str());
        uiRenderer.drawTextRight(priorityTag, 230, boxY + 15, TFT_WHITE, 1);
    }

    uiRenderer.drawFooter("[;/.] Sel  [ENTER] Detalhe  [L] Sair");
}

// =============================================================================
// SCR-005: Detalhes da Ordem de Servico
// =============================================================================
void AppController::renderOrderDetailScreen() {
    uiRenderer.clear(TFT_BLACK);

    if (_selectedOrderIndex < 0 || _selectedOrderIndex >= (int)_mockOrders.size()) {
        setScreen(SCR_ORDER_LIST);
        return;
    }

    const MockOrder& order = _mockOrders[_selectedOrderIndex];

    char titleBuf[32];
    snprintf(titleBuf, sizeof(titleBuf), "%s", order.orderNumber.c_str());
    uiRenderer.drawHeader(titleBuf, pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    // Linha 1: Status e Prioridade
    uint16_t statusColor = TFT_YELLOW;
    if (order.status == "IN_PROGRESS") statusColor = TFT_CYAN;
    if (order.status == "COMPLETED") statusColor = TFT_GREEN;

    char line1[64];
    snprintf(line1, sizeof(line1), "Status: %s", order.status.c_str());
    uiRenderer.drawText(line1, 8, 23, statusColor, 1);

    char prioStr[32];
    snprintf(prioStr, sizeof(prioStr), "Prio: %s", order.priority.c_str());
    uiRenderer.drawTextRight(prioStr, 232, 23, TFT_WHITE, 1);

    // Linha 2: Equipamento
    uiRenderer.drawText("Equip:", 8, 36, TFT_LIGHTGRAY, 1);
    uiRenderer.drawText(order.equipment.c_str(), 45, 36, TFT_CYAN, 1);

    // Linha 3: Setor
    uiRenderer.drawText("Setor:", 8, 49, TFT_LIGHTGRAY, 1);
    uiRenderer.drawText(order.sector.c_str(), 45, 49, TFT_WHITE, 1);

    // Caixa de Defeito / Problema (com suporte a rolagem)
    uiRenderer.drawFastHLine(6, 62, 228, TFT_DARKGRAY);
    uiRenderer.drawText("Defeito Reportado:", 8, 65, TFT_YELLOW, 1);

    // Quebra a descricao em linhas legiveis
    int startY = 77;
    int maxCharsPerLine = 36;
    int lineCount = 0;
    for (size_t i = 0; i < order.problemDesc.length(); i += maxCharsPerLine) {
        if (lineCount >= _detailScrollLine && (startY <= 104)) {
            String chunk = order.problemDesc.substring(i, min((int)order.problemDesc.length(), (int)(i + maxCharsPerLine)));
            uiRenderer.drawText(chunk.c_str(), 8, startY, TFT_WHITE, 1);
            startY += 11;
        }
        lineCount++;
    }

    // Legenda contextual de atalhos dependendo do status atual
    if (order.status == "ASSIGNED") {
        uiRenderer.drawFooter("[ESC] Voltar     [I] Iniciar OS");
    } else if (order.status == "IN_PROGRESS") {
        uiRenderer.drawFooter("[ESC] Voltar     [C] Concluir");
    } else {
        uiRenderer.drawFooter("[ESC] Voltar     [OS Concluida]");
    }
}

// =============================================================================
// Processamento de Teclado Fisico do CardPuter
// =============================================================================
void AppController::handleKeyPress(char key, bool isDel, bool isEnter, bool isFn) {
    _screenNeedsRedraw = true;

    // Tecla global de som para feedback de clique
    HardwareInit::beepKey();

    switch (_currentScreen) {
        case SCR_BOOT: {
            // Qualquer tecla ou Enter avanca
            setScreen(SCR_CONN_STATUS);
            break;
        }

        case SCR_CONN_STATUS: {
            if (isEnter) {
                setScreen(SCR_LOGIN);
            } else if (key == 'r' || key == 'R') {
                pocketNetworkManager.reconnect();
            }
            break;
        }

        case SCR_LOGIN: {
            // Teclas de alternancia de campo: ponto e virgula (;), ponto (.), Tab
            if (key == ';' || key == '.' || key == '\t' || key == ',') {
                _loginActiveField = (_loginActiveField == 0) ? 1 : 0;
            } else if (isDel) {
                // Backspace / Apagar caractere
                if (_loginActiveField == 0 && _loginUser.length() > 0) {
                    _loginUser.remove(_loginUser.length() - 1);
                } else if (_loginActiveField == 1 && _loginPass.length() > 0) {
                    _loginPass.remove(_loginPass.length() - 1);
                }
            } else if (isEnter) {
                // Confirmar login
                HardwareInit::beepConfirm();
                sessionManager.setSession("jwt-mock-token-carlos-123", _loginUser, "Carlos Mendes", 1L, 28800UL);
                setScreen(SCR_ORDER_LIST);
            } else if (key >= 32 && key <= 126) {
                // Caractere digitavel
                if (_loginActiveField == 0 && _loginUser.length() < 24) {
                    _loginUser += key;
                } else if (_loginActiveField == 1 && _loginPass.length() < 24) {
                    _loginPass += key;
                }
            }
            break;
        }

        case SCR_ORDER_LIST: {
            // Seta para cima (;)
            if (key == ';') {
                if (_selectedOrderIndex > 0) {
                    _selectedOrderIndex--;
                }
            }
            // Seta para baixo (.)
            else if (key == '.') {
                if (_selectedOrderIndex < (int)_mockOrders.size() - 1) {
                    _selectedOrderIndex++;
                }
            }
            // Enter: abre detalhes da ordem
            else if (isEnter) {
                setScreen(SCR_ORDER_DETAIL);
            }
            // L ou l: Logout
            else if (key == 'l' || key == 'L') {
                sessionManager.clearSession();
                HardwareInit::beepConfirm();
                setScreen(SCR_LOGIN);
            }
            break;
        }

        case SCR_ORDER_DETAIL: {
            // Voltar para a lista (ESC / Del / Backspace)
            if (isDel || key == 27) {
                setScreen(SCR_ORDER_LIST);
            }
            // Rolar texto do defeito com ; e .
            else if (key == ';') {
                if (_detailScrollLine > 0) _detailScrollLine--;
            } else if (key == '.') {
                _detailScrollLine++;
            }
            // Iniciar atendimento [I] (se ASSIGNED -> IN_PROGRESS)
            else if ((key == 'i' || key == 'I') && _selectedOrderIndex >= 0) {
                MockOrder& order = _mockOrders[_selectedOrderIndex];
                if (order.status == "ASSIGNED") {
                    order.status = "IN_PROGRESS";
                    HardwareInit::beepConfirm();
                }
            }
            // Concluir atendimento [C] (se IN_PROGRESS -> COMPLETED)
            else if ((key == 'c' || key == 'C') && _selectedOrderIndex >= 0) {
                MockOrder& order = _mockOrders[_selectedOrderIndex];
                if (order.status == "IN_PROGRESS") {
                    order.status = "COMPLETED";
                    HardwareInit::beepConfirm();
                }
            }
            break;
        }
    }
}
