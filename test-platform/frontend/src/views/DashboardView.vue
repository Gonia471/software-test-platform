<template>
  <div class="dashboard">
    <el-page-header content="仪表盘" class="page-header" />
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">UI 用例数</div>
          <div class="stat-value">12</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">API 接口数</div>
          <div class="stat-value">34</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">最近执行</div>
          <div class="stat-value">2 次</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-title">失败率</div>
          <div class="stat-value">8%</div>
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
  gap: 16px;
}

.page-header {
  padding: 0;
}

.stat-card {
  text-align: left;
}

.stat-title {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}
</style>

