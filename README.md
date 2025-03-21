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
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.
