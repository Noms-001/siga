<template>
    <section class="notifications-page">
        <h1>Notifications</h1>
        <p v-if="loading">Chargement…</p>
        <p v-else-if="notifications.length === 0">Aucune notification.</p>
        <article v-for="item in notifications" :key="item.id" class="notification" :class="{ unread: !item.dateLecture }">
            <div class="notification__main">
                <strong>{{ item.titre }}</strong>
                <p>{{ item.message }}</p>
                <RouterLink v-if="item.idActivite" :to="`/activites/${item.idActivite}`">{{ item.activite || 'Voir l’activité' }}</RouterLink>
                <small>{{ new Date(item.dateCreation).toLocaleString() }} · {{ item.dateLecture ? 'Lue' : 'Non lue' }}</small>
            </div>
            <button v-if="!item.dateLecture" type="button" @click="lire(item.id)">Marquer comme lue</button>
        </article>
    </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useNotifications, actualiserNotifications, marquerNotificationLue } from '@/services/notification'
const state = useNotifications()
const notifications = computed(() => state.notifications)
const loading = ref(true)
onMounted(async () => { await actualiserNotifications(); loading.value = false })
async function lire(id: number) { await marquerNotificationLue(id) }
</script>

<style scoped>
.notifications-page { max-width: 900px; margin: 0 auto; }
.notification { display:flex; justify-content:space-between; gap:1rem; padding:1rem; margin:0.75rem 0; background:white; border:1px solid #dce3eb; border-radius:0.75rem; }
.notification.unread { border-left:4px solid #1769aa; background:#f2f8ff; }
.notification p { margin:.4rem 0; }
.notification small { display:block; color:#667085; margin-top:.5rem; }
button { align-self:center; }
</style>
