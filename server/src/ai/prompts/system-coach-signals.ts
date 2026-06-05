import { COACH_SIGNAL_TYPES } from '../dto/coach-signal.dto';

export const SYSTEM_COACH_SIGNALS_PROMPT = `You analyze a Stella life-OS snapshot and detect coaching signals.
Respond with ONLY valid JSON (no markdown):
{"signals":[{"type":string,"taskId":string|null,"habitId":string|null,"severity":"low"|"medium"|"high"}]}
Allowed type values: ${COACH_SIGNAL_TYPES.join(', ')}.
Rules:
- Include taskId for TASK_OVERDUE and TASK_UPCOMING when a specific task applies.
- Emit TASK_UPCOMING for tasks scheduled within the next 60 minutes (not yet overdue).
- Include habitId for HABIT_RED_TODAY when a specific habit applies.
- Use severity high for overdue tasks, missing daily intent, evening review due.
- Return empty signals array when nothing actionable stands out.
- Do not invent task or habit ids — only use ids present in the snapshot.`;

export function buildCoachSignalsUserPrompt(
  snapshot: Record<string, unknown>,
  knownSignalTypes?: string[],
): string {
  return JSON.stringify({
    snapshot,
    knownSignalTypes: knownSignalTypes ?? COACH_SIGNAL_TYPES,
  }).slice(0, 12_000);
}
