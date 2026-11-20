<template>
  <RouterView v-slot="{ Component }">
    <Transition name="page" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>

  <BaseToast ref="toastRef" />
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'

import { BaseToast } from '@/components/base'
import type { ToastInstance } from '@/components/base/BaseToast'
import { useSessionWatchdog } from '@/composables/useSessionWatchdog'
import { useToast } from '@/composables/useToast'

const toastRef = ref<ToastInstance | null>(null)
const toast = useToast()

onMounted(() => {
  toast.setToastInstance(toastRef.value)
})

/*
 * Déconnexion automatique après une période d'inactivité, avec
 * redirection immédiate vers la page de connexion. Monté ici, à la racine,
 * pour être actif sur toutes les pages y compris celles hors layout.
 */
useSessionWatchdog()
</script>

<style>
/* ============================================
   STYLES DE LAYOUT GLOBAUX
   ============================================ */

body {
  padding-top: 64px;
}

/* Footer */
.footer {
  margin-left: 260px;
  transition: margin-left 0.3s ease;
}

/* Sidebar réduite */
body.sidebar-collapsed .footer {
  margin-left: 72px;
}

/* ============================================
   TRANSITION ENTRE LES PAGES
   ============================================ */

.page-enter-active,
.page-leave-active {
  transition: opacity 0.2s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
}

/* ============================================
   RESPONSIVE
   ============================================ */

@media (max-width: 991.98px) {
  .footer {
    margin-left: 0;
  }

  body.sidebar-collapsed .footer {
    margin-left: 0;
  }
}
</style>