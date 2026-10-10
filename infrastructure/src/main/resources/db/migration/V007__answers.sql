-- Respostas às questões objetivas. Toda resposta fica, certa ou errada, com a hora: a situação de
-- cada questão, a taxa de acerto, o XP, a sequência, a meta e o calendário saem daqui na leitura.
-- O XP de cada resposta já vem decidido pela aplicação (só a primeira resposta certa da pessoa na
-- questão ganha); o índice único parcial garante isso também contra duas respostas simultâneas.
-- A chave estrangeira para a alternativa impede que ela suma enquanto houver resposta apontando
-- para ela: o seed do catálogo troca só título e dificuldade de quem já existe.
create table answers (
    id          uuid primary key,
    user_id     uuid        not null references users (id) on delete cascade,
    question_id uuid        not null references questions (id),
    option_id   uuid        not null references question_options (id),
    correct     boolean     not null,
    xp_gained   int         not null check (xp_gained >= 0),
    answered_at timestamptz not null
);

-- Leituras por pessoa (situação, progresso, questão do dia) e por questão (taxa de acerto).
create index answers_user_idx on answers (user_id, answered_at);
create index answers_question_idx on answers (question_id, user_id, answered_at);
-- O Postgres não indexa chave estrangeira sozinho: este serve às checagens de quem aponta para a alternativa.
create index answers_option_idx on answers (option_id);
create unique index answers_first_solve_idx on answers (user_id, question_id) where xp_gained > 0;
