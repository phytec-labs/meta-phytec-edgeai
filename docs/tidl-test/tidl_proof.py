#!/usr/bin/env python3
"""Evidence of where inference runs: TI benchmark counters (C7x-side subgraph
timestamps and DDR traffic), ONNX Runtime per-node provider profiling, and host
CPU time versus wall time, for TIDL and CPU-only sessions.

usage: tidl_proof.py <rgb224.raw> <models_dir> <tfl|onnx> <tidl|cpu> [iterations] [hold_s]

With hold_s > 0 the session stays open for hold_s seconds after the runs, and
/tmp/tidl_ready is created so another tool can inspect the remote cores meanwhile.
"""
import glob
import json
import os
import sys
import time

import numpy as np

RAW, MDIR, RT, MODE = sys.argv[1:5]
N = int(sys.argv[5]) if len(sys.argv) > 5 else 100
HOLD = int(sys.argv[6]) if len(sys.argv) > 6 else 0
IMG = np.fromfile(RAW, np.uint8).reshape(224, 224, 3)


def run(fn):
    fn()
    w0, c0 = time.perf_counter(), time.process_time()
    for _ in range(N):
        fn()
    w, c = time.perf_counter() - w0, time.process_time() - c0
    print(f'{RT} {MODE}: wall {w / N * 1000:.2f} ms/inf, host CPU time {c / N * 1000:.2f} ms/inf '
          f'(CPU/wall {c / w:.2f})', flush=True)


def ti_bench(get):
    try:
        d = get()
    except Exception as e:
        print(f'   TI benchmark data: unavailable ({e})')
        return
    if not d:
        print('   TI benchmark data: empty')
        return
    ks = sorted(d)
    subs = sorted({k[len('ts:subgraph_'):-len('_proc_start')] for k in ks if k.endswith('_proc_start')})
    proc = sum(d[f'ts:subgraph_{s}_proc_end'] - d[f'ts:subgraph_{s}_proc_start'] for s in subs)
    tot = d.get('ts:run_end', 0) - d.get('ts:run_start', 0)
    rd = d.get('ddr:read_end', 0) - d.get('ddr:read_start', 0)
    wr = d.get('ddr:write_end', 0) - d.get('ddr:write_start', 0)
    print(f'   TI benchmark (last inference): {len(ks)} keys, TIDL subgraphs {subs}, total {tot / 1e6:.3f} ms, '
          f'subgraph proc {proc / 1e6:.3f} ms, DDR read {rd / 1e6:.2f} MB, write {wr / 1e6:.2f} MB')


if RT == 'tfl':
    import tflite_runtime.interpreter as tfl
    d = sorted(glob.glob(os.path.join(MDIR, '*_tflitert_*')))[0]
    dl = [tfl.load_delegate('libtidl_tfl_delegate.so',
                            {'artifacts_folder': os.path.join(d, 'artifacts')})] if MODE == 'tidl' else []
    it = tfl.Interpreter(model_path=glob.glob(os.path.join(d, 'model', '*.tflite'))[0],
                         experimental_delegates=dl)
    it.allocate_tensors()
    inp = it.get_input_details()[0]
    x = IMG[np.newaxis].astype(inp['dtype'])

    def f():
        it.set_tensor(inp['index'], x)
        it.invoke()
    run(f)
    ti_bench(it.get_TI_benchmark_data)
else:
    import onnxruntime as ort
    d = sorted(glob.glob(os.path.join(MDIR, '*_onnxrt_*')))[0]
    m = glob.glob(os.path.join(d, 'model', '*.onnx'))[0]
    so = ort.SessionOptions()
    so.enable_profiling = True
    so.profile_file_prefix = f'/tmp/ort_{MODE}'
    if MODE == 'tidl':
        s = ort.InferenceSession(m, providers=['TIDLExecutionProvider', 'CPUExecutionProvider'],
                                 provider_options=[{'artifacts_folder': os.path.join(d, 'artifacts')}, {}],
                                 sess_options=so)
    else:
        s = ort.InferenceSession(m, providers=['CPUExecutionProvider'], sess_options=so)
    name, x = s.get_inputs()[0].name, IMG.transpose(2, 0, 1)[np.newaxis].copy()
    run(lambda: s.run(None, {name: x}))
    ti_bench(s.get_TI_benchmark_data)
    agg = {}
    for ev in json.load(open(s.end_profiling())):
        if ev.get('cat') == 'Node' and ev.get('name', '').endswith('_kernel_time'):
            k = (ev['args'].get('provider'), ev['args'].get('op_name'))
            n, t = agg.get(k, (0, 0))
            agg[k] = (n + 1, t + ev.get('dur', 0))
    per_prov = {}
    for (p, op), (n, t) in agg.items():
        per_prov.setdefault(p, [0, 0, set()])
        per_prov[p][0] += n
        per_prov[p][1] += t
        per_prov[p][2].add(op)
    print('   ORT profile, node kernels per provider (calls, total us, op types):')
    for p, (n, t, ops) in per_prov.items():
        print(f'      {p}: {n} calls, {t} us, ops={sorted(ops)[:8]}{" ..." if len(ops) > 8 else ""}')

if HOLD:
    open('/tmp/tidl_ready', 'w').close()
    time.sleep(HOLD)
