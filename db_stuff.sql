-- CREATE ROLE nbg_user WITH PASSWORD 'nbg' LOGIN;
-- createdb nbg_cbank -O nbg_user

-- should be enums like this, but we'll go with strings for now
CREATE TYPE project_state AS ENUM ('created', 'pending_greenlight',
       'pending_approval', 'approved', 'funded', 'rejected');


-- different tables for now, no need to overcomplexify things
CREATE TABLE investors (
       user_id UUID PRIMARY KEY,
       name character varying (30) NOT NULL,
       inv_weight real default 0.1,
       password character varying (1000) NOT NULL);

CREATE TABLE bank_agents (
       agent_id UUID PRIMARY KEY,
       name character varying (30) NOT NULL,
       password character varying (1000) NOT NULL);

-- normally, these would be in different tabes
-- for now, denormalize
CREATE TABLE projects (
       project_id UUID PRIMARY KEY,
       -- investor / creator related
       state character varying(40) default 'created',
       creating_user_id UUID NOT NULL,
       project_name character varying (100),
       project_description character varying (5000),
       required_amount decimal (10, 2),
       current_amount decimal (10, 2),
       FOREIGN KEY (creating_user_id) REFERENCES investors(user_id)
       );


insert into investors (user_id, name, password)
values ('d3ac7e07-8d9c-4e86-aa8d-c3e2f725f262', 'John Doe', 'asdqwe');

insert into investors (user_id, name, password)
values ('f3872015-ee21-4c33-9bc2-e1ac80073665', 'Jane Doe', 'asdqwe');

insert into investors (user_id, name, password)
values ('92d5e6d0-f4df-4726-a6bd-d12da1118ff2', 'John von Neumann', 'asdqwe');

insert into investors (user_id, name, password)
values ('b9b1836c-4060-41b6-ba54-b780eea9fdd4', 'Johannes Kepler', 'asdqwe');

9c4920cf-713c-4df8-9557-512e9b31e2c5
382f5c2e-7165-47e5-8149-a6a21348722f
a72d4df7-f76e-4ae3-b36b-338a293aaf8d
0b6238af-8cfb-4732-9809-b7bea71776b7
10f06f0c-72c7-46fc-955f-814d1d236eb0
ff8808d5-7bfe-458a-b5f5-e7b1387a9f91
9ffdb4e4-aa70-42d5-8396-678bee7269d3
2d265a57-270c-437a-a4f5-a9bf84e17edf
1ef0fa82-2e7a-4040-a88d-227101e50d8e

insert into projects  (project_id, state, creating_user_id,
project_name, project_description, required_amount, current_amount)
values ('9c4920cf-713c-4df8-9557-512e9b31e2c5', 'approved',
'd3ac7e07-8d9c-4e86-aa8d-c3e2f725f262',
'Awesome Project 1', 'This should be awesome', 200000.00, 133000.00);

insert into projects  (project_id, state, creating_user_id,
project_name, project_description, required_amount, current_amount)
values ('0b6238af-8cfb-4732-9809-b7bea71776b7', 'pending_greenlight',
'92d5e6d0-f4df-4726-a6bd-d12da1118ff2',
'Hyperproduct #2', 'The father of all products', 50000.00, 2000.00);


insert into projects  (project_id, state, creating_user_id,
project_name, project_description, required_amount, current_amount)
values ('ff8808d5-7bfe-458a-b5f5-e7b1387a9f91', 'pending_greenlight',
'f3872015-ee21-4c33-9bc2-e1ac80073665',
'Megastartup', 'Ubiquitus computing', 33000.00, 900.00);

