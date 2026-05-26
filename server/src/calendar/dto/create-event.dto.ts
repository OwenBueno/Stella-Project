import {
  IsISO8601,
  IsOptional,
  IsString,
  IsUUID,
} from 'class-validator';

export class CreateEventDto {
  @IsUUID()
  id!: string;

  @IsString()
  title!: string;

  @IsISO8601()
  startAt!: string;

  @IsISO8601()
  endAt!: string;

  @IsOptional()
  @IsUUID()
  linkedTaskId?: string;

  @IsOptional()
  @IsString()
  recurrenceRuleJson?: string;

  @IsOptional()
  @IsString()
  reminderOffsetsJson?: string;

  @IsISO8601()
  createdAt!: string;

  @IsISO8601()
  updatedAt!: string;

  @IsOptional()
  @IsISO8601()
  deletedAt?: string;
}
