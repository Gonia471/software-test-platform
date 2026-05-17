<template>
  <div class="page">
    <section class="manual-hero">
      <div class="manual-hero__main">
        <div class="page-header">
          <h2>用户手册</h2>
        </div>
        <p class="hero-desc">
          本手册面向 ET 软件测试平台的使用人员，帮助快速了解平台模块、典型操作流程与常见问题处理方式。
        </p>
        <div class="hero-tags">
          <el-tag effect="plain">组织协作</el-tag>
          <el-tag effect="plain">项目合集</el-tag>
          <el-tag effect="plain">API 测试</el-tag>
          <el-tag effect="plain">UI 测试</el-tag>
          <el-tag effect="plain">测试报告</el-tag>
        </div>
      </div>
      <div class="manual-hero__side">
        <div class="hero-stat">
          <span class="hero-stat__label">文档范围</span>
          <strong class="hero-stat__value">平台全流程说明</strong>
        </div>
        <div class="hero-stat">
          <span class="hero-stat__label">适用对象</span>
          <strong class="hero-stat__value">管理员 / 测试人员</strong>
        </div>
      </div>
    </section>

    <section class="manual-grid">
      <el-card class="manual-card manual-card--toc" shadow="never">
        <template #header>
          <div class="card-header">
            <span>快速目录</span>
            <span class="card-tip">建议按顺序阅读</span>
          </div>
        </template>
        <div class="toc-list">
          <div v-for="item in tocItems" :key="item.key" class="toc-item">
            <div class="toc-item__index">{{ item.index }}</div>
            <div class="toc-item__content">
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="manual-card manual-card--summary" shadow="never">
        <template #header>
          <div class="card-header">
            <span>平台概览</span>
            <span class="card-tip">核心能力</span>
          </div>
        </template>
        <div class="summary-list">
          <div v-for="item in summaryItems" :key="item.label" class="summary-item">
            <span class="summary-item__label">{{ item.label }}</span>
            <strong class="summary-item__value">{{ item.value }}</strong>
            <p class="summary-item__desc">{{ item.desc }}</p>
          </div>
        </div>
      </el-card>
    </section>

    <el-card class="manual-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>一、平台定位与角色说明</span>
          <span class="card-tip">基础认知</span>
        </div>
      </template>
      <div class="manual-section">
        <p>
          ET 软件测试平台用于统一管理测试组织、测试用例、测试合集、自动化执行与测试报告，适合面向团队协作的 B 端测试场景。
          平台支持 API 测试与 UI 测试两类核心能力，并通过组织机制实现数据隔离与成员协作。
        </p>
        <div class="bullet-list">
          <div class="bullet-item">空间管理员：负责组织治理、成员维护、权限分配与跨组织管理。</div>
          <div class="bullet-item">组织管理员：负责当前组织内资源建设、合集编排与执行管理。</div>
          <div class="bullet-item">测试人员：负责维护 API / UI 用例、执行测试并分析测试报告。</div>
        </div>
      </div>
    </el-card>

    <el-card class="manual-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>二、首次使用流程</span>
          <span class="card-tip">推荐上手路径</span>
        </div>
      </template>
      <div class="manual-section">
        <div class="step-list">
          <div v-for="item in gettingStartedSteps" :key="item.step" class="step-item">
            <div class="step-item__no">{{ item.step }}</div>
            <div class="step-item__body">
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="manual-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>三、核心模块说明</span>
          <span class="card-tip">常用功能说明</span>
        </div>
      </template>
      <div class="module-grid">
        <div v-for="item in moduleItems" :key="item.title" class="module-item">
          <div class="module-item__header">
            <el-icon><component :is="item.icon" /></el-icon>
            <strong>{{ item.title }}</strong>
          </div>
          <p class="module-item__desc">{{ item.desc }}</p>
          <div class="bullet-list compact">
            <div v-for="point in item.points" :key="point" class="bullet-item">{{ point }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="manual-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>四、推荐操作规范</span>
          <span class="card-tip">团队协作建议</span>
        </div>
      </template>
      <div class="manual-section">
        <div class="standard-grid">
          <div v-for="item in standards" :key="item.title" class="standard-item">
            <strong>{{ item.title }}</strong>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="manual-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>五、常见问题</span>
          <span class="card-tip">FAQ</span>
        </div>
      </template>
      <div class="faq-list">
        <div v-for="item in faqItems" :key="item.q" class="faq-item">
          <strong class="faq-item__q">Q：{{ item.q }}</strong>
          <p class="faq-item__a">A：{{ item.a }}</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import {
  Connection,
  Document,
  FolderOpened,
  Histogram,
  Monitor,
  OfficeBuilding,
} from '@element-plus/icons-vue'

const tocItems = [
  {
    key: 'intro',
    index: '01',
    title: '平台定位',
    desc: '了解平台面向的业务场景、角色划分与主要能力。',
  },
  {
    key: 'start',
    index: '02',
    title: '首次使用',
    desc: '按照推荐路径完成组织切换、资源准备与首轮执行。',
  },
  {
    key: 'module',
    index: '03',
    title: '模块说明',
    desc: '快速掌握组织管理、项目合集、API、UI、报告等功能。',
  },
  {
    key: 'standard',
    index: '04',
    title: '使用规范',
    desc: '统一命名、分类、执行与分析习惯，降低协作成本。',
  },
  {
    key: 'faq',
    index: '05',
    title: '常见问题',
    desc: '查看典型问题及处理建议，提升问题排查效率。',
  },
]

const summaryItems = [
  {
    label: '组织隔离',
    value: '按组织工作',
    desc: '顶部切换组织后，项目、API、UI 与首页统计都以当前组织为准。',
  },
  {
    label: '测试类型',
    value: 'API + UI',
    desc: '既支持接口级自动化，也支持页面级自动化编排与执行。',
  },
  {
    label: '执行方式',
    value: '手动 + 定时',
    desc: '支持手动触发合集执行，也支持通过定时任务形成持续回归。',
  },
]

const gettingStartedSteps = [
  {
    step: '1',
    title: '进入平台并确认当前组织',
    desc: '登录后优先查看顶部当前组织，后续看到的数据、用例与合集都会受当前组织影响。',
  },
  {
    step: '2',
    title: '准备基础测试资源',
    desc: '先在 API 测试页沉淀接口目录与接口用例，在 UI 测试页创建并编排 UI 用例。',
  },
  {
    step: '3',
    title: '创建项目合集',
    desc: '在项目管理中创建 API 或 UI 类型合集，从当前组织现有用例中选择加入合集并调整执行顺序。',
  },
  {
    step: '4',
    title: '配置执行参数',
    desc: '可根据场景配置循环次数、定时任务等参数，适合冒烟回归、日常巡检和定时构建。',
  },
  {
    step: '5',
    title: '执行并查看报告',
    desc: '执行后在测试报告中查看 API / UI / 合集的执行结果、失败信息与趋势表现。',
  },
]

const moduleItems = [
  {
    title: '组织管理',
    icon: OfficeBuilding,
    desc: '负责组织创建、成员管理、权限治理与协作边界控制。',
    points: [
      '支持创建组织、查看组织成员与组织项目。',
      '支持邀请码和成员批量操作等协作管理场景。',
      '切换顶部组织后，各业务模块会同步切换当前组织上下文。',
    ],
  },
  {
    title: '项目管理',
    icon: FolderOpened,
    desc: '用于管理 API / UI 合集，承担批量执行与持续回归入口。',
    points: [
      '创建项目即创建合集，可选择 API 或 UI 类型。',
      '合集内的用例来源于当前组织现有用例，并支持顺序调整。',
      '支持定时任务、循环次数等执行配置。',
    ],
  },
  {
    title: 'API 测试',
    icon: Connection,
    desc: '用于维护接口目录、接口用例、断言与执行记录。',
    points: [
      '支持接口分组、请求配置、断言维护与执行。',
      '接口树与数据展示严格按当前组织隔离。',
      '执行结果会同步进入测试报告模块。',
    ],
  },
  {
    title: 'UI 测试',
    icon: Monitor,
    desc: '用于管理 UI 自动化用例、步骤编排、定位器与执行。',
    points: [
      '支持空白新建用例，由用户自行拖拽步骤进行编排。',
      '支持浏览器操作、页面元素定位、XPath 优化等能力。',
      'UI 用例同样严格遵循当前组织隔离规则。',
    ],
  },
  {
    title: '测试报告',
    icon: Histogram,
    desc: '集中展示 API、UI 与合集执行后的结果数据与问题线索。',
    points: [
      '支持查看最近执行记录、成功失败状态与执行详情。',
      '失败记录可用于快速定位断言异常、环境问题或脚本问题。',
      '首页仪表盘中的统计数据也来源于这些真实执行记录。',
    ],
  },
  {
    title: '用户手册',
    icon: Document,
    desc: '用于沉淀平台说明、操作步骤、演示介绍与常见问题。',
    points: [
      '适合作为平台培训、内部推广与新成员上手材料。',
      '建议后续补充关键页面截图与案例说明。',
      '可作为测试团队内部培训材料的基础文档。',
    ],
  },
]

const standards = [
  {
    title: '命名规范',
    desc: '建议统一 API 用例名称、UI 用例标题、合集名称的业务含义，避免出现大量无语义命名。',
  },
  {
    title: '分类规范',
    desc: 'UI 用例建议按模块或业务域建立分类；API 目录建议按系统、服务或业务链路组织。',
  },
  {
    title: '执行规范',
    desc: '冒烟用例建议纳入固定合集并配置定时任务，回归类用例则按版本或迭代统一执行。',
  },
  {
    title: '报告复盘',
    desc: '发现失败后应先区分脚本问题、断言问题、环境问题与业务缺陷，再进行后续处理。',
  },
]

const faqItems = [
  {
    q: '为什么切换组织后，项目或用例列表发生变化？',
    a: '因为平台以顶部当前组织为统一工作上下文，不同组织之间的数据默认隔离展示。',
  },
  {
    q: '为什么创建合集时看不到某些用例？',
    a: '合集只允许选择当前组织下已经存在的对应类型用例，请先确认顶部组织是否正确，再确认用例是否已创建。',
  },
  {
    q: '为什么没有权限修改某个用例？',
    a: '该用例可能不属于当前组织，或当前账号没有对应写权限。请联系组织管理员确认成员权限。',
  },
  {
    q: '为什么首页仪表盘数据与当前组织相关？',
    a: '首页仪表盘按当前组织聚合真实数据库数据，因此切换组织后，首页的资源与执行统计会同步刷新。',
  },
  {
    q: '执行失败后应该优先看哪里？',
    a: '建议优先查看测试报告中的失败记录、状态说明和报错信息，再结合接口断言或 UI 步骤逐项排查。',
  },
]
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
  min-width: 240px;
  display: grid;
  gap: 14px;
  align-content: start;
}

.hero-desc {
  max-width: 760px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.hero-tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  min-height: 108px;
  padding: 18px 20px;
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
  border-radius: 18px;
}

.manual-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.9fr) minmax(0, 1.1fr);
  gap: 18px;
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

.toc-list,
.faq-list,
.step-list,
.bullet-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.toc-item,
.faq-item,
.step-item,
.standard-item {
  border: 1px solid rgba(226, 232, 240, 0.95);
  background: #f8fbff;
  border-radius: 16px;
}

.toc-item {
  display: flex;
  gap: 14px;
  padding: 16px;
}

.toc-item__index {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(59, 130, 246, 0.1);
  color: #2563eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.toc-item__content strong,
.step-item__body strong,
.module-item__header strong,
.standard-item strong,
.faq-item__q {
  color: var(--text-primary);
  font-size: 15px;
}

.toc-item__content p,
.step-item__body p,
.module-item__desc,
.standard-item p,
.faq-item__a,
.summary-item__desc,
.bullet-item,
.manual-section p {
  margin: 6px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.8;
}

.summary-list {
  display: grid;
  gap: 12px;
}

.summary-item {
  padding: 16px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.summary-item__label {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
}

.summary-item__value {
  display: block;
  margin-top: 6px;
  font-size: 20px;
  color: var(--text-primary);
}

.manual-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bullet-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.95);
  margin: 0;
}

.bullet-list.compact .bullet-item {
  padding: 10px 12px;
}

.step-item {
  display: flex;
  gap: 14px;
  padding: 16px;
}

.step-item__no {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  background: linear-gradient(135deg, #2563eb 0%, #60a5fa 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.module-item {
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.95);
}

.module-item__header {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #2563eb;
}

.standard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.standard-item {
  padding: 18px;
}

.faq-item {
  padding: 18px;
}

@media (max-width: 1100px) {
  .manual-hero {
    flex-direction: column;
  }

  .manual-hero__side {
    min-width: 0;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .manual-grid,
  .module-grid,
  .standard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .manual-hero__side {
    grid-template-columns: 1fr;
  }
}
</style>
