<script setup lang="ts">
import BaseButton from '../BaseButton'
import {
    baseCardProps,
    baseCardEmits,
    useBaseCard
} from './BaseCard'

const props = defineProps(baseCardProps)
const emit = defineEmits(baseCardEmits)

const {
    badgeItems,
    variantClasses,
    sizeClasses,
    trendVariantClasses,
    showValueSection,
    showPriceSection,
    handleClick,
    handleActionClick,
    badgeVariantClasses
} = useBaseCard(props, emit)
</script>

<template>
    <article :class="[
        'card position-relative overflow-hidden',
        variantClasses,
        sizeClasses,
        {
            'h-100': fullHeight,
            'opacity-50': disabled,
            'pointer-events-none': loading
        },
        customClass
    ]" @click="handleClick" role="button" :aria-disabled="disabled || loading">
        <div v-if="$slots.image || image" class="card-img-top overflow-hidden" style="aspect-ratio: 16 / 9;">
            <slot name="image">
                <img :src="image" class="w-100 h-100 object-fit-cover" alt="" />
            </slot>
        </div>

        <template v-if="!loading">

            <div class="card-body d-flex flex-column gap-3">

                <div v-if="showValueSection" class="d-flex justify-content-between align-items-start gap-4 flex-wrap">

                    <div class="d-flex align-items-center gap-3">

                        <span v-if="icon" :class="[
                            'd-inline-flex align-items-center justify-content-center rounded-3 bg-primary bg-opacity-10 text-primary',
                            icon
                        ]" style="padding: 20px; font-size: 25px;" />

                        <div class="d-flex flex-column gap-1">

                            <slot name="label">

                                <span v-if="label"
                                    class="text-secondary-emphasis small fw-semibold text-uppercase tracking-wide">
                                    {{ label }}
                                </span>

                            </slot>

                            <slot name="value">

                                <span v-if="value !== ''" class="display-6 fw-bold lh-1">
                                    {{ value }}
                                </span>

                            </slot>

                        </div>

                    </div>

                    <div class="d-flex align-items-center">

                        <slot name="trend">

                            <span v-if="trend" :class="[
                                'badge rounded-pill px-3 py-2 fw-semibold',
                                trendVariantClasses
                            ]">
                                {{ trend }}
                            </span>

                        </slot>

                    </div>

                </div>

                <div v-if="badgeItems.length && !$slots.badges" class="d-flex flex-wrap gap-2">

                    <span v-for="(badge, index) in badgeItems" :key="index" :class="[
                        'badge rounded-pill px-3 py-2 fw-bold d-inline-flex align-items-center gap-2',
                        badgeVariantClasses(badge.variant)
                    ]">
                        <i v-if="badge.icon" :class="badge.icon"></i>

                        {{ badge.label }}
                    </span>

                </div>

                <slot name="badges">
                    <template v-if="false"></template>
                </slot>

                <div class="d-flex flex-column gap-3">

                    <div class="d-flex flex-column gap-2">

                        <slot name="title">

                            <h3 v-if="title" class="h5 fw-bold mb-0">
                                {{ title }}
                            </h3>

                        </slot>

                        <slot name="subtitle">

                            <p v-if="subtitle" class="card-subtitle mb-0 small">
                                {{ subtitle }}
                            </p>

                        </slot>

                    </div>
                    <slot name="description">
                        <p v-if="description" class="text-secondary mb-0 lh-base small">
                            {{ description }}
                        </p>
                    </slot>

                    <div v-if="showPriceSection" class="d-flex align-items-center gap-3 flex-wrap">
                        <span v-if="price" class="h4 fw-bold mb-0">
                            {{ price }}
                        </span>

                        <span v-if="oldPrice" class="text-secondary-emphasis text-decoration-line-through small mb-0">
                            {{ oldPrice }}
                        </span>
                    </div>

                    <slot />

                </div>

                <div v-if="$slots.actions || actionLabel" class="d-flex flex-wrap gap-2">

                    <slot name="actions">

                        <BaseButton v-if="actionLabel" :label="actionLabel" :variant="actionVariant"
                            :disabled="disabled" @click="handleActionClick" />

                    </slot>

                </div>

                <div v-if="$slots.footer" class="pt-2 border-top">
                    <slot name="footer" />
                </div>

            </div>

        </template>

        <template v-else>

            <div class="card-body card-skeleton">

                <div class="skeleton skeleton-label"></div>

                <div class="skeleton skeleton-value"></div>

                <div class="skeleton skeleton-line"></div>

                <div class="skeleton skeleton-line short"></div>

                <div class="skeleton skeleton-line"></div>

            </div>

        </template>

    </article>
</template>

<style scoped>
.card {
    border: none;
    border-radius: var(--radius-xl);
    background: var(--card-bg);
    transition:
        transform var(--transition-normal),
        box-shadow var(--transition-normal);
    box-shadow: var(--shadow-sm);
}

.card:hover {
    transform: translateY(-3px);
    box-shadow: var(--shadow-md);
}

.object-fit-cover {
    object-fit: cover;
}

.card-body {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.card h3 {
    margin: 0;
    color: var(--text-color);
}

.card-body,
.card-body p,
.card-body span,
.card-body h3,
.card-subtitle {
    color: var(--text-color);
}

.card-subtitle,
.card .text-secondary-emphasis,
.card .text-secondary {
    color: var(--text-muted-custom) !important;
}

.badge {
    font-weight: 600;
}

.card-img-top {
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
    overflow: hidden;
}

.stat-card {
    min-height: 180px;
    padding: 1rem;
}

.stat-card .card-body {
    justify-content: center;
}

.stat-card .display-6 {
    font-size: clamp(2rem, 3vw, 3rem);
}

@media (prefers-color-scheme: dark) {
    .card {
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
    }
}
</style>
