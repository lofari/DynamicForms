import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import type { FormSummary } from '../types'

export default function Dashboard() {
  const navigate = useNavigate()
  const [forms, setForms] = useState<FormSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchForms = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await api.getForms()
      setForms(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load forms')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchForms()
  }, [fetchForms])

  const handleDelete = async (formId: string, title: string) => {
    if (!window.confirm(`Are you sure you want to delete "${title}"?`)) {
      return
    }
    try {
      await api.deleteForm(formId)
      setForms((prev) => prev.filter((f) => f.formId !== formId))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete form')
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="mb-8 flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">Form Dashboard</h1>
          <button
            onClick={() => navigate('/admin/forms/new/edit')}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white shadow-sm transition-colors hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            Create New Form
          </button>
        </div>

        {/* Loading state */}
        {loading && (
          <div className="flex flex-col items-center justify-center py-24">
            <svg
              className="mb-4 h-8 w-8 animate-spin text-blue-600"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            <p className="text-sm text-gray-500">Loading forms...</p>
          </div>
        )}

        {/* Error state */}
        {!loading && error && (
          <div className="rounded-lg border border-red-200 bg-red-50 p-6 text-center">
            <p className="mb-4 text-sm text-red-700">{error}</p>
            <button
              onClick={fetchForms}
              className="rounded-lg bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
            >
              Retry
            </button>
          </div>
        )}

        {/* Empty state */}
        {!loading && !error && forms.length === 0 && (
          <div className="rounded-lg border-2 border-dashed border-gray-300 bg-white p-12 text-center">
            <h3 className="mb-1 text-lg font-medium text-gray-900">
              No forms yet
            </h3>
            <p className="mb-4 text-sm text-gray-500">
              Get started by creating your first form.
            </p>
            <button
              onClick={() => navigate('/admin/forms/new/edit')}
              className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
            >
              Create New Form
            </button>
          </div>
        )}

        {/* Form card grid */}
        {!loading && !error && forms.length > 0 && (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {forms.map((form) => (
              <div
                key={form.formId}
                className="flex flex-col rounded-lg bg-white shadow-sm ring-1 ring-gray-200 transition-shadow hover:shadow-md"
              >
                <div className="flex flex-1 flex-col p-5">
                  {/* Title */}
                  <h2 className="text-lg font-bold text-gray-900">
                    {form.title}
                  </h2>

                  {/* Description */}
                  <p className="mt-1 line-clamp-2 text-sm text-gray-500">
                    {form.description}
                  </p>

                  {/* Stats badges */}
                  <div className="mt-4 flex items-center gap-3">
                    <span className="inline-flex items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-medium text-blue-700">
                      {form.pageCount} {form.pageCount === 1 ? 'page' : 'pages'}
                    </span>
                    <span className="inline-flex items-center rounded-full bg-gray-100 px-2.5 py-0.5 text-xs font-medium text-gray-600">
                      {form.fieldCount}{' '}
                      {form.fieldCount === 1 ? 'field' : 'fields'}
                    </span>
                  </div>
                </div>

                {/* Action buttons */}
                <div className="flex items-center gap-2 border-t border-gray-100 px-5 py-3">
                  <button
                    onClick={() =>
                      navigate(`/admin/forms/${form.formId}/edit`)
                    }
                    className="rounded-md bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition-colors hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-1"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() =>
                      navigate(`/admin/forms/${form.formId}/submissions`)
                    }
                    className="rounded-md bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-700 transition-colors hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-1"
                  >
                    Submissions
                  </button>
                  <button
                    onClick={() => handleDelete(form.formId, form.title)}
                    className="ml-auto rounded-md bg-red-50 px-3 py-1.5 text-xs font-medium text-red-600 transition-colors hover:bg-red-100 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-1"
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
