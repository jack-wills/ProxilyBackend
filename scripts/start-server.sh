#!/bin/bash
cd /var/server/
./scripts/add-permissions.sh
java -jar CombinedService.jar &