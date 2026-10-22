<template>
    <div class="backoffice-layout">
        <Navbar @toggle-sidebar="toggleSidebar" />

        <div class="backoffice-layout__body">
            <Sidebar :is-open="isSidebarOpen" :is-collapsed="isSidebarCollapsed" @close="closeSidebar" />

            <main class="backoffice-layout__content">
                <RouterView />
            </main>
        </div>

        <Footer app-version="1.0.0" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterView } from 'vue-router'
import { Navbar, Sidebar, Footer } from '@/components/partial/backoffice'

const isSidebarOpen = ref(false)
const isSidebarCollapsed = ref(false)

const toggleSidebar = () => {
    if (window.innerWidth < 992) {
        isSidebarOpen.value = !isSidebarOpen.value
    } else {
        isSidebarCollapsed.value = !isSidebarCollapsed.value
        document.body.classList.toggle('backoffice-collapsed', isSidebarCollapsed.value)
    }
}

const closeSidebar = () => {
    isSidebarOpen.value = false
}

const handleResize = () => {
    if (window.innerWidth >= 992) {
        isSidebarOpen.value = false
    }
}

onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    document.body.classList.remove('backoffice-collapsed')
})
</script>

<style scoped>

.backoffice-layout {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    box-sizing: border-box;
    background: #f5f7fa;
}

.backoffice-layout__content div {
    margin: 10px;
}

.backoffice-layout__body {
    display: flex;
    flex: 1 0 auto;
    min-height: 0;
}

.backoffice-layout__content {
    flex: 1 1 auto;
    min-width: 0;
    min-height: 0;
    margin-left: var(--dts-sidebar-w);
    padding: 0;
    transition: margin-left 0.3s ease;
}

body.backoffice-collapsed .backoffice-layout__content {
    margin-left: var(--dts-sidebar-w-sm);
}

.back-footer {
    flex-shrink: 0;
    margin-left: var(--dts-sidebar-w);
    transition: margin-left 0.3s ease;
}

@media (max-width: 991.98px) {
    .backoffice-layout__content,
    .back-footer {
        margin-left: 0;
    }

    body.backoffice-collapsed .backoffice-layout__content,
    body.backoffice-collapsed .back-footer {
        margin-left: 0;
    }
}

@media (max-width: 575.98px) {
    .backoffice-layout {
        padding: 5px;
    }

    .backoffice-layout__content {
        padding: 0;
    }
}
</style>