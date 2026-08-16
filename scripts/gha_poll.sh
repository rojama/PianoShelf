#!/usr/bin/env bash
# ============================================================
#  GitHub Actions 最新 Run 轮询监控（curl HTML，不耗 REST API 配额）
#  - 每个检测周期：下载 /actions 页面 HTML，正则提取最新 run 的状态
#  - 轮询间隔：60 秒
#  - 最长轮询：180 轮 ≈ 3 小时
#  - 退出码：
#      0 = 最新 run 已 success (BUILD 成功)
#      1 = 最新 run 已 failure (BUILD 失败)
#      2 = cancelled
#      3 = 3 小时超时仍无结论
#  - 实时输出写入：/workspace/artifacts/gha_poll.log
# ============================================================
set -u
OUT="/workspace/artifacts/gha_poll.log"
mkdir -p "$(dirname "$OUT")"
: > "$OUT"

REPO="rojama/PianoShelf"
URL="https://github.com/${REPO}/actions"
MAX_ROUNDS=180
SLEEP_SEC=60

log() {
  local ts
  ts="$(date '+%F %T')"
  printf '[%s] %s\n' "$ts" "$*" | tee -a "$OUT"
}

extract_latest_status() {
  # GitHub Actions 列表页：每一次 run 对应 1 条 <a> 链接，
  # 它的 aria-label 形如："failed:  Run 38 of Build PianoShelf APK"
  #                        "succeeded:  Run 39 of Build PianoShelf APK"
  #                        "in_progress:  Run 39 of Build PianoShelf APK"
  #                        "queued:  Run 39 of Build PianoShelf APK"
  #                        "cancelled:  Run 39 of Build PianoShelf APK"
  # 注意冒号后 + 可能存在多个空格
  local html="$1"
  local label
  label=$(printf '%s' "$html" \
    | grep -oE 'aria-label="(failed|succeeded|in_progress|queued|cancelled|skipped|action_required):[^"]*"' \
    | head -1 \
    | sed -E 's/^aria-label="//;s/:.*$//')
  [ -z "$label" ] && { echo "unknown"; return; }
  case "$label" in
    succeeded)            echo "success" ;;
    failed)               echo "failure" ;;
    in_progress|queued|action_required) echo "in_progress" ;;
    cancelled|skipped)    echo "$label" ;;
    *)                    echo "label=$label" ;;
  esac
}

extract_latest_runid() {
  local html="$1"
  local href
  href=$(printf '%s' "$html" \
    | grep -oE "/${REPO}/actions/runs/[0-9]+" \
    | head -1)
  basename "$href" 2>/dev/null || echo ""
}

extract_latest_duration() {
  # 最新一次 run 的耗时 —— " 53s"  " 1m 20s"  " 12m 3s"
  local html="$1"
  local d
  d=$(printf '%s' "$html" \
    | grep -oE '/actions/runs/[0-9]+/workflow[^<]{0,800}' \
    | head -1 \
    | grep -oE '[0-9]+h\s*[0-9]+m|[0-9]+m\s*[0-9]+s|[0-9]+m|[0-9]+s' \
    | head -1)
  echo "$d"
}

log "========== GitHub Actions 轮询监控启动 =========="
log "Repo: $REPO   URL: $URL"
log "间隔=${SLEEP_SEC}s  最多轮数=$MAX_ROUNDS  最长时长=$(( MAX_ROUNDS*SLEEP_SEC/60 )) min"
log ""
log "⚠️  说明："
log "   - 如果你还没有应用 patch 并 push，请先在有 GitHub 凭证的电脑执行："
log "       git am < /workspace/artifacts/0001-fix-ci-GitHub-Actions-v4-v5-Node-24-setup-gradle-PAT.patch"
log "       git push origin master"
log "   - Push 成功之后本次脚本会自动检测到新的 run #，轮询直到 success"
log ""

last_runid=""
last_status=""
initial_runid=""
have_seen_new_run=0

for round in $(seq 1 $MAX_ROUNDS); do
  html=$(curl -fsSL --compressed --max-time 25 -A "Mozilla/5.0 (compatible; GHA-Poll/1.0; +https://example.invalid/bot)" \
         -H "Accept: text/html,application/xhtml+xml" \
         "$URL" 2>/dev/null)
  rc=$?
  if [ $rc -ne 0 ] || [ -z "$html" ]; then
    log "round #$round: curl 下载失败(rc=$rc)，$SLEEP_SEC 秒后重试"
    sleep "$SLEEP_SEC"
    continue
  fi
  status=$(extract_latest_status "$html")
  runid=$(extract_latest_runid "$html")
  duration=$(extract_latest_duration "$html")
  title=$(printf '%s' "$html" | grep -oE '<bdi[^>]*>[^<]*</bdi>' | head -2 | sed -E 's/<[^>]+>//g' | paste -sd'|' -)

  changed=""
  if [ -z "$initial_runid" ] && [ -n "$runid" ]; then
    initial_runid="$runid"
    log "round #$round: 首次观测到的 run#=$runid (status=$status)，将作为「基线 run」—— 继续等 NEW_RUNID 出现才做成功/失败判断。"
  fi

  if [ "$runid" != "$last_runid" ] && [ -n "$runid" ]; then
    changed="${changed}[NEW_RUN#$runid]"
    # 出现 NEW_RUN，并且 NEW_RUN 不是基线 run → 已经到用户 push 触发的新 run 了，可以开始判断 exit
    if [ "$initial_runid" != "$runid" ]; then
      have_seen_new_run=1
      log "round #$round: ⭐ 检测到新触发的 run#=$runid（基线=$initial_runid），开始跟踪其状态直到 success/failure"
    fi
  fi
  if [ "$status" != "$last_status" ] && [ -n "$status" ]; then
    changed="${changed}[STATUS:$last_status→$status]"
  fi

  if [ -n "$changed" ]; then
    log "round #$round: run#=${runid:-(无)}  status=${status}  duration=${duration:-?}  title=${title}  变化=${changed}  have_seen_new_run=$have_seen_new_run"
    last_runid="$runid"
    last_status="$status"
  else
    log "round #$round: run#=$runid  status=$status duration=${duration:-?} (不变) [title=${title}] have_seen_new_run=$have_seen_new_run"
  fi

  # ====== 退出判定（关键：只有 have_seen_new_run=1 或 status=success 时才允许 exit）======
  case "$status" in
    success)
      # success 无论新旧都是成功，直接退出
      log "✅ 最新 run #$runid 构建成功！(BUILD SUCCESS)"
      exit 0
      ;;
    failure)
      if [ "$have_seen_new_run" -eq 1 ]; then
        log "❌ 新触发的 run #$runid 仍然失败！(BUILD FAILURE)"
        log "详情页：https://github.com/$REPO/actions/runs/$runid"
        exit 1
      else
        log "  (这是旧的失败 run，忽略退出；等待用户 push 产生 NEW_RUNID 才判断)"
      fi
      ;;
    cancelled)
      if [ "$have_seen_new_run" -eq 1 ]; then
        log "⛔ 新触发的 run #$runid 被取消。"
        exit 2
      else
        log "  (这是旧的 cancelled run，忽略退出)"
      fi
      ;;
  esac

  if [ $round -eq $MAX_ROUNDS ]; then
    log "⌛ 已达最大轮数（$MAX_ROUNDS 轮 ≈ 3h）仍未得最终结论，退出。最后 run#=$runid status=$status。"
    exit 3
  fi
  sleep "$SLEEP_SEC"
done
