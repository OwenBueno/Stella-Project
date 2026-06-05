import { config } from 'dotenv';
import { resolve } from 'path';
import mongoose from 'mongoose';

config({ path: resolve(__dirname, '../.env') });

/** Mongo collection names — must match @Schema collection values in src/database/schemas. */
export const STELLA_COLLECTIONS = [
  'Habit',
  'HabitCheckIn',
  'Task',
  'CalendarEvent',
  'DailyIntent',
  'EveningReview',
  'LifeLog',
  'DeviceToken',
  'Transaction',
  'Debt',
  'AssistantThread',
  'AssistantMessage',
] as const;

export type StellaCollection = (typeof STELLA_COLLECTIONS)[number];

export function getDatabaseUrl(): string {
  return (
    process.env.DATABASE_URL ??
    'mongodb://127.0.0.1:27017/stella?retryWrites=false&directConnection=true'
  );
}

export async function connectDatabase(): Promise<typeof mongoose> {
  const url = getDatabaseUrl();
  await mongoose.connect(url);
  return mongoose;
}

export async function disconnectDatabase(): Promise<void> {
  await mongoose.disconnect();
}

export function uuid(): string {
  return crypto.randomUUID();
}

export function dateKey(d: Date): string {
  return d.toISOString().slice(0, 10);
}

export function iso(d: Date): string {
  return d.toISOString();
}

export function addDays(d: Date, days: number): Date {
  const copy = new Date(d);
  copy.setUTCDate(copy.getUTCDate() + days);
  return copy;
}

export function addMinutes(d: Date, minutes: number): Date {
  return new Date(d.getTime() + minutes * 60_000);
}

export function atLocalTime(base: Date, hour: number, minute = 0): Date {
  const copy = new Date(base);
  copy.setHours(hour, minute, 0, 0);
  return copy;
}

export function mondayOfWeek(d: Date): string {
  const copy = new Date(d);
  const day = copy.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  copy.setDate(copy.getDate() + diff);
  return dateKey(copy);
}
