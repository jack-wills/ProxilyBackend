#!/bin/bash
cd /var/server/
java -jar CombinedService.jar & echo $! > ./pid.file &