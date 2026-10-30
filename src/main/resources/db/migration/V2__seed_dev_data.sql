-- =============================================================================
-- PocketMaint - Carga Inicial de Dados de Desenvolvimento (V2__seed_dev_data.sql)
-- Descrição: Insere usuários iniciais de teste (Admin, Supervisor e Técnicos),
-- setores e equipamentos de referência para homologação e desenvolvimento local.
-- Senha padrão de todos os usuários de teste: 'password'
-- Hash BCrypt (custo 10): $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
-- =============================================================================

-- 1. Inserção de Usuários de Teste
INSERT INTO users (id, username, password_hash, full_name, role, matriculation_code, active, created_at)
VALUES 
    (1, 'admin.sistema', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Administrador do Sistema', 'ADMIN', 'ADM-001', TRUE, NOW()),
    (2, 'sup.roberto', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Roberto Almeida (Supervisor)', 'SUPERVISOR', 'SUP-001', TRUE, NOW()),
    (3, 'tec.carlos', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Carlos Mendes (Técnico)', 'TECHNICIAN', 'TEC-001', TRUE, NOW()),
    (4, 'tec.ana', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', 'Ana Souza (Técnica)', 'TECHNICIAN', 'TEC-002', TRUE, NOW())
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 2. Inserção de Setores Iniciais
INSERT INTO sectors (id, code, name, description, active, created_at)
VALUES 
    (1, 'USIN-01', 'Usinagem Pesada', 'Setor de usinagem e tornos CNC', TRUE, NOW()),
    (2, 'UTIL-02', 'Casa de Máquinas e Utilidades', 'Compressores, bombas e geração de vapor', TRUE, NOW()),
    (3, 'ELET-03', 'Manutenção Elétrica', 'Painéis de distribuição e subestações', TRUE, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3. Inserção de Equipamentos de Referência
INSERT INTO equipments (id, asset_tag, name, model, serial_number, sector_id, active, created_at)
VALUES 
    (1, 'EQP-102', 'Torno CNC Romi', 'Centur 30D', 'SN-2021-9921', 1, TRUE, NOW()),
    (2, 'EQP-305', 'Compressor Schulz', 'SRP 3030', 'SN-2019-3312', 2, TRUE, NOW()),
    (3, 'EQP-401', 'Transformador 500kVA', 'TrafoTech T500', 'SN-2018-7711', 3, TRUE, NOW())
ON DUPLICATE KEY UPDATE name = VALUES(name);
