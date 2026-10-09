<template>
    <section class="notifications-page">
        <header class="notifications-page__header">
            <h1>Notifications</h1>
            <button v-if="state.unreadCount > 0" type="button" :disabled="markingAll" @click="toutLire">
                {{ markingAll ? 'Mise à jour…' : 'Tout marquer comme lu' }}
            </button>
        </header>
        <form class="filters" @submit.prevent="appliquerFiltres">
            <label class="filters__search">
                <span>Rechercher</span>
                <input v-model="search" type="search" placeholder="Titre, message ou activité…">
            </label>
            <label>
                <span>État</span>
                <select v-model="lecture" @change="appliquerFiltres">
                    <option value="TOUTES">Toutes</option>
                    <option value="NON_LUES">Non lues</option>
                    <option value="LUES">Lues</option>
                </select>
            </label>
            <label>
                <span>Priorité</span>
                <select v-model="priorite" @change="appliquerFiltres">
                    <option value="">Toutes</option>
                    <option value="CRITIQUE">Critique</option>
                    <option value="HAUTE">Haute</option>
                    <option value="NORMALE">Normale</option>
                    <option value="FAIBLE">Faible</option>
                </select>
            </label>
            <button type="submit">Rechercher</button>
        </form>
        <p v-if="loading">Chargement…</p>
        <p v-else-if="notifications.length === 0">Aucune notification ne correspond à ces critères.</p>
        <template v-else>
            <article v-for="item in notifications" :key="item.id" class="notification" :class="{ unread: !item.dateLecture }">
                <div class="notification__main">
                    <div class="notification__title">
                        <strong>{{ item.titre }}</strong>
                        <span v-if="item.prioriteLibelle" class="priority" :class="`priority--${item.prioriteCode?.toLowerCase()}`">
                            {{ item.prioriteLibelle }}
                        </span>
                    </div>
                    <p>{{ item.message }}</p>
                    <RouterLink v-if="item.idActivite" :to="`/activites/${item.idActivite}`">{{ item.activite || 'Voir l’activité' }}</RouterLink>
                    <small>{{ new Date(item.dateCreation).toLocaleString() }} · {{ item.dateLecture ? 'Lue' : 'Non lue' }}</small>
                </div>
                <button v-if="!item.dateLecture" type="button" @click="lire(item.id)">Marquer comme lue</button>
            </article>
            <nav v-if="totalPages > 1" class="pagination" aria-label="Pagination des notifications">
                <button type="button" :disabled="page === 0 || loading" @click="changerPage(page - 1)">Précédent</button>
                <span>Page {{ page + 1 }} sur {{ totalPages }}</span>
                <button type="button" :disabled="page + 1 >= totalPages || loading" @click="changerPage(page + 1)">Suivant</button>
            </nav>
        </template>
    </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import {
    chargerPageNotifications,
    actualiserNotifications,
    marquerNotificationLue,
    marquerToutesNotificationsLues,
    useNotifications,
    type NotificationItem,
    type NotificationFiltres,
} from '@/services/notification'

const state = useNotifications()
const notifications = ref<NotificationItem[]>([])
const loading = ref(true)
const markingAll = ref(false)
const page = ref(0)
const totalPages = ref(0)
const pageSize = 10
const search = ref('')
const lecture = ref<NotificationFiltres['lecture']>('TOUTES')
const priorite = ref('')

function filtresCourants(): NotificationFiltres {
    return { search: search.value, lecture: lecture.value, priorite: priorite.value }
}

async function charger() {
    loading.value = true
    const resultat = await chargerPageNotifications(page.value, pageSize, filtresCourants())
    if (resultat) {
        totalPages.value = resultat.totalPages
        if (resultat.totalPages > 0 && page.value >= resultat.totalPages) {
            page.value = resultat.totalPages - 1
            await charger()
            return
        }
        if (resultat.totalPages === 0) page.value = 0
        notifications.value = resultat.content
    }
    loading.value = false
}

onMounted(async () => {
    await actualiserNotifications()
    await charger()
})

async function changerPage(nouvellePage: number) {
    page.value = nouvellePage
    await charger()
}

async function appliquerFiltres() {
    page.value = 0
    await charger()
}

async function lire(id: number) {
    if (await marquerNotificationLue(id)) {
        await Promise.all([charger(), actualiserNotifications()])
    }
}

async function toutLire() {
    markingAll.value = true
    if (await marquerToutesNotificationsLues()) {
        notifications.value.forEach(item => { item.dateLecture = new Date().toISOString() })
        await charger()
    }
    markingAll.value = false
}
</script>

<style scoped>
.notifications-page { max-width: 900px; margin: 0 auto; }
.notifications-page__header { display:flex; align-items:center; justify-content:space-between; gap:1rem; }
.filters { display:flex; align-items:end; flex-wrap:wrap; gap:.75rem; margin:1rem 0 1.5rem; padding:1rem; background:#f8fafc; border:1px solid #dce3eb; border-radius:.75rem; }
.filters label { display:grid; gap:.3rem; color:#475467; font-size:.875rem; }
.filters input, .filters select { min-height:2.5rem; padding:.4rem .65rem; border:1px solid #cbd5e1; border-radius:.4rem; background:white; }
.filters__search { flex:1 1 16rem; }
.filters__search input { width:100%; }
.notification { display:flex; justify-content:space-between; gap:1rem; padding:1rem; margin:0.75rem 0; background:white; border:1px solid #dce3eb; border-radius:0.75rem; }
.notification.unread { border-left:4px solid #1769aa; background:#f2f8ff; }
.notification__title { display:flex; flex-wrap:wrap; align-items:center; gap:.5rem; }
.priority { padding:.15rem .5rem; border-radius:999px; background:#e2e8f0; color:#344054; font-size:.75rem; }
.priority--critique { background:#fee2e2; color:#b42318; }
.priority--haute { background:#ffedd5; color:#c2410c; }
.priority--normale { background:#dbeafe; color:#1d4ed8; }
.priority--faible { background:#dcfce7; color:#15803d; }
.notification p { margin:.4rem 0; }
.notification small { display:block; color:#667085; margin-top:.5rem; }
button { align-self:center; }
.pagination { display:flex; justify-content:center; align-items:center; gap:1rem; margin:1.5rem 0; }
</style>
