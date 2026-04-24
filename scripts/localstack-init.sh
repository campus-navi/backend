#!/bin/bash
awslocal s3 mb s3://campus-navi-local
awslocal s3api put-bucket-acl --bucket campus-navi-local --acl public-read
