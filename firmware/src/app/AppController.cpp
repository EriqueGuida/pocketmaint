#include "AppController.h"
#include "../ui/UiRenderer.h"
#include "../hal/HardwareInit.h"
#include "../net/NetworkManager.h"
#include "../session/SessionManager.h"
#include "secrets.h"

AppController appController;

AppController::AppController()
    : _currentScreen(SCR_BOOT)
    , _returnScreen(SCR_ORDER_LIST)
    , _screenEnterTimeMs(0)
    , _screenNeedsRedraw(true)
    , _loginActiveField(0)
    , _loginUser("tec.carlos")
    , _loginPass("password")
    , _selectedOrderIndex(0)
    , _scrollOffset(0)
    , _detailScrollLine(0)
    , _solutionText("")
    , _solutionWarning(false)
    , _feedbackTitle("")
    , _feedbackMessage("")
    , _feedbackTimestamp("2026-10-31 19:30:00")
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
        "Equipamento apresentando vibracao excessiva no mancal do motor principal. Substituir conjunto de rolamentos e filtro de oleo lubrificante conforme manual.",
        ""
    });

    _mockOrders.push_back({
        2L,
        "OS-2026-0002",
        "Calibracao sensor pressao",
        "IN_PROGRESS",
        "CRITICA",
        "Torno CNC 02 (EQ-104)",
        "Usinagem de Precisao (SEC-02)",
        "Pressostato de seguranca indicando corte indevido da bomba refrigerante. Necessario calibrar ponto de desarme e inspecionar fiacao do sensor.",
        ""
    });

    _mockOrders.push_back({
        3L,
        "OS-2026-0003",
        "Troca retentor hidraulico",
        "ASSIGNED",
        "MEDIA",
        "Prensa Hidraulica 50T (EQ-108)",
        "Estamparia Pesada (SEC-03)",
        "Vazamento visivel de fluido no cilindro de compressao. Substituir gaxetas de vedacao e verificar nivel do reservatorio ISO VG 68.",
        ""
    });

    _mockOrders.push_back({
        4L,
        "OS-2026-0004",
        "Inspecao preventiva geral",
        "ASSIGNED",
        "BAIXA",
        "Furadeira Radial (EQ-112)",
        "Manutencao Geral (SEC-01)",
        "Lubrificacao das guias lineares e verificacao da folga da arvore de furacao. Intervencao semestral programada.",
        ""
    });
}

void AppController::setScreen(ScreenId screen) {
    _currentScreen = screen;
    _screenEnterTimeMs = millis();
    _screenNeedsRedraw = true;
    _detailScrollLine = 0;
    _solutionWarning = false;

    Serial.printf("[AppController] Mudanca de tela -> SCR-%03d\n", (int)screen);
}

void AppController::showSuccess(const String& title, const String& message, ScreenId returnTo) {
    _feedbackTitle = title;
    _feedbackMessage = message;
    _returnScreen = returnTo;
    HardwareInit::beepConfirm();
    setScreen(SCR_SUCCESS);
}

void AppController::showError(const String& title, const String& message, ScreenId returnTo) {
    _feedbackTitle = title;
    _feedbackMessage = message;
    _returnScreen = returnTo;
    HardwareInit::beepError();
    setScreen(SCR_COMM_ERROR);
}

void AppController::showAccessDenied(const String& message) {
    _feedbackMessage = message;
    HardwareInit::beepError();
    setScreen(SCR_ACCESS_DENIED);
}

void AppController::showSessionExpired() {
    HardwareInit::beepError();
    setScreen(SCR_SESSION_EXPIRED);
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
        case SCR_CONFIRM_START:
            renderConfirmStartScreen();
            break;
        case SCR_SOLUTION_FORM:
            renderSolutionFormScreen();
            break;
        case SCR_CONFIRM_COMPLETE:
            renderConfirmCompleteScreen();
            break;
        case SCR_SUCCESS:
            renderSuccessScreen();
            break;
        case SCR_COMM_ERROR:
            renderCommErrorScreen();
            break;
        case SCR_ACCESS_DENIED:
            renderAccessDeniedScreen();
            break;
        case SCR_SESSION_EXPIRED:
            renderSessionExpiredScreen();
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

    // Campo 2: Senha
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

    int maxVisible = 3;
    if (_selectedOrderIndex < _scrollOffset) _scrollOffset = _selectedOrderIndex;
    if (_selectedOrderIndex >= _scrollOffset + maxVisible) _scrollOffset = _selectedOrderIndex - maxVisible + 1;

    int startY = 22;
    for (int i = 0; i < maxVisible; i++) {
        int orderIdx = _scrollOffset + i;
        if (orderIdx >= (int)_mockOrders.size()) break;

        const MockOrder& order = _mockOrders[orderIdx];
        bool isSelected = (orderIdx == _selectedOrderIndex);
        int boxY = startY + (i * 30);

        if (isSelected) {
            uiRenderer.fillRect(4, boxY, 232, 28, 0x01E0); // Verde escuro destacado
            uiRenderer.drawRect(4, boxY, 232, 28, TFT_GREEN);
            uiRenderer.drawText(">", 7, boxY + 4, TFT_YELLOW, 1);
        } else {
            uiRenderer.fillRect(4, boxY, 232, 28, 0x10A2); // Cinza azulado suave
            uiRenderer.drawRect(4, boxY, 232, 28, TFT_DARKGRAY);
        }

        char line1[64];
        snprintf(line1, sizeof(line1), "%s: %s", order.orderNumber.c_str(), order.title.c_str());
        line1[26] = '\0';
        uiRenderer.drawText(line1, isSelected ? 16 : 8, boxY + 3, isSelected ? TFT_WHITE : TFT_LIGHTGRAY, 1);

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

    uint16_t statusColor = TFT_YELLOW;
    if (order.status == "IN_PROGRESS") statusColor = TFT_CYAN;
    if (order.status == "COMPLETED") statusColor = TFT_GREEN;

    char line1[64];
    snprintf(line1, sizeof(line1), "Status: %s", order.status.c_str());
    uiRenderer.drawText(line1, 8, 23, statusColor, 1);

    char prioStr[32];
    snprintf(prioStr, sizeof(prioStr), "Prio: %s", order.priority.c_str());
    uiRenderer.drawTextRight(prioStr, 232, 23, TFT_WHITE, 1);

    uiRenderer.drawText("Equip:", 8, 36, TFT_LIGHTGRAY, 1);
    uiRenderer.drawText(order.equipment.c_str(), 45, 36, TFT_CYAN, 1);

    uiRenderer.drawText("Setor:", 8, 49, TFT_LIGHTGRAY, 1);
    uiRenderer.drawText(order.sector.c_str(), 45, 49, TFT_WHITE, 1);

    uiRenderer.drawFastHLine(6, 62, 228, TFT_DARKGRAY);
    uiRenderer.drawText("Defeito Reportado:", 8, 65, TFT_YELLOW, 1);

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

    if (order.status == "ASSIGNED") {
        uiRenderer.drawFooter("[ESC] Voltar     [I] Iniciar OS");
    } else if (order.status == "IN_PROGRESS") {
        uiRenderer.drawFooter("[ESC] Voltar     [C] Concluir");
    } else {
        uiRenderer.drawFooter("[ESC] Voltar     [OS Concluida]");
    }
}

// =============================================================================
// SCR-006: Confirmacao de Inicio de Atendimento
// =============================================================================
void AppController::renderConfirmStartScreen() {
    uiRenderer.clear(TFT_BLACK);
    const MockOrder& order = _mockOrders[_selectedOrderIndex];
    uiRenderer.drawHeader("CONFIRMACAO", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.fillRect(8, 24, 224, 86, 0x0014); // Fundo azul escuro de destaque
    uiRenderer.drawRect(8, 24, 224, 86, TFT_YELLOW);

    uiRenderer.drawTextCenter("INICIAR ATENDIMENTO?", 32, TFT_YELLOW, 1);

    char buf[64];
    snprintf(buf, sizeof(buf), "Ordem: %s", order.orderNumber.c_str());
    uiRenderer.drawTextCenter(buf, 48, TFT_WHITE, 1);

    snprintf(buf, sizeof(buf), "%s", order.equipment.c_str());
    buf[34] = '\0';
    uiRenderer.drawTextCenter(buf, 62, TFT_CYAN, 1);

    uiRenderer.drawTextCenter("O horario de inicio sera", 78, TFT_LIGHTGRAY, 1);
    uiRenderer.drawTextCenter("registrado no servidor.", 90, TFT_LIGHTGRAY, 1);

    uiRenderer.drawFooter("[ESC] Cancelar   [ENTER] Confirmar");
}

// =============================================================================
// SCR-007: Formulario de Solucao Tecnica
// =============================================================================
void AppController::renderSolutionFormScreen() {
    uiRenderer.clear(TFT_BLACK);
    const MockOrder& order = _mockOrders[_selectedOrderIndex];
    uiRenderer.drawHeader("REGISTRO DE SOLUCAO", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    // Titulo e contador de caracteres
    uiRenderer.drawText("Solucao executada:", 8, 23, TFT_YELLOW, 1);

    char cntStr[32];
    int len = _solutionText.length();
    snprintf(cntStr, sizeof(cntStr), "%d/500 (min 10)", len);
    uint16_t cntColor = (len >= 10) ? TFT_GREEN : TFT_ORANGE;
    uiRenderer.drawTextRight(cntStr, 232, 23, cntColor, 1);

    // Caixa de digitacao de texto
    uiRenderer.drawRect(6, 36, 228, 58, (len >= 10) ? TFT_GREEN : TFT_DARKGRAY);
    uiRenderer.fillRect(7, 37, 226, 56, 0x0841);

    // Renderiza o texto com quebra de linha visual
    int startY = 41;
    int charsPerLine = 35;
    for (size_t i = 0; i < _solutionText.length(); i += charsPerLine) {
        if (startY <= 78) {
            String chunk = _solutionText.substring(i, min((int)_solutionText.length(), (int)(i + charsPerLine)));
            uiRenderer.drawText(chunk.c_str(), 10, startY, TFT_WHITE, 1);
            startY += 12;
        }
    }
    // Cursor piscante
    if (startY <= 88) {
        int cursorX = 10 + (_solutionText.length() % charsPerLine) * 6;
        uiRenderer.drawText("_", cursorX, startY - 1, TFT_GREEN, 1);
    }

    // Mensagem de aviso se tentar avancar com menos de 10 caracteres
    if (_solutionWarning) {
        uiRenderer.drawTextCenter("Minimo de 10 caracteres obrigatorio!", 98, TFT_RED, 1);
    } else {
        uiRenderer.drawText("Digite pelo teclado fisico do CardPuter", 8, 100, TFT_DARKGRAY, 1);
    }

    uiRenderer.drawFooter("[ESC] Voltar     [ENTER] Avancar");
}

// =============================================================================
// SCR-008: Confirmacao de Conclusao
// =============================================================================
void AppController::renderConfirmCompleteScreen() {
    uiRenderer.clear(TFT_BLACK);
    const MockOrder& order = _mockOrders[_selectedOrderIndex];
    uiRenderer.drawHeader("CONCLUSAO", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.fillRect(8, 24, 224, 86, 0x0014);
    uiRenderer.drawRect(8, 24, 224, 86, TFT_GREEN);

    uiRenderer.drawTextCenter("CONFIRMAR CONCLUSAO DA OS?", 30, TFT_GREEN, 1);

    char buf[64];
    snprintf(buf, sizeof(buf), "Ordem: %s", order.orderNumber.c_str());
    uiRenderer.drawTextCenter(buf, 44, TFT_WHITE, 1);

    uiRenderer.drawText("Solucao registrada:", 14, 58, TFT_YELLOW, 1);
    String preview = _solutionText;
    if (preview.length() > 32) preview = preview.substring(0, 29) + "...";
    uiRenderer.drawText(preview.c_str(), 14, 70, TFT_CYAN, 1);

    uiRenderer.drawTextCenter("Acao definitiva. A OS sera fechada.", 88, TFT_ORANGE, 1);

    uiRenderer.drawFooter("[ESC] Editar     [ENTER] Concluir");
}

// =============================================================================
// SCR-009: Feedback de Sucesso
// =============================================================================
void AppController::renderSuccessScreen() {
    uiRenderer.clear(TFT_BLACK);
    uiRenderer.drawHeader("SERVIDOR", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    // Banner verde de sucesso
    uiRenderer.fillRect(6, 24, 228, 86, 0x02E0); // Verde bandeira escuro
    uiRenderer.drawRect(6, 24, 228, 86, TFT_GREEN);

    uiRenderer.drawTextCenter("[ OK ] SUCESSO CONFIRMADO", 34, TFT_WHITE, 1);
    uiRenderer.drawTextCenter(_feedbackTitle.c_str(), 52, TFT_YELLOW, 1);
    uiRenderer.drawTextCenter(_feedbackMessage.c_str(), 68, TFT_WHITE, 1);

    char timeBuf[64];
    snprintf(timeBuf, sizeof(timeBuf), "Gravado em: %s", _feedbackTimestamp.c_str());
    uiRenderer.drawTextCenter(timeBuf, 88, TFT_LIGHTGRAY, 1);

    uiRenderer.drawFooter("[ENTER] Continuar");
}

// =============================================================================
// SCR-010: Alerta de Erro de Comunicacao / Timeout
// =============================================================================
void AppController::renderCommErrorScreen() {
    uiRenderer.clear(TFT_BLACK);
    uiRenderer.drawHeader("ERRO DE REDE", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.fillRect(6, 24, 228, 86, 0x6000); // Vermelho escuro
    uiRenderer.drawRect(6, 24, 228, 86, TFT_RED);

    uiRenderer.drawTextCenter("[ FALHA DE COMUNICACAO ]", 34, TFT_YELLOW, 1);
    uiRenderer.drawTextCenter(_feedbackTitle.c_str(), 52, TFT_WHITE, 1);
    uiRenderer.drawTextCenter(_feedbackMessage.c_str(), 68, TFT_LIGHTGRAY, 1);
    uiRenderer.drawTextCenter("Nenhum estado foi alterado.", 88, TFT_ORANGE, 1);

    uiRenderer.drawFooter("[R] Tentar Novamente   [ESC] Voltar");
}

// =============================================================================
// SCR-011: Acesso Negado (403 BOLA / Anti-IDOR)
// =============================================================================
void AppController::renderAccessDeniedScreen() {
    uiRenderer.clear(TFT_BLACK);
    uiRenderer.drawHeader("SEGURANCA", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.fillRect(6, 24, 228, 86, 0x4800); // Laranja escuro / vermelho
    uiRenderer.drawRect(6, 24, 228, 86, TFT_ORANGE);

    uiRenderer.drawTextCenter("[ 403 - ACESSO NEGADO ]", 34, TFT_RED, 1);
    uiRenderer.drawTextCenter("Permissao Insuficiente", 52, TFT_YELLOW, 1);
    uiRenderer.drawTextCenter(_feedbackMessage.c_str(), 70, TFT_WHITE, 1);

    uiRenderer.drawFooter("[ENTER] Voltar para Lista");
}

// =============================================================================
// SCR-012: Sessao Expirada (401 AUTH_TOKEN_EXPIRED)
// =============================================================================
void AppController::renderSessionExpiredScreen() {
    uiRenderer.clear(TFT_BLACK);
    uiRenderer.drawHeader("SEGURANCA", pocketNetworkManager.isConnected(), HardwareInit::getBatteryLevel());

    uiRenderer.fillRect(6, 24, 228, 86, 0x4010);
    uiRenderer.drawRect(6, 24, 228, 86, TFT_YELLOW);

    uiRenderer.drawTextCenter("[ 401 - SESSAO EXPIRADA ]", 34, TFT_YELLOW, 1);
    uiRenderer.drawTextCenter("Credencial JWT Vencida", 52, TFT_WHITE, 1);
    uiRenderer.drawTextCenter("Faca login novamente no terminal.", 72, TFT_LIGHTGRAY, 1);

    uiRenderer.drawFooter("[ENTER] Ir para Login");
}

// =============================================================================
// Processamento de Teclado Fisico do CardPuter
// =============================================================================
void AppController::handleKeyPress(char key, bool isDel, bool isEnter, bool isFn) {
    _screenNeedsRedraw = true;
    HardwareInit::beepKey();

    switch (_currentScreen) {
        case SCR_BOOT: {
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
            if (key == ';' || key == '.' || key == '\t' || key == ',') {
                _loginActiveField = (_loginActiveField == 0) ? 1 : 0;
            } else if (isDel) {
                if (_loginActiveField == 0 && _loginUser.length() > 0) {
                    _loginUser.remove(_loginUser.length() - 1);
                } else if (_loginActiveField == 1 && _loginPass.length() > 0) {
                    _loginPass.remove(_loginPass.length() - 1);
                }
            } else if (isEnter) {
                HardwareInit::beepConfirm();
                sessionManager.setSession("jwt-mock-token-carlos-123", _loginUser, "Carlos Mendes", 1L, 28800UL);
                setScreen(SCR_ORDER_LIST);
            } else if (key >= 32 && key <= 126) {
                if (_loginActiveField == 0 && _loginUser.length() < 24) {
                    _loginUser += key;
                } else if (_loginActiveField == 1 && _loginPass.length() < 24) {
                    _loginPass += key;
                }
            }
            break;
        }

        case SCR_ORDER_LIST: {
            if (key == ';') {
                if (_selectedOrderIndex > 0) _selectedOrderIndex--;
            } else if (key == '.') {
                if (_selectedOrderIndex < (int)_mockOrders.size() - 1) _selectedOrderIndex++;
            } else if (isEnter) {
                setScreen(SCR_ORDER_DETAIL);
            } else if (key == 'l' || key == 'L') {
                sessionManager.clearSession();
                HardwareInit::beepConfirm();
                setScreen(SCR_LOGIN);
            }
            break;
        }

        case SCR_ORDER_DETAIL: {
            if (isDel || key == 27) {
                setScreen(SCR_ORDER_LIST);
            } else if (key == ';') {
                if (_detailScrollLine > 0) _detailScrollLine--;
            } else if (key == '.') {
                _detailScrollLine++;
            }
            // Iniciar atendimento [I] (leva para SCR-006 Confirmacao)
            else if ((key == 'i' || key == 'I') && _selectedOrderIndex >= 0) {
                MockOrder& order = _mockOrders[_selectedOrderIndex];
                if (order.status == "ASSIGNED") {
                    setScreen(SCR_CONFIRM_START);
                }
            }
            // Concluir atendimento [C] (leva para SCR-007 Registro de Solucao)
            else if ((key == 'c' || key == 'C') && _selectedOrderIndex >= 0) {
                MockOrder& order = _mockOrders[_selectedOrderIndex];
                if (order.status == "IN_PROGRESS") {
                    _solutionText = "";
                    _solutionWarning = false;
                    setScreen(SCR_SOLUTION_FORM);
                }
            }
            // Teclas de demonstracao para inspecionar telas de erro / seguranca:
            else if (key == 'e' || key == 'E') {
                showError("Timeout HTTP (5000ms)", "Servidor backend nao respondeu.", SCR_ORDER_DETAIL);
            } else if (key == 'a' || key == 'A') {
                showAccessDenied("Ordem de servico atribuida a outro tecnico.");
            } else if (key == 'x' || key == 'X') {
                showSessionExpired();
            }
            break;
        }

        case SCR_CONFIRM_START: {
            if (isEnter && _selectedOrderIndex >= 0) {
                MockOrder& order = _mockOrders[_selectedOrderIndex];
                order.status = "IN_PROGRESS";
                showSuccess("ATENDIMENTO INICIADO", "Status alterado para IN_PROGRESS", SCR_ORDER_DETAIL);
            } else if (isDel || key == 27) {
                setScreen(SCR_ORDER_DETAIL);
            }
            break;
        }

        case SCR_SOLUTION_FORM: {
            if (isDel) {
                if (_solutionText.length() > 0) {
                    _solutionText.remove(_solutionText.length() - 1);
                }
            } else if (isEnter) {
                if (_solutionText.length() >= 10) {
                    setScreen(SCR_CONFIRM_COMPLETE);
                } else {
                    _solutionWarning = true;
                    HardwareInit::beepError();
                }
            } else if (key == 27) {
                setScreen(SCR_ORDER_DETAIL);
            } else if (key >= 32 && key <= 126) {
                if (_solutionText.length() < 500) {
                    _solutionText += key;
                    _solutionWarning = false;
                }
            }
            break;
        }

        case SCR_CONFIRM_COMPLETE: {
            if (isEnter && _selectedOrderIndex >= 0) {
                MockOrder& order = _mockOrders[_selectedOrderIndex];
                order.status = "COMPLETED";
                order.solutionDesc = _solutionText;
                showSuccess("ORDEM CONCLUIDA", "Status alterado para COMPLETED", SCR_ORDER_LIST);
            } else if (isDel || key == 27) {
                setScreen(SCR_SOLUTION_FORM);
            }
            break;
        }

        case SCR_SUCCESS: {
            if (isEnter) {
                setScreen(_returnScreen);
            }
            break;
        }

        case SCR_COMM_ERROR: {
            if (key == 'r' || key == 'R' || isEnter) {
                setScreen(_returnScreen);
            } else if (isDel || key == 27) {
                setScreen(SCR_ORDER_DETAIL);
            }
            break;
        }

        case SCR_ACCESS_DENIED: {
            if (isEnter || isDel || key == 27) {
                setScreen(SCR_ORDER_LIST);
            }
            break;
        }

        case SCR_SESSION_EXPIRED: {
            if (isEnter) {
                sessionManager.clearSession();
                setScreen(SCR_LOGIN);
            }
            break;
        }
    }
}
