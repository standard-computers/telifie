## JSON Configuration File

config.json contains all the settings Telifie needs to be properly configured. See the example in this directory and properties are explained herein.

### Installation Type

1. Local - A local installation will save files on the server that are parsed or uploaded. The database will have to have local access and the server will need to be able to write to the directory where the files are stored. Alternatively, the database could be on another server. With either installation type, the MongoDB can be local or remote.

2. Remote - Files are stored in a S3 space provided the correct information and the same rules for local installation apply to the database. The database can be local or remote.

### IP List

This is a list of IP addresses that the server can connect to.

### IP Access

This is a list of IP addresses that can connect to the server. If blank, then any computer can connect to the server.