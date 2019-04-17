#!/bin/bash
echo "Test" >> /tmp/test.txt
cd /var/server/
java -jar CombinedService.jar &