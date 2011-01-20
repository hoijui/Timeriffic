#!/bin/bash
cd $(dirname "$0")
pwd
ant release && cp -v bin2/Timeriffic-unsigned.apk ../distrib/Timeriffic.apk
