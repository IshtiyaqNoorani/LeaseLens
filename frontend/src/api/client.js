// Thin wrapper around fetch for the LeaseLens REST API.
// All analytical results come from the Java backend; this module never computes them.

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || problem?.title || `Request failed with status ${status}`)
    this.status = status
    this.problem = problem
  }
}

async function request(method, path, { body, form, signal } = {}) {
  const init = { method, headers: { Accept: 'application/json, application/problem+json' }, signal }
  if (body !== undefined) {
    init.headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(body)
  }
  if (form) init.body = form
  let response
  try {
    response = await fetch(`/api${path}`, init)
  } catch (e) {
    if (e.name === 'AbortError') throw e
    throw new ApiError(0, { title: 'Backend unreachable', detail: 'The Java API is not responding. Start it (see README) and retry.' })
  }
  if (response.status === 204) return null
  const isJson = (response.headers.get('content-type') || '').includes('json')
  const data = isJson ? await response.json() : null
  if (!response.ok) throw new ApiError(response.status, data)
  return data
}

export const apiGet = (path, opts) => request('GET', path, opts)
export const apiPost = (path, body) => request('POST', path, { body })
export const apiPut = (path, body) => request('PUT', path, { body })
export const apiDelete = (path) => request('DELETE', path)
export const apiUpload = (path, form) => request('POST', path, { form })

/** Builds a query string, skipping empty values. */
export function qs(params) {
  const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== '')
  return entries.length ? `?${new URLSearchParams(entries).toString()}` : ''
}
