-- ============================================================
-- 初始数据脚本（非破坏性）
-- 
-- 只插入不存在的初始数据，不会覆盖已有数据
-- ============================================================

-- 1. 开发者模式测试用户（如果不存在）
INSERT INTO USERS (USERNAME, PHONE, CREATED_AT, IS_DEV_MODE) 
SELECT 'test', '13800138000', CURRENT_TIMESTAMP, TRUE
WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE USERNAME = 'test');

-- 2. 添加更多测试用户（如果需要）
INSERT INTO USERS (USERNAME, PHONE, CREATED_AT) 
SELECT 'admin', '13900139000', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM USERS WHERE USERNAME = 'admin');

-- 3. 默认 UI 执行实例（由 ExecutionInstanceService 自动初始化，此处不再重复插入以防编码冲突）
-- INSERT INTO UI_EXECUTION_INSTANCE (NAME, TYPE, ENABLED, CONFIG_JSON, CREATED_AT, UPDATED_AT)
-- SELECT '本地浏览器', 'LOCAL', TRUE, '{"browser":"chrome","headlessSupported":true}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
-- WHERE NOT EXISTS (SELECT 1 FROM UI_EXECUTION_INSTANCE WHERE NAME = '本地浏览器');