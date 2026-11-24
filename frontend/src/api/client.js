// Thin wrapper around fetch for the LeaseLens REST API.
// All analytical results come from the Java backend; this module never computes them.

export class ApiError extends Error {
  constructor(status, problem) {
    super(problem?.detail || problem?.title || `Request failed with status ${status}`)
    this.status = status
    this.problem = problem
  }
}

export async function apiGet(path, { signal } = {}) {
  const response = await fetch(`/api${path}`, { headers: { Accept: 'application/json' }, signal })
  const isJson = (response.headers.get('content-type') || '').includes('json')
  const body = isJson ? await response.json() : null
  if (!response.ok) throw new ApiError(response.status, body)
  return body
}
