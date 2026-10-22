<template>
    <div class="app-layout" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
        <Sidebar
            :is-open="isSidebarOpen"
            :notification-count="notificationCount"
            @close="closeSidebar"
            @collapse="onSidebarCollapse"
        />

        <Navbar
            :collapsed="isSidebarCollapsed"
            @toggle-sidebar="toggleSidebar"
            @search="onSearch"
        />

        <main class="main-content">
            <RouterView />
        </main>

        <Footer app-version="1.0.0" />
    </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { Navbar, Sidebar, Footer } from '@/components/partial/frontoffice'
import { useNotifications, actualiserNotifications } from '@/services/notification'

const route = useRoute()
const router = useRouter()
const isSidebarOpen = ref(false)
const isSidebarCollapsed = ref(false)   // ← état partagé
const { unreadCount: notificationCount } = useNotifications()

const toggleSidebar = () => { isSidebarOpen.value = !isSidebarOpen.value }
const closeSidebar  = () => { isSidebarOpen.value = false }

// ← écoute l'événement `collapse` émis par la Sidebar
const onSidebarCollapse = (collapsed: boolean) => {
    isSidebarCollapsed.value = collapsed
}

const onSearch = (q: string) => {
    const query = q.trim()
    if (!query) return
    router.push({ path: '/activites', query: { q: query } })
}

watch(() => route.path, () => { isSidebarOpen.value = false })

const handleResize = () => {
    if (window.innerWidth >= 992) isSidebarOpen.value = false
}

onMounted(() => {
    window.addEventListener('resize', handleResize)
    void actualiserNotifications()
})

onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.app-layout {
    --sidebar-width: 248px;
    --topnav-height: 58px;
    min-height: 100vh;
    background:
        radial-gradient(circle at 12% 8%, rgba(26, 42, 108, .07) 0, transparent 38%),
        radial-gradient(circle at 92% 94%, rgba(212, 175, 55, .10) 0, transparent 36%),
        linear-gradient(135deg, #f5f7fa 0%, #e9edf5 100%);
    background-attachment: fixed;
    font-family: "DM Sans", system-ui, sans-serif;
}

/* État replié : la variable pilote tout le layout */
.app-layout.sidebar-collapsed {
    --sidebar-width: 76px;
}

.main-content {
    margin-left: var(--sidebar-width);
    min-height: calc(100vh - var(--topnav-height));
    padding-bottom: 8px;
    transition: margin-left .25s cubic-bezier(.4, 0, .2, 1);
    padding: 20px;
}

.main-content div {
    border-radius: 10px;
}

/* Footer aligné sous la topbar */
.app-layout :deep(.footer) {
    margin-left: var(--sidebar-width);
    transition: margin-left .25s cubic-bezier(.4, 0, .2, 1);
}

/* Mobile : la sidebar est un drawer, donc aucune marge */
@media (max-width: 991.98px) {
    .app-layout,
    .app-layout.sidebar-collapsed {
        --sidebar-width: 0px;
    }
    .main-content {
        margin-left: 0;
    }
    .app-layout :deep(.footer) {
        margin-left: 0;
    }
}
</style>