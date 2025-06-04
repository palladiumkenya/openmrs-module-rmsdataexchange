[![Build Status](https://travis-ci.org/openmrs/openmrs-module-rmsdataexchange.svg?branch=master)](https://travis-ci.org/openmrs/openmrs-module-rmsdataexchange)

# openmrs-module-rmsdataexchange

Description
-----------
For data sync between openmrs and revenue management systems.

The project WIKI page:
https://wiki.openmrs.org/display/projects/OpenMRS+rmsdataexchange+module

Development
-----------
Install latest Docker.

To create or update a demo server run:
```bash
mvn openmrs-sdk:build-distro -Ddistro=openmrs-distro.properties -Ddir=docker
cd docker
docker-compose up --build
```
The server will be available at http://localhost:8080/openmrs

The `--build` flag forces to rebuild a docker image, if you are updating the server.

You can adjust ports in the .env file.
If you want to remote debug add `DEBUG=True` in the .env file.

Installation 
------------
If you want to upload this module into OpenMRS instance then please follow
1. Build the module to produce the .omod file.
2. To do a clean install, clear the liquibase logs and associated files:
    SET FOREIGN_KEY_CHECKS = 0;
    use openmrs;
    DELETE FROM openmrs.liquibasechangelog where id like 'kenyaemr_rms_data_exchange%';           
    Drop table rms_queue;
    Drop table rms_queue_system;
    Drop table rms_bill_attribute_type;
    Drop table rms_payment_attribute_type;
    Drop table rms_bill_attribute;
    Drop table rms_payment_attribute;
    SET FOREIGN_KEY_CHECKS = 1;
3. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file
    OR
    copy the .omod file into the openmrs modules directory and restart the server
