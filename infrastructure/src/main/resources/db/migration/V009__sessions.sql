-- Sessões: a lição (objetivas de uma competência da trilha, sem prazo) e a simulação (objetivas e
-- desafios de uma área e um nível, com prazo gravado aqui). Uma sessão em andamento por pessoa, de
-- qualquer formato: o índice único parcial barra duas abertas ao mesmo tempo. A sessão muda por
-- comandos condicionais na situação (responder, gastar um envio, encerrar, vencer, corrigir), que
-- deixam duas gravações simultâneas sem uma desfazer a outra.
create table sessions (
    id               uuid primary key,
    user_id          uuid             not null references users (id) on delete cascade,
    format           varchar(20)      not null check (format in ('LESSON', 'SIMULATION')),
    area_id          uuid             not null references areas (id),
    level            varchar(10)      not null,
    -- Só na lição. Sem chave estrangeira: o seed de carreira pode apagar uma competência.
    competency_slug  varchar(50),
    status           varchar(20)      not null check (status in ('IN_PROGRESS', 'SUBMITTED',
                                                                 'COMPLETED', 'FAILED')),
    -- O peso das objetivas da área no começo da simulação; nulo na lição.
    objective_weight double precision,
    started_at       timestamptz      not null,
    deadline_at      timestamptz,
    finished_at      timestamptz,
    objective_score  double precision,
    challenge_score  double precision,
    final_score      double precision,
    xp_awarded       int              not null default 0 check (xp_awarded >= 0),
    failure_reason   text,
    check ((format = 'LESSON') = (competency_slug is not null)),
    check ((format = 'SIMULATION') = (deadline_at is not null))
);

-- O histórico da pessoa e as sessões em andamento que o agendador vence.
create index sessions_user_idx on sessions (user_id, started_at);
create unique index sessions_one_in_progress_idx on sessions (user_id) where status = 'IN_PROGRESS';
create index sessions_due_idx on sessions (deadline_at) where status = 'IN_PROGRESS';

-- As questões da sessão, na ordem sorteada (a partir de 1). A alternativa e o acerto ficam nulos
-- até a resposta; na simulação, a alternativa troca até encerrar.
create table session_questions (
    session_id  uuid    not null references sessions (id) on delete cascade,
    position    int     not null check (position >= 1),
    question_id uuid    not null references questions (id),
    option_id   uuid references question_options (id),
    correct     boolean,
    primary key (session_id, position),
    unique (session_id, question_id),
    check ((option_id is null) = (correct is null))
);

-- O Postgres não indexa chave estrangeira sozinho: este serve às checagens de quem aponta para a
-- alternativa (o seed do catálogo não regrava alternativas, mas a checagem existe).
create index session_questions_option_idx on session_questions (option_id);

-- Os envios da sessão ficam em code_submissions, com o session_id; aqui ficam o contador de envios
-- (no máximo 5) e, depois da correção, o melhor envio.
create table session_challenges (
    session_id         uuid not null references sessions (id) on delete cascade,
    position           int  not null check (position >= 1),
    challenge_id       uuid not null references challenges (id),
    submissions_used   int  not null default 0 check (submissions_used between 0 and 5),
    best_submission_id uuid references code_submissions (id) on delete set null,
    primary key (session_id, position),
    unique (session_id, challenge_id)
);

-- Envio feito dentro de uma sessão: não vale XP por si (a sessão dá o bônus dela) e fica fora da
-- situação, da taxa e da lista de envios da prática.
alter table code_submissions
    add column session_id uuid references sessions (id) on delete cascade;
create index code_submissions_session_idx on code_submissions (session_id);

-- Conquistas desbloqueadas, com a hora da primeira vez que valeram. session_id é a sessão cuja
-- correção desbloqueou a conquista; nulo quando veio de uma resposta, de um envio ou da trilha.
create table user_achievements (
    user_id     uuid        not null references users (id) on delete cascade,
    slug        varchar(50) not null,
    unlocked_at timestamptz not null,
    session_id  uuid references sessions (id) on delete set null,
    primary key (user_id, slug)
);
