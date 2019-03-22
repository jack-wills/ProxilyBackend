#!/bin/bash
cd /var/server/
if [ -f ./pid.file ]; then
    kill $(cat ./pid.file)
fi