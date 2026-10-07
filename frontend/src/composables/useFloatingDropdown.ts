import {
    nextTick,
    onBeforeUnmount,
    onMounted,
    ref,
    watch,
    type Ref,
} from 'vue'

interface FloatingDropdownOptions {
    /**
     * Hauteur estimée utilisée au premier calcul, avant que le dropdown
     * ne soit rendu et que `offsetHeight` ne soit disponible. 280px
     * correspond à ~7 lignes — au-delà, la plupart des menus défilent.
     */
    estimatedHeight?: number

    /** Marge de sécurité avec les bords du viewport. */
    viewportPadding?: number

    /** Décalage vertical entre le trigger et le dropdown. */
    offset?: number

    /** z-index appliqué au dropdown. Doit rester au-dessus des modales Bootstrap (1050). */
    zIndex?: number
}

/**
 * Positionne un dropdown flottant en choisissant automatiquement le côté
 * qui offre le plus d'espace disponible.
 *
 * Le composable ne rend rien et ne se préoccupe pas du contenu : il
 * calcule un objet de style à appliquer sur le conteneur du dropdown.
 * L'appelant doit :
 *   - rendre son dropdown dans un <Teleport to="body"> ;
 *   - fournir une ref sur le trigger (l'élément cliqué) ;
 *   - fournir une ref sur le dropdown (une fois monté) ;
 *   - fournir une ref sur l'état d'ouverture.
 *
 * Le recalcul se déclenche :
 *   - à chaque ouverture ;
 *   - au scroll (capture — couvre les scrolls internes à un conteneur) ;
 *   - au resize de la fenêtre.
 */
export function useFloatingDropdown(
    triggerRef: Ref<HTMLElement | null>,
    dropdownRef: Ref<HTMLElement | null>,
    isOpen: Ref<boolean>,
    options: FloatingDropdownOptions = {},
) {
    const estimatedHeight = options.estimatedHeight ?? 280
    const viewportPadding = options.viewportPadding ?? 8
    const offset = options.offset ?? 4
    const zIndex = options.zIndex ?? 1050

    const style = ref<Record<string, string>>({})
    const placement = ref<'bottom' | 'top'>('bottom')

    function recalculer(): void {
        const trigger = triggerRef.value
        if (!trigger) return

        const rect = trigger.getBoundingClientRect()
        const viewportHeight = window.innerHeight
        const viewportWidth = window.innerWidth

        // offsetHeight est 0 tant que le dropdown n'est pas monté ; on
        // utilise alors l'estimation pour décider du côté.
        const dropdownHeight = dropdownRef.value?.offsetHeight || estimatedHeight

        const spaceBelow = viewportHeight - rect.bottom - viewportPadding
        const spaceAbove = rect.top - viewportPadding

        /*
         * Choix du côté :
         *   1. Si le dropdown tient en bas sans scroll → bas.
         *   2. Sinon s'il tient en haut → haut.
         *   3. Sinon on prend le côté qui a le plus d'espace, avec scroll
         *      interne — c'est la seule option praticable quand aucune
         *      direction n'est confortable.
         */
        let cote: 'bottom' | 'top'
        if (spaceBelow >= dropdownHeight) {
            cote = 'bottom'
        } else if (spaceAbove >= dropdownHeight) {
            cote = 'top'
        } else {
            cote = spaceBelow >= spaceAbove ? 'bottom' : 'top'
        }

        const maxHeight = Math.max(120, cote === 'bottom' ? spaceBelow : spaceAbove)

        // Position verticale.
        // - Bottom : le dropdown commence sous le trigger.
        // - Top : il commence au-dessus, avec la hauteur qu'il occupera
        //   réellement (bornée par maxHeight).
        const top = cote === 'bottom'
            ? rect.bottom + offset
            : Math.max(viewportPadding, rect.top - Math.min(dropdownHeight, maxHeight) - offset)

        // Alignement horizontal sur le trigger, borné pour ne pas dépasser
        // le viewport — utile si le trigger est près du bord droit.
        const width = Math.min(rect.width, viewportWidth - 2 * viewportPadding)
        const left = Math.min(
            Math.max(viewportPadding, rect.left),
            viewportWidth - width - viewportPadding,
        )

        style.value = {
            position: 'fixed',
            top: `${top}px`,
            left: `${left}px`,
            width: `${width}px`,
            maxHeight: `${maxHeight}px`,
            zIndex: String(zIndex),
        }
        placement.value = cote
    }

    /** Le dropdown suit le trigger au scroll et au resize. */
    function onScrollOrResize(): void {
        if (isOpen.value) recalculer()
    }

    /*
     * Au passage à "ouvert", on attend le prochain tick : le dropdown
     * vient d'être monté, et son `offsetHeight` n'est exploitable qu'une
     * fois le rendu effectué.
     */
    watch(isOpen, async (ouvert) => {
        if (ouvert) {
            await nextTick()
            recalculer()
        }
    })

    onMounted(() => {
        // Capture : true permet d'attraper le scroll de n'importe quel
        // conteneur interne, pas seulement celui du document.
        window.addEventListener('scroll', onScrollOrResize, true)
        window.addEventListener('resize', onScrollOrResize)
    })

    onBeforeUnmount(() => {
        window.removeEventListener('scroll', onScrollOrResize, true)
        window.removeEventListener('resize', onScrollOrResize)
    })

    return { style, placement, recalculer }
}