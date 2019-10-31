#!/usr/bin/env bash
DIRNAME=$(dirname ${BASH_SOURCE[0]})



if [[ -z "${GS_DIR}" ]]; then
    echo "GS_DIR environment variable is not set"
    exit 1
fi


#SERVER=""

if [[ -n "${SERVER}" ]]; then
   SERVER_PARAM="--server=${SERVER}"
fi


#${GS_DIR}/bin/gs.sh pu deploy Mirror ${DIRNAME}/mirror/target/mirror.jar

#${GS_DIR}/bin/gs.sh pu deploy --partitions=2 --backups=1 ProductsCatalog ${DIRNAME}/products-catalog/target/products-catalog.jar

#${GS_DIR}/bin/gs.sh pu deploy ProductsLoader ${DIRNAME}/products-loader/target/products-loader.jar

#${GS_DIR}/bin/gs.sh pu deploy ProductsFeeder ${DIRNAME}/products-feeder/target/products-feeder.jar

${GS_DIR}/bin/gs.sh pu deploy --partitions=2 WebApplication ${DIRNAME}/web-application/target/web-application.war

