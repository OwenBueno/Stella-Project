export * from './habit.schema';
export * from './habit-check-in.schema';
export * from './task.schema';
export * from './calendar-event.schema';
export * from './daily-intent.schema';
export * from './evening-review.schema';
export * from './life-log.schema';
export * from './device-token.schema';

import { CalendarEvent, CalendarEventSchema } from './calendar-event.schema';
import { DailyIntent, DailyIntentSchema } from './daily-intent.schema';
import { DeviceToken, DeviceTokenSchema } from './device-token.schema';
import { EveningReview, EveningReviewSchema } from './evening-review.schema';
import { Habit, HabitSchema } from './habit.schema';
import { HabitCheckIn, HabitCheckInSchema } from './habit-check-in.schema';
import { LifeLog, LifeLogSchema } from './life-log.schema';
import { Task, TaskSchema } from './task.schema';

export const stellaModels = [
  { name: Habit.name, schema: HabitSchema },
  { name: HabitCheckIn.name, schema: HabitCheckInSchema },
  { name: Task.name, schema: TaskSchema },
  { name: CalendarEvent.name, schema: CalendarEventSchema },
  { name: DailyIntent.name, schema: DailyIntentSchema },
  { name: EveningReview.name, schema: EveningReviewSchema },
  { name: LifeLog.name, schema: LifeLogSchema },
  { name: DeviceToken.name, schema: DeviceTokenSchema },
];
