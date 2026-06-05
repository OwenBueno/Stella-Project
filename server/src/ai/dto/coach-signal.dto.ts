import { IsIn, IsOptional, IsString } from 'class-validator';

export const COACH_SIGNAL_TYPES = [
  'TASK_OVERDUE',
  'TASK_UPCOMING',
  'HABIT_RED_TODAY',
  'NO_DAILY_INTENT',
  'EVENING_REVIEW_DUE',
  'DEBT_DUE_SOON',
  'SYNC_STALE',
] as const;

export type CoachSignalType = (typeof COACH_SIGNAL_TYPES)[number];

export class CoachSignalDto {
  @IsString()
  @IsIn([...COACH_SIGNAL_TYPES])
  type!: CoachSignalType;

  @IsOptional()
  @IsString()
  taskId?: string;

  @IsOptional()
  @IsString()
  habitId?: string;

  @IsOptional()
  @IsString()
  @IsIn(['low', 'medium', 'high'])
  severity?: string;
}
