drop table if exists any_data_model;
drop table if exists any_data_model_field;
drop table if exists any_data_model_type;
drop table if exists any_ct;
drop table if exists any_cmp;


create table any_data_model_field(
field_name varchar(64),
display_name varchar(64) not null ,
data_type varchar(30) not null ,
data_type_name varchar(50) not null ,
data_length int,
data_scale int,
nullable tinyint(1),
orders int,
col_width int,
hidden tinyint(1),
is_primary tinyint(1),
fixed tinyint(1),
is_foreign tinyint(1),
ref_type varchar(255),
data_model_id bigint,
ref_data_model_id bigint,
ref_field_id bigint,
ref_field_display_id bigint,
reverse_data_model_id bigint,
reverse_field_id bigint,
input_type varchar(30),
jscode text,
is_sys tinyint(1),
is_file tinyint(1),
in_grid tinyint(1),
in_form tinyint(1),
in_query tinyint(1),
create_time datetime,
ref_table_name varchar(64),
ref_display_col varchar(64),
ref_col varchar(64),
as_tree_col tinyint(1),
as_tree_ds tinyint(1),
id bigint primary key );

create table any_data_model_type(
name varchar(50),
descn varchar(255),
orders int,
path varchar(500),
icon varchar(180),
parent_id bigint,
id bigint primary key );

create table any_ct(
cols int,
height int,
width int,
orders int,
cmp_id varchar(255),
data_model_id bigint,
id bigint primary key );


create table any_cmp(
any_ct_id bigint,
orders int,
data_model_field_id bigint,
cmp_id varchar(255),
id bigint primary key );


create table any_data_model(
display_name varchar(64) not null  unique ,
table_name varchar(64) not null  unique ,
is_active tinyint(1),
create_user_id bigint,
create_time datetime,
read_roles varchar(255),
create_roles varchar(255),
delete_roles varchar(255),
update_roles varchar(255),
read_role_names varchar(500),
create_role_names varchar(500),
delete_role_names varchar(500),
update_role_names varchar(500),
has_table tinyint(1),
has_code tinyint(1),
in_menu tinyint(1),
process_key varchar(128),
ui_style varchar(30),
form_pos varchar(30),
form_width int,
form_height int,
orders int,
icon_cls varchar(30),
type_id bigint,
descn varchar(1000),
is_completed tinyint(1),
icon varchar(120),
package_name varchar(180),
viewable tinyint(1),
exportable tinyint(1),
importable tinyint(1),
searchable tinyint(1),
priv_type varchar(255),
priv_str varchar(255),
id bigint primary key );

create index idx_fk_any_data_model_field_data_model_id on any_data_model_field(data_model_id);

create index idx_fk_any_data_model_field_ref_data_model_id on any_data_model_field(ref_data_model_id);

create index idx_fk_any_data_model_field_ref_field_id on any_data_model_field(ref_field_id);

create index idx_fk_any_data_model_field_ref_field_display_id on any_data_model_field(ref_field_display_id);

create index idx_fk_any_data_model_field_reverse_data_model_id on any_data_model_field(reverse_data_model_id);

create index idx_fk_any_data_model_field_reverse_field_id on any_data_model_field(reverse_field_id);

create index idx_fk_any_ct_data_model_id on any_ct(data_model_id);

create index idx_fk_any_cmp_any_ct_id on any_cmp(any_ct_id);

create index idx_fk_any_cmp_data_model_field_id on any_cmp(data_model_field_id);
