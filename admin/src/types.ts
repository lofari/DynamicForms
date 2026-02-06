export interface FormSummary {
  formId: string
  title: string
  description: string
  pageCount: number
  fieldCount: number
}

export interface SelectOption {
  value: string
  label: string
}

export interface VisibilityCondition {
  fieldId: string
  operator: string
  value: string | number | boolean
}

export interface TextValidation {
  minLength?: number
  maxLength?: number
  pattern?: string
  errorMessage?: string
}

export interface NumberValidation {
  min?: number
  max?: number
  errorMessage?: string
}

export interface SelectionValidation {
  minSelections?: number
  maxSelections?: number
  errorMessage?: string
}

export interface FormElement {
  type: string
  id: string
  label: string
  required: boolean
  visibleWhen?: VisibilityCondition
  // text_field
  multiline?: boolean
  validation?: TextValidation | NumberValidation | SelectionValidation
  // dropdown, radio, multi_select
  options?: SelectOption[]
  // checkbox, toggle
  defaultValue?: boolean
  // date_picker
  minDate?: string
  maxDate?: string
  format?: string
  // slider
  min?: number
  max?: number
  step?: number
  // file_upload
  maxFileSize?: number
  allowedTypes?: string[]
  // section_header
  subtitle?: string
  // label
  text?: string
  // repeating_group
  minItems?: number
  maxItems?: number
  elements?: FormElement[]
}

export interface Page {
  pageId: string
  title: string
  elements: FormElement[]
}

export interface FormDefinition {
  formId: string
  title: string
  description: string
  pages: Page[]
}

export interface Submission {
  id: string
  formId: string
  values: Record<string, string>
  submittedAt: number
}
