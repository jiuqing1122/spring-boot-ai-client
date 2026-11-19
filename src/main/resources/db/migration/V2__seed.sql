-- 种子账号：admin / admin123，demo / demo1234（仅存 BCrypt 哈希，禁止明文）
INSERT INTO sys_user (username, password_hash, nickname, role) VALUES
    ('admin', '$2a$10$SsSgXHG409FXQ5nfw0T/Bem.KE4.lBQ6zt8o075neSgGeSNZmzQB.', '管理员', 'ADMIN'),
    ('demo',  '$2a$10$2BRvfHL3kRX/7qBSPI9hLeXEqdi5ujqkZtsA4TqiZfDnjhex7i24m', '演示用户', 'USER');