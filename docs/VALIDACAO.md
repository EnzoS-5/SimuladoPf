# Validação da implementação

## Executado neste ambiente

| Verificação | Resultado |
| --- | --- |
| Maven — unidade, controller/persistência H2 e JWT criptográfico | 33 testes passaram |
| Maven Failsafe — rotas e autorização com PostgreSQL 17 real | 21 testes passaram |
| JaCoCo AvaliacaoService | 100% de linhas e 100% de ramos |
| Front — Vitest/Testing Library | 6 testes passaram |
| Front — npm run build | Compilou a distribuição de produção |
| Navegador desktop e mobile | Landing real e dashboard com fixtures conferidos, sem overflow horizontal |
| Interação no navegador com fixtures | ADMIN cadastra e vê novo registro; USER não recebe formulário; sem erros de JavaScript |
| YAML e JSON | Workflows, Compose e collection analisados sintaticamente |
| git diff --check | Sem erros de whitespace |

**Total: 60 testes automatizados passaram**, além da verificação de interface no navegador.

O PostgreSQL utilizado foi um processo descartável 17.11, em diretório temporário, isolado dos dados de uso. A suíte de integração executou todos os casos com zero falhas, erros ou testes pulados. O CI usa PostgreSQL 17 em serviço Docker e as mesmas variáveis IT_DB_*.

O runtime Java 21 deste ambiente não incluía `lib/ct.sym`. Para a execução local, Maven recebeu `-Dmaven.compiler.release= -Dmaven.compiler.source=21 -Dmaven.compiler.target=21`, mantendo linguagem e bytecode Java 21. Também foi usada uma configuração de proxy do ambiente para baixar dependências. Essas adaptações não foram gravadas no pom.xml: o comando normal do projeto e do CI é `./mvnw -B clean verify` em um JDK 21 completo.

As identidades e os registros dos testes de interface foram simulados somente no ambiente de QA. Esse mock não faz parte da aplicação entregue. A validação de assinatura JWT foi executada separadamente com RSA/JWKS local e tokens válidos, expirados, com outro emissor, outra audience, audience ausente e outra assinatura. Isso não equivale a configurar ou testar um tenant Auth0 do usuário.

## Etapas externas ainda necessárias

- Publicar o commit no GitHub com uma sessão que tenha permissão de escrita.
- Configurar tenant/API/SPA, roles, usuários e Action Post Login do Auth0.
- Cadastrar Secrets e Variables do repositório.
- Executar Actions e conferir a imagem Docker e a API na AWS.
- Substituir o host do rewrite e publicar o front na Vercel.
- Validar login real e fluxos de ADMIN/USER nas URLs publicadas.

Docker Engine não estava disponível neste ambiente para construir/rodar a imagem. A aplicação Java foi compilada e empacotada pelo Maven; PostgreSQL real e front foram testados como descrito acima. Os workflows não foram executados aqui e o deploy externo não deve ser considerado concluído antes dessas etapas.
