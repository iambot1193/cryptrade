create table accounts (
    address varchar(64) primary key,
    public_key varchar(200) not null,
    role varchar(10) not null default 'USER',
    balance numeric(19, 2) not null default 0
);

create table ledger_blocks (
    block_index bigint primary key,
    prev_hash varchar(64) not null,
    hash varchar(64) not null unique,
    created_at timestamp not null,
    validator_signature varchar(200) not null
);

create table ledger_entries (
    id bigserial primary key,
    block_index bigint not null references ledger_blocks (block_index),
    sequence_in_block int not null,
    type varchar(20) not null,
    payload bytea not null,
    quote_id varchar(64),
    author_address varchar(64),
    signature varchar(200) not null,
    unique (block_index, sequence_in_block)
);

create table ledger_positions (
    id bigserial primary key,
    address varchar(64) not null references accounts (address),
    symbol varchar(20) not null,
    quantity numeric(28, 8) not null default 0,
    average_price numeric(19, 2) not null default 0,
    unique (address, symbol)
);
