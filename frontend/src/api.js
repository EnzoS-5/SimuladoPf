export function createApi(baseUrl = import.meta.env.VITE_API_URL || '/api') {
  const base = baseUrl.replace(/\/$/, '');
  async function request(method, token, body, signal) {
    const response = await fetch(`${base}/avaliacoes`, {
      method, signal,
      headers: { Authorization: `Bearer ${token}`, ...(body ? { 'Content-Type': 'application/json' } : {}) },
      ...(body ? { body: JSON.stringify(body) } : {})
    });
    const contentType = response.headers.get('content-type') || '';
    const payload = contentType.includes('json') ? await response.json() : null;
    if (!response.ok) {
      const defaults = { 401: 'Sua sessão não é válida. Entre novamente.', 403: 'Seu perfil não tem permissão para esta ação.' };
      throw new Error(payload?.message || defaults[response.status] || `Não foi possível concluir a operação (HTTP ${response.status}).`);
    }
    if (method === 'GET' && !Array.isArray(payload)) throw new Error('Resposta inesperada da API. Confira a URL do back-end.');
    return payload;
  }
  return { list: (token, signal) => request('GET', token, null, signal), create: (token, body) => request('POST', token, body) };
}
