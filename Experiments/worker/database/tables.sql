-- This script was created by Jason Flood.
DO $$
BEGIN

RAISE NOTICE 'Dropping existing tables with cascade!';
drop table if exists public.tb_user CASCADE;


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





END;$$;