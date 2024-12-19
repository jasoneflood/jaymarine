-- This script was created by Jason Flood.
DO $$
BEGIN

RAISE NOTICE 'Dropping existing tables with cascade!';
drop table if exists public.tb_user CASCADE;
drop table if exists public.tb_query CASCADE;
drop table if exists public.tb_databaseConnections CASCADE;

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

insert into public.tb_user(firstname, surname, email, username, password, active, authlevel) VALUES('user_firstname', 'user_surname', 'user_email', 'username', 'password', 'active', 1);


CREATE TABLE IF NOT EXISTS public.tb_query
(
    id serial NOT NULL,
	query_db_type int NOT NULL,
	query_string  TEXT NOT NULL, 
    query_type character varying(100),
    PRIMARY KEY (id)
);

RAISE NOTICE 'Created tb_query';




/*{
        "status": "option",
        "active": "true",
        "db_type": "db2",
        "db_version": "14.00",
        "db_username": "db2inst1",
        "db_password": "guardium",
        "db_port": "50000",
        "db_database": "GOSALES",
        "db_url": "127.0.0.1",
        "db_jdbcClassName": "com.ibm.db2.jcc.DB2Driver",
        "db_user_icon":"personbeardcolor.png",
        "db_databaseIcon":""
    }
/************************************************************************************/
CREATE TABLE IF NOT EXISTS public.tb_databaseConnections
(
    id serial NOT NULL,
    status character varying(100),
    db_type character varying(100),
    db_version character varying(100),
    db_username character varying(100),
    db_password character varying(100),
   	db_port character varying(100),
    db_database character varying(100),
    db_url character varying(100),
   	db_jdbcClassName character varying(100),
    db_userIcon character varying(100),
   	db_databaseIcon character varying(100),
    PRIMARY KEY (id)
);

RAISE NOTICE 'Created tb_databaseConnections';


END;$$;
/*************************************************************************************/

CREATE FUNCTION function_login(var_username TEXT, var_password TEXT)
returns table (
		id integer,
        firstname varchar,
        surname varchar,
        email varchar,
        username varchar,
        active varchar,
		authlevel integer
        
	)
    AS $$ 

    BEGIN
    RAISE NOTICE 'Username:  % Password: %' , var_username, var_password;
    
   	return query 
        select
		    tb_user.id,
            tb_user.firstname,
            tb_user.surname,
            tb_user.email,
            tb_user.username,
            tb_user.active,
		    tb_user.authlevel
		from
			tb_user
        where tb_user.username = var_username AND tb_user.password = var_password;
END
$$  LANGUAGE plpgsql;

