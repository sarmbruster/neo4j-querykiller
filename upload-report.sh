#!/bin/sh
tar -zcf - build/reports/tests | curl --upload-file - https://transfer.sh/report.tar.gz