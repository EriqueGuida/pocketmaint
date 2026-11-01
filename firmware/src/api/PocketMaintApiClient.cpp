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

bool PocketMaintApiClient::fetchMyOrders(std::vector<OrderItem>& outOrders, ApiError& outError) {
    if (!pocketNetworkManager.isConnected()) {
        outError.status = 0;
        outError.errorCode = "WIFI_DISCONNECTED";
        outError.title = "Sem Conexao";
        outError.userMessage = "Wi-Fi nao conectado.";
        _lastHttpCode = 0;
        return false;
    }

    HTTPClient http;
    configureClient(http, "/api/v1/me/orders?status=ASSIGNED,IN_PROGRESS&page=0&size=10");
    http.addHeader("Authorization", sessionManager.getAuthorizationHeader());
    http.addHeader("X-Origin-Channel", "CARDPUTER_TERMINAL");

    int httpCode = http.GET();
    _lastHttpCode = httpCode;

    if (httpCode <= 0) {
        outError.status = 0;
        outError.errorCode = "HTTP_TIMEOUT";
        outError.title = "Falha de Conexao";
        outError.userMessage = "Timeout ao consultar ordens.";
        http.end();
        return false;
    }

    String responseBody = http.getString();
    http.end();

    if (httpCode == 200) {
        return PayloadCodec::decodePagedOrders(responseBody, outOrders, outError);
    }

    if (!PayloadCodec::decodeErrorResponse(responseBody, outError)) {
        outError.status = httpCode;
        outError.errorCode = "HTTP_ERROR";
        outError.title = "Erro ao buscar ordens";
        outError.userMessage = "HTTP " + String(httpCode);
    }

    return false;
}

bool PocketMaintApiClient::fetchOrderDetail(long orderId, OrderDetail& outDetail, ApiError& outError) {
    if (!pocketNetworkManager.isConnected()) {
        outError.status = 0;
        outError.errorCode = "WIFI_DISCONNECTED";
        outError.title = "Sem Conexao";
        outError.userMessage = "Wi-Fi nao conectado.";
        _lastHttpCode = 0;
        return false;
    }

    HTTPClient http;
    String endpoint = "/api/v1/orders/" + String(orderId);
    configureClient(http, endpoint);
    http.addHeader("Authorization", sessionManager.getAuthorizationHeader());
    http.addHeader("X-Origin-Channel", "CARDPUTER_TERMINAL");

    int httpCode = http.GET();
    _lastHttpCode = httpCode;

    if (httpCode <= 0) {
        outError.status = 0;
        outError.errorCode = "HTTP_TIMEOUT";
        outError.title = "Falha de Conexao";
        outError.userMessage = "Timeout ao carregar detalhes.";
        http.end();
        return false;
    }

    String responseBody = http.getString();
    http.end();

    if (httpCode == 200) {
        return PayloadCodec::decodeOrderDetail(responseBody, outDetail, outError);
    }

    if (!PayloadCodec::decodeErrorResponse(responseBody, outError)) {
        outError.status = httpCode;
        outError.errorCode = (httpCode == 403) ? "ACCESS_DENIED_NOT_OWNER" : "HTTP_ERROR";
        outError.title = (httpCode == 403) ? "Acesso Negado" : "Erro no Servidor";
        outError.userMessage = (httpCode == 403) ? "Ordem nao atribuida a voce." : ("HTTP " + String(httpCode));
    }

    return false;
}

bool PocketMaintApiClient::startOrder(long orderId, OrderActionResult& outResult, ApiError& outError) {
    if (!pocketNetworkManager.isConnected()) {
        outError.status = 0;
        outError.errorCode = "WIFI_DISCONNECTED";
        outError.title = "Sem Conexao";
        outError.userMessage = "Wi-Fi nao conectado.";
        _lastHttpCode = 0;
        return false;
    }

    HTTPClient http;
    String endpoint = "/api/v1/orders/" + String(orderId) + "/start";
    configureClient(http, endpoint);
    http.addHeader("Authorization", sessionManager.getAuthorizationHeader());
    http.addHeader("X-Origin-Channel", "CARDPUTER_TERMINAL");
    
    // Idempotency-Key
    String idempKey = "cp-start-" + String(orderId) + "-" + String(millis());
    http.addHeader("Idempotency-Key", idempKey);

    int httpCode = http.POST("{}");
    _lastHttpCode = httpCode;

    if (httpCode <= 0) {
        outError.status = 0;
        outError.errorCode = "HTTP_TIMEOUT";
        outError.title = "Falha de Comunicacao";
        outError.userMessage = "Timeout ao iniciar atendimento.";
        http.end();
        return false;
    }

    String responseBody = http.getString();
    http.end();

    if (httpCode == 200) {
        return PayloadCodec::decodeOrderActionResult(responseBody, outResult, outError);
    }

    if (!PayloadCodec::decodeErrorResponse(responseBody, outError)) {
        outError.status = httpCode;
        outError.errorCode = "HTTP_ERROR";
        outError.title = "Falha ao iniciar";
        outError.userMessage = "HTTP " + String(httpCode);
    }

    return false;
}

bool PocketMaintApiClient::completeOrder(long orderId, const String& solution, OrderActionResult& outResult, ApiError& outError) {
    if (!pocketNetworkManager.isConnected()) {
        outError.status = 0;
        outError.errorCode = "WIFI_DISCONNECTED";
        outError.title = "Sem Conexao";
        outError.userMessage = "Wi-Fi nao conectado.";
        _lastHttpCode = 0;
        return false;
    }

    HTTPClient http;
    String endpoint = "/api/v1/orders/" + String(orderId) + "/complete";
    configureClient(http, endpoint);
    http.addHeader("Authorization", sessionManager.getAuthorizationHeader());
    http.addHeader("X-Origin-Channel", "CARDPUTER_TERMINAL");
    
    // Idempotency-Key
    String idempKey = "cp-complete-" + String(orderId) + "-" + String(millis());
    http.addHeader("Idempotency-Key", idempKey);

    String requestBody = PayloadCodec::encodeCompleteOrderRequest(solution);
    int httpCode = http.POST(requestBody);
    _lastHttpCode = httpCode;

    if (httpCode <= 0) {
        outError.status = 0;
        outError.errorCode = "HTTP_TIMEOUT";
        outError.title = "Falha de Comunicacao";
        outError.userMessage = "Timeout ao concluir ordem.";
        http.end();
        return false;
    }

    String responseBody = http.getString();
    http.end();

    if (httpCode == 200) {
        return PayloadCodec::decodeOrderActionResult(responseBody, outResult, outError);
    }

    if (!PayloadCodec::decodeErrorResponse(responseBody, outError)) {
        outError.status = httpCode;
        outError.errorCode = "HTTP_ERROR";
        outError.title = "Falha ao concluir";
        outError.userMessage = "HTTP " + String(httpCode);
    }

    return false;
}
