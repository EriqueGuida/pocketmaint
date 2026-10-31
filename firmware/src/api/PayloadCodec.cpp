#include "PayloadCodec.h"

String PayloadCodec::encodeLoginRequest(const String& username, const String& password) {
    JsonDocument doc;
    doc["username"] = username;
    doc["password"] = password;
    doc["clientChannel"] = "CARDPUTER_M5";

    String output;
    serializeJson(doc, output);
    return output;
}

bool PayloadCodec::decodeLoginResponse(const String& json, LoginPayload& outPayload, ApiError& outError) {
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, json);

    if (err) {
        outError.status = 500;
        outError.errorCode = "JSON_PARSE_ERROR";
        outError.title = "Falha ao processar resposta";
        outError.userMessage = err.c_str();
        return false;
    }

    // Se contiver campo "errorCode" ou "status", pode ser erro RFC 9457
    if (!doc["errorCode"].isNull()) {
        outError.status = doc["status"] | 400;
        outError.errorCode = doc["errorCode"].as<const char*>() ? doc["errorCode"].as<const char*>() : "UNKNOWN_ERROR";
        outError.title = doc["title"].as<const char*>() ? doc["title"].as<const char*>() : "Erro na autenticacao";
        outError.userMessage = doc["userMessage"].as<const char*>() ? doc["userMessage"].as<const char*>() : outError.title;
        return false;
    }

    if (doc["accessToken"].isNull()) {
        outError.status = 500;
        outError.errorCode = "MISSING_TOKEN";
        outError.title = "Token ausente";
        outError.userMessage = "Servidor nao retornou accessToken";
        return false;
    }

    outPayload.token = doc["accessToken"].as<String>();
    outPayload.tokenType = doc["tokenType"].as<const char*>() ? doc["tokenType"].as<const char*>() : "Bearer";
    outPayload.expiresIn = doc["expiresInSeconds"] | 28800L;

    if (!doc["user"].isNull()) {
        JsonObject userObj = doc["user"];
        outPayload.userId = userObj["id"] | 0L;
        outPayload.username = userObj["username"].as<const char*>() ? userObj["username"].as<const char*>() : "";
        outPayload.fullName = userObj["fullName"].as<const char*>() ? userObj["fullName"].as<const char*>() : outPayload.username;
        outPayload.role = userObj["role"].as<const char*>() ? userObj["role"].as<const char*>() : "ROLE_TECHNICIAN";
    } else {
        outPayload.userId = 0L;
        outPayload.username = "";
        outPayload.fullName = "";
        outPayload.role = "ROLE_TECHNICIAN";
    }

    return true;
}

bool PayloadCodec::decodeHealthResponse(const String& json, HealthPayload& outPayload) {
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, json);

    if (err) {
        return false;
    }

    outPayload.status = doc["status"] | "UNKNOWN";
    outPayload.service = doc["service"] | "PocketMaint API";
    outPayload.version = doc["version"] | "1.0.0";
    outPayload.serverTime = doc["serverTime"] | "";
    return true;
}

bool PayloadCodec::decodeErrorResponse(const String& json, ApiError& outError) {
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, json);

    if (err) {
        outError.status = 500;
        outError.errorCode = "PARSE_FAILED";
        outError.title = "Erro interno";
        outError.userMessage = "Nao foi possivel decodificar o erro";
        return false;
    }

    outError.status = doc["status"] | 500;
    outError.errorCode = doc["errorCode"] | "UNKNOWN";
    outError.title = doc["title"] | "Erro do servidor";
    outError.userMessage = doc["userMessage"] | doc["title"] | "Operacao recusada";
    return true;
}
