-- Fase 2: Wallet/positions/orders da era pre-ledger saem, ordem passa a viver so como
-- LedgerEntry (tipo ORDER). ledger_positions vira o nome definitivo agora que nao ha mais
-- colisao com a tabela antiga.
drop table orders;
drop table positions;
drop table wallet;

alter table ledger_positions rename to positions;

create table quotes (
    quote_id varchar(64) primary key,
    address varchar(64) not null references accounts (address),
    symbol varchar(20) not null,
    side varchar(10) not null,
    quantity numeric(28, 8) not null,
    price numeric(19, 2) not null,
    expires_at timestamp not null,
    used_at timestamp,
    result_block_index bigint,
    validator_signature varchar(200) not null
);

create table refresh_tokens (
    token varchar(64) primary key,
    address varchar(64) not null references accounts (address),
    expires_at timestamp not null,
    revoked_at timestamp
);

create table audit_log (
    id bigserial primary key,
    address varchar(64) not null,
    action varchar(50) not null,
    timestamp timestamp not null,
    metadata varchar(500)
);
