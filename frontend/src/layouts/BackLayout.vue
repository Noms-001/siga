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

// --- State ---
const isSidebarOpen = ref(false)
const isSidebarCollapsed = ref(false)

// --- Methods ---
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

// --- Responsive ---
const handleResize = () => {
    if (window.innerWidth >= 992) {
        isSidebarOpen.value = false
    }
}

onMounted(() => {
    window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    document.body.classList.remove('backoffice-collapsed')
})
</script>

<style scoped>
.backoffice-layout {
    flex: 1 0 auto;
    display: flex;
    flex-direction: column;
    min-height: 0;
}

.backoffice-layout__body {
    display: flex;
    flex: 1 0 auto;
}

.backoffice-layout__content {
    margin-left: var(--dts-sidebar-w);
    padding: 1.5rem 2rem;
    flex: 1 0 auto;
    transition: margin-left 0.3s ease;
    min-height: calc(100vh - var(--dts-navbar-h) - 56px);
}

body.backoffice-collapsed .backoffice-layout__content {
    margin-left: var(--dts-sidebar-w-sm);
}

body.backoffice-collapsed .back-footer {
    margin-left: var(--dts-sidebar-w-sm);
}

.back-footer {
    margin-left: var(--dts-sidebar-w);
    transition: margin-left 0.3s ease;
}

@media (max-width: 991.98px) {
    .backoffice-layout__content {
        margin-left: 0;
    }

    .back-footer {
        margin-left: 0;
    }

    body.backoffice-collapsed .backoffice-layout__content {
        margin-left: 0;
    }

    body.backoffice-collapsed .back-footer {
        margin-left: 0;
    }
}

@media (max-width: 575.98px) {
    .backoffice-layout__content {
        padding: 1rem;
    }
}
</style>