-- Senha provisória dada pelo admin: a pessoa precisa definir uma nova no próximo acesso.
alter table users add column must_change_password boolean not null default false;
