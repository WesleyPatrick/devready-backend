-- Envios de código: rodar (só os casos visíveis, não vale nada) e enviar (todos os casos). A API
-- grava o envio na fila e o worker grava o fim. O XP que o envio vale (xp_reward) vem decidido
-- pela prática; o worker só grava xp_gained no primeiro aceito da pessoa no desafio, e o índice
-- único parcial garante isso também contra dois aceitos processados ao mesmo tempo.
create table code_submissions (
    id             uuid primary key,
    user_id        uuid        not null references users (id) on delete cascade,
    challenge_id   uuid        not null references challenges (id),
    language       varchar(20) not null,
    source_code    text        not null,
    kind           varchar(10) not null check (kind in ('RUN', 'SUBMIT')),
    status         varchar(20) not null check (status in ('QUEUED', 'RUNNING', 'PASSED', 'FAILED',
                                                          'COMPILE_ERROR', 'RUNTIME_ERROR',
                                                          'TIME_LIMIT', 'ERROR')),
    passed_count   int         not null default 0 check (passed_count >= 0),
    total_count    int         not null check (total_count >= 1),
    compile_output text,
    error_message  text,
    xp_reward      int         not null default 0 check (xp_reward >= 0),
    xp_gained      int         not null default 0 check (xp_gained >= 0),
    created_at     timestamptz not null,
    finished_at    timestamptz
);

-- Leituras por pessoa (progresso, situação) e por desafio (taxa de acerto, lista de envios).
create index code_submissions_user_idx on code_submissions (user_id, created_at);
create index code_submissions_challenge_idx on code_submissions (challenge_id, user_id, created_at);
create unique index code_submissions_first_solve_idx on code_submissions (user_id, challenge_id)
    where xp_gained > 0;

-- O resultado aponta para a posição do caso no desafio, não para o id: o catálogo recria os ids
-- dos casos quando regrava um desafio.
create table submission_results (
    submission_id uuid        not null references code_submissions (id) on delete cascade,
    position      int         not null,
    visible       boolean     not null,
    status        varchar(20) not null check (status in ('PASSED', 'FAILED', 'ERROR', 'TIME_LIMIT')),
    actual_json   text,
    error_message text,
    time_ms       int,
    primary key (submission_id, position)
);
