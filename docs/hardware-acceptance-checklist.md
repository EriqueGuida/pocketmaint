# Checklist de Homologação Física de Hardware — PocketMaint ADV

> **Dispositivo Alvo:** M5Stack CardPuter ADV  
> **Microcontrolador:** Espressif ESP32-S3 (Dual-Core Xtensa LX7 @ 240 MHz, 320 KB SRAM, 4/8 MB Flash)  
> **Periféricos Integrados:** Display TFT ST7789V2 240x135 px, Matriz de 56 Teclas, Power Management AXP2101/ADC, Speaker I2S  
> **Backend Integrado:** PocketMaint REST API (Spring Boot 3.3.x / Java 17 LTS / MySQL 8)  
> **Responsável Técnico / Homologador:** Erique (Carlos Mendes — `ROLE_TECHNICIAN`)  
> **Data de Homologação:** 31 de Outubro de 2026  
> **Veredito Geral:** **APROVADO (100% dos testes concluídos com sucesso)**  

---

## 1. Matriz de Homologação e Testes de Aceitação em Bancada

| Código | Item de Homologação | Descrição do Teste | Critério de Aceitação | Resultado em Bancada | Status |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **VAL-01** | **Bring-up & Periféricos Básicos** | Inicialização do ESP32-S3 via USB CDC (`COM5`), teste de renderização do display TFT e resposta de teclas. | Display exibindo grade de diagnóstico sem artefatos, ADC de bateria lendo tensão e buzzer respondendo ao toque. | Executado com 100% de sucesso. Foto registrada em `cardputer-bringup-val01.jpg`. | **APROVADO** |
| **VAL-02** | **Conectividade Wi-Fi 2.4 GHz** | Associação à rede sem fio  com isolamento de credenciais em `secrets.h`. | Conexão em menos de 5 segundos, IP obtido (`192.168.101.51`) e RSSI medido continuamente. | Conexão estável com sinal excelente (-32 dBm / 100%). | **APROVADO** |
| **VAL-03** | **Estabilidade Visual & Double-Buffering** | Eliminação de flickering na tela de 240x135 px durante atualização de textos e cursores. | Renderização intermediária em `M5Canvas` (RAM) transferida via DMA direto. Zero efeito estroboscópico. | Tela perfeitamente estável em todas as 12 telas (`SCR-001` a `SCR-012`). | **APROVADO** |
| **VAL-04** | **Autenticação REST & Sessão JWT** | Disparo de `POST /api/v1/auth/login` com credenciais do técnico digitadas no teclado. | Retorno HTTP `200 OK`, extração de Bearer Token JWT e persistência ESTREITAMENTE em RAM (`SessionManager`). | Autenticação validada em bancada com saudação *"Bem-vindo, Carlos Mendes"*. Foto `cardputer-screen-auth-success-m11.jpg`. | **APROVADO** |
| **VAL-05** | **Consumo de Fila Real & Anti-IDOR** | Consulta da fila atribuída via `GET /api/v1/me/orders` com Bearer Token. | Exibição correta das ordens do técnico logado ou aviso de fila vazia sem travar o dispositivo. | Fila real consumida com sucesso com atalhos de atualização (`[R] Recarregar`). Foto `cardputer-screen-empty-orders-m12.jpg`. | **APROVADO** |
| **VAL-06** | **Ciclo Transacional & Regra BR-005** | Digitação de solução técnica no teclado de 56 teclas com contador e envio de `POST /complete`. | Bloqueio de textos com menos de 10 caracteres (BR-005) e transição confirmada para `COMPLETED`. | Bloqueio em vermelho validado com sucesso e submissão de texto aprovada. | **APROVADO** |
| **VAL-07** | **Resiliência de Rede (NFR-001)** | Simulação de timeout e desconexão de Wi-Fi durante requisições de alteração de estado. | O CardPuter exibe tela vermelha `SCR-010` e NUNCA finge sucesso local nem corrompe o estado em memória. | Comprovado: falhas de rede acionam `SCR-010` e mantêm o estado íntegro. | **APROVADO** |

---

## 2. Conformidade com Requisitos Não-Funcionais (NFRs)

- **NFR-001 (Integridade Transacional Sem Falsos Positivos):**  
  *Atendido.* O CardPuter somente atualiza o status de uma ordem (`IN_PROGRESS` ou `COMPLETED`) após receber a confirmação HTTP `200 OK` do backend com timestamp assinado. Falhas de comunicação disparam `SCR-010` e preservam o estado anterior.
- **NFR-002 (Consumo de Memória e Particionamento):**  
  *Atendido.* O particionamento `huge_app.csv` liberou 3.0 MB de espaço Flash, consumindo apenas 49.3% (1.55 MB) e apenas 16.1% de RAM volátil (52 KB).
- **NFR-003 (Segurança e Volatilidade de Credenciais):**  
  *Atendido.* Nenhum token JWT, credencial ou senha é persistido em NVS, EEPROM ou Flash. Ao desligar a chavinha lateral ou pressionar `[L] Logout`, toda a sessão na RAM é instantaneamente zerada.
- **NFR-004 (Ergonomia e Usabilidade em Tela Compacta):**  
  *Atendido.* Layout de 3 zonas estritas: Header industrial com ícones e telemetria (18 px), Área central de trabalho (97 px) e Footer invertido de teclas de atalho ativas (20 px).

---

## 3. Evidências Fotográficas do Hardware em Operação Real

Todas as fotos originais de bancada estão consolidadas no diretório [`docs/img/`](docs/img/):

1. `docs/img/cardputer-bringup-val01.jpg` — Diagnóstico de tela, teclado e ADC (VAL-01).
2. `docs/img/cardputer-wifi-infra-m08.jpg` — Associação Wi-Fi e medição de sinal em 2.4 GHz (VAL-02).
3. `docs/img/cardputer-screen-login-m09.jpg` — Formulário de autenticação com máscara `*` (SCR-003).
4. `docs/img/cardputer-screen-network-m11.jpg` — Diagnóstico com rota de saúde `/health` e IP real da API (SCR-002).
5. `docs/img/cardputer-screen-auth-success-m11.jpg` — Autenticação REST `200 OK` confirmando Bearer JWT na RAM (SCR-009).
6. `docs/img/cardputer-screen-solution-error-m10.jpg` — Validação de regra de negócio BR-005 bloqueando textos curtos (SCR-007).
7. `docs/img/cardputer-screen-solution-typed-m10.jpg` — Digitação pelo teclado físico com contador `X/500` (SCR-007).
8. `docs/img/cardputer-screen-confirm-complete-m10.jpg` — Diálogo prévio de confirmação de conclusão (SCR-008).
9. `docs/img/cardputer-screen-empty-orders-m12.jpg` — Consumo real da fila de ordens da API em bancada (SCR-004).

---

## 4. Veredito e Assinatura Técnica

A solução integrada **PocketMaint (Backend Spring Boot + Firmware ESP32-S3 CardPuter ADV)** atende integralmente a todas as especificações técnicas, padrões de segurança, protocolos de comunicação RESTful e requisitos de robustez industrial.

**Veredito:** `SISTEMA HOMOLOGADO PARA PRODUÇÃO / DEMONSTRAÇÃO TÉCNICA`
