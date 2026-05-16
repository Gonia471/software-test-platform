<template>
  <div class="dashboard">
    <section class="hero">
      <div class="hero-main">
        <el-page-header content="仪表盘" class="page-header" />
        <h2 class="hero-title">测试平台总览</h2>
        <p class="hero-desc">
          在这里集中查看测试资源、执行状态和常用入口，作为平台首页展示更符合常见 B 端管理后台的结构。
        </p>
      </div>
    </section>

    <el-row :gutter="18" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--blue">
          <div class="stat-meta">
            <div class="stat-title">UI 用例数</div>
            <div class="stat-value">12</div>
          </div>
          <span class="stat-icon">📋</span>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--cyan">
          <div class="stat-meta">
            <div class="stat-title">API 接口数</div>
            <div class="stat-value">34</div>
          </div>
          <span class="stat-icon">🔗</span>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--amber">
          <div class="stat-meta">
            <div class="stat-title">最近执行</div>
            <div class="stat-value">2 次</div>
          </div>
          <span class="stat-icon">⚡</span>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-card--green">
          <div class="stat-meta">
            <div class="stat-title">失败率</div>
            <div class="stat-value">8%</div>
          </div>
          <span class="stat-icon">📉</span>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :span="8">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-head">
              <span>常用入口</span>
              <span class="panel-tip">业务导航</span>
            </div>
          </template>
          <div class="quick-links">
            <div class="quick-link">
              <span class="quick-link__title">组织管理</span>
              <p class="quick-link__desc">管理组织、成员和邀请码，控制测试协作范围。</p>
            </div>
            <div class="quick-link">
              <span class="quick-link__title">项目管理</span>
              <p class="quick-link__desc">进入项目后继续开展 API 测试与 UI 自动化测试。</p>
            </div>
            <div class="quick-link">
              <span class="quick-link__title">测试报告</span>
              <p class="quick-link__desc">集中查看最近执行记录、接口响应和 UI 自动化截图。</p>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-head">
              <span>待处理事项</span>
              <span class="panel-tip">平台任务</span>
            </div>
          </template>
          <div class="todo-list">
            <div class="todo-item">
              <strong>补充 API 测试环境变量</strong>
              <span>建议优先配置测试环境和通用请求头，便于后续接口验证。</span>
            </div>
            <div class="todo-item">
              <strong>完善 UI 自动化用例编排</strong>
              <span>将关键业务流程沉淀为可复用步骤，提高展示完整度。</span>
            </div>
            <div class="todo-item">
              <strong>查看最近测试报告</strong>
              <span>检查失败步骤与断言结果，作为系统演示时的结果说明依据。</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="panel-card" shadow="never">
          <template #header>
            <div class="panel-head">
              <span>最近动态</span>
              <span class="panel-tip">执行摘要</span>
            </div>
          </template>
          <div class="activity-list">
            <div class="activity-item">
              <span class="activity-item__dot activity-item__dot--success"></span>
              <div class="activity-item__content">
                <strong>UI 自动化回归完成</strong>
                <p>最近执行 2 次，其中 1 次全部通过，1 次存在失败步骤。</p>
              </div>
            </div>
            <div class="activity-item">
              <span class="activity-item__dot activity-item__dot--primary"></span>
              <div class="activity-item__content">
                <strong>接口集合已同步</strong>
                <p>当前已维护 34 个接口测试节点，可继续补充断言和前置脚本。</p>
              </div>
            </div>
            <div class="activity-item">
              <span class="activity-item__dot activity-item__dot--warning"></span>
              <div class="activity-item__content">
                <strong>失败率维持在 8%</strong>
                <p>建议优先排查失败用例对应的环境变量、接口返回和页面元素定位。</p>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  padding: 24px;
  border-radius: var(--border-radius);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 250, 255, 0.98) 100%);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: var(--card-shadow);
}

.hero-main {
  min-width: 0;
}

.page-header {
  padding: 0;
  margin-bottom: 12px;
}

.hero-title {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.2;
  color: var(--text-primary);
}

.hero-desc {
  max-width: 720px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.stats-row {
  margin: 0 !important;
}

.stat-card {
  text-align: left;
  border-radius: var(--border-radius);
  transition: var(--transition);
  position: relative;
  overflow: hidden;
  min-height: 142px;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--primary-gradient);
}

.stat-card--blue::before {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.stat-card--cyan::before {
  background: linear-gradient(135deg, #06b6d4 0%, #38bdf8 100%);
}

.stat-card--amber::before {
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
}

.stat-card--green::before {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
}

.stat-meta {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-title {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.stat-value {
  font-size: 34px;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 40px;
  opacity: 0.15;
}

.panel-card {
  height: 100%;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.panel-tip {
  color: var(--text-tertiary);
  font-size: 12px;
}

.quick-links,
.todo-list,
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.quick-link,
.todo-item,
.activity-item {
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.quick-link__title,
.todo-item strong,
.activity-item__content strong {
  display: block;
  margin-bottom: 6px;
  color: var(--text-primary);
  font-size: 15px;
}

.quick-link__desc,
.todo-item span,
.activity-item__content p {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.activity-item__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.activity-item__dot--success {
  background: var(--success-color);
}

.activity-item__dot--primary {
  background: var(--primary-color);
}

.activity-item__dot--warning {
  background: var(--warning-color);
}
</style>

