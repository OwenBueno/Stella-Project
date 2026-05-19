import { IsIn, IsISO8601, IsString, IsUUID } from 'class-validator';

export class UpsertCheckInDto {
  @IsUUID()
  id!: string;

  @IsUUID()
  habitId!: string;

  @IsString()
  date!: string;

  @IsIn(['DONE', 'MISSED'])
  status!: string;

  @IsISO8601()
  updatedAt!: string;
}
