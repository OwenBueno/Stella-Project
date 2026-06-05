import type { Db } from 'mongodb';

export interface SeedOptions {
  cleanFirst: boolean;
  seed: number;
  fromKey: string;
  toKey: string;
  minDocs: number;
}

export interface DateRange {
  from: Date;
  to: Date;
  fromKey: string;
  toKey: string;
}

export interface SeedContext {
  now: Date;
  today: string;
  range: DateRange;
  rng: () => number;
  options: SeedOptions;
}

export interface HabitRecord {
  _id: string;
  name: string;
  sortOrder: number;
  active: boolean;
  createdAt: Date;
  updatedAt: Date;
  deletedAt: null;
}

export interface TaskRecord {
  _id: string;
  title: string;
  notes: string | null;
  scheduledAt: Date | null;
  durationMinutes: number | null;
  status: string;
  sortOrder: number;
  priority: string | null;
  createdAt: Date;
  updatedAt: Date;
  deletedAt: null;
  dayKey: string;
}

export interface DailyIntentRecord {
  _id: string;
  date: string;
  plannedTaskIds: string[];
  completedAt: Date;
  nfcTagId: string;
  updatedAt: Date;
}

export interface GeneratedSeedData {
  habits: Record<string, unknown>[];
  habitCheckIns: Record<string, unknown>[];
  tasks: Record<string, unknown>[];
  calendarEvents: Record<string, unknown>[];
  dailyIntents: Record<string, unknown>[];
  eveningReviews: Record<string, unknown>[];
  transactions: Record<string, unknown>[];
  debts: Record<string, unknown>[];
  assistantThreads: Record<string, unknown>[];
  assistantMessages: Record<string, unknown>[];
  lifeLogs: Record<string, unknown>[];
}

export interface TodayFixtureIds {
  habitIds: {
    morning: string;
    exercise: string;
    reading: string;
    meditation: string;
  };
  taskIds: {
    overdue: string;
    upcoming: string;
    deepWork: string;
    admin: string;
    review: string;
    done: string;
  };
  threadId: string;
  userMessageId: string;
  coachMessageId: string;
  dailyIntentId: string;
  eveningReviewId: string;
  calendarEventId: string;
  debtId: string;
  yesterdayThreadId: string;
}

export interface TodayFixtures extends GeneratedSeedData {
  ids: TodayFixtureIds;
}

export interface CollectionInsert {
  collection: string;
  docs: Record<string, unknown>[];
}

export type SeedDb = Db;
