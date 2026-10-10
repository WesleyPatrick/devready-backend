-- Catálogo: áreas, tags, questões objetivas e desafios de código.
create table areas (
    id               uuid primary key,
    slug             varchar(50)  not null unique,
    name             varchar(80)  not null,
    question_count   int          not null,
    challenge_count  int          not null,
    duration_minutes int          not null,
    objective_weight numeric(3,2) not null,
    active           boolean      not null default true
);

create table tags (
    id   uuid primary key,
    slug varchar(50) not null unique,
    name varchar(80) not null
);

create table questions (
    id          uuid primary key,
    slug        varchar(120) not null unique,
    area_id     uuid         not null references areas (id),
    level       varchar(10)  not null,
    statement   text         not null,
    explanation text         not null,
    active      boolean      not null default true,
    created_at  timestamptz  not null default now()
);

create table question_options (
    id          uuid primary key,
    question_id uuid    not null references questions (id) on delete cascade,
    text        text    not null,
    correct     boolean not null,
    position    int     not null,
    unique (question_id, position)
);

create table question_tags (
    question_id uuid not null references questions (id) on delete cascade,
    tag_id      uuid not null references tags (id),
    primary key (question_id, tag_id)
);

create table challenges (
    id               uuid primary key,
    slug             varchar(120) not null unique,
    title            varchar(160) not null,
    area_id          uuid         not null references areas (id),
    level            varchar(10)  not null,
    statement        text         not null,
    approach_summary text         not null,
    time_limit_ms    int          not null,
    memory_limit_kb  int          not null,
    comparison_mode  varchar(20)  not null,
    max_submissions  int          not null,
    active           boolean      not null default true
);

create table challenge_languages (
    id                 uuid primary key,
    challenge_id       uuid        not null references challenges (id) on delete cascade,
    language           varchar(20) not null,
    function_name      varchar(80) not null,
    starter_code       text        not null,
    reference_solution text,
    unique (challenge_id, language)
);

create table challenge_test_cases (
    id            uuid primary key,
    challenge_id  uuid    not null references challenges (id) on delete cascade,
    position      int     not null,
    args_json     text    not null,
    expected_json text    not null,
    visible       boolean not null,
    unique (challenge_id, position)
);

create table challenge_tags (
    challenge_id uuid not null references challenges (id) on delete cascade,
    tag_id       uuid not null references tags (id),
    primary key (challenge_id, tag_id)
);

create index questions_area_level_active_idx on questions (area_id, level, active);
create index challenges_area_level_active_idx on challenges (area_id, level, active);
