#pragma once

#ifndef POCKETMAINT_PAYLOAD_CODEC_H
#define POCKETMAINT_PAYLOAD_CODEC_H

#include <Arduino.h>
#include <ArduinoJson.h>

/**
 * PocketMaint — PayloadCodec
 * 
 * Codificador e decodificador JSON utilizando ArduinoJson v7.
 * Converte payloads REST em structs C++ e serializa requisições.
 */

struct ApiError {
    int status;
    String errorCode;
    String title;
    String userMessage;
};

struct LoginPayload {
    String token;
    String tokenType;
    long expiresIn;
    long userId;
    String username;
    String fullName;
    String role;
};

struct HealthPayload {
    String status;
    String service;
    String version;
    String serverTime;
};

struct OrderItem {
    long id;
    String orderNumber;
    String title;
    String status;
    String priority;
    String equipmentTag;
    String equipmentName;
    String sectorCode;
};

struct OrderDetail {
    long id;
    String orderNumber;
    String title;
    String problemDescription;
    String solutionDescription;
    String status;
    String priority;
    String equipment;
    String sector;
    String startedAt;
    String completedAt;
};

struct OrderActionResult {
    long orderId;
    String orderNumber;
    String status;
    String timestamp;
    String message;
};

class PayloadCodec {
public:
    // Serializa requisição de autenticação para JSON
    static String encodeLoginRequest(const String& username, const String& password);

    // Desserializa resposta de login (200 OK)
    static bool decodeLoginResponse(const String& json, LoginPayload& outPayload, ApiError& outError);

    // Desserializa resposta de health check (/api/v1/health)
    static bool decodeHealthResponse(const String& json, HealthPayload& outPayload);

    // Desserializa erro padronizado RFC 9457
    static bool decodeErrorResponse(const String& json, ApiError& outError);

    // Desserializa lista paginada de ordens (GET /api/v1/me/orders)
    static bool decodePagedOrders(const String& json, std::vector<OrderItem>& outOrders, ApiError& outError);

    // Desserializa detalhes completos da ordem (GET /api/v1/orders/{id})
    static bool decodeOrderDetail(const String& json, OrderDetail& outDetail, ApiError& outError);

    // Serializa requisição de conclusão de ordem com solução técnica (POST /api/v1/orders/{id}/complete)
    static String encodeCompleteOrderRequest(const String& solutionDescription);

    // Desserializa resposta de transição de estado da ordem
    static bool decodeOrderActionResult(const String& json, OrderActionResult& outResult, ApiError& outError);
};

#endif // POCKETMAINT_PAYLOAD_CODEC_H
