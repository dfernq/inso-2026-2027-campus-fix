\getenv app_user CAMPUSFIX_DB_USER
\getenv app_password CAMPUSFIX_DB_PASSWORD

CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password';
CREATE DATABASE campusfix OWNER :"app_user";
CREATE DATABASE campusfix_test OWNER :"app_user";
