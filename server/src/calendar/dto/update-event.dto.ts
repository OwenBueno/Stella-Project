import { IsISO8601, IsOptional, IsString, IsUUID } from 'class-validator';

export class UpdateEventDto {
  @IsOptional()
  @IsString()
  title?: string;

  @IsOptional()
  @IsISO8601()
  startAt?: string;

  @IsOptional()
  @IsISO8601()
  endAt?: string;

  @IsOptional()
  @IsUUID()
  linkedTaskId?: string | null;

  @IsOptional()
  @IsString()
  recurrenceRuleJson?: string | null;

  @IsOptional()
  @IsString()
  reminderOffsetsJson?: string | null;

  @IsOptional()
  @IsISO8601()
  updatedAt?: string;

  @IsOptional()
  @IsISO8601()
  deletedAt?: string | null;
}
