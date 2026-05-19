import { Type } from 'class-transformer';
import {
  IsArray,
  IsBoolean,
  IsInt,
  IsISO8601,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  ValidateNested,
  ArrayMinSize,
  ArrayMaxSize,
} from 'class-validator';

export class SyncHabitDto {
  @IsUUID() id!: string;
  @IsString() name!: string;
  @IsInt() sortOrder!: number;
  @IsBoolean() active!: boolean;
  @IsISO8601() createdAt!: string;
  @IsISO8601() updatedAt!: string;
  @IsOptional() @IsISO8601() deletedAt?: string;
}

export class SyncCheckInDto {
  @IsUUID() id!: string;
  @IsUUID() habitId!: string;
  @IsString() date!: string;
  @IsString() status!: string;
  @IsISO8601() updatedAt!: string;
}

export class SyncTaskDto {
  @IsUUID() id!: string;
  @IsString() title!: string;
  @IsOptional() notes?: string | null;
  @IsOptional() @IsISO8601() scheduledAt?: string | null;
  @IsOptional() durationMinutes?: number | null;
  @IsString() status!: string;
  @IsOptional() priority?: string | null;
  @IsISO8601() createdAt!: string;
  @IsISO8601() updatedAt!: string;
  @IsOptional() @IsISO8601() deletedAt?: string;
}

export class SyncEventDto {
  @IsUUID() id!: string;
  @IsString() title!: string;
  @IsISO8601() startAt!: string;
  @IsISO8601() endAt!: string;
  @IsOptional() linkedTaskId?: string | null;
  @IsISO8601() createdAt!: string;
  @IsISO8601() updatedAt!: string;
  @IsOptional() @IsISO8601() deletedAt?: string;
}

export class SyncDailyIntentDto {
  @IsUUID() id!: string;
  @IsString() date!: string;
  @IsArray()
  @ArrayMinSize(3)
  @ArrayMaxSize(3)
  @IsUUID('4', { each: true })
  top3TaskIds!: string[];
  @IsISO8601() completedAt!: string;
  @IsString() nfcTagId!: string;
  @IsISO8601() updatedAt!: string;
}

export class SyncEveningReviewDto {
  @IsUUID() id!: string;
  @IsString() date!: string;
  @IsOptional() @IsString() plannedVsActual?: string;
  @IsOptional() @IsString() reflectionText?: string;
  @IsString() habitGridSnapshot!: string;
  @IsISO8601() completedAt!: string;
  @IsISO8601() updatedAt!: string;
}

export class SyncLifeLogDto {
  @IsUUID() id!: string;
  @IsString() type!: string;
  @IsString() payload!: string;
  @IsISO8601() timestamp!: string;
  @IsISO8601() updatedAt!: string;
}

export class SyncPushDto {
  @IsString() deviceId!: string;
  @IsISO8601() pushedAt!: string;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => SyncHabitDto)
  habits!: SyncHabitDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => SyncCheckInDto)
  habitCheckIns!: SyncCheckInDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => SyncTaskDto)
  tasks!: SyncTaskDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => SyncEventDto)
  events!: SyncEventDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => SyncDailyIntentDto)
  dailyIntents!: SyncDailyIntentDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => SyncEveningReviewDto)
  eveningReviews!: SyncEveningReviewDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => SyncLifeLogDto)
  lifeLogs!: SyncLifeLogDto[];
}
