LAS2peer-iStarMLModel-Service
=======================

This service manages creation and modification of iStarML using a model repository (eXist-db).

Short setup guide:

Install eXist: http://exist-db.org/exist/apps/homepage/index.html

Enable versioning: http://exist-db.org/exist/apps/doc/versioning.xml

Modify service-config files in ./config

Create a new group in the db: all

Create a new admin user in the db, as specified in ./config/serviceConfig.conf and make him a member of the group 'all' and 'dba'(this account is used by the service to create new db accounts automatically)

set the group of RootCollection to 'all'

build with ant

start service using start_service.bat in ./bin