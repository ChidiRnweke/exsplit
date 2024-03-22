CREATE EXTENSION IF NOT EXISTS pgcrypto;

create table "user" (
    id text primary key default md5(now()::text || random()::text),
    password varchar(255) not null,
    email varchar(255) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table circle (
    id text primary key default md5(now()::text || random()::text),
    name varchar(255) not null,
    description text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table circle_member (
    display_id serial primary key,
    user_id text,
    circle_id text,
    foreign key (user_id) references "user"(id) on delete cascade,
    foreign key (circle_id) references circle(id) on delete cascade
);

create table expense_list (
    id text primary key default md5(now()::text || random()::text),
    name varchar(255) not null,
    circle_id text not null references circle(id)
);

create table expense (
    id text primary key default md5(now()::text || random()::text),
    expense_list_id text not null references expense_list(id),
    initial_payer varchar(255) not null references "user"(id),
    description text not null,
    price float not null,
    date date not null
);

create table owed_expense (
    id text primary key default md5(now()::text || random()::text),
    expense_id text not null references expense(id),
    person_to varchar(255) not null references "user"(id),
    person_from varchar(255) not null references "user"(id),
    amount float not null
);
