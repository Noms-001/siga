import { describe, expect, it } from 'vitest'

import {
    PermissionAction as A,
    PermissionResource as R,
    hasAnyPermission,
    hasPermission,
    permission,
} from './authorization'

describe('hasAnyPermission', () => {
    it('autorise une route sans permission requise', () => {
        expect(hasAnyPermission(undefined, undefined)).toBe(true)
        expect(hasAnyPermission([], [])).toBe(true)
    })

    it('refuse quand aucune permission requise n\'est accordee', () => {
        expect(hasAnyPermission([], [permission(R.ACTIVITE, A.CONSULTER)])).toBe(false)
    })

    it('accepte une permission alternative', () => {
        const required = [
            permission(R.VALIDATION_ACTIVITE, A.CONSULTER),
            permission(R.ACTIVITE, A.CONSULTER),
        ]

        expect(hasAnyPermission(['activite:consulter'], required)).toBe(true)
        expect(hasAnyPermission(['validation_activite:consulter'], required)).toBe(true)
        expect(hasAnyPermission(['indicateur:consulter'], required)).toBe(false)
    })

    it('compare les permissions sans tenir compte de la casse', () => {
        expect(hasAnyPermission(
            ['ACTIVITE:CONSULTER'],
            [permission(R.ACTIVITE, A.CONSULTER)],
        )).toBe(true)
    })
})

describe('hasPermission', () => {
    it('verifie une permission precise', () => {
        expect(hasPermission(['activite:creer'], R.ACTIVITE, A.CREER)).toBe(true)
        expect(hasPermission(['activite:creer'], R.ACTIVITE, A.CONSULTER)).toBe(false)
    })
})
