#!/bin/sh
# Negative control: TIDL inference must fail with the C7x stopped, CPU inference
# must still work, and TIDL must work again once the C7x is restarted.
# Run on target as root from the directory holding tidl_check.py and cat224.raw.
# usage: negative_control.sh <models_dir>
M=${1:?models dir}
R=""
for r in /sys/class/remoteproc/remoteproc*; do [ "$(cat $r/name)" = "7e000000.dsp" ] && R=$r; done
[ -n "$R" ] || { echo "C7x remoteproc (7e000000.dsp) not found"; exit 1; }
echo "C7x: $R state=$(cat $R/state)"

echo stop > $R/state; sleep 2; echo "after stop: $(cat $R/state)"
for t in tfl_tidl onnx_tidl tfl_cpu; do
    timeout 90 python3 tidl_check.py cat224.raw "$M" $t 0 20 > /tmp/nc_$t.log 2>&1
    echo "== $t with C7x stopped: exit=$? (expect TIDL to fail, CPU to pass)"
    grep -E "^(TFLite|ONNX)|FAILED|IPC init failed|dumped" /tmp/nc_$t.log | head -3
done

echo start > $R/state; sleep 6; echo "after start: $(cat $R/state)"
timeout 120 python3 tidl_check.py cat224.raw "$M" tfl_tidl,onnx_tidl 0 20 2>&1 | grep -E "^(TFLite|ONNX)|FAILED"
