#!/bin/bash
cd /var/server/
java -jar CombinedService.jar & echo $! | sudo tee ./pid.file &