#!/usr/bin/env bash
set +e
# 专用 Token 轮询脚本 - 跟踪指定 RUN_ID，直到 success / failure / 超时
cd /workspace
export GH_TK="${1:-}"
export RUN_ID="${2:-}"
export REPO="${3:-rojama/PianoShelf}"
export SLEEP="${4:-60}"
export MAX_ROUNDS="${5:-180}"
export LOG="/workspace/artifacts/gha_poll_token.log"
: > "$LOG"
[ -z "$GH_TK" -o -z "$RUN_ID" ] && { echo "usage: $0 <GH_TOKEN> <RUN_ID> [repo] [sleep_s] [max_rounds]"; exit 99; }

t() { date '+%Y-%m-%d %H:%M:%S'; }
log() { echo "[$(t)] $*" | tee -a "$LOG"; }

log "========== Token 认证 REST API 轮询启动 =========="
log "Repo=$REPO RunID=$RUN_ID 每${SLEEP}s 最多 $MAX_ROUNDS 轮 ≈ 3h"

for round in $(seq 1 "$MAX_ROUNDS"); do
  RAW=$(curl -sS -m 20 \
    -H "Authorization: token ${GH_TK}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID" 2>/dev/null)
  STATUS=$(python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('status',''))" <<< "$RAW")
  CONC=$(python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('conclusion',''))" <<< "$RAW")
  NR=$(python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('run_number',''))" <<< "$RAW")
  SHA=$(python3 -c "import sys,json;d=json.load(sys.stdin);s=d.get('head_sha','');print(s[:8])" <<< "$RAW")

  STEP_INFO=""
  if [ "$STATUS" = "in_progress" -o "$STATUS" = "queued" ]; then
    JOBS=$(curl -sS -m 20 \
      -H "Authorization: token ${GH_TK}" \
      -H "Accept: application/vnd.github+json" \
      "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/jobs" 2>/dev/null)
    STEP_INFO=$(printf '%s' "$JOBS" | python3 -c '
import sys, json
try:
    d = json.load(sys.stdin)
    for j in d.get("jobs",[])[:3]:
        parts=[]
        for s in j.get("steps",[]):
            c = s.get("conclusion") or s.get("status","?")
            if c == "success": parts.append("+")
            elif c == "failure": parts.append("X")
            elif c == "skipped": parts.append("-")
            elif c == "in_progress": parts.append(">")
            elif c == "queued": parts.append(".")
            else: parts.append("?")
        name=(j.get("name") or "")[:20]
        status=(j.get("conclusion") or j.get("status") or "?")
        print(f"  [{name:<20}] {' '.join(parts)} | {status}")
except Exception as e:
    print(f"  (jobs parse err: {e})")
')
  fi

  log "round #$round → run #$NR (id=$RUN_ID commit=$SHA)  status=$STATUS  conclusion=$CONC"
  if [ -n "$STEP_INFO" ]; then printf '%s\n' "$STEP_INFO" | tee -a "$LOG"; fi

  if [ "$STATUS" = "completed" ]; then
    case "$CONC" in
      success)
        log "✅✅✅ RUN #$NR ($RUN_ID) CONCLUSION = SUCCESS (BUILD SUCCESSFUL)"
        ARTS=$(curl -sS -m 20 \
          -H "Authorization: token ${GH_TK}" \
          -H "Accept: application/vnd.github+json" \
          "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/artifacts" 2>/dev/null)
        CNT=$(printf '%s' "$ARTS" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('total_count',0))")
        log "Artifacts total=$CNT"
        printf '%s' "$ARTS" | python3 -c '
import json, sys
d = json.load(sys.stdin)
for a in d.get("artifacts",[]):
    size=a.get("size_in_bytes",0)
    n=a["name"]
    i=a["id"]
    ex=a.get("expired","?")
    print(f"   📦 {n:<30} id={i:<10} size={size:>10}B expired={ex} dl=POST /actions/artifacts/{i}/zip")
' | tee -a "$LOG"
        log "APK 详情页：https://github.com/$REPO/actions/runs/$RUN_ID"
        exit 0
        ;;
      failure)
        log "❌❌❌ RUN #$NR ($RUN_ID) CONCLUSION = FAILURE (BUILD FAILED)"
        OUTF="/tmp/run${NR}_fail_logs.tgz"
        curl -sS -L -m 60 \
          -H "Authorization: token ${GH_TK}" \
          -H "Accept: application/vnd.github+json" \
          "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/logs" -o "$OUTF" 2>/dev/null
        SZ=$(wc -c < "$OUTF")
        log "失败日志已保存：$OUTF ($SZ bytes)"
        log "查看详情：https://github.com/$REPO/actions/runs/$RUN_ID"
        exit 1
        ;;
      cancelled|skipped|timed_out|action_required)
        log "⛔ RUN #$NR ($RUN_ID) conclusion = $CONC (非正常结束)"
        exit 2
        ;;
      *)
        log "? 未知 conclusion=$CONC"
        ;;
    esac
  fi

  if [ $round -eq $MAX_ROUNDS ]; then
    log "⌛ 已达 $MAX_ROUNDS 轮仍未 completed，退出。last status=$STATUS"
    exit 3
  fi
  sleep "$SLEEP"
done
