import { IsIn, IsISO8601, IsOptional, IsString, IsUUID } from 'class-validator';

export class UpsertCheckInDto {
  @IsUUID()
  id!: string;

  @IsUUID()
  habitId!: string;

  @IsString()
  date!: string;

  @IsIn(['DONE', 'MISSED'])
  status!: string;

  @IsOptional()
  @IsISO8601()
  completedAt?: string;

  @IsISO8601()
  updatedAt!: string;
}
