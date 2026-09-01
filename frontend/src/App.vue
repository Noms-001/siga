<template>
  <RouterView v-slot="{ Component }">
    <Transition name="page" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
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