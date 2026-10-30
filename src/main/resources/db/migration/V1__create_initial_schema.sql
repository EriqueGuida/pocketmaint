-- =============================================================================
-- PocketMaint - Migração Inicial Flyway (V1__create_initial_schema.sql)
-- Descrição: Criação das tabelas centrais do modelo relacional, chaves
-- estrangeiras com ON DELETE RESTRICT, índices de busca e idempotência.
-- =============================================================================

-- 1. Tabela de Usuários (Técnicos, Supervisores, Administradores)
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    matriculation_code VARCHAR(50) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_matriculation UNIQUE (matriculation_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Tabela de Setores (Localizações físicas / departamentos)
CREATE TABLE sectors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sectors_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabela de Equipamentos (Ativos sujeitos a manutenção)
CREATE TABLE equipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_tag VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    model VARCHAR(100) NULL,
    serial_number VARCHAR(100) NULL,
    sector_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_equipments_asset_tag UNIQUE (asset_tag),
    CONSTRAINT fk_equipments_sector FOREIGN KEY (sector_id)
        REFERENCES sectors (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabela de Ordens de Serviço (Entidade central do ciclo de vida)
CREATE TABLE service_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(30) NOT NULL,
    equipment_id BIGINT NOT NULL,
    sector_id BIGINT NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    assigned_technician_id BIGINT NULL,
    title VARCHAR(60) NOT NULL,
    problem_description VARCHAR(500) NOT NULL,
    solution_description VARCHAR(500) NULL,
    cancellation_reason VARCHAR(300) NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP NULL,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_service_orders_number UNIQUE (order_number),
    CONSTRAINT fk_so_equipment FOREIGN KEY (equipment_id)
        REFERENCES equipments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_sector FOREIGN KEY (sector_id)
        REFERENCES sectors (id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_created_by FOREIGN KEY (created_by_user_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_so_assigned_tech FOREIGN KEY (assigned_technician_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_so_tech_status (assigned_technician_id, status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Tabela de Histórico de Atribuições
CREATE TABLE order_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_order_id BIGINT NOT NULL,
    technician_id BIGINT NOT NULL,
    assigned_by_user_id BIGINT NOT NULL,
    active_assignment BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(200) NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMP NULL,
    CONSTRAINT fk_oa_service_order FOREIGN KEY (service_order_id)
        REFERENCES service_orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_oa_technician FOREIGN KEY (technician_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_oa_assigned_by FOREIGN KEY (assigned_by_user_id)
        REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Tabela de Eventos de Auditoria (Append-only e imutável)
CREATE TABLE audit_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    service_order_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    event_type VARCHAR(30) NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NULL,
    origin_channel VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(64) NULL,
    details VARCHAR(500) NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_service_order FOREIGN KEY (service_order_id)
        REFERENCES service_orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_audit_actor_user FOREIGN KEY (actor_user_id)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_audit_idempotency UNIQUE (service_order_id, event_type, idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
