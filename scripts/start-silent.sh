#!/bin/bash
cd /var/server/scripts
chmod 777 ./start-server.sh
nohup ./start-server.sh > proxily.out 2> proxily.err < /dev/null &