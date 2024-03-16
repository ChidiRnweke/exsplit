create table expense (
    id uuid primary key default uuid_generate_v4(),
    expense_list uuid not null references expense_list(id),
    initialPayer varchar(255) not null references user(username),
    description text not null,
    price float not null,
    date date not null
)

create table owed_expense (
    id uuid primary key default uuid_generate_v4(),
    expenseId int not uuid references expense(id),
    person_to varchar(255) not null references user(username),
    person_from varchar(255) not null references user(username),
    amount float not null
)

create table expense_list (
    id uuid primary key default uuid_generate_v4(),
    name varchar(255) not null,
    circle int not null references circle(id),
)