CREATE EXTENSION IF NOT EXISTS pgcrypto;

create table "users" (
    id text primary key default md5(now()::text || random()::text),
    password varchar(255) not null,
    email varchar(255) not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table circles (
    id text primary key default md5(now()::text || random()::text),
    name varchar(255) not null,
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
    unique (user_id)
);

create table settled_tabs (
    id text primary key default md5(now()::text || random()::text),
    expense_list_id text not null references expense_lists(id),
    from_member text not null references circle_members(id),
    to_member text not null references circle_members(id),
    amount float not null,  
    settled_at date not null default current_date
);

create table expense_lists (
    id text primary key default md5(now()::text || random()::text),
    name varchar(255) not null,
    circle_id text not null references circles(id),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table expenses (
    id text primary key default md5(now()::text || random()::text),
    expense_list_id text not null references expense_lists(id),
    paid_by varchar(255) not null references circle_members(id),
    description text not null,
    price float not null,
    date date not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table owed_amounts (
    id text primary key default md5(now()::text || random()::text),
    expense_id text not null references expenses(id),
    from_member varchar(255) not null references circle_members(id),
    to_member varchar(255) not null references circle_members(id),
    amount float not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create view owed_amounts_view as (
    select
        owed_amounts.id as id,
        owed_amounts.expense_id as expense_id,
        owed_amounts.from_member as from_member,
        owed_amounts.to_member as to_member,
        owed_amounts.amount as amount,
        owed_amounts.created_at as created_at,
        owed_amounts.updated_at as updated_at
        expenses.paid_by as paid_by,
        expenses.expense_list_id as expense_list_id
    from owed_amounts
    inner join expenses on expenses.id = owed_amounts.expense_id
    inner join circle_members on circle_members.id = paid_by
);

create view expenses_detail_view as (
    select
        expenses.id,
        expenses.expense_list_id,
        expenses.paid_by,
        expenses.description,
        expenses.price,
        expenses.date,
        expenses.created_at,
        expenses.updated_at,
        owed_amounts_view.id as owed_amount_id,
        owed_amounts_view.from_member,
        owed_amounts_view.to_member,
        owed_amounts_view.amount
    from expenses
    left join owed_amounts_view on expenses.id = owed_amounts_view.expense_id
);

create view expense_list_circle_view as (
    select
        expense_lists.id,
        expense_lists.name,
        expense_lists.circle_id,
        expense_lists.created_at,
        expense_lists.updated_at,
        circles.name as circle_name,
        circles.description as circle_description
    from expense_lists
    left join circles on expense_lists.circle_id = circles.id
);

create view expense_lists_detail_view as (
    select
        expense_list_circle_view.id,
        expense_list_circle_view.name,
        expense_list_circle_view.circle_id,
        expense_list_circle_view.created_at,
        expense_list_circle_view.updated_at,
        expenses_detail_view.id as expense_id,
        circle_name,
        circle_description,
        expenses_detail_view.paid_by,
        expenses_detail_view.description,
        expenses_detail_view.price,
        expenses_detail_view.date,
        expenses_detail_view.created_at as expense_created_at,
        expenses_detail_view.updated_at as expense_updated_at,
        expenses_detail_view.owed_amount_id,
        expenses_detail_view.from_member,
        expenses_detail_view.to_member,
        expenses_detail_view.amount
    from expense_list_circle_view
    left join expenses_detail_view on expense_list_circle_view.id = expenses_detail_view.expense_list_id
);