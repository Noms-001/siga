// Extension `.vue` explicite dans l'export, et non resolue implicitement.
//
// Un chemin sans extension vers un fichier `.vue` n'est PAS resolu ici, dans
// un composant compile par vue-tsc : l'import echoue avec "Cannot find module".
// Le projet contourne cela partout par un fichier d exports comme celui-ci, et
// les tests qui importent une vue ecrivent son extension. Les deux formes sont
// donc ici, et seule celle avec extension fonctionne.
export { default as FichiersModal } from './FichiersModal.vue'
export { default as LivrableModal } from './LivrableModal.vue'
export { default as DecisionActiviteModal } from './DecisionActiviteModal.vue'
