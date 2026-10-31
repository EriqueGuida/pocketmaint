#pragma once

#ifndef POCKETMAINT_POCKET_NETWORK_MANAGER_H
#define POCKETMAINT_POCKET_NETWORK_MANAGER_H

#include <Arduino.h>
#include <WiFi.h>

/**
 * PocketMaint — PocketNetworkManager
 * 
 * Gerencia a conectividade Wi-Fi de 2.4 GHz no ESP32-S3:
 * - Conexão inicial com timeout configurável.
 * - Monitoramento contínuo de status e medição de RSSI.
 * - Reconexão automática resiliente em caso de oscilação do sinal de bancada.
 */
class PocketNetworkManager {
public:
    PocketNetworkManager();

    // Inicia a conexão com a rede Wi-Fi 2.4 GHz
    void begin(const char* ssid, const char* password);

    // Deve ser chamado periodicamente no loop() principal
    void update();

    // Força uma tentativa de reconexão imediata
    void reconnect();

    // Informações de conectividade
    bool isConnected() const;
    String getSsid() const;
    String getIpAddress() const;
    String getGatewayIp() const;
    String getSubnetMask() const;
    String getMacAddress() const;
    
    // Métricas de sinal de rádio
    int getRssi() const;
    int getSignalQualityPercent() const;
    String getSignalQualityText() const;

private:
    const char* _ssid;
    const char* _password;
    bool _connecting;
    unsigned long _lastConnectionAttemptMs;
    unsigned long _lastRssiCheckMs;
    int _cachedRssi;

    void connectInternal();
};

extern PocketNetworkManager pocketNetworkManager;

#endif // POCKETMAINT_POCKET_NETWORK_MANAGER_H
