# Especificação Técnica de Engenharia de Software e Hardware — PocketMaint

> **Documento:** Especificação Técnica Canônica do Sistema  
> **Sistema:** PocketMaint — Sistema de Gestão e Execução de Ordens de Serviço Industriais  
> **Versão do Sistema:** `1.2.0-full-system`  
> **Data de Emissão:** 31 de Outubro de 2026  
> **Classificação:** Documento Técnico de Arquitetura e Engenharia de Campo  
> **Status:** Aprovado e Homologado em Bancada  

---

## 1. Visão Geral e Declaração de Escopo

O **PocketMaint** é uma plataforma transacional de missão crítica projetada para digitalização, despacho, atendimento presencial de campo e auditoria de Ordens de Serviço (OS) industriais.

### 1.1 O Problema Industrial
Em operações fabris convencionais, ordens de manutenção sofrem com:
- **Apontamentos em papel:** Formulários físicos sujeitos a perdas, ilegibilidade e atraso na alimentação do sistema ERP/CMMS.
- **Vulnerabilidade a fraudes de horário e execução:** Registro de atendimentos retroativos sem garantia de presença física do técnico junto à máquina.
- **Insegurança de dados e posse indevida:** Técnicos executando ou visualizando manutenções que não foram atribuídas à sua responsabilidade (violação BOLA/IDOR).
- **Falta de rastreabilidade formal:** Inexistência de uma trilha cronológica imutável que certifique quem abriu, quem despachou, quem iniciou e quem concluiu cada ordem.

### 1.2 A Solução PocketMaint
A plataforma resolve esses desafios através de uma divisão arquitetural estrita:
1. **Retaguarda Centralizada (Backend):** Servidor transacional em Spring Boot 3.3.x / Java 17 LTS com autoridade central absoluta sobre a máquina de estados, segurança stateless JWT com RBAC, validações semânticas RFC 9457 e persistência em banco relacional MySQL 8.
2. **Terminal Portátil de Campo (Firmware):** Microterminal industrial **M5Stack CardPuter ADV** (ESP32-S3 Dual-Core @ 240 MHz) com teclado físico QWERTY de 56 teclas, display colorido ST7789V2 e conexão Wi-Fi 2.4 GHz, permitindo ao técnico consultar a sua fila exclusiva, iniciar atendimentos com carimbo de tempo real e registrar justificativas técnicas com validação de regras de negócio na ponta.

---

## 2. Arquitetura Global e Topologia Fim a Fim

O sistema opera sob o modelo Cliente-Servidor transacional, garantindo integridade estrita e semântica idempotente:

```
┌───────────────────────────────────────────────┐
│           M5Stack CardPuter ADV               │
│        (Terminal Portátil de Campo)           │
│                                               │
│  [Teclado 56 Teclas]   [Display 240x135 px]   │
│  [ESP32-S3 Dual-Core]  [Bateria LiPo + ADC]   │
│                                               │
│  ┌─────────────────────────────────────────┐  │
│  │ Firmware C++ / FreeRTOS                 │  │
│  │ - UiRenderer (Double-Buffering RAM)     │  │
│  │ - AppController (State Machine SCR-001) │  │
│  │ - PocketMaintApiClient (HTTPClient)     │  │
│  │ - PayloadCodec (ArduinoJson v7)         │  │
│  │ - SessionManager (Volátil em RAM)       │  │
│  └─────────────────────────────────────────┘  │
└───────────────────────┬───────────────────────┘
                        │
                        │ 802.11 b/g/n (Wi-Fi 2.4 GHz)
                        │ HTTP/1.1 RESTful JSON
                        │ Header: Authorization: Bearer <JWT>
                        │ Header: Idempotency-Key: <UUID>
                        │ Header: X-Origin-Channel: CARDPUTER_TERMINAL
                        ▼
┌────────────────────────────────────────────────────────┐
│             PocketMaint Backend API                    │
│    (Spring Boot 3.3.x / Java 17 LTS / Tomcat 10)       │
│                                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Filtro de Segurança JWT (JwtAuthenticationFilter)│ │
│  │  - Validação Stateless HMAC-SHA256               │  │
│  │  - Captura ExpiredJwtException (AUTH_TOKEN_EXP.) │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────▼─────────────────────────┐  │
│  │  Controladores REST (EP-001 a EP-010 / OpenAPI)  │  │
│  │  - TechnicianOrderController (Anti-IDOR/BOLA)    │  │
│  │  - AdminOrderController (Supervisão / Atribuição)│  │
│  │  - AuthController & HealthController             │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────▼─────────────────────────┐  │
│  │  Camada de Serviços e Regras de Negócio          │  │
│  │  - ServiceOrderService (@Transactional ACID)     │  │
│  │  - AuditService (Trilha cronológica imutável)    │  │
│  │  - Máquina de Estados (BR-001 a BR-009)          │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           │                            │
│  ┌────────────────────────▼─────────────────────────┐  │
│  │  Persistência Spring Data JPA / Hibernate        │  │
│  │  - Entidades JPA ricas com versionamento @Version│  │
│  │  - Repositórios otimizados com consultas JPQL    │  │
│  └────────────────────────┬─────────────────────────┘  │
└───────────────────────────┼────────────────────────────┘
                            │
                            │ TCP Porta 3306 (Pool HikariCP)
                            ▼
           ┌─────────────────────────────────┐
           │       MySQL 8.0 Database        │
           │  (Flyway Migrations V1 e V2)    │
           │  ACID Transacional, UTF8MB4     │
           └─────────────────────────────────┘
```

---

## 3. Modelo de Domínio e Máquina de Estados

### 3.1 Ciclo de Vida da Ordem de Serviço
Toda Ordem de Serviço obedece a um fluxo formal e determinístico de 5 estados:

```
        ┌────────────────────────────────────────────────────────┐
        │                                                        │ (BR-006: Cancelar)
        ▼                                                        │
    [ OPEN ] ────(BR-002: Atribuir)───► [ ASSIGNED ] ───────────┤
                                             │                   │
                                   (BR-003: Iniciar)             │
                                             │                   │
                                             ▼                   │
                                     [ IN_PROGRESS ] ────────────┤
                                             │                   │
                                   (BR-004: Concluir)            │
                                             │                   │
                                             ▼                   ▼
                                       [ COMPLETED ]       [ CANCELLED ]
                                    (Estado Terminal)    (Estado Terminal)
```

### 3.2 Matriz de Transições de Estado

| Estado Atual | Ação Disparada | Perfil Autorizado | Próximo Estado | Regras Aplicadas |
| :--- | :--- | :--- | :--- | :--- |
| *Inexistente* | Criar Ordem | `ADMIN`, `SUPERVISOR` | **`OPEN`** | `BR-001` (Equipamento ativo obrigatório) |
| **`OPEN`** | Atribuir Técnico | `ADMIN`, `SUPERVISOR` | **`ASSIGNED`** | `BR-002` (Técnico ativo com `ROLE_TECHNICIAN`) |
| **`ASSIGNED`** | Iniciar Atendimento | `TECHNICIAN` (Dono) | **`IN_PROGRESS`** | `BR-003` (Anti-IDOR), `BR-007` (Idempotência) |
| **`IN_PROGRESS`** | Concluir Atendimento | `TECHNICIAN` (Dono) | **`COMPLETED`** | `BR-003` (Anti-IDOR), `BR-004` (Sem salto), `BR-005` (Solução 10-500 chars) |
| **`OPEN`** | Cancelar Ordem | `ADMIN`, `SUPERVISOR` | **`CANCELLED`** | `BR-006` (Justificativa 10-300 chars) |
| **`ASSIGNED`** | Cancelar Ordem | `ADMIN`, `SUPERVISOR` | **`CANCELLED`** | `BR-006` (Justificativa 10-300 chars) |
| **`IN_PROGRESS`** | Cancelar Ordem | `ADMIN`, `SUPERVISOR` | **`CANCELLED`** | `BR-006` (Justificativa 10-300 chars) |
| **`COMPLETED`** | *Qualquer transição* | *Nenhum* | *Bloqueado* | Transição Ilegal (HTTP 409) — Estado Terminal |
| **`CANCELLED`** | *Qualquer transição* | *Nenhum* | *Bloqueado* | Transição Ilegal (HTTP 409) — Estado Terminal |

---

## 4. Regras de Negócio Canônicas (BR-001 a BR-009)

As regras de negócio são encapsuladas diretamente nas entidades de domínio e orquestradas transacionalmente pela camada de serviços:

- **BR-001 — Vínculo Obrigatório com Equipamento Ativo:**  
  Nenhuma OS pode ser cadastrada sem apontar para um equipamento pré-cadastrado no sistema cujo flag `active` seja `true`. Tentativa com equipamento inativo resulta em `422 Unprocessable Entity`.
- **BR-002 — Atribuição Exclusiva a Técnicos Ativos:**  
  Apenas usuários com o papel `ROLE_TECHNICIAN` e status `active = true` podem receber a designação de uma ordem. A tentativa de atribuir um administrador ou supervisor como responsável técnico é barrada na validação de domínio.
- **BR-003 — Autorização Estrita Anti-IDOR / Anti-BOLA:**  
  O técnico autenticado no terminal só pode consultar detalhes, ler o defeito, iniciar ou concluir ordens explicitamente atribuídas ao seu identificador (`assignedTechnician.id == user.id`). Qualquer tentativa de manipular ordens de outros colegas resulta imediatamente em **HTTP 403 Forbidden** (`ACCESS_DENIED_NOT_OWNER`).
- **BR-004 — Proibição de Conclusão Direta sem Atendimento:**  
  Uma ordem em estado `ASSIGNED` nunca pode saltar diretamente para `COMPLETED`. A transição intermediária por `IN_PROGRESS` é obrigatória, garantindo que o tempo de atendimento presencial (`startedAt` até `completedAt`) seja computado com precisão.
- **BR-005 — Justificativa Técnica Obrigatória na Conclusão:**  
  O encerramento de uma ordem exige descrição técnica da solução executada com extensão de **10 a 500 caracteres**. Textos vazios ou inferiores a 10 caracteres são rejeitados tanto no teclado do CardPuter (`SCR-007`) quanto no validador do backend.
- **BR-006 — Regras de Cancelamento e Estados Terminais:**  
  Ordens em `COMPLETED` ou `CANCELLED` nunca podem sofrer cancelamento ou reabertura. O cancelamento exige justificativa formal de **10 a 300 caracteres** e permissão de `ADMIN` ou `SUPERVISOR`.
- **BR-007 — Idempotência de Transições com Auditoria Única:**  
  Chamadas repetidas aos endpoints de transição (ex: múltiplos cliques no botão de iniciar ou retransmissões do microcontrolador) com a mesma chave `Idempotency-Key` ou para uma ordem já no estado desejado devolvem `200 OK` sem gerar erros nem duplicar registros de auditoria.
- **BR-008 — Auditoria Transacional Atômica Imutável:**  
  Toda e qualquer transição gera obrigatoriamente um registro na tabela `audit_events` na mesma transação atômica (`@Transactional`). Se a persistência do evento falhar, toda a operação da ordem sofre rollback automático.
- **BR-009 — Inativação Lógica de Ativos (Soft Delete):**  
  Equipamentos e Setores que possuam ordens de serviço vinculadas nunca sofrem exclusão física (`DELETE FROM ...`); são inativados logicamente marcando `active = false`.

---

## 5. Modelo de Segurança, Autenticação e Perfis (RBAC)

### 5.1 Credenciais e Perfis do Sistema

| Usuário Padrão | Perfil (`Role`) | Permissões no Sistema |
| :--- | :--- | :--- |
| `admin.sistema` | `ROLE_ADMIN` | Acesso irrestrito: gestão de ativos, setores, usuários, abertura, despacho e cancelamento de ordens. |
| `sup.roberto` | `ROLE_SUPERVISOR` | Gestão de operações: abertura de ordens, atribuição de técnicos, cancelamento e auditoria. |
| `tec.carlos` | `ROLE_TECHNICIAN` | Operação de campo (CardPuter): consulta de fila própria (`/me/orders`), início e encerramento de suas OSs. |
| `tec.ana` | `ROLE_TECHNICIAN` | Técnico de bancada (usada nos testes automatizados para validação cruzada anti-IDOR). |

### 5.2 Arquitetura de Tokens JWT Stateless
- **Algoritmo de Assinatura:** HMAC-SHA256 com chave simétrica segura configurada no servidor.
- **Tempo de Expiração:** 8 horas (28.800 segundos), compatível com o turno padrão industrial.
- **Claims Embutidos:** `sub` (username), `userId` (Long), `role` (Role), `iat` (emissão) e `exp` (expiração).
- **Tratamento de Token Vencido:** O filtro [`JwtAuthenticationFilter`](file:///c:/Users/eriqu/Downloads/abertura-ordens-servico-main/src/main/java/br/com/pocketmaint/security/JwtAuthenticationFilter.java) captura explicitamente `ExpiredJwtException` e emite resposta RFC 9457 com código `AUTH_TOKEN_EXPIRED` (HTTP 401), orientando o CardPuter a apresentar a tela `SCR-012` e redirecionar ao login.

---

## 6. Padronização de Erros e Respostas (RFC 9457)

Todas as respostas de erro emitidas pelo backend seguem estritamente a especificação canônica da IETF **RFC 9457 (Problem Details for HTTP APIs)**:

```json
{
  "timestamp": "2026-10-31T20:30:00Z",
  "status": 403,
  "errorCode": "ACCESS_DENIED_NOT_OWNER",
  "title": "Acesso negado ao recurso",
  "userMessage": "Esta ordem de serviço não está atribuída ao seu usuário.",
  "path": "/api/v1/orders/1",
  "fieldErrors": []
}
```

### Códigos Canônicos de Máquina (`errorCode`):
- `AUTH_REQUIRED`: Credenciais ausentes, incorretas ou malformadas (HTTP 401).
- `AUTH_TOKEN_EXPIRED`: Credencial JWT expirada após 8 horas (HTTP 401).
- `ACCESS_DENIED_NOT_OWNER`: Violação da barreira Anti-IDOR/BOLA (HTTP 403).
- `ACCESS_DENIED_INSUFFICIENT_ROLE`: Tentativa de acesso a rota protegida sem perfil exigido (HTTP 403).
- `RESOURCE_NOT_FOUND`: Ordem, equipamento ou setor não localizado por ID (HTTP 404).
- `INVALID_ORDER_STATE_TRANSITION`: Tentativa de salto ou transição proibida na máquina de estados (HTTP 409).
- `VALIDATION_FAILED`: Violação de regras de validação nos campos do DTO (HTTP 400 ou 422).

---

## 7. Catálogo Canônico de Endpoints da API

| Código | Método | Rota | Perfis Permitidos | Descrição e Finalidade |
| :---: | :---: | :--- | :---: | :--- |
| **EP-001** | `GET` | `/api/v1/health` | Público | Verificação de disponibilidade, versão da API e sincronismo de relógio UTC. |
| **EP-002** | `POST` | `/api/v1/auth/login` | Público | Autentica técnico ou supervisor e emite Bearer Token JWT (8h). |
| **EP-003** | `GET` | `/api/v1/auth/me` | Autenticado | Identificação dos dados cadastrais e perfil da sessão ativa. |
| **EP-004** | `GET` | `/api/v1/me/orders` | `TECHNICIAN` | Fila compacta de ordens (`ASSIGNED`, `IN_PROGRESS`) atribuídas ao técnico. |
| **EP-005** | `GET` | `/api/v1/orders/{id}` | `TECH`*, `SUP`, `ADMIN` | Consulta detalhada do defeito da OS (*se técnico, apenas suas). |
| **EP-006** | `POST` | `/api/v1/orders/{id}/start` | `TECHNICIAN`* | Inicia atendimento presencial (`ASSIGNED` $\rightarrow$ `IN_PROGRESS`). |
| **EP-007** | `POST` | `/api/v1/orders/{id}/complete` | `TECHNICIAN`* | Registra solução técnica e conclui OS (`IN_PROGRESS` $\rightarrow$ `COMPLETED`). |
| **EP-008** | `POST` | `/api/v1/orders` | `SUPERVISOR`, `ADMIN` | Abertura de nova ordem vinculada a equipamento ativo. |
| **EP-009** | `POST` | `/api/v1/orders/{id}/assign` | `SUPERVISOR`, `ADMIN` | Designação formal de técnico responsável para atendimento. |
| **EP-010** | `POST` | `/api/v1/orders/{id}/cancel` | `SUPERVISOR`, `ADMIN` | Cancelamento com justificativa obrigatória de 10 a 300 caracteres. |
| **EP-010** | `GET` | `/api/v1/orders/{id}/audit` | `SUPERVISOR`, `ADMIN` | Trilha cronológica imutável de eventos de auditoria transacional. |
| **Ativos** | `*` | `/api/v1/equipments/**` | `ADMIN` | Gestão completa de equipamentos industriais com soft-delete. |
| **Ativos** | `*` | `/api/v1/sectors/**` | `ADMIN` | Gestão completa de setores fabris com soft-delete. |

---

## 8. Especificação do Terminal Portátil (M5Stack CardPuter ADV)

### 8.1 Arquitetura do Firmware Embarcado
O firmware foi estruturado em módulos C++ com isolamento estrito de responsabilidades:
- **`HardwareInit`:** Inicialização do display TFT, calibração do barramento I2C, leitura com filtro passa-baixa do ADC de bateria e efeitos sonoros piezoelétricos.
- **`PocketNetworkManager`:** Gerenciador resiliente de Wi-Fi em 2.4 GHz, com medição contínua de RSSI e rotinas de reconexão automática.
- **`SessionManager`:** Armazenamento **estritamente em RAM volátil** das credenciais e do Bearer Token JWT. Atende ao requisito **NFR-003**: ao desligar o aparelho, nenhum dado permanece em Flash, EEPROM ou NVS.
- **`UiRenderer`:** Motor gráfico baseado em **Double-Buffering** via `M5Canvas` (RAM). Os elementos visuais são compostos na memória e descarregados atomicamente no LCD físico, eliminando 100% de oscilações ópticas (*zero flickering*).
- **`PayloadCodec`:** Serialização e desserialização via **ArduinoJson v7** de structs C++ em JSON RESTful.
- **`PocketMaintApiClient`:** Cliente HTTP RESTful nativo com gerenciamento de timeouts e injeção de cabeçalhos de segurança.
- **`AppController`:** Máquina de estados determinística de navegação de telas do terminal.

### 8.2 Catálogo de Telas da Interface de Usuário

| Código da Tela | Título / Função | Descrição de Operação | Teclas Ativas / Atalhos |
| :---: | :--- | :--- | :--- |
| **SCR-001** | **Splash / Boot** | Apresentação do sistema e inicialização dos barramentos de hardware. | `[ENTER]` Avançar |
| **SCR-002** | **Diagnóstico de Conexão** | Exibe SSID, IP local, RSSI em tempo real, URL da API e rota de saúde. | `[ENTER]` Login, `[H]` Health, `[R]` Reconectar |
| **SCR-003** | **Autenticação Segura** | Formulário de credenciais com máscara de asteriscos (`*`) no campo senha. | `[;/.]` Alternar Campo, `[ENTER]` Entrar |
| **SCR-004** | **Fila de Ordens de Serviço** | Fila real do técnico com cursor (`>`), status colorido e nível de prioridade. | `[;/.]` Navegar, `[ENTER]` Detalhes, `[R]` Atualizar, `[L]` Sair |
| **SCR-005** | **Detalhes da Ordem** | Consulta de equipamento, setor e texto do defeito com rolagem vertical. | `[ESC]` Voltar, `[I]` Iniciar OS, `[C]` Concluir OS |
| **SCR-006** | **Confirmação de Início** | Diálogo de confirmação prévia com código da OS e equipamento. | `[ESC]` Cancelar, `[ENTER]` Confirmar Início |
| **SCR-007** | **Registro de Solução** | Buffer de texto com contador dinâmico `X/500` e bloqueio de menos de 10 chars. | Teclado QWERTY, `[DEL]` Apagar, `[ENTER]` Avançar |
| **SCR-008** | **Confirmação de Conclusão** | Resumo da solução digitada antes do envio irrevogável ao servidor. | `[ESC]` Editar, `[ENTER]` Concluir OS |
| **SCR-009** | **Feedback Verde (Sucesso)** | Faixa verde com status confirmado pelo servidor e carimbo de data/hora. | `[ENTER]` Continuar |
| **SCR-010** | **Feedback Vermelho (Erro)** | Alerta de falha de conexão, timeout ou queda de Wi-Fi sem alterar estado local. | `[ENTER]` Continuar, `[R]` Tentar novamente |
| **SCR-011** | **Acesso Negado (403/401)** | Alerta visual de violação de posse anti-IDOR ou credenciais inválidas. | `[ENTER]` Voltar |
| **SCR-012** | **Sessão Expirada (401)** | Alerta de expiração do Bearer Token JWT com redirecionamento ao login. | `[ENTER]` Fazer Novo Login |

### 8.3 Ergonomia e Padronização de Display (240x135 px)
A interface organiza a tela compacta de 240x135 pixels em 3 zonas estritas:
1. **Header Industrial (18 px):** Barra azul escura com nome da tela à esquerda, status Wi-Fi ao centro e porcentagem da bateria à direita.
2. **Área de Trabalho Útil (97 px):** Área central reservada para formulários, listas de ordens ou caixas de texto com fontes limpas de alta nitidez.
3. **Footer de Teclas Ativas (20 px):** Barra cinza escura invertida na base exibindo apenas os atalhos válidos no contexto da tela corrente.

---

## 9. Requisitos Não-Funcionais (NFRs)

- **NFR-001 — Integridade Transacional e Semântica Sem Falso Positivo:**  
  O CardPuter opera com autoridade servidora estrita: o status local da ordem só é transicionado após confirmação HTTP `200 OK` do backend. Se o sinal de Wi-Fi cair durante a requisição, o CardPuter aciona `SCR-010` e preserva o estado anterior intacto.
- **NFR-002 — Eficiência de Memória e Particionamento no ESP32-S3:**  
  O firmware utiliza o particionamento `huge_app.csv`, alocando 3.0 MB de espaço Flash para a aplicação. A compilação final consome 1.55 MB de Flash (49.3%) e apenas 52 KB de RAM (16.1%), assegurando ampla folga térmica e de execução.
- **NFR-003 — Proteção de Dados e Volatilidade de Credenciais:**  
  Tokens JWT e senhas são armazenados exclusivamente em variáveis de RAM volátil (`SessionManager`). Nenhum segredo é escrito em Flash, EEPROM, NVS ou microSD. Ao reiniciar o dispositivo, a sessão deixa de existir fisicamente.
- **NFR-004 — Estabilidade Visual e Telemetria de Bateria:**  
  A amostragem da tensão da bateria utiliza filtro passa-baixa ($80\%$ histórico $+ 20\%$ leitura atual) a cada 5 segundos para suprimir oscilações provocadas por consumo transitório do transmissor Wi-Fi.

---

## 10. Matriz de Rastreabilidade e Cobertura de Testes

O projeto conta com mais de 30 testes automatizados cobrindo todos os cenários da especificação:

| Suíte de Testes | Tipo de Teste | Qtd | Cobertura e Regras Validadas |
| :--- | :--- | :---: | :--- |
| **`DtoValidationUnitTest`** | Unidade | 4 | Validação de restrições de DTOs Hibernate (`min/max` de caracteres, campos nulos). |
| **`OrderStateMachineUnitTest`** | Unidade | 15 | Ciclo de vida completo, transições ilegais, bloqueio em estados terminais, regras BR-004/005/006 e idempotência BR-007. |
| **`JwtAuthenticationFilterUnitTest`** | Unidade | 4 | Tratamento de Bearer tokens ausentes, tokens malformados e captura de expiração `AUTH_TOKEN_EXPIRED`. |
| **`OrderSecurityIntegrationTest`** | Integração (MockMvc) | 10 | Barreiras Anti-IDOR/BOLA (BR-003), autorização RBAC por perfil, fila exclusiva do técnico e auditoria atômica (BR-008). |
| **`Bancada CardPuter ADV`** | Homologação Física | 7 | Testes de hardware `VAL-01` a `VAL-07` registrados em [`docs/hardware-acceptance-checklist.md`](hardware-acceptance-checklist.md). |

---

## 11. Conclusão e Selo de Homologação

A plataforma **PocketMaint** encontra-se plenamente desenvolvida, testada contra regressões e validada em hardware físico, demonstrando a perfeita convergência entre **Engenharia de Software Corporativa (Spring Boot)** e **Sistemas Embarcados de Baixo Consumo (ESP32-S3 / CardPuter ADV)**.
