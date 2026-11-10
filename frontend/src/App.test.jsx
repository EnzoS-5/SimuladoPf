import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useAuth0 } from '@auth0/auth0-react';
import App from './App.jsx';
vi.mock('@auth0/auth0-react', () => ({ useAuth0: vi.fn() }));
const entry = { id: 1, emailAvaliador: 'admin@empresa.com', emailAvaliado: 'pessoa@empresa.com', comentarios: 'Boa entrega', nota: 9, dataAvaliacao: '2026-11-10T18:00:00Z' };
let auth;
const response = (payload, status = 200) => new Response(JSON.stringify(payload), { status, headers: { 'Content-Type': 'application/json' } });
function login(roles) {
  const payload = btoa(JSON.stringify({ 'https://social-insper.com/roles': roles, 'https://social-insper.com/email': 'admin@empresa.com' }));
  auth = { isAuthenticated: true, isLoading: false, user: { email: 'admin@empresa.com' }, getAccessTokenSilently: vi.fn().mockResolvedValue(`a.${payload}.b`), logout: vi.fn(), loginWithRedirect: vi.fn() };
  useAuth0.mockReturnValue(auth);
}
beforeEach(() => { vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response([]))); });
describe('Fluxo de avaliações', () => {
  it('exige login e não consulta a API anonimamente', async () => {
    login([]); auth.isAuthenticated = false;
    render(<App />);
    await userEvent.click(screen.getByRole('button', { name: /Entrar na plataforma/ }));
    expect(auth.loginWithRedirect).toHaveBeenCalled(); expect(fetch).not.toHaveBeenCalled();
  });
  it('USER consulta com Bearer e não vê formulário de cadastro', async () => {
    login(['USER']); fetch.mockResolvedValue(response([entry])); render(<App />);
    expect(await screen.findByText('Boa entrega')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Publicar avaliação/ })).not.toBeInTheDocument();
    expect(fetch.mock.calls[0][1].headers.Authorization).toMatch(/^Bearer /);
  });
  it('ADMIN cadastra e atualiza a lista sem enviar avaliador nem data', async () => {
    login(['ADMIN']); fetch.mockResolvedValueOnce(response([])).mockResolvedValueOnce(response(entry, 201)); render(<App />);
    const email = await screen.findByLabelText('E-mail da pessoa avaliada');
    await userEvent.type(email, 'pessoa@empresa.com');
    await userEvent.type(screen.getByLabelText(/Nota/), '9');
    await userEvent.type(screen.getByLabelText('Comentários'), 'Boa entrega');
    await userEvent.click(screen.getByRole('button', { name: /Publicar avaliação/ }));
    expect(await screen.findByText('Avaliação cadastrada com sucesso.')).toBeInTheDocument();
    expect(await screen.findByText('Boa entrega')).toBeInTheDocument();
    const post = fetch.mock.calls.find(([, options]) => options.method === 'POST')[1];
    expect(JSON.parse(post.body)).toEqual({ emailAvaliado: 'pessoa@empresa.com', comentarios: 'Boa entrega', nota: 9 });
  });
  it('mostra erro 403 sem anunciar sucesso', async () => {
    login(['ADMIN']); fetch.mockResolvedValue(response({}, 403)); render(<App />);
    expect(await screen.findByRole('alert')).toHaveTextContent('Seu perfil não tem permissão');
    expect(screen.queryByText('Avaliação cadastrada com sucesso.')).not.toBeInTheDocument();
  });
  it('perfil ausente não ganha acesso nem chama a API', async () => {
    login([]); render(<App />);
    expect(await screen.findByText('Acesso aguardando liberação')).toBeInTheDocument();
    expect(fetch).not.toHaveBeenCalled();
  });
  it('encerra a sessão pelo Auth0', async () => {
    login(['USER']); render(<App />);
    await waitFor(() => expect(fetch).toHaveBeenCalled());
    await userEvent.click(screen.getByRole('button', { name: /Sair/ }));
    expect(auth.logout).toHaveBeenCalledWith({ logoutParams: { returnTo: window.location.origin } });
  });
});
