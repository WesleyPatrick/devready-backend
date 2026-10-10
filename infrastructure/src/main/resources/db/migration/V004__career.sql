-- Carreira: empresas-alvo, stacks e o objetivo de carreira de cada pessoa.
create table companies (
    id            uuid primary key,
    slug          varchar(50) not null unique,
    name          varchar(80) not null,
    company_group varchar(20) not null,
    about         text        not null default '',
    active        boolean     not null default true
);

create table company_careers (
    company_id uuid         not null references companies (id) on delete cascade,
    position   int          not null,
    url        varchar(500) not null,
    primary key (company_id, position)
);

create table stacks (
    id        uuid primary key,
    slug      varchar(50) not null unique,
    name      varchar(80) not null,
    area_slug varchar(50) not null references areas (slug)
);

create table career_profiles (
    user_id    uuid primary key references users (id) on delete cascade,
    stack_id   uuid        not null references stacks (id),
    seniority  varchar(10) not null,
    updated_at timestamptz not null default now()
);

-- Sem unique (user_id, company_id): ao reordenar, o Hibernate reescreve posição por posição e uma
-- restrição de unicidade quebraria no meio da troca. O domínio já impede empresa repetida.
create table career_profile_companies (
    user_id    uuid not null references career_profiles (user_id) on delete cascade,
    position   int  not null,
    company_id uuid not null references companies (id),
    primary key (user_id, position)
);
