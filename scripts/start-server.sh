#!/bin/bash
sudo yum install -y java-1.8.0
sudo yum remove -y java-1.7.0-openjdk
sudo java -jar /var/server/CombinedService.jar