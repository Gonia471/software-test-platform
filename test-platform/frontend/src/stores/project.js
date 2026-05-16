import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserProjects, createProject } from '../api/project'
import { useOrgStore } from './org'

export const useProjectStore = defineStore('project', () => {
  const projects = ref([])
  const currentProjectId = ref(localStorage.getItem('currentProjectId') || null)
  const currentProject = computed(() =>
    projects.value.find(p => p.id === Number(currentProjectId.value))
  )

  async function fetchProjects() {
    const orgStore = useOrgStore()
    if (!orgStore.currentOrganizationId) {
      projects.value = []
      return
    }
    const res = await getUserProjects()
    projects.value = res.data || []
  }

  async function fetchProjectsByOrg(orgId) {
    const res = await getUserProjects()
    projects.value = res.data || []
  }

  function setCurrentProject(id) {
    currentProjectId.value = id
    localStorage.setItem('currentProjectId', id)
  }

  async function createProj(data) {
    const res = await createProject(data)
    await fetchProjects()
    return res
  }

  return {
    projects,
    currentProjectId,
    currentProject,
    fetchProjects,
    fetchProjectsByOrg,
    setCurrentProject,
    createProj
  }
})