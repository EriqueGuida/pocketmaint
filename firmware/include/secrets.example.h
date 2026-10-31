#pragma once

/**
 * PocketMaint — Template de Configuração de Segredos e Conectividade
 * 
 * INSTRUÇÕES:
 * 1. Copie este arquivo para 'firmware/include/secrets.h'.
 * 2. Preencha com o SSID e senha da sua rede Wi-Fi 2.4 GHz.
 * 3. Preencha com o IP da máquina onde o backend Spring Boot está rodando.
 * 4. O arquivo 'secrets.h' NUNCA será versionado no Git (protegido por .gitignore).
 */

#ifndef POCKETMAINT_SECRETS_H
#define POCKETMAINT_SECRETS_H

// Rede Wi-Fi (obrigatório frequência de 2.4 GHz)
#define WIFI_SSID       "SEU_WIFI_2_4GHZ"
#define WIFI_PASSWORD   "SUA_SENHA_WIFI"

// URL base do backend PocketMaint (IP da máquina na rede local)
// Exemplo: "http://192.168.1.100:9090"
#define API_BASE_URL    "http://192.168.1.100:9090"

#endif // POCKETMAINT_SECRETS_H
