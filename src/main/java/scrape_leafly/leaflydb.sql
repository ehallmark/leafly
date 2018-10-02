\connect leaflydb

create table strains (
    id text primary key,
    name text not null,
    type text not null,
    description text not null,
    rating double precision
);


drop table strain_reviews;
create table strain_reviews (
    strain_id text not null,
    review_num integer not null,
    review_text text not null,
    review_rating integer not null,
    review_profile text not null,
    primary key (strain_id, review_num)
);

drop table strain_photos;
create table strain_photos (
    strain_id text not null,
    photo_id text not null,
    photo_url text not null,
    primary key(strain_id, photo_id)
);

drop table strain_lineage;
create table strain_lineage (
    strain_id text not null,
    parent_strain_id text not null,
    primary key(strain_id, parent_strain_id)
);

drop table strain_flavors;
create table strain_flavors (
    strain_id text not null,
    flavor text not null,
    primary key(strain_id, flavor)
);

drop table strain_effects;
create table strain_effects (
    strain_id text not null,
    effect text not null,
    effect_type text not null,
    effect_percent double precision not null,
    primary key(strain_id, effect)
);


drop table effects;
create table effects (
    effect text primary key
);
insert into effects ( select distinct effect from strain_effects );

drop table flavors;
create table flavors (
    flavor text primary key
);
insert into flavors ( select distinct flavor from strain_flavors );

drop table profiles;
create table profiles (
    profile text primary key
);
insert into profiles ( select distinct review_profile from strain_reviews );

drop table parent_strains;
create table parent_strains (
    strain_id text primary key
);
insert into parent_strains ( select distinct parent_strain_id from strain_lineage );

drop table products;
create table products (
    product_id text primary key,
    product_name text not null,
    brand_name text not null,
    short_description text,
    description text,
    price double precision,
    rating double precision
);

drop table product_reviews;
create table product_reviews (
    product_id text not null,
    review_num integer not null,
    author text not null,
    rating integer not null,
    upvotes integer not null,
    downvotes integer not null,
    text text not null,
    primary key(product_id, review_num)
);



\copy strains to /home/ehallmark/Downloads/strains.csv delimiter ',' csv header;
\copy strain_reviews to /home/ehallmark/Downloads/strain_reviews.csv delimiter ',' csv header;
\copy strain_photos to /home/ehallmark/Downloads/strain_photos.csv delimiter ',' csv header;
\copy strain_flavors to /home/ehallmark/Downloads/strain_flavors.csv delimiter ',' csv header;
\copy strain_effects to /home/ehallmark/Downloads/strain_effects.csv delimiter ',' csv header;
\copy strain_lineage to /home/ehallmark/Downloads/strain_lineage.csv delimiter ',' csv header;

\encoding UTF8

pg_dump -Fc --dbname=postgresql://postgres:password@127.0.0.1:5432/leaflydb > leaflydb.dump
pg_restore -Fc --dbname=postgresql://postgres:password@127.0.0.1:5432/leaflydb leaflydb.dump
