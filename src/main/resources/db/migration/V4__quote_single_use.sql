-- Fase 3 (fix): uma cotacao = no maximo um lancamento no ledger. Sem isso, duas requisicoes
-- concorrentes com o mesmo quoteId passam as duas pelo check de idempotencia (que le
-- resultBlockIndex fora de qualquer lock) e executam a ordem duas vezes. O indice unico faz
-- o segundo append() estourar DataIntegrityViolationException e dar rollback.
-- quote_id e NULL para entries CREATE_ACCOUNT/MINT; Postgres trata NULLs como distintos em
-- indice unico, entao esses entries nao colidem.
create unique index ix_ledger_entries_quote_id on ledger_entries (quote_id);

-- Fase 3 (fix): coluna usedAt nunca era lida (so resultBlockIndex decide idempotencia).
alter table quotes drop column used_at;
