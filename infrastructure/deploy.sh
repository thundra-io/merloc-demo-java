#!/bin/bash -ex

cdk bootstrap
cdk deploy --require-approval never