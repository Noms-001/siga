-- Migration idempotente pour les bases déjà initialisées avant la génération centralisée.
-- Les codes existants ne sont pas modifiés. Les index scoped refuseront la migration
-- si un même service (ou une même activité parente) contient des doublons de casse.
CREATE TABLE IF NOT EXISTS code_sequence (
    cle VARCHAR(120) PRIMARY KEY,
    dernier_numero INTEGER NOT NULL CHECK (dernier_numero >= 0)
);

DO $$
DECLARE contrainte RECORD;
BEGIN
    FOR contrainte IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'activite'::regclass
          AND contype = 'u'
          AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                              WHERE attrelid = 'activite'::regclass
                                AND attname = 'code')]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE activite DROP CONSTRAINT %I', contrainte.conname);
    END LOOP;
END $$;

DROP INDEX IF EXISTS uq_activite_code_ci;
CREATE UNIQUE INDEX IF NOT EXISTS uq_activite_service_code_ci
    ON activite (id_service, LOWER(code));

DO $$
DECLARE contrainte RECORD;
BEGIN
    FOR contrainte IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'sous_activite'::regclass
          AND contype = 'u'
          AND conkey = ARRAY[(SELECT attnum FROM pg_attribute
                              WHERE attrelid = 'sous_activite'::regclass
                                AND attname = 'code')]::smallint[]
    LOOP
        EXECUTE format('ALTER TABLE sous_activite DROP CONSTRAINT %I', contrainte.conname);
    END LOOP;
END $$;

DROP INDEX IF EXISTS uq_sous_activite_code_ci;
CREATE UNIQUE INDEX IF NOT EXISTS uq_sous_activite_parent_code_ci
    ON sous_activite (id_activite, LOWER(code));

-- Conserve le maximum déjà observé afin qu'une suppression ultérieure
-- n'autorise jamais la réutilisation d'une séquence historique.
INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:PTA:' || a.id_service || ':' || code_parts[1] || ':' || a.id_objectif_specifique,
             MAX(code_parts[3]::INTEGER)
FROM activite a
JOIN objectif_specifique os ON os.id_objectif_specifique = a.id_objectif_specifique
CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)-([0-9]+)$') code_parts
CROSS JOIN LATERAL regexp_match(os.code, '([0-9]+)$') objectif_parts
WHERE code_parts IS NOT NULL
    AND objectif_parts IS NOT NULL
    AND code_parts[2]::INTEGER = objectif_parts[1]::INTEGER
GROUP BY code_parts[1], a.id_service, a.id_objectif_specifique
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);

INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:NON_PTA:' || a.id_service || ':' || code_parts[1], MAX(code_parts[2]::INTEGER)
FROM activite a
CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)$') code_parts
WHERE a.id_objectif_specifique IS NULL
    AND code_parts IS NOT NULL
GROUP BY a.id_service, code_parts[1]
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);

-- Carry forward the earlier organization-wide counters, including numbers
-- whose corresponding activities may already have been deleted.
INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:PTA:' || a.id_service || ':' || code_parts[1] || ':' || a.id_objectif_specifique,
             old_sequence.dernier_numero
FROM activite a
CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)-([0-9]+)$') code_parts
JOIN code_sequence old_sequence
    ON old_sequence.cle = 'ACT:PTA:' || code_parts[1] || ':' || a.id_objectif_specifique
WHERE code_parts IS NOT NULL
GROUP BY a.id_service, code_parts[1], a.id_objectif_specifique, old_sequence.dernier_numero
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);

INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'ACT:NON_PTA:' || a.id_service || ':' || code_parts[1],
             old_sequence.dernier_numero
FROM activite a
CROSS JOIN LATERAL regexp_match(a.code, '^A-([0-9]{4})-([0-9]+)$') code_parts
JOIN code_sequence old_sequence
    ON old_sequence.cle = 'ACT:NON_PTA:' || code_parts[1]
WHERE a.id_objectif_specifique IS NULL
    AND code_parts IS NOT NULL
GROUP BY a.id_service, code_parts[1], old_sequence.dernier_numero
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);

INSERT INTO code_sequence (cle, dernier_numero)
SELECT 'SA:' || parent.id_activite,
             MAX(substring(sa.code FROM char_length('SA-' || substring(parent.code FROM 3) || '-') + 1)::INTEGER)
FROM sous_activite sa
JOIN activite parent ON parent.id_activite = sa.id_activite
WHERE left(sa.code, char_length('SA-' || substring(parent.code FROM 3) || '-'))
                = 'SA-' || substring(parent.code FROM 3) || '-'
    AND substring(sa.code FROM char_length('SA-' || substring(parent.code FROM 3) || '-') + 1) ~ '^[0-9]+$'
GROUP BY parent.id_activite
ON CONFLICT (cle) DO UPDATE
SET dernier_numero = GREATEST(code_sequence.dernier_numero, EXCLUDED.dernier_numero);
