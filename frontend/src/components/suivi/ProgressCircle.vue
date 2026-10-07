<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
    value: number
    size?: number
    strokeWidth?: number
    label?: string
}>()

const emit = defineEmits<{
    (e: 'click'): void
}>()

const size = computed(() => props.size ?? 44)
const strokeWidth = computed(() => props.strokeWidth ?? 4)

const radius = computed(() => (size.value - strokeWidth.value) / 2)
const circumference = computed(() => 2 * Math.PI * radius.value)
const dashOffset = computed(() =>
    circumference.value * (1 - Math.min(100, Math.max(0, props.value)) / 100)
)

const center = computed(() => size.value / 2)
</script>

<template>
    <button
        type="button"
        class="progress-circle"
        :style="{ width: `${size}px`, height: `${size}px` }"
        :title="label ?? 'Modifier l\'avancement'"
        :aria-label="`Avancement : ${value}%`"
        @click="emit('click')">
        <svg :width="size" :height="size" class="progress-circle__svg">
            <!-- Piste de fond -->
            <circle
                class="progress-circle__track"
                :cx="center"
                :cy="center"
                :r="radius"
                :stroke-width="strokeWidth"
                fill="transparent" />
            <!-- Portion remplie -->
            <circle
                class="progress-circle__fill"
                :cx="center"
                :cy="center"
                :r="radius"
                :stroke-width="strokeWidth"
                fill="transparent"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="dashOffset"
                stroke-linecap="round" />
        </svg>
        <span class="progress-circle__value">{{ Math.round(value) }}%</span>
    </button>
</template>

<style scoped>
.progress-circle {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0;
    border: none;
    background: transparent;
    cursor: pointer;
    border-radius: 50%;
    transition: transform 0.15s ease;
}

.progress-circle:hover {
    transform: scale(1.05);
}

.progress-circle:focus-visible {
    outline: 2px solid var(--dts-blue);
    outline-offset: 2px;
}

.progress-circle__svg {
    transform: rotate(-90deg); /* commence en haut du cercle */
}

.progress-circle__track {
    stroke: var(--dts-bg);
}

.progress-circle__fill {
    stroke: var(--dts-blue);
    transition: stroke-dashoffset 0.3s ease;
}

.progress-circle__value {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    font-size: 0.7rem;
    font-weight: 700;
    color: var(--dts-navy);
    pointer-events: none;
}
</style>