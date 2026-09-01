<script setup lang="ts">
import {
    baseProgressBarProps,
    useBaseProgressBar
} from './BaseProgressBar'

import { ref, watch, nextTick } from "vue";

const props =
    defineProps(baseProgressBarProps)


const logContainer = ref<HTMLDivElement | null>(null);

const {
    percent,
    variantClasses,
    statusText
} = useBaseProgressBar(props)

watch(
    () => props.logs.length,
    async () => {
        await nextTick();

        if (logContainer.value) {
            logContainer.value.scrollTop =
                logContainer.value.scrollHeight;
        }
    }
);

watch(
    () => props.logs,
    async () => {
        await nextTick();

        logContainer.value?.scrollTo({
            top: logContainer.value.scrollHeight,
            behavior: "smooth"
        });
    },
    { deep: true }
);
</script>

<template>
    <div class="base-progress">

        <div class="progress-header">

            <div v-if="label" class="progress-label">
                {{ label }}
            </div>

            <div class="progress-percent">
                {{ percent }}%
            </div>

        </div>

        <div class="progress-track" role="progressbar" :aria-valuenow="percent" aria-valuemin="0" :aria-valuemax="max">
            <div class="progress-fill" :class="variantClasses" :style="{
                width: `${percent}%`
            }" />
        </div>

        <div class="progress-meta">

            <span class="progress-status">
                {{ statusText }}
            </span>

        </div>

        <div ref="logContainer" class="progress-logs">
            <div v-for="(log, i) in logs" :key="i" class="progress-log" :class="`progress-log--${log.type}`">
                <i class="bi log-icon" :class="{
                    'bi-info-circle-fill': log.type === 'info',
                    'bi-check-circle-fill': log.type === 'success',
                    'bi-exclamation-triangle-fill': log.type === 'warning',
                    'bi-x-circle-fill': log.type === 'error',
                }"></i>
                <span class="log-text">{{ log.message }}</span>
                <span class="log-time">{{ log.time }}</span>
            </div>
        </div>

    </div>
</template>

<style scoped>
.base-progress {
    display: flex;
    flex-direction: column;
    gap: 1rem;
}

.progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 1rem;
}

.progress-label {
    font-weight: 700;
    color: var(--text-color);
}

.progress-percent {
    font-size: 0.95rem;
    color: var(--text-muted-custom);
}

.progress-track {
    width: 100%;
    height: 16px;
    border-radius: 999px;
    background: var(--border-color);
    overflow: hidden;
}

.progress-fill {
    height: 100%;
    border-radius: 999px;
    transition: width 0.35s ease;
}

.progress-fill--primary {
    background: var(--primary-color);
}

.progress-fill--success {
    background: var(--success-color);
}

.progress-fill--danger {
    background: var(--danger-color);
}

.progress-fill--warning {
    background: var(--warning-color);
}

.progress-fill--secondary {
    background: var(--secondary-color);
}

.progress-meta {
    display: flex;
}

.progress-status {
    color: var(--text-muted-custom);
    font-size: 0.95rem;
}

/* Dans <style scoped> de ton composant Vue */

.progress-logs {
    --log-radius: 0.75rem;
    --log-border: #dee2e6;
    --log-bg: #f8f9fa;
    --log-bg-hover: #f1f3f5;
    --log-text: #495057;
    --log-time-color: #adb5bd;

    display: flex;
    flex-direction: column;
    gap: 0;
    max-height: 174px;
    overflow-y: auto;
    border: 1px solid var(--log-border);
    border-radius: var(--log-radius);
    background: var(--log-bg);
    scrollbar-width: thin;
    scrollbar-color: #dee2e6 transparent;
}

.progress-log {
    display: flex;
    align-items: flex-start;
    gap: 0.65rem;
    padding: 0.55rem 1rem;
    border-top: 1px solid var(--log-border);
    border-left: 3px solid transparent;
    font-size: 0.875rem;
    line-height: 1.5;
    color: var(--log-text);
    transition: background 0.15s ease;
}

.progress-log:hover {
    background: var(--log-bg-hover);
}

.log-icon {
    font-size: 0.95rem;
    flex-shrink: 0;
    margin-top: 2px;
}

.log-time {
    font-size: 0.78rem;
    color: var(--log-time-color);
    white-space: nowrap;
    font-variant-numeric: tabular-nums;
    margin-top: 3px;
    flex-shrink: 0;
}

.log-text {
    flex: 1;
}

/* --- Types --- */

.progress-log--info {
    border-left-color: #0d6efd;
}

.progress-log--info .log-icon {
    color: #0d6efd;
}

.progress-log--info .log-text {
    color: #084298;
}

.progress-log--success {
    border-left-color: #198754;
}

.progress-log--success .log-icon {
    color: #198754;
}

.progress-log--success .log-text {
    color: #0a3622;
}

.progress-log--warning {
    border-left-color: #ffc107;
}

.progress-log--warning .log-icon {
    color: #e0a800;
}

.progress-log--warning .log-text {
    color: #664d03;
}

.progress-log--error {
    border-left-color: #dc3545;
}

.progress-log--error .log-icon {
    color: #dc3545;
}

.progress-log--error .log-text {
    color: #58151c;
}
</style>