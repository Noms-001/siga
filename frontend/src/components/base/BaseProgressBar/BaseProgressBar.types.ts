export type ProgressVariant =
    | 'primary'
    | 'success'
    | 'danger'
    | 'warning'
    | 'secondary'

export interface BaseProgressBarProps {
    value?: number
    label?: string
    variant?: ProgressVariant
    logs?: string[]
    showLogs?: boolean
    max?: number
}