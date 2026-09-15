#!/usr/bin/env python3
"""TIDL (C7x) versus CPU inference check for AM62A model-zoo classification models.

usage: tidl_check.py <rgb224.raw> <models_dir> [tests] [debug_level] [iterations]

<rgb224.raw> is a 224x224 RGB uint8 image (resize 256, centre crop 224), the
preprocessing the models' param.yaml specify. Normalisation is folded into the
TIDL network (input_optimization: true), so the input stays uint8.
<models_dir> holds one *_tflitert_* and/or one *_onnxrt_* model-zoo directory.
tests: comma list of tfl_cpu, tfl_tidl, onnx_cpu, onnx_tidl (default: all).
"""
import glob
import os
import sys
import time
import traceback

import numpy as np

RAW, MDIR = sys.argv[1], sys.argv[2]
SEL = sys.argv[3].split(',') if len(sys.argv) > 3 else ['tfl_cpu', 'tfl_tidl', 'onnx_cpu', 'onnx_tidl']
DBG = int(sys.argv[4]) if len(sys.argv) > 4 else 0
N = int(sys.argv[5]) if len(sys.argv) > 5 else 20
IMG = np.fromfile(RAW, dtype=np.uint8).reshape(224, 224, 3)
TFL = next(iter(sorted(glob.glob(os.path.join(MDIR, '*_tflitert_*')))), '')
ONX = next(iter(sorted(glob.glob(os.path.join(MDIR, '*_onnxrt_*')))), '')


def top5(v, offset=0):
    v = np.asarray(v).reshape(-1)
    return [(int(i) + offset, round(float(v[i]), 4)) for i in np.argsort(v)[::-1][:5]]


SAME = [0]


def timed(fn, n=N):
    ref = np.array(fn(), copy=True)
    SAME[0] = 0
    t = time.perf_counter()
    for _ in range(n):
        out = fn()
        SAME[0] += bool(np.array_equal(out, ref))
    return out, (time.perf_counter() - t) / n * 1000


def tflite(use_tidl):
    import tflite_runtime.interpreter as tfl
    dl = [tfl.load_delegate('libtidl_tfl_delegate.so',
                            {'artifacts_folder': os.path.join(TFL, 'artifacts'),
                             'debug_level': DBG})] if use_tidl else []
    it = tfl.Interpreter(model_path=glob.glob(os.path.join(TFL, 'model', '*.tflite'))[0],
                         experimental_delegates=dl)
    it.allocate_tensors()
    i, o = it.get_input_details()[0], it.get_output_details()[0]
    x = IMG[np.newaxis].astype(i['dtype'])

    def f():
        it.set_tensor(i['index'], x)
        it.invoke()
        return it.get_tensor(o['index'])
    out, ms = timed(f)
    # 1001-class TFLite models reserve class 0 for background (label_offset_pred: -1)
    return top5(out, offset=-1 if out.size == 1001 else 0), ms, [os.path.basename(TFL)]


def onnx(use_tidl):
    import onnxruntime as ort
    m = glob.glob(os.path.join(ONX, 'model', '*.onnx'))[0]
    if use_tidl:
        s = ort.InferenceSession(m, providers=['TIDLExecutionProvider', 'CPUExecutionProvider'],
                                 provider_options=[{'artifacts_folder': os.path.join(ONX, 'artifacts'),
                                                    'debug_level': DBG}, {}],
                                 sess_options=ort.SessionOptions())
    else:
        s = ort.InferenceSession(m, providers=['CPUExecutionProvider'])
    name, x = s.get_inputs()[0].name, IMG.transpose(2, 0, 1)[np.newaxis].copy()
    out, ms = timed(lambda: s.run(None, {name: x})[0])
    return top5(out), ms, [os.path.basename(ONX)] + s.get_providers()


TESTS = {'tfl_cpu': ('TFLite  CPU ', tflite, False), 'tfl_tidl': ('TFLite  TIDL', tflite, True),
         'onnx_cpu': ('ONNX    CPU ', onnx, False), 'onnx_tidl': ('ONNX    TIDL', onnx, True)}
for label, fn, arg in (TESTS[s] for s in SEL):
    try:
        top, ms, info = fn(arg)
        print(f'{label}: {ms:8.2f} ms/inference over {N}, identical outputs {SAME[0]}/{N}  '
              f'top5={top}  {info}', flush=True)
    except Exception:
        print(f'{label}: FAILED', flush=True)
        traceback.print_exc()
