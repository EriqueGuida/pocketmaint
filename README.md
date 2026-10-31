# PocketMaint — Backend de Ordens de Serviço de Manutenção

> 📌 **Status do Repositório — Backend Concluído com Sucesso**  
> **Versão:** `1.0.0` | **Tag de Conclusão:** `v1.0.0-backend`  
> Este repositório contém a implementação completa da API RESTful do **PocketMaint**, estruturada para servir tanto terminais portáteis industriais (**M5Stack CardPuter ADV**) quanto interfaces web administrativas de supervisão de manutenção.

---

## 1. Visão Geral e Arquitetura

O **PocketMaint** é um sistema transacional com autoridade centralizada para abertura, despacho, atendimento de campo e auditoria de Ordens de Serviço (OS) industriais.

### 1.1 Arquitetura Monólito Modular
O backend foi concebido sob o padrão de **Monólito Modular** em camadas (*Layered Architecture*), priorizando alta coesão, acoplamento controlado, facilidade de teste e baixa complexidade operacional:

```
src/main/java/br/com/pocketmaint/
├── config/              # Configurações do OpenAPI/Swagger UI e Beans de infraestrutura
├── controller/          # Endpoints RESTful versionados (/api/v1/...) com anotações OpenAPI
├── dto/                 # Contratos de entrada e saída imutáveis (Records nativos do Java 17)
├── exception/           # Manipulador global RFC 9457 e exceções semânticas de negócio
├── model/               # Entidades JPA ricas, encapsulando transições da máquina de estados
│   └── enums/           # Enums tipados (OrderStatus, Role, Priority, AuditEventType, OriginChannel)
├── repository/          # Interfaces Spring Data JPA para persistência otimizada
├── security/            # Spring Security, filtros JWT stateless, RBAC e validação anti-IDOR
└── service/             # Orquestração transacional, regras de negócio e serviço de auditoria
```

---

## 2. Máquina de Estados e Regras de Negócio

O ciclo de vida de uma Ordem de Serviço obedece a uma máquina de estados estrita com 5 estados formais:

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

### Regras de Negócio Canônicas (BR-001 a BR-009):
- **BR-001 — Vínculo Obrigatório com Equipamento Ativo:** Toda OS deve estar associada a um equipamento ativo cadastrado.
- **BR-002 — Atribuição Exclusiva a Técnicos Ativos:** Apenas usuários ativos com o papel `TECHNICIAN` podem ser atribuídos a uma OS.
- **BR-003 — Autorização Anti-IDOR de Atendimento:** O técnico autenticado só pode visualizar detalhes completos, iniciar ou concluir ordens designadas especificamente ao seu ID.
- **BR-004 — Proibição de Conclusão Direta:** Uma OS não pode saltar de `ASSIGNED` diretamente para `COMPLETED`; a transição intermediária por `IN_PROGRESS` é obrigatória.
- **BR-005 — Justificativa Técnica Obrigatória na Conclusão:** O fechamento da ordem exige descrição técnica da solução com extensão entre 10 e 500 caracteres.
- **BR-006 — Proibição de Cancelamento em Estados Terminais:** Ordens em `COMPLETED` ou `CANCELLED` não podem ser canceladas. O cancelamento exige justificativa entre 10 e 300 caracteres.
- **BR-007 — Idempotência de Transições:** Repetições de requisições de início ou encerramento com a mesma chave/estado retornam `200 OK` sem gerar erros nem duplicar registros de auditoria.
- **BR-008 — Auditoria Transacional Obrigatória:** Toda transição de estado gera registro imutável em `audit_events` na mesma transação atômica (`@Transactional`).
- **BR-009 — Inativação em vez de Exclusão Física (Soft Delete):** Setores e Equipamentos nunca sofrem `DELETE` físico se houver registros vinculados; recebem marcação `active = false`.

---

## 3. Roteiro Incremental de Milestones (Histórico de Entrega)

Todas as metas estabelecidas no plano de modernização foram integralmente cumpridas:

- **[x] Milestone 0:** Publicação do baseline legado acadêmico e documentação inicial. *(Tag: `v0.0.0-baseline-legado`)*
- **[x] Milestone 1:** Dependências Maven (`pom.xml`), configuração de banco e migrações Flyway (`V1__schema`, `V2__seed`).
- **[x] Milestone 2:** Entidades de domínio completas (`User`, `ServiceOrder`, `OrderAssignment`, `AuditEvent`), Enums e Repositórios JPA.
- **[x] Milestone 3:** Máquina de estados (5 estados), regras de negócio (`BR-001` a `BR-008`) e `AuditService` transacional.
- **[x] Milestone 4:** Spring Security, autenticação JWT, emissão de sessão (`/api/v1/auth`) e autorização RBAC anti-IDOR.
- **[x] Milestone 5:** Padronização RFC 9457 (`GlobalExceptionHandler`), `/api/v1/health` e endpoints de técnico (`/me/orders`, `/start`, `/complete`), migração dos DTOs para Java 17 records.
- **[x] Milestone 6:** Endpoints de supervisor/admin (criação, atribuição, cancelamento, auditoria e gestão de ativos com exclusão lógica).
- **[x] Milestone 7:** Documentação interativa OpenAPI/Swagger UI, finalização do README e encerramento do backend. *(Tag: `v1.0.0-backend`)*

---

## 4. Como Executar o Backend Localmente

### Pré-requisitos:
* **Java Development Kit (JDK):** Versão 17 LTS instalada e acessível no terminal (`java -version`).
* **Banco de Dados:** Servidor MySQL 8.x em execução na porta padrão `3306`.
* **Criação do Banco de Dados:**
  ```sql
  CREATE DATABASE IF NOT EXISTS ordens_servico CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  ```

### Variáveis de Ambiente e Configuração:
O arquivo `src/main/resources/application.properties` está preparado com fallbacks automáticos para desenvolvimento local:

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/ordens_servico?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
server.port=9090
```

Se o seu MySQL local exigir usuário ou senha específicos:
```powershell
# Exemplo no PowerShell (Windows):
$env:DB_USERNAME="root"
$env:DB_PASSWORD="SuaSenhaAqui"
.\mvnw.cmd spring-boot:run
```
```bash
# Exemplo no Bash (Linux / macOS):
export DB_USERNAME="root"
export DB_PASSWORD="SuaSenhaAqui"
./mvnw spring-boot:run
```

---

## 5. Credenciais Iniciais de Teste (Migração Flyway V2)

Ao iniciar a aplicação pela primeira vez, as migrações Flyway (`V1__create_initial_schema.sql` e `V2__seed_dev_data.sql`) estruturam o banco e inserem usuários de teste com senhas criptografadas em **BCrypt**:

| Usuário | Perfil / Papel (`Role`) | Nome Completo | Senha Padrão | Cenário de Uso |
| :--- | :--- | :--- | :--- | :--- |
| `admin.sistema` | `ROLE_ADMIN` | Administrador Geral | `password` | Gestão de setores, equipamentos e ordens |
| `sup.roberto` | `ROLE_SUPERVISOR` | Roberto Lima | `password` | Abertura, atribuição, cancelamento e auditoria |
| `tec.carlos` | `ROLE_TECHNICIAN` | Carlos Mendes | `password` | Operação no terminal móvel (CardPuter) |
| `tec.ana` | `ROLE_TECHNICIAN` | Ana Paula Silva | `password` | Testes de controle de posse e anti-IDOR |

---

## 6. Documentação Interativa OpenAPI / Swagger UI

A documentação interativa da API está disponível diretamente no navegador assim que o servidor é iniciado:

* **URL Principal / Redirecionamento Automático:** [`http://localhost:9090/`](http://localhost:9090/)
* **URL Direta do Swagger UI:** [`http://localhost:9090/swagger-ui.html`](http://localhost:9090/swagger-ui.html)
* **Especificação OpenAPI em JSON:** [`http://localhost:9090/v3/api-docs`](http://localhost:9090/v3/api-docs)

### Como autenticar no Swagger UI:
1. Acesse o endpoint **`POST /api/v1/auth/login`**.
2. Execute a requisição com o usuário desejado (ex.: `tec.carlos` e senha `password`).
3. Copie o valor do campo `accessToken` da resposta JSON.
4. Clique no botão **Authorize** (ícone de cadeado verde no topo direito do Swagger UI).
5. Cole o token no campo de valor e clique em **Authorize**. Todas as requisições subsequentes incluirão o cabeçalho `Authorization: Bearer <token>`.

---

## 7. Catálogo Canônico de Endpoints da API

| Código | Método | Rota | Perfis Permitidos | Descrição |
| :---: | :---: | :--- | :---: | :--- |
| **EP-001** | `GET` | `/api/v1/health` | Público | Verificação de disponibilidade e horário do servidor. |
| **EP-002** | `POST` | `/api/v1/auth/login` | Público | Autentica credenciais e emite token Bearer JWT (8h). |
| **EP-003** | `GET` | `/api/v1/auth/me` | Autenticado | Identificação dos dados da sessão ativa. |
| **EP-004** | `GET` | `/api/v1/me/orders` | `TECHNICIAN` | Lista compacta e paginada das ordens do técnico logado. |
| **EP-005** | `GET` | `/api/v1/orders/{id}` | `TECHNICIAN`*, `SUPERVISOR`, `ADMIN` | Consulta detalhada da OS (*se técnico, apenas as suas). |
| **EP-006** | `POST` | `/api/v1/orders/{id}/start` | `TECHNICIAN`* | Inicia atendimento presencial (`ASSIGNED` $\rightarrow$ `IN_PROGRESS`). |
| **EP-007** | `POST` | `/api/v1/orders/{id}/complete` | `TECHNICIAN`* | Registra solução e conclui OS (`IN_PROGRESS` $\rightarrow$ `COMPLETED`). |
| **EP-008** | `POST` | `/api/v1/orders` | `SUPERVISOR`, `ADMIN` | Abertura de nova OS vinculada a equipamento ativo. |
| **EP-009** | `POST` | `/api/v1/orders/{id}/assign` | `SUPERVISOR`, `ADMIN` | Designação ou reatribuição de técnico responsável. |
| **EP-010** | `POST` | `/api/v1/orders/{id}/cancel` | `SUPERVISOR`, `ADMIN` | Cancelamento de OS com justificativa obrigatória. |
| **EP-010** | `GET` | `/api/v1/orders/{id}/audit` | `SUPERVISOR`, `ADMIN` | Trilha cronológica imutável de eventos de auditoria. |
| **Ativos** | `POST/PUT/DELETE` | `/api/v1/sectors/**` | `ADMIN` | Gestão de setores com inativação lógica. |
| **Ativos** | `POST/PUT/DELETE` | `/api/v1/equipments/**` | `ADMIN` | Gestão de equipamentos com inativação lógica. |

---

## 8. Padronização de Erros (RFC 9457)

Qualquer falha de requisição ou violação de regra de negócio devolve respostas uniformes no padrão canônico RFC 9457:

```json
{
  "timestamp": "2026-10-30T17:15:00Z",
  "status": 403,
  "errorCode": "ACCESS_DENIED_NOT_OWNER",
  "title": "Acesso negado ao recurso",
  "userMessage": "Esta ordem de serviço não está atribuída ao seu usuário.",
  "path": "/api/v1/orders/42",
  "fieldErrors": []
}
```