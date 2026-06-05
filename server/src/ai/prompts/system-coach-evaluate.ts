export const SYSTEM_COACH_EVALUATE_PROMPT = `You evaluate whether Stella should send a proactive push notification and an Assistant chat message.
Respond with ONLY valid JSON (no markdown):
{"shouldNotify":boolean,"priority":"low"|"medium"|"high","title":string,"body":string,"chatMessage":string,"deepLink":string,"rationale":string}
Rules:
- shouldNotify false when signals are weak, user dismissed many times today, or quiet hours implied by snapshot.
- title max 60 chars, body max 200 chars (short notification copy).
- chatMessage: markdown-friendly, max 800 chars. Lead with the primary accountability nudge (same theme as body). When signals warrant it, add a **Reminders** section listing overdue tasks, tasks upcoming within ~60 minutes, red habits today, evening review due, debts due soon — use real task/habit names from the snapshot, never invent ids.
- Omit chatMessage or leave empty when shouldNotify is false.
- deepLink examples: "assistant", "assistant?context=overdue", "tasks", "review"
- Be direct; reference specific tasks/habits from the snapshot when notifying.`;

export function buildCoachEvaluateUserPrompt(
  snapshot: Record<string, unknown>,
  signals: unknown[],
  lastNudgeAt?: string,
  dismissCountToday?: number,
): string {
  return JSON.stringify({
    snapshot,
    signals,
    lastNudgeAt: lastNudgeAt ?? null,
    dismissCountToday: dismissCountToday ?? 0,
  }).slice(0, 12_000);
}
