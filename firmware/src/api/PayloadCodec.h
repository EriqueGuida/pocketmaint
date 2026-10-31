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
};

#endif // POCKETMAINT_PAYLOAD_CODEC_H
