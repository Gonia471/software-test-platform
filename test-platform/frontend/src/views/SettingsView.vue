<template>
  <div class="page">
    <section class="manual-hero">
      <div class="manual-hero__main">
        <div class="page-header">
          <h2>用户手册</h2>
        </div>
        <p class="hero-desc">
          这里用于展示平台使用说明、模块介绍和操作流程，适合后续补充论文演示截图与系统说明文字。
        </p>
      </div>
      <div class="manual-hero__side">
        <div class="hero-stat">
          <span class="hero-stat__label">当前状态</span>
          <strong class="hero-stat__value">持续完善中</strong>
        </div>
      </div>
    </section>

    <el-card class="manual-card">
      <template #header>
        <div class="card-header">
          <span>ET 软件测试平台使用指南</span>
          <span class="card-tip">文档中心</span>
        </div>
      </template>
      <div class="manual-content">
        <el-empty description="用户手册正在编写中，敬请期待...">
          <template #image>
            <el-icon :size="60" color="#909399"><Document /></el-icon>
          </template>
        </el-empty>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { Document } from '@element-plus/icons-vue'
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.manual-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 24px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.manual-hero__main {
  min-width: 0;
}

.manual-hero__side {
  min-width: 220px;
}

.hero-desc {
  max-width: 760px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 16px;
  background: var(--primary-soft-gradient);
  border: 1px solid rgba(59, 130, 246, 0.12);
}

.hero-stat__label {
  font-size: 12px;
  color: var(--text-secondary);
}

.hero-stat__value {
  font-size: 20px;
  color: var(--text-primary);
}

.page-header {
  margin-bottom: 8px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.manual-card {
  max-width: 800px;
  border-radius: 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-tip {
  font-size: 12px;
  color: var(--text-tertiary);
}

.manual-content {
  padding: 56px 24px;
  text-align: center;
}
</style>
