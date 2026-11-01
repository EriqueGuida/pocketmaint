#pragma once

#ifndef POCKETMAINT_API_CLIENT_H
#define POCKETMAINT_API_CLIENT_H

#include <Arduino.h>
#include <HTTPClient.h>
#include <WiFiClient.h>
#include "PayloadCodec.h"
#include "../session/SessionManager.h"

/**
 * PocketMaint — PocketMaintApiClient
 * 
 * Cliente HTTP RESTful nativo para ESP32.
 * Realiza chamadas com timeout, tratamento RFC 9457 e persistência de token na RAM.
 */
class PocketMaintApiClient {
public:
    PocketMaintApiClient();

    // Inicializa a URL base da API
    void begin(const String& baseUrl);

    // Validação de conectividade (GET /api/v1/health)
    bool checkHealth(HealthPayload& outHealth, String& outErrorMsg);

    // Autenticação de credenciais (POST /api/v1/auth/login)
    bool login(const String& username, const String& password, LoginPayload& outLogin, ApiError& outError);

    // Consulta fila de ordens do técnico autenticado (GET /api/v1/me/orders)
    bool fetchMyOrders(std::vector<OrderItem>& outOrders, ApiError& outError);

    // Consulta detalhes completos da ordem (GET /api/v1/orders/{id})
    bool fetchOrderDetail(long orderId, OrderDetail& outDetail, ApiError& outError);

    // Inicia atendimento da ordem (POST /api/v1/orders/{id}/start com Idempotency-Key)
    bool startOrder(long orderId, OrderActionResult& outResult, ApiError& outError);

    // Registra solução e conclui a ordem (POST /api/v1/orders/{id}/complete)
    bool completeOrder(long orderId, const String& solution, OrderActionResult& outResult, ApiError& outError);

    // Getters auxiliares
    String getBaseUrl() const { return _baseUrl; }
    bool isLastCallSuccessful() const { return _lastHttpCode >= 200 && _lastHttpCode < 300; }
    int getLastHttpCode() const { return _lastHttpCode; }

private:
    String _baseUrl;
    int _lastHttpCode;
    WiFiClient _wifiClient;

    // Configura headers padrão para requisições
    void configureClient(HTTPClient& http, const String& endpoint);
};

// Instância global do cliente da API
extern PocketMaintApiClient apiClient;

#endif // POCKETMAINT_API_CLIENT_H
