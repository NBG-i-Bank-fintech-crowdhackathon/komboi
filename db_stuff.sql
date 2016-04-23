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

-- normally an enum or FK to other table
alter table projects add column estimated_risk_factor character varying (100);

insert into investors (user_id, name, password)
values ('d3ac7e07-8d9c-4e86-aa8d-c3e2f725f262', 'John Doe', 'asdqwe');

insert into investors (user_id, name, password)
values ('f3872015-ee21-4c33-9bc2-e1ac80073665', 'Jane Doe', 'asdqwe');

insert into investors (user_id, name, password)
values ('92d5e6d0-f4df-4726-a6bd-d12da1118ff2', 'John von Neumann', 'asdqwe');

insert into investors (user_id, name, password)
values ('b9b1836c-4060-41b6-ba54-b780eea9fdd4', 'Johannes Kepler', 'asdqwe');


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
values ('1ef0fa82-2e7a-4040-a88d-227101e50d8e', 'approved',
'd3ac7e07-8d9c-4e86-aa8d-c3e2f725f262',
'Awesome Project 2', 'Some project', 100000.00, 50000.00);

insert into projects  (project_id, state, creating_user_id,
project_name, project_description, required_amount, current_amount)
values ('0b6238af-8cfb-4732-9809-b7bea71776b7', 'pending_greenlight',
'92d5e6d0-f4df-4726-a6bd-d12da1118ff2',
'Hyperproduct #2', 'The father of all products', 50000.00, 25000.00);


insert into projects  (project_id, state, creating_user_id,
project_name, project_description, required_amount, current_amount)
values ('ff8808d5-7bfe-458a-b5f5-e7b1387a9f91', 'pending_greenlight',
'f3872015-ee21-4c33-9bc2-e1ac80073665',
'Megastartup', 'Ubiquitus computing', 33000.00, 22000.00);


update projects set estimated_risk_factor='low' where project_id='0b6238af-8cfb-4732-9809-b7bea71776b7';

update projects set estimated_risk_factor='medium' where project_id='ff8808d5-7bfe-458a-b5f5-e7b1387a9f91';

update projects set estimated_risk_factor='high' where project_id='1ef0fa82-2e7a-4040-a88d-227101e50d8e';

update projects set estimated_risk_factor='normal' where project_id='9c4920cf-713c-4df8-9557-512e9b31e2c5';

create table project_investor_credibility (
       project_id UUID NOT NULL,
       required_credibility real NOT NULL,
       current_credibility real default 0.0,
       FOREIGN KEY (project_id) references projects(project_id));

insert into project_investor_credibility
(project_id, required_credibility, current_credibility)
VALUES ('9c4920cf-713c-4df8-9557-512e9b31e2c5', 200.0, 220.0);

insert into project_investor_credibility
(project_id, required_credibility, current_credibility)
VALUES ('1ef0fa82-2e7a-4040-a88d-227101e50d8e', 500.0, 610.0);

insert into project_investor_credibility
(project_id, required_credibility, current_credibility)
VALUES ('0b6238af-8cfb-4732-9809-b7bea71776b7', 250.0, 30.0);

insert into project_investor_credibility
(project_id, required_credibility, current_credibility)
VALUES ('ff8808d5-7bfe-458a-b5f5-e7b1387a9f91', 300.0, 90.0);


create table investor_portfolio (
       user_id UUID NOT NULL,
       project_id UUID NOT NULL,
       amount DECIMAL(10, 2) NOT NULL,
       -- this needs unique constraint -- not now
       FOREIGN KEY (user_id) REFERENCES investors(user_id),
       FOREIGN KEY (project_id) REFERENCES projects(project_id));


insert into investor_portfolio (user_id, project_id, amount)
values ('f3872015-ee21-4c33-9bc2-e1ac80073665',
'9c4920cf-713c-4df8-9557-512e9b31e2c5', 250.0);

insert into investor_portfolio (user_id, project_id, amount)
values ('f3872015-ee21-4c33-9bc2-e1ac80073665',
'1ef0fa82-2e7a-4040-a88d-227101e50d8e', 250.0);


CREATE TABLE triumphs (
       user_id UUID NOT NULL,
       date_unlocked TIMESTAMP WITH TIME ZONE NOT NULL,
       triumph_type character varying (200) NOT NULL, -- normally a FK to relevant table
       description character varying (1000) NOT NULL,
       award character varying (200) NOT NULL, -- normally a FK
       FOREIGN KEY (user_id) REFERENCES investors(user_id));


insert into triumphs (user_id, date_unlocked, triumph_type,
description, award)
VALUES('f3872015-ee21-4c33-9bc2-e1ac80073665', '2016-04-08 12:34:56+00',
'Angel', 'Invest in a high risk project', '1000 card loyalty bonus points');

insert into triumphs (user_id, date_unlocked, triumph_type,
description, award)
VALUES('f3872015-ee21-4c33-9bc2-e1ac80073665', '2016-03-08 13:37:54+00',
'Frantic Investor', 'Invest in 15 projects', '3% refund on a months worth of credit card purchases');

insert into triumphs (user_id, date_unlocked, triumph_type,
description, award)
VALUES('f3872015-ee21-4c33-9bc2-e1ac80073665', '2016-03-11 00:06:07+00',
'Awesomeness', 'One of your high risk investments was approved', 'Free tickets to the cinema');

