import { Type } from 'class-transformer';
import { IsArray, IsIn, IsObject, IsOptional, IsString } from 'class-validator';
import { COACH_SIGNAL_TYPES } from './coach-signal.dto';

export class CoachSignalsRequestDto {
  @IsObject()
  @Type(() => Object)
  contextSnapshot!: Record<string, unknown>;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  @IsIn([...COACH_SIGNAL_TYPES], { each: true })
  knownSignalTypes?: string[];
}

export class CoachSignalsResponseDto {
  signals!: {
    type: string;
    taskId?: string;
    habitId?: string;
    severity?: string;
  }[];
}
