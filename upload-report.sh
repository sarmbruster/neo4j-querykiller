#!/bin/sh

env
cat /master/settings.gradle
#tar -zcf - build/reports/tests | curl --upload-file - https://transfer.sh/report.tar.gz
