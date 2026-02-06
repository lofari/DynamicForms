import { useEffect, useState, useCallback, useMemo } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { api } from '../api'
import type { FormDefinition, Submission } from '../types'

interface ColumnDef {
  fieldId: string
  label: string
}

const NON_INPUT_TYPES = new Set(['section_header', 'label'])

function extractColumns(form: FormDefinition): ColumnDef[] {
  const columns: ColumnDef[] = []

  for (const page of form.pages) {
    for (const el of page.elements) {
      if (NON_INPUT_TYPES.has(el.type)) continue

      if (el.type === 'repeating_group' && el.elements) {
        for (const child of el.elements) {
          if (NON_INPUT_TYPES.has(child.type)) continue
          columns.push({
            fieldId: `${el.id}[].${child.id}`,
            label: `${el.label} [].${child.label}`,
          })
        }
      } else {
        columns.push({ fieldId: el.id, label: el.label })
      }
    }
  }

  return columns
}

function formatTimestamp(ts: number): string {
  return new Date(ts).toLocaleString()
}

function truncateId(id: string): string {
  return id.length > 8 ? id.slice(0, 8) + '\u2026' : id
}

function escapeCsvCell(value: string): string {
  if (value.includes(',') || value.includes('"') || value.includes('\n')) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}

export default function Submissions() {
  const { formId } = useParams<{ formId: string }>()
  const navigate = useNavigate()

  const [form, setForm] = useState<FormDefinition | null>(null)
  const [submissions, setSubmissions] = useState<Submission[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedRow, setExpandedRow] = useState<string | null>(null)

  const fetchData = useCallback(async () => {
    if (!formId) return
    setLoading(true)
    setError(null)
    try {
      const [formData, subsData] = await Promise.all([
        api.getForm(formId),
        api.getSubmissions(formId),
      ])
      setForm(formData)
      setSubmissions(subsData)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data')
    } finally {
      setLoading(false)
    }
  }, [formId])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const columns = useMemo(() => (form ? extractColumns(form) : []), [form])

  const handleExportCsv = useCallback(() => {
    const headers = ['Submission ID', 'Submitted At', ...columns.map((c) => c.label)]
    const rows = submissions.map((sub) => [
      sub.id,
      formatTimestamp(sub.submittedAt),
      ...columns.map((col) => sub.values[col.fieldId] ?? ''),
    ])

    const csvContent = [
      headers.map(escapeCsvCell).join(','),
      ...rows.map((row) => row.map(escapeCsvCell).join(',')),
    ].join('\n')

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${formId}_submissions.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  }, [columns, submissions, formId])

  const toggleRow = (id: string) => {
    setExpandedRow((prev) => (prev === id ? null : id))
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full min-h-[400px]">
        <div className="flex flex-col items-center gap-3">
          <svg
            className="animate-spin h-8 w-8 text-blue-600"
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
              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
            />
          </svg>
          <span className="text-sm text-gray-500">Loading submissions...</span>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center h-full min-h-[400px]">
        <div className="text-center">
          <div className="text-red-500 text-lg font-medium mb-2">Error loading data</div>
          <p className="text-gray-500 text-sm mb-4">{error}</p>
          <button
            onClick={fetchData}
            className="px-4 py-2 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 transition-colors"
          >
            Retry
          </button>
        </div>
      </div>
    )
  }

  if (submissions.length === 0) {
    return (
      <div className="p-6">
        <div className="mb-6">
          <button
            onClick={() => navigate('/admin')}
            className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
            Back to Dashboard
          </button>
        </div>
        <div className="flex flex-col items-center justify-center py-20">
          <svg
            className="w-16 h-16 text-gray-300 mb-4"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={1}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <h2 className="text-lg font-medium text-gray-700 mb-1">No submissions yet</h2>
          <p className="text-sm text-gray-400 mb-4">
            This form hasn't received any submissions.
          </p>
          <Link
            to="/admin"
            className="text-sm text-blue-600 hover:text-blue-700 hover:underline"
          >
            Back to dashboard
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div className="p-6 bg-gray-100 min-h-full">
      {/* Top bar */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/admin')}
            className="inline-flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
            Back
          </button>
          <div>
            <h1 className="text-xl font-semibold text-gray-900">
              {form?.title ?? 'Submissions'}
            </h1>
          </div>
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
            {submissions.length} submission{submissions.length !== 1 ? 's' : ''}
          </span>
        </div>
        <button
          onClick={handleExportCsv}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50 transition-colors shadow-sm"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          CSV Export
        </button>
      </div>

      {/* Scrollable table container */}
      <div className="bg-white rounded-lg shadow overflow-auto max-h-[calc(100vh-160px)]">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50 sticky top-0 z-10">
            <tr>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                Submission ID
              </th>
              <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap">
                Submitted At
              </th>
              {columns.map((col) => (
                <th
                  key={col.fieldId}
                  className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider whitespace-nowrap"
                >
                  {col.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
            {submissions.map((sub, idx) => (
              <SubmissionRow
                key={sub.id}
                submission={sub}
                columns={columns}
                index={idx}
                isExpanded={expandedRow === sub.id}
                onToggle={() => toggleRow(sub.id)}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

interface SubmissionRowProps {
  submission: Submission
  columns: ColumnDef[]
  index: number
  isExpanded: boolean
  onToggle: () => void
}

function SubmissionRow({ submission, columns, index, isExpanded, onToggle }: SubmissionRowProps) {
  const rowBg = index % 2 === 0 ? 'bg-white' : 'bg-gray-50'
  const totalCols = columns.length + 2

  return (
    <>
      <tr
        className={`${rowBg} hover:bg-blue-50 cursor-pointer transition-colors`}
        onClick={onToggle}
      >
        <td className="px-4 py-3 whitespace-nowrap">
          <span className="text-xs font-mono text-gray-600" title={submission.id}>
            {truncateId(submission.id)}
          </span>
        </td>
        <td className="px-4 py-3 whitespace-nowrap text-sm text-gray-700">
          {formatTimestamp(submission.submittedAt)}
        </td>
        {columns.map((col) => {
          const value = submission.values[col.fieldId]
          return (
            <td key={col.fieldId} className="px-4 py-3 whitespace-nowrap text-sm">
              {value ? (
                <span className="text-gray-900">{value}</span>
              ) : (
                <span className="text-gray-300">&mdash;</span>
              )}
            </td>
          )
        })}
      </tr>
      {isExpanded && (
        <tr className="bg-blue-50/50">
          <td colSpan={totalCols} className="px-6 py-4">
            <dl className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-x-6 gap-y-3">
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase">Submission ID</dt>
                <dd className="mt-0.5 text-sm font-mono text-gray-800">{submission.id}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium text-gray-500 uppercase">Submitted At</dt>
                <dd className="mt-0.5 text-sm text-gray-800">
                  {formatTimestamp(submission.submittedAt)}
                </dd>
              </div>
              {columns.map((col) => {
                const value = submission.values[col.fieldId]
                return (
                  <div key={col.fieldId}>
                    <dt className="text-xs font-medium text-gray-500 uppercase">{col.label}</dt>
                    <dd className="mt-0.5 text-sm text-gray-800">
                      {value || <span className="text-gray-300">&mdash;</span>}
                    </dd>
                  </div>
                )
              })}
            </dl>
          </td>
        </tr>
      )}
    </>
  )
}
