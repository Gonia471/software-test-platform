import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const CASES_KEY = 'ui-test-cases-v1'
const CASE_SEQ_KEY = 'ui-test-cases-seq-v1'

const defaultTeams = [
  { id: 'team-core', name: '核心平台团队' },
  { id: 'team-teaching', name: '教学系统团队' },
]

const defaultModules = [
  { key: 'test', name: 'test' },
]

function loadCases() {
  try {
    const raw = localStorage.getItem(CASES_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
  } catch {
    return []
  }
}

function saveCases(cases) {
  localStorage.setItem(CASES_KEY, JSON.stringify(cases))
}

function loadNextSeq(existingCases) {
  try {
    const raw = localStorage.getItem(CASE_SEQ_KEY)
    if (raw) {
      const parsed = Number(raw)
      if (!Number.isNaN(parsed) && parsed > 0) return parsed
    }
  } catch {
    // ignore
  }
  const maxSeq = existingCases.reduce(
    (max, c) => (typeof c.seq === 'number' && c.seq > max ? c.seq : max),
    0,
  )
  const next = maxSeq + 1
  localStorage.setItem(CASE_SEQ_KEY, String(next))
  return next
}

function saveNextSeq(nextSeq) {
  localStorage.setItem(CASE_SEQ_KEY, String(nextSeq))
}

export const useUiTestStore = defineStore('uiTest', () => {
  const teams = ref(defaultTeams)
  const modules = ref(defaultModules)
  const cases = ref(loadCases())
  const nextSeq = ref(loadNextSeq(cases.value))

  // 为历史用例补齐缺失的 seq（仅首次加载时执行）
  if (cases.value.length) {
    let changed = false
    cases.value.forEach((c) => {
      if (typeof c.seq !== 'number') {
        c.seq = nextSeq.value
        nextSeq.value += 1
        changed = true
      }
    })
    if (changed) {
      saveNextSeq(nextSeq.value)
      saveCases(cases.value)
    }
  }

  const selectedTeamId = ref(teams.value[0]?.id || '')
  const selectedModuleKey = ref('all')

  const filteredCases = computed(() => {
    return cases.value.filter((c) => {
      if (selectedTeamId.value && c.teamId !== selectedTeamId.value) {
        return false
      }
      if (selectedModuleKey.value && selectedModuleKey.value !== 'all') {
        return c.moduleKey === selectedModuleKey.value
      }
      return true
    })
  })

  function setTeam(teamId) {
    selectedTeamId.value = teamId
  }

  function setModule(moduleKey) {
    selectedModuleKey.value = moduleKey
  }

  function createCase({ teamId, moduleKey, name, creator }) {
    const id = `case-${Date.now()}-${Math.floor(Math.random() * 1000)}`
    const now = new Date().toISOString()
    const newCase = {
      id,
      seq: nextSeq.value,
      teamId,
      moduleKey,
      name: name || '未命名用例',
      creator: creator || '未知',
      summary: '',
      updatedAt: now,
    }
    cases.value.unshift(newCase)
    nextSeq.value += 1
    saveNextSeq(nextSeq.value)
    saveCases(cases.value)
    return newCase
  }

  function updateCaseMeta(id, payload) {
    const idx = cases.value.findIndex((c) => c.id === id)
    if (idx === -1) return
    cases.value[idx] = {
      ...cases.value[idx],
      ...payload,
      updatedAt: new Date().toISOString(),
    }
    saveCases(cases.value)
  }

  function removeCase(id) {
    cases.value = cases.value.filter((c) => c.id !== id)
    saveCases(cases.value)
  }

  function getCaseById(id) {
    return cases.value.find((c) => c.id === id) || null
  }

  return {
    teams,
    modules,
    cases,
    selectedTeamId,
    selectedModuleKey,
    filteredCases,
    nextSeq,
    setTeam,
    setModule,
    createCase,
    updateCaseMeta,
    removeCase,
    getCaseById,
  }
})

