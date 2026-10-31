#include "NetworkManager.h"

PocketNetworkManager pocketNetworkManager;

PocketNetworkManager::PocketNetworkManager()
    : _ssid(nullptr)
    , _password(nullptr)
    , _connecting(false)
    , _lastConnectionAttemptMs(0)
    , _lastRssiCheckMs(0)
    , _cachedRssi(-100)
{
}

void PocketNetworkManager::begin(const char* ssid, const char* password) {
    _ssid = ssid;
    _password = password;

    // Configura o rádio do ESP32-S3 como cliente (Station Mode)
    WiFi.mode(WIFI_STA);
    WiFi.disconnect(true);
    delay(100);

    connectInternal();
}

void PocketNetworkManager::connectInternal() {
    if (_ssid == nullptr || strlen(_ssid) == 0) {
        Serial.println("[PocketNetworkManager] ERRO: SSID nao configurado!");
        return;
    }

    Serial.printf("[PocketNetworkManager] Conectando a rede Wi-Fi 2.4 GHz: '%s'...\n", _ssid);
    WiFi.begin(_ssid, _password);
    _connecting = true;
    _lastConnectionAttemptMs = millis();
}

void PocketNetworkManager::update() {
    wl_status_t status = WiFi.status();

    if (status == WL_CONNECTED) {
        if (_connecting) {
            _connecting = false;
            _cachedRssi = WiFi.RSSI();
            Serial.printf("[PocketNetworkManager] Conectado com sucesso! IP: %s | RSSI: %d dBm\n",
                          WiFi.localIP().toString().c_str(), _cachedRssi);
        }

        // Atualiza medicao de RSSI a cada 3 segundos
        if (millis() - _lastRssiCheckMs > 3000) {
            _lastRssiCheckMs = millis();
            _cachedRssi = WiFi.RSSI();
        }
    } else {
        // Se desconectou, tenta reconectar a cada 6 segundos
        if (!_connecting && (millis() - _lastConnectionAttemptMs > 6000)) {
            Serial.println("[PocketNetworkManager] Conexao perdida. Tentando reconectar...");
            connectInternal();
        }
    }
}

void PocketNetworkManager::reconnect() {
    WiFi.disconnect();
    delay(200);
    connectInternal();
}

bool PocketNetworkManager::isConnected() const {
    return WiFi.status() == WL_CONNECTED;
}

String PocketNetworkManager::getSsid() const {
    return _ssid != nullptr ? String(_ssid) : "";
}

String PocketNetworkManager::getIpAddress() const {
    if (!isConnected()) return "0.0.0.0";
    return WiFi.localIP().toString();
}

String PocketNetworkManager::getGatewayIp() const {
    if (!isConnected()) return "0.0.0.0";
    return WiFi.gatewayIP().toString();
}

String PocketNetworkManager::getSubnetMask() const {
    if (!isConnected()) return "0.0.0.0";
    return WiFi.subnetMask().toString();
}

String PocketNetworkManager::getMacAddress() const {
    return WiFi.macAddress();
}

int PocketNetworkManager::getRssi() const {
    return _cachedRssi;
}

int PocketNetworkManager::getSignalQualityPercent() const {
    if (!isConnected()) return 0;
    // Mapeamento padrão de RSSI: -100 dBm (0%) a -50 dBm (100%)
    if (_cachedRssi <= -100) return 0;
    if (_cachedRssi >= -50) return 100;
    return 2 * (_cachedRssi + 100);
}

String PocketNetworkManager::getSignalQualityText() const {
    if (!isConnected()) return "Desconectado";
    int quality = getSignalQualityPercent();
    if (quality >= 80) return "Excelente";
    if (quality >= 60) return "Bom";
    if (quality >= 40) return "Regular";
    return "Fraco";
}
