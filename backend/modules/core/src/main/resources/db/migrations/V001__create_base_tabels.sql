CREATE EXTENSION IF NOT EXISTS pgcrypto;

create table "users" (
    id text primary key default md5(now()::text || random()::text),
    password text not null,
    email text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table circles (
    id text primary key default md5(now()::text || random()::text),
    name text not null,
    description text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table circle_members (
    id text primary key default md5(now()::text || random()::text),
    display_name text,
    user_id text,
    circle_id text,
    foreign key (user_id) references "users"(id) on delete cascade,
    foreign key (circle_id) references circles(id) on delete cascade,
    unique (user_id, circle_id)
);

create table expense_lists (
    id text primary key default md5(now()::text || random()::text),
    name text not null,
    circle_id text not null references circles(id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table expenses (
    id text primary key default md5(now()::text || random()::text),
    expense_list_id text not null references expense_lists(id),
    paid_by text not null references circle_members(id),
    description text not null,
    price float4 not null,
    date int8 not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table owed_amounts (
    id text primary key default md5(now()::text || random()::text),
    expense_id text not null references expenses(id),
    from_member text not null references circle_members(id),
    to_member text not null references circle_members(id),
    amount float4 not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table settled_tabs (
    id text primary key default md5(now()::text || random()::text),
    expense_list_id text not null references expense_lists(id),
    from_member text not null references circle_members(id),
    to_member text not null references circle_members(id),
    amount float4 not null,  
    settled_at date not null default current_date
);


