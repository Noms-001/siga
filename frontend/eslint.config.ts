import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginPlaywright from 'eslint-plugin-playwright'
import pluginVitest from '@vitest/eslint-plugin'
import pluginOxlint from 'eslint-plugin-oxlint'
import skipFormatting from 'eslint-config-prettier/flat'

// To allow more languages other than `ts` in `.vue` files, uncomment the following lines:
// import { configureVueProject } from '@vue/eslint-config-typescript'
// configureVueProject({ scriptLangs: ['ts', 'tsx'] })
// More info at https://github.com/vuejs/eslint-config-typescript/#advanced-setup

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{vue,ts,mts,tsx}'],
  },

  globalIgnores(['**/dist/**', '**/dist-ssr/**', '**/coverage/**']),

  ...pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,

  {
    name: 'app/partials-single-word-names',
    // Navbar / Sidebar / Footer sont importés explicitement (pas d'enregistrement
    // global) : aucune collision possible, on garde ces noms de fichiers.
    files: ['src/components/partial/**/*.vue'],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  {
    name: 'app/views-single-word-names',
    // Noms de vues imposés par le cahier des charges (Liste, Detail, Ajout).
    // Comme les partials, ces vues sont importées par le router ou par une
    // autre vue, jamais enregistrées globalement.
    //
    // Le dossier `activites/` est celui de laReleve : sans lui, ces trois
    // chemins ne desactivaient plus rien et la regle revenait a interdire
    // les noms que le cahier des charges demande.
    files: [
      'src/views/frontoffice/activites/Liste.vue',
      'src/views/frontoffice/activites/Detail.vue',
      'src/views/frontoffice/activites/Ajout.vue',
      'src/views/frontoffice/activites/Brouillons.vue',
    ],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  {
    ...pluginPlaywright.configs['flat/recommended'],
    files: ['e2e/**/*.{test,spec}.{js,ts,jsx,tsx}'],
  },

  {
    ...pluginVitest.configs.recommended,
    files: ['src/**/__tests__/*'],
  },

  ...pluginOxlint.buildFromOxlintConfigFile('.oxlintrc.json'),

  skipFormatting,
)
