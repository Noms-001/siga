<script setup lang="ts">

import { onMounted, ref } from 'vue'
import {
    BaseButton,
    BaseCard,
    BaseInput,
    BaseSelect,
    BaseModal,
    BaseTable
} from '@/components/base'

// State pour BaseButton
const isLoading = ref(false)

// State pour BaseInput
const inputText = ref('')
const inputEmail = ref('')
const inputPassword = ref('')
const inputError = ref('')

// State pour BaseSelect
const selectedCity = ref('')
const selectedStatus = ref('')

const cities = [
    { id: 1, name: 'Paris' },
    { id: 2, name: 'Lyon' },
    { id: 3, name: 'Marseille' },
    { id: 4, name: 'Toulouse' }
]

const statuses = [
    { value: 'active', label: 'Actif' },
    { value: 'inactive', label: 'Inactif' },
    { value: 'pending', label: 'En attente' }
]

// State pour modales
const showSimpleModal = ref(false)
const showFormModal = ref(false)
const showConfirmModal = ref(false)
const showTableModal = ref(false)

// Form data
const formData = ref({
    name: '',
    email: '',
    city: ''
})

const multipleFiles = ref([])

const oui = ref(true)

const users = [
    { id: 1, name: 'Jean Dupont', email: 'jean@example.com', role: 'Admin', status: 'active' },
    { id: 2, name: 'Marie Martin', email: 'marie@example.com', role: 'User', status: 'inactive' },
    { id: 3, name: 'Pierre Bernard', email: 'pierre@example.com', role: 'User' },
    { id: 4, name: 'Sophie Rousseau', email: 'sophie@example.com', role: 'Moderator' }
]

const profileImage = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=800'

const blogImage = 'https://images.unsplash.com/photo-1552664730-d307ca884978?w=1200'

const productImage = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=1200'

const handleCardClick = (label: string) => {
    alert(`Carte cliquée : ${label}`)
}

const handleActionClick = (label: string) => {
    alert(`Action sélectionnée : ${label}`)
}

// Methods
const handleButtonClick = () => {
    isLoading.value = true
    setTimeout(() => {
        isLoading.value = false
        alert('Bouton cliqué !')
    }, 2000)
}

const validateInput = () => {
    if (inputText.value.length < 3) {
        inputError.value = 'Le texte doit contenir au moins 3 caractères'
    } else {
        inputError.value = ''
        alert('Validation réussie : ' + inputText.value)
    }
}

const submitForm = () => {
    console.log('Formulaire soumis :', formData.value)
    showFormModal.value = false
    formData.value = { name: '', email: '', city: '' }
}

const confirmAction = () => {
    console.log('Action confirmée')
    showConfirmModal.value = false
}

const table = ref([])
</script>

<template>
        <!-- Section BaseButton -->
        <section class="section">
            <h2>BaseButton</h2>
            <div class="component-showcase">
                <div class="demo-block">
                    <h3>Variantes</h3>
                    <div class="demo-content">
                        <BaseButton label="Primaire" variant="primary" />
                        <BaseButton label="Secondaire" variant="secondary" />
                        <BaseButton label="Succès" variant="success" />
                        <BaseButton label="Danger" variant="danger" />
                    </div>
                </div>

                <div class="demo-block">
                    <h3>Tailles</h3>
                    <div class="demo-content">
                        <BaseButton label="Small" size="sm" />
                        <BaseButton label="Medium" size="md" />
                        <BaseButton label="Large" size="lg" />
                    </div>
                </div>

                <div class="demo-block">
                    <h3>Avec icône et loading</h3>
                    <div class="demo-content">
                        <BaseButton label="Avec icône" icon="bi bi-star-fill" customClass="d-inline-flex" />
                        <BaseButton label="Enregistrer" variant="success" :loading="isLoading"
                            @click="handleButtonClick" />
                    </div>
                </div>
            </div>
        </section>

        <!-- Section BaseCard -->
        <section class="section">
            <h2>BaseCard</h2>
            <div class="component-showcase">
                <div class="demo-block">
                    <h3>Stat Cards</h3>
                    <div class="demo-content cards-row">
                        <BaseCard value="150" />
                        <BaseCard label="Utilisateurs" value="150" custom-class="col-md-7" />
                        <BaseCard label="Tickets ouverts" value="42" icon="bi bi-ticket-fill" trend="+12%"
                            trendType="positive" />
                        <BaseCard label="Retours" value="-5%" trend="-5%" trendType="negative"
                            :badges="[{ label: 'Alert', variant: 'danger' }]" />
                    </div>
                </div>

                <div class="demo-block">
                    <h3>Content Cards</h3>
                    <div class="demo-content cards-row">
                        <BaseCard :image="productImage" title="Casque sans fil" subtitle="Audio premium"
                            description="Confortable, batterie longue durée et réduction de bruit active." price="79€"
                            oldPrice="99€"
                            :badges="[{ label: 'Promo', variant: 'danger' }, { label: 'Bestseller', variant: 'success' }]"
                            actionLabel="Ajouter au panier" actionVariant="primary" />

                        <BaseCard :image="profileImage" title="Sophie Rousseau" subtitle="Product Owner"
                            description="Experte UX & produit, disponible pour de nouveaux projets."
                            :badges="[{ label: 'New', variant: 'primary' }]" actionLabel="Contacter"
                            actionVariant="success" />
                    </div>
                </div>
            </div>
            <div class="component-showcase mt-4">

                <div class="demo-block">
                    <h3>Personnalisation avec slots</h3>
                    <div class="demo-content">
                        <BaseCard :image="blogImage" customClass="col-md-4" @click="handleCardClick('Blog')">
                            <template #title>
                                <h3 class="custom-title">Nouveauté Produit</h3>
                            </template>

                            <template #subtitle>
                                <span class="custom-subtitle">Dernière mise à jour</span>
                            </template>

                            <template #description>
                                <p>Découvrez les nouvelles fonctionnalités de notre service avec un design moderne
                                    et
                                    accessible.</p>
                            </template>

                            <template #actions>
                                <BaseButton label="Lire" variant="secondary"
                                    @click.stop.prevent="handleActionClick('Lire')" />
                                <BaseButton label="Essayer" variant="primary"
                                    @click.stop.prevent="handleActionClick('Essayer')" />
                            </template>
                        </BaseCard>
                    </div>
                </div>
            </div>
        </section>

        <!-- Section BaseInput -->
        <section class="section">
            <h2>BaseInput</h2>
            <div class="component-showcase">
                <div class="demo-block">
                    <h3>Types d'input</h3>
                    <div class="demo-content inputs-column">
                        <BaseInput v-model="inputText" label="Texte" placeholder="Entrez du texte"
                            icon="bi bi-keyboard" />

                        <BaseInput v-model="inputEmail" type="email" label="Email" placeholder="votreemail@example.com"
                            icon="bi bi-envelope" />

                        <BaseInput v-model="inputPassword" type="password" label="Mot de passe" placeholder="••••••••"
                            icon="bi bi-lock" />

                        <BaseInput v-model="oui" type="radio" :value="true" label="Oui" />
                        <BaseInput v-model="oui" type="radio" :value="false" label="Non" />

                        <BaseInput type="color" label="Couleur" />

                        <BaseInput type="checkbox" label="Accepter" />

                        <BaseInput type="file" label="Fichier" multiple />

                        <BaseInput type="textarea" label="Description" />
                    </div>
                </div>

                <div class="demo-block">
                    <h3>Avec validation</h3>
                    <div class="demo-content inputs-column">
                        <BaseInput v-model="inputText" label="Texte (min 3 caractères)" placeholder="Entrez du texte"
                            :error="inputError" required />

                        <BaseButton label="Valider" variant="success" @click="validateInput"
                            customClass="text-center" />
                    </div>
                </div>
            </div>
        </section>

        <!-- Section BaseSelect -->
        <section class="section">
            <h2>BaseSelect</h2>
            <div class="component-showcase">
                <div class="demo-block">
                    <h3>Sélections simples</h3>
                    <div class="demo-content inputs-column">
                        <BaseSelect v-model="selectedCity" label="Sélectionnez une ville" :options="cities"
                            optionLabel="name" optionValue="id" />

                        <BaseSelect v-model="selectedStatus" label="Statut (requis)" :options="statuses" required />

                        <p v-if="selectedCity" class="info">
                            ✅ Ville sélectionnée : {{cities.find(c => c.id === Number(selectedCity))?.name}}
                        </p>
                        <p v-if="selectedStatus" class="info">
                            ✅ Statut sélectionné : {{ selectedStatus }}
                        </p>
                    </div>
                </div>
            </div>
        </section>

        <!-- Section BaseModal -->
        <section class="section">
            <h2>BaseModal</h2>
            <div class="component-showcase">
                <div class="demo-block">
                    <h3>Actions</h3>
                    <div class="demo-content">
                        <BaseButton label="Ouvrir modale simple" @click="showSimpleModal = true" />

                        <BaseButton label="Ouvrir modale formulaire" variant="success" @click="showFormModal = true" />

                        <BaseButton label="Ouvrir modale confirmation" variant="danger"
                            @click="showConfirmModal = true" />
                    </div>
                </div>
            </div>

            <!-- Modale simple -->
            <BaseModal v-model="showSimpleModal" title="Modale Simple" size="md">
                <p>Ceci est une modale simple. Vous pouvez cliquer sur le bouton × ou en dehors pour la fermer.</p>
                <p class="info">La modale se ferme au clic sur l'overlay car <code>closeOnOutside</code> est à true.
                </p>
            </BaseModal>

            <!-- Modale avec formulaire -->
            <BaseModal v-model="showFormModal" title="Créer un nouvel utilisateur" size="lg" :closeOnOutside="false">
                <div class="modal-form">
                    <BaseInput v-model="formData.name" label="Nom complet" type="checkbox" required />

                    <BaseInput type="file" label="photo" multiple v-model="multipleFiles" />

                    <BaseInput v-model="formData.email" type="email" label="Email" placeholder="jean@example.com"
                        required />

                    <BaseSelect v-model="formData.city" label="Ville" :options="cities" optionLabel="name"
                        optionValue="id" required />
                </div>

                <template #footer>
                    <BaseButton label="Annuler" variant="secondary" @click="showFormModal = false" />
                    <BaseButton label="Créer" variant="success" @click="submitForm" />
                </template>
            </BaseModal>

            <!-- Modale confirmation -->
            <BaseModal v-model="showConfirmModal" title="Confirmation" size="sm" :closeOnOutside="false">
                <p>Êtes-vous sûr de vouloir effectuer cette action ?</p>
                <p class="warning">⚠️ Cette action est irréversible.</p>

                <template #footer>
                    <BaseButton label="Annuler" variant="secondary" @click="showConfirmModal = false" />
                    <BaseButton label="Confirmer" variant="danger" @click="confirmAction" />
                </template>
            </BaseModal>
        </section>

        <!-- Section BaseTable -->
        <section class="section">
            <h2>BaseTable</h2>

            <!-- Tableau modal -->
            <BaseTable v-model:selectedItems="table" :items="users" searchable :onRowClick="(item) => console.log(item)"
                :columns="[
                    { key: 'id', label: 'ID', total: true },
                    { key: 'name', label: 'Nom', formatter: (value, item) => 'M. ' + item.name, onClick: (value, item) => { console.log(value) } },
                    { key: 'email', label: 'Email' },
                    { key: 'statut', label: 'Statut' },
                    { key: 'actions', label: 'Actions', footer: 'Total' }
                ]" multiSelect>
                <template #cell-statut="{ item }">
                    <BaseSelect size="sm" v-model="item.status" :options="statuses" />
                </template>

                <template #cell-actions="{ item }">
                    <BaseButton size="sm" label="Modifier" @click="console.log(item)" customClass="me-1" />

                    <BaseButton size="sm" label="Supprimer" variant="danger" @click="console.log(item)" />
                </template>
            </BaseTable>
        </section>
</template>
