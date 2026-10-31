#pragma once

#ifndef POCKETMAINT_APP_CONTROLLER_H
#define POCKETMAINT_APP_CONTROLLER_H

#include <Arduino.h>
#include <vector>

enum ScreenId {
    SCR_BOOT = 1,             // SCR-001 (Boot / Splash)
    SCR_CONN_STATUS = 2,      // SCR-002 (Status de Conexao)
    SCR_LOGIN = 3,            // SCR-003 (Formulario de Login)
    SCR_ORDER_LIST = 4,       // SCR-004 (Lista de Ordens)
    SCR_ORDER_DETAIL = 5,     // SCR-005 (Detalhes da Ordem)
    SCR_CONFIRM_START = 6,    // SCR-006 (Confirmacao de Inicio de Atendimento)
    SCR_SOLUTION_FORM = 7,    // SCR-007 (Formulario de Solucao Tecnica)
    SCR_CONFIRM_COMPLETE = 8, // SCR-008 (Confirmacao de Conclusao)
    SCR_SUCCESS = 9,          // SCR-009 (Feedback de Sucesso Confirmado)
    SCR_COMM_ERROR = 10,      // SCR-010 (Alerta de Erro de Rede / Timeout)
    SCR_ACCESS_DENIED = 11,   // SCR-011 (Acesso Negado 403 / BOLA)
    SCR_SESSION_EXPIRED = 12  // SCR-012 (Sessao Expirada 401)
};

struct MockOrder {
    long id;
    String orderNumber;
    String title;
    String status;      // ASSIGNED, IN_PROGRESS, COMPLETED
    String priority;    // ALTA, CRITICA, MEDIA, BAIXA
    String equipment;   // Nome e Tag do equipamento
    String sector;      // Nome e Codigo do setor
    String problemDesc; // Descricao completa do defeito
    String solutionDesc;// Solucao registrada
};

/**
 * PocketMaint — AppController
 * 
 * Maquina de estados completa de navegacao de telas no CardPuter ADV (SCR-001 a SCR-012).
 */
class AppController {
public:
    AppController();

    // Inicializa a maquina de estados e os dados simulados em memoria
    void begin();

    // Ciclo principal de atualizacao chamado no loop()
    void update();

    // Processamento de eventos do teclado fisico
    void handleKeyPress(char key, bool isDel, bool isEnter, bool isFn);

    // Navegacao direta de tela
    void setScreen(ScreenId screen);
    ScreenId getCurrentScreen() const { return _currentScreen; }

    // Helpers para acionar telas de feedback
    void showSuccess(const String& title, const String& message, ScreenId returnTo);
    void showError(const String& title, const String& message, ScreenId returnTo);
    void showAccessDenied(const String& message);
    void showSessionExpired();

private:
    ScreenId _currentScreen;
    ScreenId _returnScreen;
    unsigned long _screenEnterTimeMs;
    bool _screenNeedsRedraw;

    // Estado do Login (SCR-003)
    int _loginActiveField; // 0 = usuario, 1 = senha
    String _loginUser;
    String _loginPass;

    // Estado da Lista de Ordens (SCR-004)
    std::vector<MockOrder> _mockOrders;
    int _selectedOrderIndex;
    int _scrollOffset;

    // Estado dos Detalhes (SCR-005)
    int _detailScrollLine;

    // Estado do Formulario de Solucao (SCR-007)
    String _solutionText;
    bool _solutionWarning;

    // Estados de Feedback (SCR-009 a SCR-012)
    String _feedbackTitle;
    String _feedbackMessage;
    String _feedbackTimestamp;

    // Estado de Saude da API REST (GET /api/v1/health)
    bool _apiHealthChecked;
    bool _apiHealthOnline;
    String _apiHealthStatus;
    String _apiServerTime;

    void doHealthCheck();
    void doRealLogin();

    // Renderizadores especificos de tela
    void renderScreen();
    void renderBootScreen();
    void renderConnStatusScreen();
    void renderLoginScreen();
    void renderOrderListScreen();
    void renderOrderDetailScreen();
    void renderConfirmStartScreen();
    void renderSolutionFormScreen();
    void renderConfirmCompleteScreen();
    void renderSuccessScreen();
    void renderCommErrorScreen();
    void renderAccessDeniedScreen();
    void renderSessionExpiredScreen();

    void initMockData();
};

extern AppController appController;

#endif // POCKETMAINT_APP_CONTROLLER_H
