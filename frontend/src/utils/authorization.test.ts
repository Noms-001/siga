import { describe, expect, it } from 'vitest'
import { permissionForRoute } from './authorization'

describe('permissionForRoute', () => {
  it('ne demande pas de permission applicative sur les routes frontoffice', () => {
    expect(permissionForRoute('/membres/suivi')).toBeNull()
    expect(permissionForRoute('/dashboard')).toBeNull()
  })

  it('conserve les permissions backoffice', () => {
    expect(permissionForRoute('/backoffice/dashboard')).toEqual({
      resource: 'dashboard',
      action: 'READ',
    })
  })
})
