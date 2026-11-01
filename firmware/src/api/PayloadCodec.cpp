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
    outError.errorCode = doc["errorCode"].as<const char*>() ? doc["errorCode"].as<const char*>() : "UNKNOWN";
    outError.title = doc["title"].as<const char*>() ? doc["title"].as<const char*>() : "Erro do servidor";
    outError.userMessage = doc["userMessage"].as<const char*>() ? doc["userMessage"].as<const char*>() : outError.title;
    return true;
}

bool PayloadCodec::decodePagedOrders(const String& json, std::vector<OrderItem>& outOrders, ApiError& outError) {
    outOrders.clear();
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, json);

    if (err) {
        outError.status = 500;
        outError.errorCode = "JSON_PARSE_ERROR";
        outError.title = "Falha ao processar lista";
        outError.userMessage = err.c_str();
        return false;
    }

    if (!doc["errorCode"].isNull()) {
        decodeErrorResponse(json, outError);
        return false;
    }

    JsonArray items = doc["items"].as<JsonArray>();
    for (JsonObject obj : items) {
        OrderItem item;
        item.id = obj["id"] | 0L;
        item.orderNumber = obj["orderNumber"].as<const char*>() ? obj["orderNumber"].as<const char*>() : "";
        item.title = obj["title"].as<const char*>() ? obj["title"].as<const char*>() : "";
        item.status = obj["status"].as<const char*>() ? obj["status"].as<const char*>() : "ASSIGNED";
        item.priority = obj["priority"].as<const char*>() ? obj["priority"].as<const char*>() : "ALTA";
        item.equipmentTag = obj["equipmentAssetTag"].as<const char*>() ? obj["equipmentAssetTag"].as<const char*>() : "";
        item.equipmentName = obj["equipmentName"].as<const char*>() ? obj["equipmentName"].as<const char*>() : "";
        item.sectorCode = obj["sectorCode"].as<const char*>() ? obj["sectorCode"].as<const char*>() : "";
        outOrders.push_back(item);
    }

    return true;
}

bool PayloadCodec::decodeOrderDetail(const String& json, OrderDetail& outDetail, ApiError& outError) {
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, json);

    if (err) {
        outError.status = 500;
        outError.errorCode = "JSON_PARSE_ERROR";
        outError.title = "Falha ao decodificar detalhes";
        outError.userMessage = err.c_str();
        return false;
    }

    if (!doc["errorCode"].isNull()) {
        decodeErrorResponse(json, outError);
        return false;
    }

    outDetail.id = doc["id"] | 0L;
    outDetail.orderNumber = doc["orderNumber"].as<const char*>() ? doc["orderNumber"].as<const char*>() : "";
    outDetail.title = doc["title"].as<const char*>() ? doc["title"].as<const char*>() : "";
    outDetail.problemDescription = doc["problemDescription"].as<const char*>() ? doc["problemDescription"].as<const char*>() : "";
    outDetail.solutionDescription = doc["solutionDescription"].as<const char*>() ? doc["solutionDescription"].as<const char*>() : "";
    outDetail.status = doc["status"].as<const char*>() ? doc["status"].as<const char*>() : "";
    outDetail.priority = doc["priority"].as<const char*>() ? doc["priority"].as<const char*>() : "";
    outDetail.startedAt = doc["startedAt"].as<const char*>() ? doc["startedAt"].as<const char*>() : "";
    outDetail.completedAt = doc["completedAt"].as<const char*>() ? doc["completedAt"].as<const char*>() : "";

    // Equipamento formatado
    if (!doc["equipment"].isNull()) {
        String eqName = doc["equipment"]["name"] | "";
        String eqTag = doc["equipment"]["assetTag"] | "";
        outDetail.equipment = eqName + " (" + eqTag + ")";
    } else {
        outDetail.equipment = "Nao informado";
    }

    // Setor formatado
    if (!doc["sector"].isNull()) {
        String secName = doc["sector"]["name"] | "";
        String secCode = doc["sector"]["code"] | "";
        outDetail.sector = secName + " (" + secCode + ")";
    } else {
        outDetail.sector = "Nao informado";
    }

    return true;
}

String PayloadCodec::encodeCompleteOrderRequest(const String& solutionDescription) {
    JsonDocument doc;
    doc["solutionDescription"] = solutionDescription;
    String output;
    serializeJson(doc, output);
    return output;
}

bool PayloadCodec::decodeOrderActionResult(const String& json, OrderActionResult& outResult, ApiError& outError) {
    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, json);

    if (err) {
        outError.status = 500;
        outError.errorCode = "JSON_PARSE_ERROR";
        outError.title = "Falha ao processar acao";
        outError.userMessage = err.c_str();
        return false;
    }

    if (!doc["errorCode"].isNull()) {
        decodeErrorResponse(json, outError);
        return false;
    }

    outResult.orderId = doc["orderId"] | 0L;
    outResult.orderNumber = doc["orderNumber"].as<const char*>() ? doc["orderNumber"].as<const char*>() : "";
    outResult.status = doc["status"].as<const char*>() ? doc["status"].as<const char*>() : "";
    outResult.message = doc["message"].as<const char*>() ? doc["message"].as<const char*>() : "";

    if (!doc["startedAt"].isNull()) {
        outResult.timestamp = doc["startedAt"].as<String>();
    } else if (!doc["completedAt"].isNull()) {
        outResult.timestamp = doc["completedAt"].as<String>();
    } else {
        outResult.timestamp = "Servidor confirmado";
    }

    return true;
}
