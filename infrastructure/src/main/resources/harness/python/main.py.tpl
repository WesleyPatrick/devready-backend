import json as _devready_json
import sys as _devready_sys
import time as _devready_time

_devready_out = _devready_sys.stdout
_devready_sys.stdout = _devready_sys.stderr


def _devready_main():
    cases = _devready_json.loads(_devready_sys.stdin.buffer.read().decode("utf-8"))
    for case in cases:
        output = None
        error = None
        started = _devready_time.perf_counter()
        try:
            solution = Solution()
            output = getattr(solution, "__DEVREADY_FUNCTION__")(*case["args"])
        except BaseException as exc:
            error = type(exc).__name__ + ": " + str(exc)
        elapsed = int((_devready_time.perf_counter() - started) * 1000)
        try:
            line = _devready_json.dumps(
                {"position": case["position"], "output": output, "error": error, "timeMs": elapsed},
                allow_nan=False,
            )
        except Exception as exc:
            line = _devready_json.dumps(
                {
                    "position": case["position"],
                    "output": None,
                    "error": "Saída que não vira JSON: " + str(exc),
                    "timeMs": elapsed,
                }
            )
        _devready_out.write("__DEVREADY_MARKER__" + line + "\n")
        _devready_out.flush()


_devready_main()
