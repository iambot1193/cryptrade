create table wallet (
    id bigint primary key,
    balance numeric(19, 2) not null
);

-- "position" e palavra reservada no Postgres; plural tambem casa com "orders"
create table positions (
    id bigserial primary key,
    symbol varchar(255) not null,
    quantity numeric(19, 8) not null,
    average_price numeric(19, 8) not null
);

create table orders (
    id bigserial primary key,
    symbol varchar(255) not null,
    side varchar(10) not null,
    quantity numeric(19, 8) not null,
    price numeric(19, 8) not null,
    total numeric(19, 2) not null,
    executed_at timestamp not null
);
