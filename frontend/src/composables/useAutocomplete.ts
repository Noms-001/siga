import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/** Suggestion d'autocomplete, telle que renvoyee par l'API. */
export interface Suggestion {
    id: number
    code?: string | null
    libelle?: string | null
    libelleSecondaire?: string | null
    /**
     * Objectif specifique uniquement.
     *
     * Annee de l'objectif, et prochain numero d'activite libre dans cet
     * objectif. Les deux servent a proposer un code d'activite, et n'ont pas
     * d'equivalent dans l'autocomplete des activites, qui les laisse a null.
     *
     * `annee` est distincte de `libelleSecondaire` bien que les deux portent
     * la meme valeur : celle-ci est une chaine faite pour etre affichee,
     * celle-la un nombre destine a etre concatene dans un code.
     */
    annee?: number | null
    prochainNumero?: number | null
}

export interface UseAutocompleteOptions {
    /**
     * Appel reseau. Doit RESOUDRE (jamais rejeter) : le composable affiche un
     * menu vide en cas d echec, et une rejection non rattrapee remonterait
     * jusqu a Vue.
     */
    rechercher: (terme: string) => Promise<Suggestion[]>
    /** Appele apres selection d une suggestion. */
    onSelectionner?: (suggestion: Suggestion) => void
    /** Appele quand la saisie est effacee, pour lever le filtre correspondant. */
    onEffacer?: () => void
    delaiMs?: number
    minimumLongueur?: number
}

export interface UseAutocomplete {
    /** A poser sur la div qui enveloppe le champ ET la liste de suggestions. */
    racine: Ref<HTMLElement | null>
    saisie: Ref<string>
    suggestions: Ref<Suggestion[]>
    ouvert: Ref<boolean>
    indexActif: Ref<number>
    charger: () => void
    selectionner: (suggestion: Suggestion) => void
    survoler: (index: number) => void
    surTouche: (event: KeyboardEvent) => void
    fermer: () => void
}

/**
 * Autocomplete partage par les champs texte de Liste.vue.
 *
 * Pourquoi un composable plutot que du code dans la vue : les deux champs de
 * recherche se comportent identiquement, et c est ce comportement qui est
 * delicat. Ecrire la logique deux fois dans la vue aurait duplique deux fois
 * la regle la plus fragile, celle de l annulation.
 *
 * Regle anti-course, la vraie difficulte d un autocomplete : une reponse est
 * ecartee des que sa saisie ne correspond plus au texte courant. Sans cela,
 * taper "abc" puis "ab" rapidement peut afficher les resultats de "ab" arrives
 * apres ceux de "abc", et la liste proposee ne correspond plus a ce que
 * l utilisateur a ecrit. On ne peut pas annuler une requete HTTP deja
 * envoyee, donc on ecarte la reponse obsolete au lieu de tenter de l annuler.
 */
export function useAutocomplete(options: UseAutocompleteOptions): UseAutocomplete {
    const delaiMs = options.delaiMs ?? 300
    const minimumLongueur = options.minimumLongueur ?? 1

    const racine = ref<HTMLElement | null>(null)
    const saisie = ref('')
    const suggestions = ref<Suggestion[]>([])
    const ouvert = ref(false)
    const indexActif = ref(-1)

    let minuteur: ReturnType<typeof setTimeout> | null = null
    // Incremente a chaque frappe. Une reponse compare son jeton a celui du
    // moment ou SA requete est partie, pas a celui du moment ou elle arrive.
    let jetonEnCours = 0

    function annulerMinuteur(): void {
        if (minuteur !== null) {
            clearTimeout(minuteur)
            minuteur = null
        }
    }

    function fermer(): void {
        ouvert.value = false
        indexActif.value = -1
    }

    async function executer(termes: string): Promise<void> {
        const monJeton = ++jetonEnCours

        let resultats: Suggestion[]
        try {
            resultats = await options.rechercher(termes)
        } catch {
            // Une erreur reseau ne doit ni laisser un menu vide ouvert, ni
            // interrompre la saisie en cours : on ferme simplement.
            if (monJeton === jetonEnCours) fermer()
            return
        }

        if (monJeton !== jetonEnCours) return

        suggestions.value = resultats
        indexActif.value = resultats.length > 0 ? 0 : -1
        ouvert.value = resultats.length > 0
    }

    function charger(): void {
        annulerMinuteur()

        const termes = saisie.value.trim()

        if (termes.length < minimumLongueur) {
            // Saisie trop courte : on invalide aussi la requete en vol, sinon
            // elle pourrait rouvrir le menu sur une saisie devenue vide.
            jetonEnCours++
            suggestions.value = []
            fermer()
            options.onEffacer?.()
            return
        }

        minuteur = setTimeout(() => void executer(termes), delaiMs)
    }

    function selectionner(suggestion: Suggestion): void {
        annulerMinuteur()

        // Invalide toute requête précédente encore en cours.
        jetonEnCours++

        suggestions.value = []
        fermer()

        options.onSelectionner?.(suggestion)
    }

    function survoler(index: number): void {
        indexActif.value = index
    }

    function surTouche(event: KeyboardEvent): void {
        const nombre = suggestions.value.length

        if (!ouvert.value || nombre === 0) {
            if (event.key === 'Escape') fermer()
            return
        }

        switch (event.key) {
            case 'ArrowDown':
                // Modulo pour passer du dernier au premier sans traitement
                // particulier du bord.
                indexActif.value = (indexActif.value + 1) % nombre
                break

            case 'ArrowUp':
                indexActif.value = (indexActif.value - 1 + nombre) % nombre
                break

            case 'Enter':
                // Empeche la soumission du formulaire par defaut : valider la
                // suggestion prime sur tout le reste.
                if (indexActif.value >= 0) {
                    const suggestion = suggestions.value[indexActif.value]
                    // Un index dans les bornes ne garantit pas l existence :
                    // le tableau a pu etre remplace entre deux rendus.
                    if (suggestion) {
                        event.preventDefault()
                        selectionner(suggestion)
                    }
                }
                break

            case 'Escape':
                fermer()
                break

            case 'Tab':
                fermer()
                break
        }
    }

    function surClicExterieur(event: MouseEvent): void {
        const noeud = racine.value
        if (!ouvert.value) return
        if (noeud && event.target instanceof Node && noeud.contains(event.target)) return
        fermer()
    }

    onMounted(() => document.addEventListener('mousedown', surClicExterieur))
    onBeforeUnmount(() => {
        annulerMinuteur()
        document.removeEventListener('mousedown', surClicExterieur)
    })

    return {
        racine,
        saisie,
        suggestions,
        ouvert,
        indexActif,
        charger,
        selectionner,
        survoler,
        surTouche,
        fermer,
    }
}
