#!/bin/bash -ex
set +x

cdk bootstrap
cdk deploy --require-approval never