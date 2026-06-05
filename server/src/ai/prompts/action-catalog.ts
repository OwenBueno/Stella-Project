export const ACTION_CATALOG_PROMPT = `
Allowed action types and payloads (max 5 actions per reply; max 2 finance actions):

Tasks:
- task.create: { title (required), scheduledAt?, notes?, priority? ("HIGH"|"NORMAL"|"LOW") }
- task.update: { id (UUID from snapshot), title?, scheduledAt?, notes?, status? ("TODO"|"IN_PROGRESS"|"DONE"), priority? }
- task.delete: { id }
- task.markDone: { id }
- task.snooze: { id, hours (1-168) }

Habits:
- habit.create: { name }
- habit.rename: { id, name }
- habit.delete: { id }
- habit.setCheckIn: { id, date? (YYYY-MM-DD, default today), status ("DONE"|"MISSED") }

Calendar:
- event.create: { title, startAt, endAt (ISO-8601), linkedTaskId? }
- event.update: { id, title?, startAt?, endAt?, linkedTaskId? }
- event.delete: { id }

Finances:
- transaction.create: { type ("ingress"|"egress"), amount (>0), category, description?, date? (ISO-8601) }
- transaction.update: { id, type?, amount?, category?, description? }
- transaction.delete: { id }
- debt.create: { contactName, direction ("owed_to_me"|"owed_by_me"), totalAmount (>0), dueDate?, notes? }
- debt.update: { id, contactName?, direction?, totalAmount?, remainingAmount?, notes? }
- debt.resolve: { id }
- debt.delete: { id }

Rules:
- For update/delete/markDone/snooze/resolve: id MUST appear in the context snapshot.
- For create: do NOT include entity id (client generates).
- Use "proposed" language in reply; user must tap Apply in the app.
- Do not propose morning lock, focus, takeover, penalties, or bulk deletes.
Each action object: { "id": "<uuid>", "type": "<type>", "summary": "<short label>", "payload": { ... } }
`;
