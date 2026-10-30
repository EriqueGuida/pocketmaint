# PocketMaint — Backend de Ordens de Serviço de Manutenção

> 📌 **Status do Repositório — Milestone 0: Ponto de Partida Legado (Baseline)**  
> **Tag:** `v0.0.0-baseline-legado`  
> Este commit registra o código original herdado de um projeto acadêmico do SENAI antes do início do processo de refatoração incremental para o padrão arquitetural do **PocketMaint**.

---

## 1. Sobre o Projeto

O **PocketMaint** é um sistema transacional de gerenciamento de ordens de serviço de manutenção técnica, projetado para operar com autoridade única sobre regras de negócio, máquina de estados estrita, autenticação por perfil (RBAC), controle anti-IDOR e trilha imutável de auditoria.

O desenvolvimento deste backend foi concebido como um caso prático de engenharia de software e refatoração evolutiva: partir de uma base acadêmica simples e transformá-la, passo a passo e com histórico auditável de commits, em uma API robusta e alinhada à especificação técnica oficial (`SPECIFICATION.md`).

---

## 2. Diagnóstico do Baseline (Situação no Commit 0)

O estado atual deste repositório representa o código funcional herdado do projeto do SENAI (`aberturaordensservico`). 

### O que já está presente no código:
- **Setores:** Cadastro, consulta por ID, listagem, atualização e exclusão física.
- **Equipamentos:** Cadastro vinculado a setor, listagem geral, consulta por setor, atualização e exclusão física.
- **Ordens de Serviço (Básico):** Cadastro de ordem (descrição e equipamento), consulta por ID, listagem de abertas e encerramento (gravando data de fechamento e texto de solução).
- **Stack Base:** Java 17, Spring Boot, Spring Data JPA, Hibernate, Bean Validation e MySQL.

### Gaps identificados em relação à especificação do PocketMaint:
| Área / Funcionalidade | Situação no Baseline (Commit 0) | Requisito Alvo (PocketMaint) |
| :--- | :--- | :--- |
| **Máquina de Estados** | Binária (`aberta` se `dataFechamento == null`, ou `encerrada`) | 5 estados formais: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| **Segurança e Acesso** | Nenhuma autenticação ou controle de perfil | Spring Security, JWT (Bearer Token), perfis `ADMIN`, `SUPERVISOR`, `TECHNICIAN` |
| **Controle Anti-IDOR** | Qualquer cliente pode manipular qualquer ordem | Validação de posse: técnico só opera ordens atribuídas a ele |
| **Modelo de Domínio** | Apenas `Setor`, `Equipamento` e `OrdemServico` simples | Inclusão de `User`, `OrderAssignment` e `AuditEvent` |
| **Auditoria** | Inexistente | Trilha imutável em `audit_events` na mesma transação `@Transactional` |
| **Esquema de Banco** | `ddl-auto: update` (criação automática de tabelas) | Migrações SQL versionadas via Flyway (`V1`, `V2`) |
| **Contratos de API** | Rotas sem versionamento (`/ordens-servico`, etc.) e erros crus | Prefixo `/api/v1/...` e tratamento de erros padronizado via RFC 9457 |
| **Integridade de Dados** | Exclusão física (`DELETE`) em setores e equipamentos | Exclusão lógica (`active = false`) para manter integridade relacional |
| **Concorrência** | Sem controle de versão | Concorrência otimista com `@Version` em `ServiceOrder` |

---

## 3. Roteiro Incremental de Milestones (Plano de Commits)

A transformação deste diretório será realizada em milestones sequenciais, onde cada etapa representa um commit específico:

- **[x] Milestone 0:** Publicação do baseline legado acadêmico e documentação inicial. *(Tag: `v0.0.0-baseline-legado`)*
- **[x] Milestone 1:** Dependências Maven (`pom.xml`), configuração de banco e migrações Flyway (`V1__schema`, `V2__seed`).
- **[x] Milestone 2:** Entidades de domínio completas (`User`, `ServiceOrder`, `OrderAssignment`, `AuditEvent`), Enums e Repositórios JPA.
- **[x] Milestone 3:** Máquina de estados (5 estados), regras de negócio (`BR-001` a `BR-008`) e `AuditService` transacional.
- **[x] Milestone 4:** Spring Security, autenticação JWT, emissão de sessão (`/api/v1/auth`) e autorização RBAC anti-IDOR.
- **[ ] Milestone 5:** Padronização RFC 9457 (`GlobalExceptionHandler`), `/api/v1/health` e endpoints de técnico (`/me/orders`, `/start`, `/complete`).
- **[ ] Milestone 6:** Endpoints de supervisor/admin (criação, atribuição, cancelamento, auditoria e gestão de ativos com exclusão lógica).
- **[ ] Milestone 7:** Documentação interativa OpenAPI/Swagger UI, finalização do README e encerramento do backend. *(Tag: `v1.0.0-backend`)*

---

## 4. Como Executar o Projeto no Estado Atual (Baseline)

### Pré-requisitos:
* **Java Development Kit (JDK):** Versão 17 LTS instalada e configurada no `PATH`.
* **Banco de Dados:** Servidor MySQL rodando localmente na porta `3306`.
* **Base de dados criada:**
  ```sql
  CREATE DATABASE ordens_servico;
  ```

### Configuração:
As credenciais do banco utilizam variáveis de ambiente com fallback para desenvolvimento local no arquivo `src/main/resources/application.properties`:
```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/ordens_servico?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
server.port=9090
```

Se o seu MySQL local possuir senha (ex.: `SENHA12345`), defina a variável `DB_PASSWORD` na sessão antes de rodar ou passe no comando:
```powershell
# No PowerShell:
$env:DB_PASSWORD="SuaSenhaAqui"
.\mvnw.cmd spring-boot:run
```

### Dados de Teste Locais (Migração Flyway V2):
A migração inicial insere as credenciais abaixo para testes em ambiente de desenvolvimento (todas com senha padrão `password`):

| Usuário | Perfil / Papel | Matrícula | Senha Padrão |
| :--- | :--- | :--- | :--- |
| `admin.sistema` | `ADMIN` | `ADM-001` | `password` |
| `sup.roberto` | `SUPERVISOR` | `SUP-001` | `password` |
| `tec.carlos` | `TECHNICIAN` | `TEC-001` | `password` |
| `tec.ana` | `TECHNICIAN` | `TEC-002` | `password` |

### Inicialização via Maven Wrapper:
No terminal do projeto:
```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

A API estará acessível em: `http://localhost:9090`

---

## 5. Rotas Disponíveis na Milestone 4

### Autenticação e Sessão (`/api/v1/auth`):
* `POST /api/v1/auth/login` — Autentica usuário com `username` e `password`, devolvendo o token JWT Bearer.
* `GET /api/v1/auth/me` — Valida o token e retorna os dados do usuário autenticado no cabeçalho `Authorization: Bearer <token>`.

### Setores (`/api/v1/sectors`):
* `POST /api/v1/sectors` — Cadastra setor (`code`, `name`, `description`) [Requer `ADMIN`]
* `GET /api/v1/sectors` — Lista setores ativos
* `GET /api/v1/sectors/{id}` — Busca setor por ID
* `PUT /api/v1/sectors/{id}` — Atualiza dados do setor [Requer `ADMIN`]
* `DELETE /api/v1/sectors/{id}` — Inativa setor (exclusão lógica) [Requer `ADMIN`]

### Equipamentos (`/api/v1/equipments`):
* `POST /api/v1/equipments` — Cadastra equipamento (`assetTag`, `name`, `model`, `serialNumber`, `sectorId`) [Requer `ADMIN`]
* `GET /api/v1/equipments` — Lista equipamentos ativos
* `GET /api/v1/equipments/{id}` — Busca equipamento por ID
* `GET /api/v1/equipments/sector/{sectorId}` — Lista equipamentos por setor
* `PUT /api/v1/equipments/{id}` — Atualiza equipamento [Requer `ADMIN`]
* `DELETE /api/v1/equipments/{id}` — Inativa equipamento (exclusão lógica) [Requer `ADMIN`]

### Ordens de Serviço (`/api/v1/orders`):
* `POST /api/v1/orders` — Cria nova ordem vinculada a equipamento [Requer `SUPERVISOR` ou `ADMIN`]
* `GET /api/v1/orders` — Lista todas as ordens
* `GET /api/v1/orders/{id}` — Busca ordem por ID
* `GET /api/v1/orders/open` — Lista ordens no estado `OPEN`
* `POST /api/v1/orders/{id}/assign` — Atribui OS ao técnico [Requer `SUPERVISOR` ou `ADMIN`]
* `POST /api/v1/orders/{id}/start` — Inicia atendimento [Exclusivo para o técnico responsável da ordem]
* `POST /api/v1/orders/{id}/complete` — Conclui registrando solução [Exclusivo para o técnico responsável]
* `POST /api/v1/orders/{id}/cancel` — Cancela com justificativa [Requer `SUPERVISOR` ou `ADMIN`]
* `GET /api/v1/orders/{id}/audit` — Trilha cronológica de auditoria [Requer `SUPERVISOR` ou `ADMIN`]
