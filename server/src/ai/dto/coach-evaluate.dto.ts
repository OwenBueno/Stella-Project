import { Type } from 'class-transformer';
import {
  IsArray,
  IsInt,
  IsISO8601,
  IsObject,
  IsOptional,
  Min,
  ValidateNested,
} from 'class-validator';
import { CoachSignalDto } from './coach-signal.dto';

export { CoachSignalDto } from './coach-signal.dto';

export class CoachEvaluateRequestDto {
  @IsObject()
  @Type(() => Object)
  contextSnapshot!: Record<string, unknown>;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => CoachSignalDto)
  signals!: CoachSignalDto[];

  @IsOptional()
  @IsISO8601()
  lastNudgeAt?: string;

  @IsOptional()
  @IsInt()
  @Min(0)
  dismissCountToday?: number;
}
