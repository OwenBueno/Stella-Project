import { Logger } from '@nestjs/common';
import { AssistantActionDto } from './dto/assistant-action.dto';

const logger = new Logger('AssistantActionValidator');

const MAX_ACTIONS = 5;
const MAX_FINANCE_ACTIONS = 2;

const FINANCE_TYPES = new Set([
  'transaction.create',
  'transaction.update',
  'transaction.delete',
  'debt.create',
  'debt.update',
  'debt.resolve',
  'debt.delete',
]);

const ALLOWED_TYPES = new Set([
  'task.create',
  'task.update',
  'task.delete',
  'task.markDone',
  'task.snooze',
  'habit.create',
  'habit.rename',
  'habit.delete',
  'habit.setCheckIn',
  'event.create',
  'event.update',
  'event.delete',
  ...FINANCE_TYPES,
]);

const INGRESS_CATEGORIES = new Set([
  'Salary',
  'Gift',
  'Refund',
  'Other Income',
]);
const EGRESS_CATEGORIES = new Set([
  'Food',
  'Rent',
  'Transport',
  'Subscriptions',
  'Fine-Penalty',
  'Penalty',
  'Other Expense',
]);

const UUID_RE =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function validateAssistantActions(
  raw: unknown[],
): AssistantActionDto[] {
  if (!Array.isArray(raw)) return [];
  const valid: AssistantActionDto[] = [];
  let financeCount = 0;

  for (const item of raw.slice(0, MAX_ACTIONS)) {
    if (!item || typeof item !== 'object') continue;
    const row = item as Record<string, unknown>;
    const id = asUuid(row.id);
    const type = typeof row.type === 'string' ? row.type.trim() : '';
    const summary =
      typeof row.summary === 'string' ? row.summary.trim().slice(0, 120) : '';
    const payload =
      row.payload && typeof row.payload === 'object' && !Array.isArray(row.payload)
        ? (row.payload as Record<string, unknown>)
        : null;

    if (!id || !type || !summary || !payload || !ALLOWED_TYPES.has(type)) {
      logger.warn(`Dropped action: invalid shape or type ${type}`);
      continue;
    }

    if (FINANCE_TYPES.has(type)) {
      if (financeCount >= MAX_FINANCE_ACTIONS) {
        logger.warn(`Dropped finance action: cap ${MAX_FINANCE_ACTIONS}`);
        continue;
      }
      financeCount += 1;
    }

    const normalized = validatePayload(type, payload);
    if (!normalized) {
      logger.warn(`Dropped action ${type}: payload validation failed`);
      continue;
    }

    valid.push({ id, type, summary, payload: normalized });
  }

  return valid;
}

function validatePayload(
  type: string,
  p: Record<string, unknown>,
): Record<string, unknown> | null {
  switch (type) {
    case 'task.create':
      return taskCreate(p);
    case 'task.update':
      return taskUpdate(p);
    case 'task.delete':
    case 'task.markDone':
      return requireId(p);
    case 'task.snooze':
      return taskSnooze(p);
    case 'habit.create':
      return habitCreate(p);
    case 'habit.rename':
      return habitRename(p);
    case 'habit.delete':
      return requireId(p);
    case 'habit.setCheckIn':
      return habitSetCheckIn(p);
    case 'event.create':
      return eventCreate(p);
    case 'event.update':
      return eventUpdate(p);
    case 'event.delete':
      return requireId(p);
    case 'transaction.create':
      return transactionCreate(p);
    case 'transaction.update':
      return transactionUpdate(p);
    case 'transaction.delete':
      return requireId(p);
    case 'debt.create':
      return debtCreate(p);
    case 'debt.update':
      return debtUpdate(p);
    case 'debt.resolve':
    case 'debt.delete':
      return requireId(p);
    default:
      return null;
  }
}

function taskCreate(p: Record<string, unknown>): Record<string, unknown> | null {
  const title = asNonEmptyString(p.title, 200);
  if (!title) return null;
  const out: Record<string, unknown> = { title };
  const scheduledAt = asIsoOptional(p.scheduledAt);
  if (scheduledAt) out.scheduledAt = scheduledAt;
  const notes = asStringOptional(p.notes, 2000);
  if (notes) out.notes = notes;
  const priority = asEnum(p.priority, ['HIGH', 'NORMAL', 'LOW']);
  if (priority) out.priority = priority;
  return out;
}

function taskUpdate(p: Record<string, unknown>): Record<string, unknown> | null {
  const id = asUuid(p.id);
  if (!id) return null;
  const out: Record<string, unknown> = { id };
  const title = asStringOptional(p.title, 200);
  if (title) out.title = title;
  const scheduledAt = asIsoOptional(p.scheduledAt);
  if (scheduledAt !== undefined) out.scheduledAt = scheduledAt;
  const notes = asStringOptional(p.notes, 2000);
  if (notes !== undefined) out.notes = notes;
  const status = asEnum(p.status, ['TODO', 'IN_PROGRESS', 'DONE']);
  if (status) out.status = status;
  const priority = asEnum(p.priority, ['HIGH', 'NORMAL', 'LOW']);
  if (priority) out.priority = priority;
  if (Object.keys(out).length === 1) return null;
  return out;
}

function taskSnooze(p: Record<string, unknown>): Record<string, unknown> | null {
  const id = asUuid(p.id);
  const hours = asInt(p.hours, 1, 168);
  if (!id || hours == null) return null;
  return { id, hours };
}

function habitCreate(p: Record<string, unknown>): Record<string, unknown> | null {
  const name = asNonEmptyString(p.name, 120);
  if (!name) return null;
  return { name };
}

function habitRename(p: Record<string, unknown>): Record<string, unknown> | null {
  const id = asUuid(p.id);
  const name = asNonEmptyString(p.name, 120);
  if (!id || !name) return null;
  return { id, name };
}

function habitSetCheckIn(
  p: Record<string, unknown>,
): Record<string, unknown> | null {
  const id = asUuid(p.id);
  const status = asEnum(p.status, ['DONE', 'MISSED']);
  if (!id || !status) return null;
  const out: Record<string, unknown> = { id, status };
  const date = asDateOptional(p.date);
  if (date) out.date = date;
  return out;
}

function eventCreate(p: Record<string, unknown>): Record<string, unknown> | null {
  const title = asNonEmptyString(p.title, 200);
  const startAt = asIso(p.startAt);
  const endAt = asIso(p.endAt);
  if (!title || !startAt || !endAt) return null;
  const out: Record<string, unknown> = { title, startAt, endAt };
  const linkedTaskId = asUuid(p.linkedTaskId);
  if (linkedTaskId) out.linkedTaskId = linkedTaskId;
  return out;
}

function eventUpdate(p: Record<string, unknown>): Record<string, unknown> | null {
  const id = asUuid(p.id);
  if (!id) return null;
  const out: Record<string, unknown> = { id };
  const title = asStringOptional(p.title, 200);
  if (title) out.title = title;
  const startAt = asIsoOptional(p.startAt);
  if (startAt) out.startAt = startAt;
  const endAt = asIsoOptional(p.endAt);
  if (endAt) out.endAt = endAt;
  const linkedTaskId = asUuid(p.linkedTaskId);
  if (linkedTaskId !== undefined && p.linkedTaskId === null) {
    out.linkedTaskId = null;
  } else if (linkedTaskId) {
    out.linkedTaskId = linkedTaskId;
  }
  if (Object.keys(out).length === 1) return null;
  return out;
}

function transactionCreate(
  p: Record<string, unknown>,
): Record<string, unknown> | null {
  const type = asEnum(p.type, ['ingress', 'egress']);
  const amount = asPositiveNumber(p.amount);
  const category = asFinanceCategory(p.category, p.type);
  if (!type || amount == null || !category) return null;
  const out: Record<string, unknown> = { type, amount, category };
  const description = asStringOptional(p.description, 500);
  if (description) out.description = description;
  const date = asIsoOptional(p.date);
  if (date) out.date = date;
  return out;
}

function transactionUpdate(
  p: Record<string, unknown>,
): Record<string, unknown> | null {
  const id = asUuid(p.id);
  if (!id) return null;
  const out: Record<string, unknown> = { id };
  const type = asEnum(p.type, ['ingress', 'egress']);
  if (type) out.type = type;
  const amount = asPositiveNumber(p.amount);
  if (amount != null) out.amount = amount;
  const category = asFinanceCategory(
    p.category,
    (p.type as string) ?? 'egress',
  );
  if (category) out.category = category;
  const description = asStringOptional(p.description, 500);
  if (description !== undefined) out.description = description;
  if (Object.keys(out).length === 1) return null;
  return out;
}

function debtCreate(p: Record<string, unknown>): Record<string, unknown> | null {
  const contactName = asNonEmptyString(p.contactName, 120);
  const direction = asEnum(p.direction, ['owed_to_me', 'owed_by_me']);
  const totalAmount = asPositiveNumber(p.totalAmount);
  if (!contactName || !direction || totalAmount == null) return null;
  const out: Record<string, unknown> = {
    contactName,
    direction,
    totalAmount,
  };
  const dueDate = asIsoOptional(p.dueDate);
  if (dueDate) out.dueDate = dueDate;
  const notes = asStringOptional(p.notes, 500);
  if (notes) out.notes = notes;
  return out;
}

function debtUpdate(p: Record<string, unknown>): Record<string, unknown> | null {
  const id = asUuid(p.id);
  if (!id) return null;
  const out: Record<string, unknown> = { id };
  const contactName = asStringOptional(p.contactName, 120);
  if (contactName) out.contactName = contactName;
  const direction = asEnum(p.direction, ['owed_to_me', 'owed_by_me']);
  if (direction) out.direction = direction;
  const totalAmount = asPositiveNumber(p.totalAmount);
  if (totalAmount != null) out.totalAmount = totalAmount;
  const remainingAmount = asNonNegativeNumber(p.remainingAmount);
  if (remainingAmount != null) out.remainingAmount = remainingAmount;
  const notes = asStringOptional(p.notes, 500);
  if (notes !== undefined) out.notes = notes;
  if (Object.keys(out).length === 1) return null;
  return out;
}

function requireId(p: Record<string, unknown>): Record<string, unknown> | null {
  const id = asUuid(p.id);
  return id ? { id } : null;
}

function asUuid(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const s = v.trim();
  return UUID_RE.test(s) ? s : null;
}

function asNonEmptyString(v: unknown, max: number): string | null {
  if (typeof v !== 'string') return null;
  const s = v.trim();
  if (!s) return null;
  return s.slice(0, max);
}

function asStringOptional(v: unknown, max: number): string | undefined {
  if (v === null) return undefined;
  if (typeof v !== 'string') return undefined;
  return v.trim().slice(0, max);
}

function asEnum(v: unknown, allowed: string[]): string | null {
  if (typeof v !== 'string') return null;
  const s = v.trim();
  return allowed.includes(s) ? s : null;
}

function asInt(v: unknown, min: number, max: number): number | null {
  const n = typeof v === 'number' ? v : typeof v === 'string' ? Number(v) : NaN;
  if (!Number.isFinite(n)) return null;
  const i = Math.floor(n);
  if (i < min || i > max) return null;
  return i;
}

function asPositiveNumber(v: unknown): number | null {
  const n = typeof v === 'number' ? v : typeof v === 'string' ? Number(v) : NaN;
  if (!Number.isFinite(n) || n <= 0) return null;
  return Math.round(n * 100) / 100;
}

function asNonNegativeNumber(v: unknown): number | null {
  const n = typeof v === 'number' ? v : typeof v === 'string' ? Number(v) : NaN;
  if (!Number.isFinite(n) || n < 0) return null;
  return Math.round(n * 100) / 100;
}

function asIso(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const s = v.trim();
  if (!s || Number.isNaN(Date.parse(s))) return null;
  return s;
}

function asIsoOptional(v: unknown): string | undefined {
  if (v === null) return undefined;
  const iso = asIso(v);
  return iso ?? undefined;
}

function asDateOptional(v: unknown): string | null {
  if (typeof v !== 'string') return null;
  const s = v.trim();
  return /^\d{4}-\d{2}-\d{2}$/.test(s) ? s : null;
}

function asFinanceCategory(
  v: unknown,
  typeHint: unknown,
): string | null {
  if (typeof v !== 'string') return null;
  const cat = v.trim();
  const type =
    typeof typeHint === 'string' && typeHint === 'ingress' ? 'ingress' : 'egress';
  const allowed = type === 'ingress' ? INGRESS_CATEGORIES : EGRESS_CATEGORIES;
  return allowed.has(cat) ? cat : null;
}
