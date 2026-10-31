#pragma once

#ifndef POCKETMAINT_APP_CONTROLLER_H
#define POCKETMAINT_APP_CONTROLLER_H

#include <Arduino.h>
#include <vector>

enum ScreenId {
    SCR_BOOT = 1,        // SCR-001 (Boot / Splash)
    SCR_CONN_STATUS = 2, // SCR-002 (Status de Conexao)
    SCR_LOGIN = 3,       // SCR-003 (Formulario de Login)
    SCR_ORDER_LIST = 4,  // SCR-004 (Lista de Ordens)
    SCR_ORDER_DETAIL = 5 // SCR-005 (Detalhes da Ordem)
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
};

/**
 * PocketMaint — AppController
 * 
 * Maquina de estados de navegacao de telas no CardPuter ADV.
 * Coordena fluxo de telas, entrada de teclado e renderizacao grafica.
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

private:
    ScreenId _currentScreen;
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

    // Renderizadores especificos de tela
    void renderScreen();
    void renderBootScreen();
    void renderConnStatusScreen();
    void renderLoginScreen();
    void renderOrderListScreen();
    void renderOrderDetailScreen();

    void initMockData();
};

extern AppController appController;

#endif // POCKETMAINT_APP_CONTROLLER_H
