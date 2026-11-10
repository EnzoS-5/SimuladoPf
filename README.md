# Simulado PF — Sistema de Avaliações

Implementação do enunciado **Simulado PF ProjSoft**: API Spring Boot/PostgreSQL e front React com Auth0. O exercício de 14/09 em `EnzoS-5/exercicio` foi usado como referência de camadas, testes, cobertura e deploy. Ele não precisa ser alterado para executar este projeto.

## Requisitos implementados

| Requisito | Implementação |
| --- | --- |
| POST /avaliacoes — ADMIN | Spring Security valida token e role no servidor |
| GET /avaliacoes — ADMIN e USER | Listagem protegida e ordenada da mais recente para a mais antiga |
| Avaliação | id, emailAvaliador, emailAvaliado, comentarios, nota e dataAvaliacao |
| Nota de 0 a 10 | Validação no servidor e formulário; aceita até duas casas decimais |
| Front de listagem e cadastro | `frontend`, com login/logout Auth0; USER tem leitura e ADMIN também cadastra |
| Deploy do back na AWS por Actions | `.github`, após testes do back e do front |
| Deploy do front | Vite/Vercel; configuração abaixo |

O avaliador vem da claim do **access token validado**, e a data UTC vem do servidor. O POST não aceita `id`, `emailAvaliador` ou `dataAvaliacao` enviados pelo cliente. A aplicação impede avaliar a si mesmo, conforme a descrição de avaliar outras pessoas. Claims e nomes dos campos são os mesmos no front e no back.

**Não há bypass de autenticação**, nem modo de demonstração com token fictício. Testes usam identidades de teste, isoladas da configuração de produção. O projeto inicial declarava Java 27 e Maven 3.10.0; a implementação alinha build, Docker e CI em Java 21 e Maven 3.9.11, com Spring Boot 3.5.7.

## 1 Configurar Auth0

1. Crie uma Application **Single Page Application**. Anote Domain e Client ID.
2. Cadastre `http://localhost:5173` em **Allowed Callback URLs**, **Allowed Logout URLs** e **Allowed Web Origins**. Acrescente a origem HTTPS da Vercel quando publicar.
3. Crie uma API para este sistema. Use seu **Identifier**, por exemplo `https://simuladopf-api`, como audience. Não use a Management API do Auth0.
4. Crie as roles **ADMIN** e **USER**, e atribua aos usuários correspondentes. Um usuário sem esses perfis não tem acesso às avaliações.
5. Publique a Action de `deploy` e conecte ao fluxo **Post Login**. Ela inclui `https://social-insper.com/email` e `https://social-insper.com/roles` no access token.
6. Após alterar roles ou a Action, saia e entre para obter um token novo.

O back verifica assinatura, issuer, audience e expiração. A inspeção de roles no navegador controla a interface; a autorização efetiva permanece no servidor.

## 2 Executar localmente

Pré-requisitos: Docker Engine/Desktop e Compose v2; Node 22.12+ (linha 22). Java 21 é necessário para executar Maven fora do Docker.

Na raiz do projeto, copie `.env.example` para `.env` e preencha `DB_PASSWORD`, `AUTH0_ISSUER` (URL do tenant com `/` final) e `AUTH0_AUDIENCE`. Em Git Bash:

```bash
cp .env.example .env
# Edite .env antes de continuar.
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 api
```

O PostgreSQL fica interno, com volume persistente; a API fica em `http://localhost:8080`. O issuer deve ser acessível por HTTPS para descoberta das chaves públicas. Uma chamada sem token a `/avaliacoes` deve retornar **401**.

Em outro terminal:

```bash
cd frontend
cp .env.example .env.local
# Preencha domain, clientId e audience do mesmo tenant/API do back.
npm ci
npm run dev
```

Abra `http://localhost:5173`. O Vite encaminha `/api` à API local. Autentique como ADMIN para cadastrar e como USER para conferir acesso somente de leitura. A tela orienta a configurar Auth0 se as variáveis estiverem ausentes. Não coloque client secret em variáveis `VITE_`: elas são públicas.

`docker compose down` preserva o banco; `down -v` apaga seus dados. Trocar a senha no `.env` não muda a senha de um volume PostgreSQL já inicializado. Para executar Spring diretamente, exporte as variáveis e publique o PostgreSQL de desenvolvimento em uma porta local; não reutilize banco de produção para testes.

## 3 Contrato HTTP e Postman

Importe `SimuladoPF.postman_collection.json`. Configure `baseUrl`, `adminToken` e `userToken` no seu ambiente local do Postman. Use **access tokens** emitidos para a audience da API; não use ID token. Não exporte credenciais preenchidas.

POST `/avaliacoes` com `Authorization: Bearer ACCESS_TOKEN_ADMIN`:

```json
{"emailAvaliado":"pessoa@empresa.com","comentarios":"Boa entrega e colaboração.","nota":8.5}
```

Retorna **201**, header Location e JSON com os seis campos. GET retorna **200** e uma lista. E-mails são normalizados para minúsculas; comentários são preservados, removendo espaços nas extremidades. Data é ISO 8601 em UTC.

| Cenário | Status |
| --- | --- |
| Ausência de token, token inválido/expirado, issuer/audience incorretos | 401 |
| USER tenta POST, perfil ausente/desconhecido | 403 |
| ADMIN sem claim de e-mail tenta POST | 403 com orientação para Action |
| Nota fora de 0–10, campo inválido, JSON malformado, autoavaliação | 400 |
| Origem CORS não cadastrada | 403 no preflight |

## 4 Testes e cobertura

```bash
./mvnw -B clean verify
# No Git Bash, após o verify:
start target/site/jacoco/index.html
cd frontend
npm ci
npm test
npm run build
```

Os testes locais usam H2 em modo PostgreSQL para controller + persistência e testes de unidade do serviço. Os testes criptográficos criam um emissor e JWKS locais e verificam assinatura, emissor, audience e expiração sem acessar o Auth0 real. O JaCoCo exige **100% de linhas e ramos do AvaliacaoService**, seguindo o padrão de qualidade do exercício-base (o enunciado final não fixa esse percentual).

O CI inicia PostgreSQL 17 e repete a suíte HTTP contra o banco real por `AvaliacaoPostgresIT`/Failsafe. Para executar esse teste fora do Actions, forneça um PostgreSQL de teste vazio e estas variáveis:

```bash
export RUN_POSTGRES_TESTS=true
export IT_DB_URL=jdbc:postgresql://localhost:5432/avaliacoes_test
export IT_DB_USER=test
export IT_DB_PASSWORD=test
./mvnw -B clean verify
```

**Esse banco é apagado/recriado pela suíte. Use somente banco descartável de teste.** `AvaliacaoPostgresIT` fica explicitamente pulado se `RUN_POSTGRES_TESTS` não estiver ativo. O resultado local não deve ser confundido com uma execução no PostgreSQL quando ele estiver pulado.

O front testa login, logout, Bearer, leitura de USER, cadastro de ADMIN, bloqueio sem perfil e mensagem de erro. O pipeline de PR verifica back e front; o deploy na main chama a mesma suíte antes de publicar a imagem.

## 5 Preparar AWS e GitHub Actions

Use uma instância Ubuntu com Docker Engine e plugin Compose v2. Instruções: https://docs.docker.com/engine/install/ubuntu/ . Adicione o usuário autorizado ao grupo docker e reconecte. A máquina também precisa de Python 3 e curl. Não publique PostgreSQL na internet.

O deploy usa `/home/ubuntu/simuladopf`, projeto Compose `simuladopf` e porta **8082** por padrão, pois o exercício de cursos pode ocupar 8080. Use a Variable `APP_PORT` para outro valor e ajuste também o rewrite do front e a regra do Security Group. Libere SSH 22 somente às origens necessárias e a porta da API às origens previstas no laboratório.

Em **Settings → Secrets and variables → Actions** deste repositório:

| Nome | Tipo | Conteúdo |
| --- | --- | --- |
| DOCKERHUB_USERNAME | Variable ou Secret | Seu usuário do Docker Hub |
| DOCKERHUB_TOKEN | Secret | Token de publicação de imagem |
| HOST_TEST | Secret | IP ou DNS público atual da instância |
| KEY_TEST | Secret | PEM completa associada à instância |
| SSH_USER | Secret opcional | ubuntu por padrão |
| DB_NAME | Secret | Nome do banco do simulado |
| DB_USER | Secret | Usuário PostgreSQL |
| DB_PASSWORD | Secret | Senha PostgreSQL |
| AUTH0_ISSUER | Variable ou Secret | https://SEU_TENANT.us.auth0.com/ |
| AUTH0_AUDIENCE | Variable ou Secret | Identifier da API |
| FRONTEND_ORIGINS | Variable ou Secret | http://localhost:5173,https://SEU_FRONT.vercel.app |
| APP_PORT | Variable opcional | 8082 por padrão |

Reutilize os valores privados enviados anteriormente **no cadastro dos Secrets**, não em arquivos versionados: o repositório é público. DB_HOST e DB_PORT não precisam de Secrets neste Compose: a conexão interna usa `postgres:5432`.

Após push na main, o workflow testa as duas aplicações, publica `USUARIO/avaliacoes-api:SHA_DO_COMMIT`, copia o Compose, grava `.env` privado na AWS, sobe banco/API e espera `/avaliacoes` responder **401 sem token**. Esse check verifica disponibilidade e proteção da rota; teste também com access token real para concluir a validação de login e perfis.

Para inspecionar a instância:

```bash
cd /home/ubuntu/simuladopf
docker compose -p simuladopf -f compose.yml ps
docker compose -p simuladopf -f compose.yml logs --tail 100 api
```

O workflow precisa de credenciais válidas e de conectividade SSH a partir do runner. Não há publicação feita apenas por adicionar os arquivos. Para rollback, restaure `IMAGE` no `.env` para uma tag anterior e execute pull/up preservando volumes. Alterações de schema não são revertidas pela imagem.

## 6 Publicar front na Vercel

1. Importe o repositório **SimuladoPf**. Configure **Root Directory: frontend**, preset Vite, build `npm run build`, saída `dist`.
2. Cadastre `VITE_AUTH0_DOMAIN`, `VITE_AUTH0_CLIENT_ID`, `VITE_AUTH0_AUDIENCE` e `VITE_API_URL=/api`.
3. Edite `frontend`: substitua `SEU_HOST_AWS` pelo host do backend e ajuste a porta, se alterada. O destino fica, por exemplo, `http://SEU_HOST_AWS:8082/:path*`. O placeholder precisa ser substituído antes de publicar.
4. Adicione a URL HTTPS efetiva da Vercel às três listas de URLs do Auth0 e a FRONTEND_ORIGINS. Reexecute o deploy da API se essa variável mudar.
5. Faça deploy e teste ADMIN, USER e conta sem perfil. Confirme no Network que as chamadas levam Bearer e que USER recebe 403 tentando POST diretamente.

O rewrite HTTP corresponde à infraestrutura de laboratório. Para implantação protegida, configure HTTPS também no backend e use destino HTTPS; HTTPS na Vercel não criptografa o salto até um backend HTTP. Com URL HTTPS própria para a API, outra opção é `VITE_API_URL=https://api.seudominio` e CORS restrito a seu front. Variáveis VITE são incorporadas no build; alterá-las exige novo deploy.

## Estrutura

- `src`: configuração de segurança, entidade, DTO, repository, service, controller e tratamento de erros.
- `src`: testes unitários, HTTP/persistência e assinatura JWT.
- `frontend`: interface React, Auth0, testes e configuração Vercel.
- `deploy`: Compose remoto e Action Post Login do Auth0.
- `.github`: testes e deploy.

## Limites da entrega

Execução local de testes/build não configura seu tenant Auth0, Secrets, AWS nem Vercel automaticamente. Consulte `docs` para os comandos realmente executados e as etapas externas ainda pendentes.
