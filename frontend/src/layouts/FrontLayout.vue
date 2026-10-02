<template>
    <div class="app-layout">
        <Navbar @toggle-sidebar="toggleSidebar" />

        <div class="app-layout__body">
            <Sidebar :is-open="isSidebarOpen" :is-collapsed="isSidebarCollapsed" :notification-count="notificationCount"
                @close="closeSidebar" />

            <main class="main-content">
                <RouterView />
            </main>
        </div>

        <Footer app-version="1.0.0" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { RouterView } from 'vue-router'
import { Navbar, Sidebar, Footer } from '@/components/partial/frontoffice'

// --- State ---
const isSidebarOpen = ref(false)
const isSidebarCollapsed = ref(false)
const notificationCount = ref(7) // Simulé, à remplacer par un store

// --- Methods ---
const toggleSidebar = () => {
    if (window.innerWidth < 992) {
        isSidebarOpen.value = !isSidebarOpen.value
    } else {
        isSidebarCollapsed.value = !isSidebarCollapsed.value
        document.body.classList.toggle('sidebar-collapsed', isSidebarCollapsed.value)
    }
}

const closeSidebar = () => {
    isSidebarOpen.value = false
}

// --- Gestion du responsive ---
const handleResize = () => {
    if (window.innerWidth >= 992) {
        isSidebarOpen.value = false
    }
}

onMounted(() => {
    // Initialiser l'état de la sidebar sur desktop
    if (window.innerWidth >= 992) {
        isSidebarCollapsed.value = false
    }
    window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
    document.body.classList.remove('sidebar-collapsed')
})
</script>

<style scoped>
/* ============================================
   FRONT LAYOUT - Styles spécifiques
   ============================================ */

/* Le body a déjà un padding-top: var(--dts-navbar-h) dans le style global,
   donc on n'ajoute rien ici */

.app-layout {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

.app-layout__body {
    display: flex;
    flex: 1;
}

.main-content {
    margin-left: var(--dts-sidebar-w);
    padding: 1.5rem 2rem;
    flex: 1;
    min-width: 0;
    transition: margin-left 0.3s ease;
}

/* Sidebar réduite */
body.sidebar-collapsed .main-content {
    margin-left: var(--dts-sidebar-w-sm);
}

/* Footer */
.footer {
    margin-left: var(--dts-sidebar-w);
    transition: margin-left 0.3s ease;
}

body.sidebar-collapsed .footer {
    margin-left: var(--dts-sidebar-w-sm);
}

/* Responsive */
@media (max-width: 991.98px) {
    .main-content {
        margin-left: 0;
    }

    .footer,
    body.sidebar-collapsed .footer {
        margin-left: 0;
    }

    body.sidebar-collapsed .main-content {
        margin-left: 0;
    }
}

@media (max-width: 575.98px) {
    .main-content {
        padding: 1rem;
    }
}
</style>