-- This script was created by Jason Flood.
DO $$
BEGIN

RAISE NOTICE 'Dropping existing tables with cascade!';
drop table if exists public.tb_user CASCADE;
drop table if exists public.tb_query CASCADE;

RAISE NOTICE 'All tables dropped!';

CREATE TABLE IF NOT EXISTS public.tb_user
(
    id serial NOT NULL,
    firstname character varying(100),
    surname character varying(100),
    email character varying(100),
    username character varying(100) UNIQUE,
    password character varying(100) NOT NULL,
   	active character varying(100),
    authlevel integer DEFAULT 0 NOT NULL,
    PRIMARY KEY (id)
);

RAISE NOTICE 'Created tb_user';


CREATE TABLE IF NOT EXISTS public.tb_query
(
    id serial NOT NULL,
	query_db_type int NOT NULL,
	query_string  TEXT NOT NULL, 
    query_type character varying(100),
    PRIMARY KEY (id)
);

RAISE NOTICE 'Created tb_query';

END;$$;