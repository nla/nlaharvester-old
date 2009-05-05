drop table collection cascade constraints;
drop table contactselections cascade constraints;
drop table collectioncontact cascade constraints;
drop table collectionprofile cascade constraints;
drop table contributor cascade constraints;
drop table contributorcontact cascade constraints;
drop table profile cascade constraints;
drop table harvest cascade constraints;
drop table harvestdata cascade constraints;
drop table harvestlog cascade constraints;
drop table note cascade constraints;
drop table profilestepparameter cascade constraints;
drop table parameterinformation cascade constraints;
drop table parameteroption cascade constraints;
drop table profilestep cascade constraints;
drop table step cascade constraints;
drop table harvestcluster cascade constraints;
drop table harvestclusterdata cascade constraints;
drop table stepfile cascade constraints;
drop table report cascade constraints;

DROP SEQUENCE profile_seq;
DROP SEQUENCE profilestep_seq;
DROP SEQUENCE collection_seq;
DROP SEQUENCE contributor_seq;
DROP SEQUENCE harvest_seq;
DROP SEQUENCE parameterinformation_seq;
DROP SEQUENCE parameteroption_seq;
DROP SEQUENCE profilestepparameter_seq;
DROP SEQUENCE harvestlog_seq;
DROP SEQUENCE note_seq;
DROP SEQUENCE collectioncontact_seq;
DROP SEQUENCE selections_seq;
DROP SEQUENCE contributorcontact_seq;
DROP SEQUENCE harvestdata_seq;
DROP SEQUENCE harvestcluster_seq;
DROP SEQUENCE harvestclusterdata_seq;
DROP SEQUENCE stepfile_seq;
DROP SEQUENCE report_seq;

CREATE SEQUENCE profile_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE profilestep_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE collection_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE contributor_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE harvest_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE parameterinformation_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE parameteroption_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE profilestepparameter_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE harvestlog_seq MINVALUE 1 START WITH 700000 INCREMENT BY 1;
CREATE SEQUENCE note_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE collectioncontact_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE selections_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE contributorcontact_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE harvestdata_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE harvestcluster_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE harvestclusterdata_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE stepfile_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;
CREATE SEQUENCE report_seq MINVALUE 1 START WITH 100000 INCREMENT BY 1;

CREATE TABLE profile(
profileid integer primary key,
name varchar(255),
description varchar(255),
type int	--OAI(0), etc.
);

CREATE TABLE step(
stepid integer primary key,
classname varchar(255), -- for reflection
name varchar(255),	--information for users
inputtype varchar(255),
outputtype varchar(255),
type integer,	--input(0), output(1), validator(2), translator(3),...
description varchar(255)
);

CREATE TABLE profilestep(
psid integer primary key,
profileid integer references profile(profileid),
stepid integer references step(stepid),
contributorid integer,
position integer, --position in pipeline, e.g. first(1), second(2), third(3) etc.
description varchar(255),
restriction integer default 0,
enabled integer default 1
);

CREATE TABLE collection(
collectionid integer primary key,
name varchar(255),
description varchar(255),
userguide varchar(1023),
psid integer,	--represents the output stage that should be used for this collection
storesize integer
);

CREATE TABLE contributor(
contributorid integer primary key,
collectionid integer references collection(collectionid),
name varchar(255),
description varchar(255), --shown on the details screen
isfinishedfirstharvest integer,	-- yes/no 
lastharvestid integer,
lastsuccessfultestid integer,
lastsuccessfulprodid integer,
htype integer,  --OAI(0), web crawl(1), other(2)
ismonitored integer,    --boolean yes/no
totalrecords integer,   
type integer,	-- test or production (0 or 1)
platform varchar(255), -- text shown on the details screen
psid integer,	--the input stage that should be used for this contributor
viewrecordsurl varchar(255),
isscheduled integer,
granularity integer,   -- zero for default, 1 for long format
hidefromworktray integer, -- zero show, 1 hide
productionid integer references profile(profileid),
testid integer references profile(profileid),
dateadded date
);
CREATE INDEX collection_idx ON contributor(collectionid);

CREATE TABLE harvest(
harvestid integer primary key,
contributorid integer references contributor(contributorid),
profileid integer references profile(profileid),
status varchar(255),
statuscode integer,
recordscompleted integer default 0, -- zero until all records are up to the last stage
totalrecords integer default 0,
deletionsread integer default 0,
deletionsperformed integer default 0,
recordsadded integer default 0,
starttime date,
harvestfrom varchar(255), -- for use by the OAI harvester
harvestuntil varchar(255), -- null most of the time, only used for test harvests
endtime date,
type integer -- test(0), production(1)
);

alter table contributor add constraint contributorlastharvest foreign key (lastharvestid) references harvest(harvestid);
alter table contributor add constraint contributorlastsuccharvestprod foreign key (lastsuccessfulprodid) references harvest(harvestid);
alter table contributor add constraint contributorlastsuccharvesttest foreign key (lastsuccessfultestid) references harvest(harvestid);

CREATE INDEX contributor_harvest_idx ON harvest(contributorid);

CREATE TABLE collectionprofile(
collectionid integer references collection(collectionid),
profileid integer references profile(profileid),
PRIMARY KEY (collectionid, profileid)
);

CREATE TABLE parameterinformation(
piid integer primary key,
stepid integer references step(stepid),
parametername varchar(255),
defaultvalue varchar(255),
type varchar(255),	-- integer, string etc.
editibility integer,    -- 0 normal, 1 required, 2 readonly
description varchar(255),
parentpiid integer references parameterinformation(piid)
);

CREATE TABLE parameteroption(
poid integer primary key,
piid integer references parameterinformation(piid),
value varchar(255),
description varchar(255)
);


CREATE TABLE profilestepparameter(
profilestepparameterid integer primary key,
psid integer references profilestep(psid),
piid integer references parameterinformation(piid),
grouplistindex integer,
value varchar(2000) --TODO : update this is test/prod when deploying to them
);
CREATE INDEX psp_idx ON profilestepparameter(psid);

CREATE TABLE harvestlog(
harvestlogid integer primary key,
harvestid integer references harvest(harvestid),
timestamp date,
errorlevel integer,
description varchar(2000), --TODO : update this is test/prod when deploying to them
recorddata blob,
hasdata integer,
stepid integer,
reason varchar(255)
);
CREATE INDEX harvestlog_idx ON harvestlog(harvestid);

CREATE TABLE note(
noteid integer primary key,
contributorid integer references contributor(contributorid),
timestamp date,
creator varchar(255),
description varchar(2000)   --DONT FORGET TO UPDATE THIS IN TEST/PROD ENVIRONMENTS, used to be 255
);


CREATE TABLE collectioncontact(
contactid integer primary key,
collectionid integer references collection(collectionid),
note varchar(255),
name varchar(255),
type integer,	--business, technical etc. (maybe a string would be better?)
email varchar(255),
phone varchar(255),
jobtitle varchar(255)
);

CREATE TABLE contactselections(
selectionid integer primary key,
contributorid integer references contributor(contributorid),
contactid integer references collectioncontact(contactid),
failure integer,
success integer,
record integer,
harvest integer,
businesstype integer
);

CREATE TABLE contributorcontact(
contactid integer primary key,
contributorid integer references contributor(contributorid), 
failure integer,
success integer,
record integer,
harvest integer,
businesstype integer,   -- nla(0), contributor org(1)
note varchar(255),
name varchar(255),
type integer,	--business, technical etc. (maybe a string would be better?)
email varchar(255),
phone varchar(255),
jobtitle varchar(255)
);

CREATE TABLE harvestdata(
harvestdataid integer primary key,
harvestid integer references harvest(harvestid),
stage int,
data blob
);
CREATE INDEX harvestdata_idx ON harvestdata(harvestid);


CREATE TABLE harvestcluster(
harvestclusterid integer primary key,
harvestid integer references harvest(harvestid),
xpath varchar(255)
);
CREATE INDEX harvestcluster_idx ON harvestcluster(harvestid);

CREATE TABLE harvestclusterdata(
harvestclusterdataid integer primary key,
harvestclusterid integer,
term varchar(255),
count integer
);
CREATE INDEX harvestclusterdata_idx ON harvestclusterdata(harvestclusterid);

CREATE TABLE stepfile(
fileid integer primary key,
stepid integer references step(stepid),
filename varchar(255),
description varchar(2000),
data blob
);

CREATE TABLE report(
reportid integer primary key,
type integer,
collectionid integer references collection(collectionid),
contributorid integer references contributor(contributorid),
startdate date,
enddate date,
timestamp date,
data blob
);
