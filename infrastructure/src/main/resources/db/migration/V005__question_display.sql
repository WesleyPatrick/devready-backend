-- Título curto de cada questão e dificuldade de questões e desafios. Os valores padrão existem só
-- para a migração passar sobre bases que já têm conteúdo; o seed, que roda a cada subida, grava os
-- valores de verdade.
alter table questions add column title varchar(160);
update questions set title = slug;
alter table questions alter column title set not null;

alter table questions add column difficulty varchar(10) not null default 'MEDIUM';
alter table questions alter column difficulty drop default;

alter table challenges add column difficulty varchar(10) not null default 'MEDIUM';
alter table challenges alter column difficulty drop default;
