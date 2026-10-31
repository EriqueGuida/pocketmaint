#pragma once

#ifndef POCKETMAINT_SESSION_MANAGER_H
#define POCKETMAINT_SESSION_MANAGER_H

#include <Arduino.h>

/**
 * PocketMaint — SessionManager
 * 
 * Gerencia a sessão do técnico autenticado ESTREITAMENTE em memória RAM volátil.
 * Atende ao requisito NFR-003 da especificação:
 * - Nenhum token JWT ou senha é gravado em NVS, EEPROM, Flash ou microSD.
 * - Ao desligar ou fazer logout, a sessão é completamente eliminada da RAM.
 */
class SessionManager {
public:
    SessionManager();

    // Registra uma nova sessão após autenticação 200 OK
    void setSession(const String& token, const String& username, const String& fullName, long technicianId, unsigned long expiresInSeconds);

    // Encerra a sessão imediatamente e limpa a memória RAM
    void clearSession();

    // Verificações de estado
    bool isAuthenticated() const;
    bool isExpired() const;

    // Getters para montagem de headers HTTP e telas
    String getToken() const;
    String getAuthorizationHeader() const;
    String getUsername() const;
    String getFullName() const;
    long getTechnicianId() const;
    unsigned long getExpiresInSecondsRemaining() const;

private:
    bool _authenticated;
    String _token;
    String _username;
    String _fullName;
    long _technicianId;
    unsigned long _loginTimestampMs;
    unsigned long _validDurationMs;
};

// Instância global para acesso pelos controladores e clientes HTTP
extern SessionManager sessionManager;

#endif // POCKETMAINT_SESSION_MANAGER_H
