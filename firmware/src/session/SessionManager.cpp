#include "SessionManager.h"

SessionManager sessionManager;

SessionManager::SessionManager()
    : _authenticated(false)
    , _token("")
    , _username("")
    , _fullName("")
    , _technicianId(0)
    , _loginTimestampMs(0)
    , _validDurationMs(0)
{
}

void SessionManager::setSession(const String& token, const String& username, const String& fullName, long technicianId, unsigned long expiresInSeconds) {
    _token = token;
    _username = username;
    _fullName = fullName;
    _technicianId = technicianId;
    _loginTimestampMs = millis();
    _validDurationMs = expiresInSeconds * 1000UL;
    _authenticated = true;
}

void SessionManager::clearSession() {
    // Sobrescreve com espaços antes de liberar para higienização de memória
    if (_token.length() > 0) {
        for (unsigned int i = 0; i < _token.length(); i++) {
            _token[i] = '\0';
        }
    }
    _token = "";
    _username = "";
    _fullName = "";
    _technicianId = 0;
    _loginTimestampMs = 0;
    _validDurationMs = 0;
    _authenticated = false;
}

bool SessionManager::isAuthenticated() const {
    if (!_authenticated) return false;
    if (isExpired()) return false;
    return true;
}

bool SessionManager::isExpired() const {
    if (!_authenticated) return true;
    if (_validDurationMs == 0) return false; // Sem expiracao definida
    return (millis() - _loginTimestampMs) >= _validDurationMs;
}

String SessionManager::getToken() const {
    return _token;
}

String SessionManager::getAuthorizationHeader() const {
    if (!_authenticated || _token.length() == 0) return "";
    return "Bearer " + _token;
}

String SessionManager::getUsername() const {
    return _username;
}

String SessionManager::getFullName() const {
    return _fullName;
}

long SessionManager::getTechnicianId() const {
    return _technicianId;
}

unsigned long SessionManager::getExpiresInSecondsRemaining() const {
    if (!_authenticated || isExpired()) return 0;
    unsigned long elapsed = millis() - _loginTimestampMs;
    if (elapsed >= _validDurationMs) return 0;
    return (_validDurationMs - elapsed) / 1000UL;
}
