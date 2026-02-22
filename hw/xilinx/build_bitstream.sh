#!/bin/bash

TARGET=$1
DEVICE=$2
UCF=$3

echo
echo "*****************************************************************************"
echo "Building design: '$TARGET' for device '$DEVICE' and UCF '$UCF'"
echo "*****************************************************************************"

source /opt/Xilinx/14.7/ISE_DS/settings64.sh

cd target

echo "**** Running xst"
xst -intstyle ise -ifn "../hw/xilinx/${TARGET}.xst" -ofn ${TARGET}.syr

echo "**** Running ngdbuild"
ngdbuild -intstyle ise -dd _ngo -nt timestamp -uc ../hw/xilinx/${UCF}.ucf -p ${DEVICE} ${TARGET}.ngc ${TARGET}.ngd

echo "**** Running map"
# Optimisation off, more predictable
#map -detail -intstyle ise -p ${DEVICE} -w -logic_opt off -ol high -t 1 -xt 1 -register_duplication off -r 4 -global_opt off -mt off -ir off -pr off -lc off -power off -o ${TARGET}_map.ncd ${TARGET}.ngd ${TARGET}.pcf

# Optimisation on, less predictable
map -detail -intstyle ise -p ${DEVICE} -w -logic_opt on -ol high -t 1 -xt 1 -register_duplication off -r 4 -global_opt off -mt off -ir off -pr b -lc off -power off -o ${TARGET}_map.ncd ${TARGET}.ngd ${TARGET}.pcf

echo "**** Running par"
par -w -intstyle ise -ol high -mt off ${TARGET}_map.ncd ${TARGET}.ncd ${TARGET}.pcf

echo "**** Running bitgen"
bitgen -intstyle ise -f ../hw/xilinx/bitgen_config.ut ${TARGET}.ncd