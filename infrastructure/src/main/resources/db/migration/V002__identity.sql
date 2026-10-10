-- Usuários e contas sociais vinculadas.
create table users (
    id            uuid primary key,
    email         varchar(320) not null unique,
    display_name  varchar(120) not null,
    avatar_url    varchar(2048),
    password_hash varchar(100),
    role          varchar(20)  not null,
    active        boolean      not null default true,
    created_at    timestamptz  not null
);

create table social_accounts (
    id               uuid primary key,
    user_id          uuid         not null references users (id) on delete cascade,
    provider         varchar(20)  not null,
    provider_user_id varchar(255) not null,
    unique (provider, provider_user_id)
);

create index social_accounts_user_id_idx on social_accounts (user_id);
