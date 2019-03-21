#!/bin/bash
cd /var/server/scripts
nohup ./start-server.sh > proxily.out 2> proxily.err < /dev/null &