#!/usr/bin/env python3
# GitHub Actions 轮询脚本：给定 run id，每 10s 拉一次 jobs/steps 状态；
# 有 job 的 step failure 就下载该 job 日志并打印失败步骤尾部；run 到 completed 退出。
import base64
import hashlib
import json
import os
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

REPO = "rojama/PianoShelf"
RUN_ID = os.environ.get("RUN_ID")
PAT = os.environ.get("PAT")
assert RUN_ID and PAT, "Need RUN_ID and PAT env vars"
AUTH = base64.b64encode(f"rojama:{PAT}".encode()).decode()
HEADERS = {
    "Authorization": f"Basic {AUTH}",
    "Accept": "application/vnd.github+json",
}
BASE = f"https://api.github.com/repos/{REPO}"
RUN_URL = f"{BASE}/actions/runs/{RUN_ID}"
JOBS_URL = f"{BASE}/actions/runs/{RUN_ID}/jobs?filter=latest&per_page=30"
DEADLINE = time.time() + int(os.environ.get("MAX_SECONDS", "1500"))


def http_json(url, timeout=20):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read().decode("utf-8", "replace"))


def http_raw_to_file(url, dest, timeout=90):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=timeout) as r, open(dest, "wb") as f:
        while True:
            chunk = r.read(1024 * 256)
            if not chunk:
                break
            f.write(chunk)


def jobs_hash(j):
    s = ""
    for job in j.get("jobs", []) or []:
        s += f"{job.get('status')}|{job.get('conclusion')}|"
        for st in job.get("steps") or []:
            s += f"{st.get('number')}|{st.get('status')}|{st.get('conclusion')}|"
    return hashlib.md5(s.encode()).hexdigest()[:10]


def print_jobs(j):
    for job in j.get("jobs", []) or []:
        parts = []
        mark_tab = {"success": "✓", "skipped": "·", "failure": "✗",
                    "in_progress": "●", "running": "●"}
        for st in job.get("steps") or []:
            c = st.get("conclusion") if st.get("status") == "completed" else st.get("status")
            parts.append(f"{st.get('number')}{mark_tab.get(c or '', '○')}")
        runner = job.get("runner_name") or ""
        print(f"  job={job.get('name','')[:52]!r:<55} status={job.get('status',''):<10} "
              f"conclusion={str(job.get('conclusion','')):<10} runner={runner}")
        print(f"       steps: {' '.join(parts)}")
        for st in job.get("steps") or []:
            if st.get("conclusion") == "failure":
                print(f"       ✗ FAILED step {st.get('number')}: {st.get('name')}")


def first_failed_step(j):
    for job in j.get("jobs", []) or []:
        for st in job.get("steps") or []:
            if st.get("conclusion") == "failure":
                return (job.get("id"), st.get("name"))
    return None


def print_failed_step_tail(job_id, step_name, tail_lines=400):
    import tempfile
    with tempfile.TemporaryDirectory() as td:
        zp = Path(td) / "logs.zip"
        ext = Path(td) / "ext"
        try:
            http_raw_to_file(f"{BASE}/actions/jobs/{job_id}/logs", zp, timeout=120)
        except Exception as e:
            print(f"   (could not fetch job logs: {e})")
            return
        try:
            with zipfile.ZipFile(zp) as z:
                z.extractall(ext)
        except Exception as e:
            print(f"   (unzip logs failed: {e})")
            return
        chosen = None
        for f in sorted(ext.rglob("*.txt")):
            try:
                txt = f.read_text(errors="replace")
            except Exception:
                continue
            if step_name and step_name in txt:
                chosen = f
                break
        if chosen is None:
            for f in sorted(ext.rglob("*.txt")):
                if f.name.startswith("1_"):
                    chosen = f
                    break
        if chosen is None:
            files = sorted(ext.rglob("*.txt"))
            if files:
                chosen = files[0]
        print(f"   using log file: {chosen}")
        if chosen is not None:
            try:
                lines = chosen.read_text(errors="replace").splitlines()
            except Exception:
                lines = []
            print("---- BEGIN (tail) ----")
            for ln in lines[-tail_lines:]:
                print(ln)
            print("---- END ----")


start = time.time()
prev_h = ""
last_fail_key = None

while True:
    now = time.time()
    if now >= DEADLINE:
        print("TIMEOUT"); sys.exit(2)
    try:
        rs = http_json(RUN_URL, 18)
        js = http_json(JOBS_URL, 18)
    except Exception as e:
        print(f"[{time.strftime('%H:%M:%S')}] fetch error: {e}")
        time.sleep(12); continue

    status = rs.get("status") or ""
    conc = rs.get("conclusion")
    h = (status or "") + "|" + (str(conc) if conc is not None else "") + "|" + jobs_hash(js)
    if h != prev_h:
        elapsed = int(now - start)
        maxs = int(DEADLINE - start)
        print(f"[{time.strftime('%H:%M:%S')}] RUN id={RUN_ID} status={status:<10} "
              f"conclusion={str(conc):<10} elapsed={elapsed:>4}s / max={maxs:>4}s")
        print(f"   URL: https://github.com/{REPO}/actions/runs/{RUN_ID}")
        print_jobs(js)
        prev_h = h

    failed = first_failed_step(js)
    if failed:
        k = f"{failed[0]}|{failed[1]}"
        if k != last_fail_key:
            last_fail_key = k
            print(f"\n--- FETCH FAILED STEP LOG: job={failed[0]} step={failed[1]} ---")
            print_failed_step_tail(failed[0], failed[1])
            print()

    if status == "completed":
        print("\n=== RUN COMPLETED ===")
        for k in ["id", "event", "status", "conclusion", "head_branch",
                  "html_url", "created_at", "updated_at"]:
            print(f"  {k}= {rs.get(k)}")
        sha = (rs.get("head_sha") or "")[:7]
        print(f"  head_sha[:7]= {sha}")
        sys.exit(0 if str(conc) == "success" else 1)

    time.sleep(10)
