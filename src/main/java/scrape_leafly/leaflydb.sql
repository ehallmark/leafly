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
    primary key(strain_id, effect)
);


\copy strains to /home/ehallmark/Downloads/strains.csv delimiter ',' csv header;
\copy strain_reviews to /home/ehallmark/Downloads/strain_reviews.csv delimiter ',' csv header;
\copy strain_photos to /home/ehallmark/Downloads/strain_photos.csv delimiter ',' csv header;
\copy strain_flavors to /home/ehallmark/Downloads/strain_flavors.csv delimiter ',' csv header;
\copy strain_effects to /home/ehallmark/Downloads/strain_effects.csv delimiter ',' csv header;
\copy strain_lineage to /home/ehallmark/Downloads/strain_lineage.csv delimiter ',' csv header;

