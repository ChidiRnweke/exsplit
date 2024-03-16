create table circle (
    id uuid primary key default uuid_generate_v4(),
    name varchar(255) not null,
    description text not null,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table circle_member (
    displayName serial primary key,
    user_id uuid foreign key references user(id) on delete cascade,
    circle_id uuid foreign key references circle(id) on delete cascade,
);