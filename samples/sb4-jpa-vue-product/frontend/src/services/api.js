const API_BASE_URL = '/api'

export class ApiError extends Error {
  constructor(message, status, problem = null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

export async function request(path, options = {}) {
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  } catch (error) {
    throw new ApiError(`Could not reach the server: ${error.message}`, 0)
  }

  if (response.status === 204) return null

  const contentType = response.headers.get('content-type') ?? ''
  const payload = contentType.includes('json') ? await response.json().catch(() => null) : null
  if (!response.ok) {
    throw new ApiError(
      payload?.detail ?? payload?.title ?? `Request failed with status ${response.status}`,
      response.status,
      payload,
    )
  }
  return payload
}
