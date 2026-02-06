import { useState, useEffect, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Editor, { type OnMount } from '@monaco-editor/react'
import { api } from '../api'
import type { FormDefinition, FormElement, Page } from '../types'

const BLANK_TEMPLATE: FormDefinition = {
  formId: 'new_form',
  title: 'New Form',
  description: '',
  pages: [
    {
      pageId: 'page_1',
      title: 'Page 1',
      elements: [],
    },
  ],
}

const TYPE_BADGE_COLORS: Record<string, string> = {
  text_field: 'bg-blue-100 text-blue-800',
  dropdown: 'bg-green-100 text-green-800',
  radio: 'bg-green-100 text-green-800',
  multi_select: 'bg-green-100 text-green-800',
  checkbox: 'bg-purple-100 text-purple-800',
  toggle: 'bg-purple-100 text-purple-800',
  slider: 'bg-orange-100 text-orange-800',
  number_field: 'bg-orange-100 text-orange-800',
  section_header: 'bg-gray-100 text-gray-800',
  label: 'bg-gray-100 text-gray-800',
  file_upload: 'bg-red-100 text-red-800',
  signature: 'bg-red-100 text-red-800',
  date_picker: 'bg-teal-100 text-teal-800',
  repeating_group: 'bg-indigo-100 text-indigo-800',
}

function TypeBadge({ type }: { type: string }) {
  const colors = TYPE_BADGE_COLORS[type] ?? 'bg-gray-100 text-gray-600'
  return (
    <span
      className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${colors}`}
    >
      {type}
    </span>
  )
}

function ElementPreview({ element }: { element: FormElement }) {
  return (
    <div className="flex items-start gap-3 py-2 px-3 rounded-md hover:bg-gray-50 transition-colors">
      <TypeBadge type={element.type} />
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1">
          <span className="text-sm text-gray-900 truncate">
            {element.label}
          </span>
          {element.required && (
            <span className="text-red-500 text-sm font-bold">*</span>
          )}
        </div>
        {element.options && element.options.length > 0 && (
          <span className="text-xs text-gray-500">
            {element.options.length} option
            {element.options.length !== 1 ? 's' : ''}
          </span>
        )}
        {element.type === 'repeating_group' &&
          element.elements &&
          element.elements.length > 0 && (
            <span className="text-xs text-gray-500">
              {element.elements.length} nested element
              {element.elements.length !== 1 ? 's' : ''}
            </span>
          )}
      </div>
    </div>
  )
}

function PageCard({ page }: { page: Page }) {
  return (
    <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
      <div className="px-4 py-3 bg-gray-50 border-b border-gray-200">
        <h3 className="text-sm font-semibold text-gray-700">{page.title}</h3>
        <span className="text-xs text-gray-400">{page.pageId}</span>
      </div>
      <div className="divide-y divide-gray-100">
        {page.elements.length === 0 ? (
          <p className="px-4 py-6 text-center text-sm text-gray-400 italic">
            No elements
          </p>
        ) : (
          page.elements.map((el, idx) => (
            <ElementPreview key={el.id ?? idx} element={el} />
          ))
        )}
      </div>
    </div>
  )
}

function PreviewPanel({ form }: { form: FormDefinition | null }) {
  if (!form) {
    return (
      <div className="flex items-center justify-center h-full text-gray-400 text-sm">
        Edit JSON to see a live preview
      </div>
    )
  }

  return (
    <div className="p-4 space-y-4 overflow-y-auto h-full">
      <div className="mb-2">
        <h2 className="text-lg font-bold text-gray-900">{form.title}</h2>
        {form.description && (
          <p className="text-sm text-gray-500 mt-1">{form.description}</p>
        )}
        <span className="text-xs text-gray-400">ID: {form.formId}</span>
      </div>
      {form.pages && form.pages.length > 0 ? (
        form.pages.map((page, idx) => (
          <PageCard key={page.pageId ?? idx} page={page} />
        ))
      ) : (
        <p className="text-sm text-gray-400 italic">No pages defined</p>
      )}
    </div>
  )
}

export default function FormEditor() {
  const { formId } = useParams<{ formId: string }>()
  const navigate = useNavigate()

  const isNew = formId === 'new'

  const [jsonText, setJsonText] = useState('')
  const [parsedForm, setParsedForm] = useState<FormDefinition | null>(null)
  const [parseError, setParseError] = useState<string | null>(null)
  const [title, setTitle] = useState('')
  const [loading, setLoading] = useState(!isNew)
  const [saving, setSaving] = useState(false)
  const [feedback, setFeedback] = useState<{
    type: 'success' | 'error'
    message: string
  } | null>(null)
  const [cursorPosition, setCursorPosition] = useState({ line: 1, column: 1 })

  const debounceTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Load form data
  useEffect(() => {
    if (isNew) {
      const text = JSON.stringify(BLANK_TEMPLATE, null, 2)
      setJsonText(text)
      setParsedForm(BLANK_TEMPLATE)
      setTitle(BLANK_TEMPLATE.title)
    } else if (formId) {
      setLoading(true)
      api
        .getForm(formId)
        .then((form) => {
          const text = JSON.stringify(form, null, 2)
          setJsonText(text)
          setParsedForm(form)
          setTitle(form.title)
        })
        .catch(() => {
          setFeedback({
            type: 'error',
            message: `Failed to load form "${formId}"`,
          })
        })
        .finally(() => setLoading(false))
    }
  }, [formId, isNew])

  // Clear feedback after a delay
  useEffect(() => {
    if (feedback) {
      const timer = setTimeout(() => setFeedback(null), 4000)
      return () => clearTimeout(timer)
    }
  }, [feedback])

  const handleEditorChange = useCallback(
    (value: string | undefined) => {
      const text = value ?? ''
      setJsonText(text)

      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current)
      }

      debounceTimer.current = setTimeout(() => {
        try {
          const parsed = JSON.parse(text) as FormDefinition
          setParsedForm(parsed)
          setParseError(null)
          if (parsed.title) {
            setTitle(parsed.title)
          }
        } catch (err) {
          setParseError(
            err instanceof Error ? err.message : 'Invalid JSON'
          )
          setParsedForm(null)
        }
      }, 300)
    },
    []
  )

  const handleEditorMount: OnMount = (editor) => {
    editor.onDidChangeCursorPosition((e) => {
      setCursorPosition({
        line: e.position.lineNumber,
        column: e.position.column,
      })
    })
  }

  const handleTitleChange = useCallback(
    (newTitle: string) => {
      setTitle(newTitle)
      // Sync title into the JSON
      try {
        const parsed = JSON.parse(jsonText) as FormDefinition
        parsed.title = newTitle
        const updated = JSON.stringify(parsed, null, 2)
        setJsonText(updated)
        setParsedForm(parsed)
        setParseError(null)
      } catch {
        // JSON is currently invalid; just update the title input
      }
    },
    [jsonText]
  )

  const handleSave = useCallback(async () => {
    let form: FormDefinition
    try {
      form = JSON.parse(jsonText) as FormDefinition
    } catch {
      setFeedback({ type: 'error', message: 'Cannot save: JSON is invalid' })
      return
    }

    setSaving(true)
    try {
      if (isNew) {
        await api.createForm(form)
      } else if (formId) {
        await api.updateForm(formId, form)
      }
      setFeedback({ type: 'success', message: 'Form saved successfully' })
      setTimeout(() => navigate('/admin/'), 600)
    } catch (err) {
      setFeedback({
        type: 'error',
        message:
          err instanceof Error ? `Save failed: ${err.message}` : 'Save failed',
      })
    } finally {
      setSaving(false)
    }
  }, [jsonText, isNew, formId, navigate])

  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center bg-gray-900 text-gray-300">
        <div className="flex items-center gap-3">
          <svg
            className="animate-spin h-5 w-5 text-blue-400"
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
          Loading form...
        </div>
      </div>
    )
  }

  return (
    <div className="h-screen flex flex-col bg-gray-100">
      {/* Top bar */}
      <div className="flex items-center gap-3 px-4 py-2 bg-white border-b border-gray-200 shadow-sm shrink-0">
        <button
          onClick={() => navigate('/admin/')}
          className="px-3 py-1.5 text-sm font-medium text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
        >
          &larr; Back
        </button>

        <input
          type="text"
          value={title}
          onChange={(e) => handleTitleChange(e.target.value)}
          className="flex-1 text-lg font-semibold text-gray-900 bg-transparent border-b border-transparent hover:border-gray-300 focus:border-blue-500 focus:outline-none px-1 py-0.5 transition-colors"
          placeholder="Form title"
        />

        <div className="flex items-center gap-2">
          {feedback && (
            <span
              className={`text-sm px-3 py-1 rounded-md ${
                feedback.type === 'success'
                  ? 'bg-green-50 text-green-700'
                  : 'bg-red-50 text-red-700'
              }`}
            >
              {feedback.message}
            </span>
          )}

          <button
            onClick={handleSave}
            disabled={saving || !!parseError}
            className={`px-4 py-1.5 text-sm font-medium rounded-md transition-colors ${
              saving || parseError
                ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>

      {/* Split pane */}
      <div className="flex-1 flex min-h-0">
        {/* Left: Monaco editor */}
        <div className="w-1/2 flex flex-col border-r border-gray-300">
          <div className="flex-1 min-h-0">
            <Editor
              language="json"
              theme="vs-dark"
              value={jsonText}
              onChange={handleEditorChange}
              onMount={handleEditorMount}
              options={{
                minimap: { enabled: false },
                fontSize: 13,
                lineNumbers: 'on',
                scrollBeyondLastLine: false,
                automaticLayout: true,
                tabSize: 2,
                wordWrap: 'on',
                formatOnPaste: true,
              }}
            />
          </div>
          {/* Status bar */}
          <div className="flex items-center justify-between px-3 py-1 bg-gray-800 text-xs shrink-0">
            <span className="text-gray-400">
              Ln {cursorPosition.line}, Col {cursorPosition.column}
            </span>
            {parseError ? (
              <span className="text-red-400 truncate max-w-[60%]">
                {parseError}
              </span>
            ) : (
              <span className="text-green-400">Valid JSON</span>
            )}
          </div>
        </div>

        {/* Right: Preview */}
        <div className="w-1/2 bg-white overflow-y-auto">
          <PreviewPanel form={parsedForm} />
        </div>
      </div>
    </div>
  )
}
