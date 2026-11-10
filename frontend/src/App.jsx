import { useEffect, useState } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { jwtDecode } from 'jwt-decode';
import { createApi } from './api.js';
const api = createApi();
const rolesClaim = import.meta.env.VITE_AUTH0_ROLES_CLAIM || 'https://social-insper.com/roles';
const emailClaim = import.meta.env.VITE_AUTH0_EMAIL_CLAIM || 'https://social-insper.com/email';
const dateFormat = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium', timeStyle: 'short' });
const scoreFormat = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 });

export default function App() {
  const { isAuthenticated, isLoading, user, error: loginError, loginWithRedirect, logout, getAccessTokenSilently } = useAuth0();
  const [session, setSession] = useState(null);
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [form, setForm] = useState({ emailAvaliado: '', comentarios: '', nota: '' });
  const canRead = session?.roles.some(role => ['ADMIN', 'USER'].includes(role));
  const isAdmin = session?.roles.includes('ADMIN');

  useEffect(() => {
    if (!isAuthenticated) return;
    const controller = new AbortController();
    async function load() {
      setBusy(true); setError('');
      try {
        const token = await getAccessTokenSilently();
        // Decodificação só controla a interface; a API valida assinatura e acesso.
        const claims = jwtDecode(token);
        const roles = Array.isArray(claims[rolesClaim]) ? claims[rolesClaim] : [];
        if (controller.signal.aborted) return;
        setSession({ roles, email: claims[emailClaim] || user?.email || '' });
        if (roles.some(role => ['ADMIN', 'USER'].includes(role))) {
          const data = await api.list(token, controller.signal);
          if (!controller.signal.aborted) setItems(data);
        }
      } catch (e) { if (!controller.signal.aborted) setError(e.message); }
      finally { if (!controller.signal.aborted) setBusy(false); }
    }
    load();
    return () => controller.abort();
  }, [isAuthenticated, getAccessTokenSilently, user?.email]);

  async function refresh() {
    setBusy(true); setError('');
    try { setItems(await api.list(await getAccessTokenSilently())); }
    catch (e) { setError(e.message); }
    finally { setBusy(false); }
  }
  async function submit(event) {
    event.preventDefault(); setError(''); setSuccess('');
    const nota = Number(form.nota);
    if (form.nota.trim() === '' || !Number.isFinite(nota) || nota < 0 || nota > 10) {
      setError('Informe uma nota entre 0 e 10.'); return;
    }
    if (form.emailAvaliado.trim().toLowerCase() === session?.email.toLowerCase()) {
      setError('Avalie outra pessoa; autoavaliação não é permitida.'); return;
    }
    setSaving(true);
    try {
      const saved = await api.create(await getAccessTokenSilently(), {
        emailAvaliado: form.emailAvaliado.trim(), comentarios: form.comentarios.trim(), nota
      });
      setItems(previous => [saved, ...previous]);
      setForm({ emailAvaliado: '', comentarios: '', nota: '' });
      setSuccess('Avaliação cadastrada com sucesso.');
    } catch (e) { setError(e.message); }
    finally { setSaving(false); }
  }
  const average = items.length ? scoreFormat.format(items.reduce((sum, item) => sum + Number(item.nota), 0) / items.length) : '—';
  const people = new Set(items.map(item => item.emailAvaliado)).size;
  return <>
    <header className="topbar"><a className="brand" href="/" aria-label="Avalia início">a<span>·</span> avalia</a><span className="top-label">DESENVOLVIMENTO DE PESSOAS</span>
      {isAuthenticated && <div className="account"><span>{user?.name || user?.email}</span><button className="button-quiet" onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>Sair ↗</button></div>}
    </header>
    {!isAuthenticated ? <main className="landing">
      <section><span className="eyebrow">FEEDBACK QUE CONSTRÓI</span><h1>Boas conversas.<br/><em>Novas perspectivas.</em></h1><p>Um espaço para reconhecer contribuições e transformar avaliações em desenvolvimento.</p><button className="button-primary" disabled={isLoading} onClick={() => loginWithRedirect()}>{isLoading ? 'Carregando…' : 'Entrar na plataforma →'}</button>{loginError && <p role="alert">{loginError.message}</p>}<small>ACESSO SEGURO COM AUTH0</small></section>
      <aside className="landing-art" aria-hidden="true"><span className="art-label">CRESCER É UM PROCESSO COLETIVO</span><div className="orbit orbit-one"/><div className="orbit orbit-two"/><div className="seed"/><span className="art-bottom">Pessoas no centro.<br/>Evolução em movimento.</span></aside>
    </main> : <main className="dashboard">
      <div className="page-title"><div><span className="eyebrow">ESPAÇO DA EQUIPE</span><h1>Avaliações<span className="title-dot">.</span></h1><p>Contribuições de hoje. Desenvolvimento de amanhã.</p></div>{session && <span className="role-pill">{isAdmin ? 'ADMINISTRADOR' : canRead ? 'LEITURA · USER' : 'SEM PERFIL'}</span>}</div>
      {error && <div className="notice error" role="alert">{error}</div>}
      {success && <div className="notice success" role="status">{success}</div>}
      {!session && busy && <p role="status">Conectando sua conta…</p>}
      {session && !canRead && <div className="empty"><h2>Acesso aguardando liberação</h2><p>Peça ao administrador o perfil USER ou ADMIN. Depois, saia e entre novamente.</p></div>}
      {canRead && <>
        <section className="stats" aria-label="Resumo das avaliações"><div><span>AVALIAÇÕES REGISTRADAS</span><strong>{items.length.toString().padStart(2, '0')}</strong></div><div><span>NOTA MÉDIA</span><strong>{average}<small> / 10</small></strong></div><div><span>PESSOAS AVALIADAS</span><strong>{people.toString().padStart(2, '0')}</strong></div></section>
        <div className={`workspace ${isAdmin ? '' : 'read-only'}`}>
          <section className="list-panel"><div className="section-head"><div><span className="eyebrow">HISTÓRICO</span><h2>O olhar da equipe</h2></div><button className="button-quiet" disabled={busy || saving} onClick={refresh}>{busy ? 'Atualizando…' : '↻ Atualizar'}</button></div>
            {!isAdmin && <p className="readonly-note">Seu perfil permite consultar as avaliações. Cadastros são feitos por administradores.</p>}
            {busy && !items.length ? <div className="empty" role="status">Carregando avaliações…</div> : !items.length ? <div className="empty"><span className="empty-symbol">↗</span><h3>A primeira perspectiva começa aqui.</h3><p>{isAdmin ? 'Cadastre uma avaliação no formulário ao lado.' : 'As avaliações aparecerão aqui quando forem cadastradas.'}</p></div> : <ol className="evaluation-list">{items.map(item => <li key={item.id}><div className="evaluation-heading"><div className="avatar" aria-hidden="true">{item.emailAvaliado.slice(0, 1).toUpperCase()}</div><div className="person"><h3>{item.emailAvaliado}</h3><p>Avaliado por {item.emailAvaliador}</p></div><span className="score">{scoreFormat.format(item.nota)}<small>/10</small></span></div><p className="comment">{item.comentarios}</p><div className="evaluation-footer"><span>AVALIAÇÃO #{item.id}</span><time dateTime={item.dataAvaliacao}>{dateFormat.format(new Date(item.dataAvaliacao))}</time></div></li>)}</ol>}
          </section>
          {isAdmin && <aside className="form-panel"><span className="eyebrow">UMA NOVA PERSPECTIVA</span><h2>Registrar avaliação</h2><p>Seja específico. Um feedback útil abre caminhos.</p><form onSubmit={submit}>
            <label htmlFor="emailAvaliado">E-mail da pessoa avaliada</label><input id="emailAvaliado" type="email" required maxLength={254} placeholder="pessoa@empresa.com" value={form.emailAvaliado} onChange={e => setForm({ ...form, emailAvaliado: e.target.value })}/>
            <label htmlFor="nota">Nota <span>de 0 a 10</span></label><input id="nota" type="number" min="0" max="10" step="0.01" required placeholder="Ex.: 8,5" value={form.nota} onChange={e => setForm({ ...form, nota: e.target.value })}/>
            <label htmlFor="comentarios">Comentários</label><textarea id="comentarios" required maxLength={4000} rows={5} placeholder="Quais contribuições você reconhece? O que pode evoluir?" value={form.comentarios} onChange={e => setForm({ ...form, comentarios: e.target.value })}/><div className="char-count">{form.comentarios.length}/4000</div>
            <p className="identity-note">Registrado por <strong>{session.email}</strong>. A data é definida automaticamente.</p><button className="button-primary" disabled={saving || busy} type="submit">{saving ? 'Salvando…' : 'Publicar avaliação ↗'}</button>
          </form></aside>}
        </div>
      </>}
    </main>}
    <footer className="footer"><span>avalia</span><span>Clareza para reconhecer. Espaço para evoluir.</span><span>PROJETO DE SOFTWARE · SIMULADO PF</span></footer>
  </>;
}
