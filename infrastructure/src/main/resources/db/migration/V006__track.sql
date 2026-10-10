-- Trilha: competências (com as tags do catálogo e os materiais de estudo) e as trilhas de cada
-- empresa, com as unidades na ordem de estudo.
create table competencies (
    id         uuid primary key,
    slug       varchar(60) not null unique,
    name       varchar(80) not null,
    category   varchar(20) not null,
    summary    text        not null default '',
    difficulty varchar(10) not null
);

create table competency_tags (
    competency_id uuid not null references competencies (id) on delete cascade,
    tag_id        uuid not null references tags (id),
    primary key (competency_id, tag_id)
);

create table materials (
    competency_id uuid         not null references competencies (id) on delete cascade,
    position      int          not null,
    title         varchar(200) not null,
    url           varchar(500) not null,
    type          varchar(20)  not null,
    language      varchar(2)   not null,
    free          boolean      not null,
    primary key (competency_id, position)
);

-- Stack nula: trilha da área inteira. Senioridade nula: vale para todas. Uma trilha por empresa,
-- área, stack e senioridade, com os nulos contando como valor (nulls not distinct).
create table tracks (
    id         uuid primary key,
    company_id uuid        not null references companies (id),
    area_slug  varchar(50) not null references areas (slug),
    stack_id   uuid        references stacks (id),
    seniority  varchar(10),
    jobs       int         not null,
    sample     boolean     not null default false,
    unique nulls not distinct (company_id, area_slug, stack_id, seniority)
);

-- A competência é única na trilha, mas a conferência fica para o fim da transação: ao reordenar,
-- o Hibernate reescreve as linhas posição por posição, e uma conferência imediata quebraria no meio
-- da troca. O peso fica em double precision, o tipo do domínio, e volta do banco igual ao que o
-- arquivo trouxe: com numeric(4,3), o 0.4118 da pesquisa voltaria 0.412 e o seed regravaria a
-- trilha a cada subida.
create table track_units (
    track_id      uuid             not null references tracks (id) on delete cascade,
    position      int              not null,
    competency_id uuid             not null references competencies (id),
    weight        double precision not null,
    requirement   varchar(12)      not null,
    jobs          int              not null,
    primary key (track_id, position),
    unique (track_id, competency_id) deferrable initially deferred
);

create index tracks_company_idx on tracks (company_id);
