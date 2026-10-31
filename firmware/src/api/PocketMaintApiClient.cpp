#include "PocketMaintApiClient.h"
#include "../net/NetworkManager.h"
#include <WiFi.h>

PocketMaintApiClient apiClient;

PocketMaintApiClient::PocketMaintApiClient()
    : _baseUrl("")
    , _lastHttpCode(0)
{
}

void PocketMaintApiClient::begin(const String& baseUrl) {
    _baseUrl = baseUrl;
    // Remove barra no final caso exista
    if (_baseUrl.endsWith("/")) {
        _baseUrl.remove(_baseUrl.length() - 1);
    }
}

void PocketMaintApiClient::configureClient(HTTPClient& http, const String& endpoint) {
    String fullUrl = _baseUrl + endpoint;
    http.begin(_wifiClient, fullUrl);
    http.setTimeout(7000); // 7 segundos de timeout
    http.addHeader("Content-Type", "application/json");
    http.addHeader("Accept", "application/json");
    http.addHeader("User-Agent", "PocketMaint-CardPuter/1.1");
}

bool PocketMaintApiClient::checkHealth(HealthPayload& outHealth, String& outErrorMsg) {
    if (!pocketNetworkManager.isConnected()) {
        outErrorMsg = "Wi-Fi desconectado";
        _lastHttpCode = 0;
        return false;
    }

    HTTPClient http;
    configureClient(http, "/api/v1/health");

    int httpCode = http.GET();
    _lastHttpCode = httpCode;

    if (httpCode <= 0) {
        outErrorMsg = "Timeout / Servidor inalcancavel";
        http.end();
        return false;
    }

    String responseBody = http.getString();
    http.end();

    if (httpCode == 200) {
        if (PayloadCodec::decodeHealthResponse(responseBody, outHealth)) {
            return true;
        }
        outErrorMsg = "Falha ao ler JSON de Saude";
        return false;
    }

    outErrorMsg = "HTTP " + String(httpCode);
    return false;
}

bool PocketMaintApiClient::login(const String& username, const String& password, LoginPayload& outLogin, ApiError& outError) {
    if (!pocketNetworkManager.isConnected()) {
        outError.status = 0;
        outError.errorCode = "WIFI_DISCONNECTED";
        outError.title = "Sem Conexao";
        outError.userMessage = "Wi-Fi nao conectado.";
        _lastHttpCode = 0;
        return false;
    }

    HTTPClient http;
    configureClient(http, "/api/v1/auth/login");

    String requestBody = PayloadCodec::encodeLoginRequest(username, password);
    int httpCode = http.POST(requestBody);
    _lastHttpCode = httpCode;

    if (httpCode <= 0) {
        outError.status = 0;
        outError.errorCode = "HTTP_TIMEOUT";
        outError.title = "Falha de Conexao";
        outError.userMessage = "Backend nao respondeu (Timeout).";
        http.end();
        return false;
    }

    String responseBody = http.getString();
    http.end();

    if (httpCode == 200) {
        if (PayloadCodec::decodeLoginResponse(responseBody, outLogin, outError)) {
            // Persistir na sessão volátil em RAM
            sessionManager.setSession(
                outLogin.token,
                outLogin.username,
                outLogin.fullName,
                outLogin.userId,
                outLogin.expiresIn
            );
            return true;
        }
        return false;
    }

    // Se HTTP 401 ou outro status de erro
    if (!PayloadCodec::decodeErrorResponse(responseBody, outError)) {
        outError.status = httpCode;
        outError.errorCode = (httpCode == 401) ? "AUTH_REQUIRED" : "HTTP_ERROR";
        outError.title = (httpCode == 401) ? "Acesso Recusado" : "Erro no Servidor";
        outError.userMessage = (httpCode == 401) ? "Usuario ou senha incorretos." : ("HTTP " + String(httpCode));
    }

    return false;
}
