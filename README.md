# DevReady Backend

API e worker da DevReady, uma plataforma para treinar entrevistas técnicas de TI. A pessoa diz onde quer trabalhar, com qual stack e em qual senioridade, recebe uma trilha com o que essas empresas cobram e pratica com questões objetivas, desafios de código executados num sandbox e simulações com tempo marcado, nota e feedback por competência.

Este repositório tem o backend. A interface fica no `devready-frontend`, e a pesquisa de vagas que deu origem às trilhas, no `devready-research`.

## Stack

Java 21 e Spring Boot 4.1, num projeto Maven com um módulo por camada. PostgreSQL 17 com Flyway, Redis 8 para os refresh tokens, RabbitMQ 4 para a fila de envios de código e Judge0 1.13.1 para executar o código da pessoa. A API atende HTTP; o worker, outro executável do mesmo código, consome a fila, fala com o Judge0 e corrige as simulações. Testes com JUnit 5, AssertJ, Testcontainers, WireMock e ArchUnit; formatação com Spotless (Google Java Format, estilo AOSP) e cobertura mínima com JaCoCo.

## Rodando localmente

Precisa de Docker (com 4 GB de memória para os contêineres) e Java 21. O Maven vem pelo `./mvnw`.

```bash
docker compose up -d                 # PostgreSQL, Redis e RabbitMQ, só em 127.0.0.1
./mvnw -q install -DskipTests        # instala os módulos no repositório local do Maven
./mvnw -pl api spring-boot:run       # API em http://localhost:8080
./mvnw -pl worker spring-boot:run    # worker, em outro terminal
curl localhost:8080/actuator/health
```

No macOS, se `java -version` mostrar outra versão: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`. Rode o `install` de novo sempre que mudar `domain`, `application` ou `infrastructure`.

Na primeira subida, a API aplica as migrações e carrega o conteúdo dos YAML em `infrastructure/src/main/resources/seed/` (áreas, questões, desafios, empresas, stacks, competências, materiais e trilhas). O `spring-boot:run` usa o perfil `dev`, o único (além do `test`) que aceita o `JWT_SECRET` de exemplo.

Rodar e enviar desafios precisa do Judge0, que fica atrás de um perfil porque pesa (a imagem tem 10,5 GB e só existe para amd64):

```bash
docker compose --profile judge0 up -d judge0-db judge0-redis judge0-server judge0-workers
curl -s 127.0.0.1:2358/languages     # têm de aparecer os ids 62, 71 e 63
```

### Variáveis

Em desenvolvimento nenhuma é obrigatória. As que importam fora do padrão:

| Variável | Quem lê | Para quê | Padrão |
|---|---|---|---|
| `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` | API e worker | PostgreSQL | `jdbc:postgresql://localhost:5432/devready`, `devready`, `devready` |
| `REDIS_URL` | API e worker | Redis | `redis://localhost:6379` |
| `RABBITMQ_URL` | API e worker | RabbitMQ | `amqp://devready:devready@localhost:5672` |
| `JWT_SECRET` | API e worker | assina os access tokens; 32 caracteres ou mais | um valor de exemplo, recusado fora dos perfis `dev` e `test` |
| `FRONTEND_URL` | API | para onde o login social volta | `http://localhost:5173` |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | API (`devready.social.*`) | login social; cada provedor só é ligado com o id e o segredo preenchidos | vazias: login social desligado, a API sobe normalmente e a tela não mostra os botões |
| `DEVREADY_ADMIN_EMAIL`, `DEVREADY_ADMIN_PASSWORD` | API | cria o primeiro administrador na subida, uma vez; senha fraca impede a subida | vazias: não cria ninguém |
| `SERVER_PORT` | API | porta HTTP | `8080` |
| `JUDGE0_URL` | worker | endereço do Judge0 | `http://localhost:2358` |
| `SPRING_PROFILES_ACTIVE` | API e worker | `dev` aceita o `JWT_SECRET` de exemplo | nenhum no jar; `dev` no `spring-boot:run` |

O `.env.example` traz as variáveis do compose de produção (`docker-compose.prod.yml`), que sobe a plataforma inteira a partir das imagens publicadas.

## Testes e formatação

```bash
./mvnw spotless:apply    # formata o código
./mvnw verify            # testes, ArchUnit, JaCoCo e a checagem do Spotless
```

O `verify` sobe PostgreSQL, Redis e RabbitMQ em contêineres pelo Testcontainers, então o Docker precisa estar de pé. Dois grupos ficam fora dele: `runtimes` roda o harness das três linguagens com o `javac`, o `python3` e o `node` da máquina, e `judge0` manda as soluções de referência ao Judge0 de verdade.

```bash
./mvnw -B -pl infrastructure test -Dgroups=runtimes -Ddevready.excludedGroups=none -Dtest=HarnessRuntimeTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw -B -pl infrastructure test -Dgroups=judge0 -Ddevready.excludedGroups=none -Dtest=ReferenceSolutionsJudge0Test -Dsurefire.failIfNoSpecifiedTests=false
```

## Estrutura

| Pasta | O que tem |
|---|---|
| `domain/` | regras de negócio puras, sem framework |
| `application/` | casos de uso, portas e DTOs, por contexto |
| `infrastructure/` | JPA, Flyway, Redis, RabbitMQ, cliente do Judge0, harness das linguagens, seed |
| `api/` | controllers REST, segurança, limite de requisições e erros em `ProblemDetail` |
| `worker/` | listener da fila de envios e agendador das simulações |
| `architecture-tests/` | regras do ArchUnit: a dependência só aponta para dentro |
| `docker/` | Dockerfile da API e do worker, configuração do Judge0 |

A documentação completa (arquitetura, decisões, referência da API, banco de dados, infraestrutura e conteúdo) acompanha o trabalho de conclusão de curso.
