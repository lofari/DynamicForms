import type { FormSummary, FormDefinition, Submission } from './types'

const BASE = ''

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${url}`, init)
  if (!res.ok) {
    throw new Error(`${res.status} ${res.statusText}`)
  }
  if (res.status === 204) return undefined as T
  return res.json()
}

export const api = {
  getForms: () => fetchJson<FormSummary[]>('/forms'),

  getForm: (formId: string) => fetchJson<FormDefinition>(`/forms/${formId}`),

  createForm: (form: FormDefinition) =>
    fetchJson<FormDefinition>('/admin/forms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form),
    }),

  updateForm: (formId: string, form: FormDefinition) =>
    fetchJson<FormDefinition>(`/admin/forms/${formId}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form),
    }),

  deleteForm: (formId: string) =>
    fetchJson<void>(`/admin/forms/${formId}`, { method: 'DELETE' }),

  getSubmissions: (formId: string) =>
    fetchJson<Submission[]>(`/admin/forms/${formId}/submissions`),
}
