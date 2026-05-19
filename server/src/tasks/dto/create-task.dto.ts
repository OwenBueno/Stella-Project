import {
  IsIn,
  IsInt,
  IsISO8601,
  IsOptional,
  IsString,
  IsUUID,
  Min,
} from 'class-validator';

export class CreateTaskDto {
  @IsUUID()
  id!: string;

  @IsString()
  title!: string;

  @IsOptional()
  @IsString()
  notes?: string;

  @IsOptional()
  @IsISO8601()
  scheduledAt?: string;

  @IsOptional()
  @IsInt()
  @Min(1)
  durationMinutes?: number;

  @IsIn(['TODO', 'IN_PROGRESS', 'DONE'])
  status!: string;

  @IsOptional()
  @IsIn(['LOW', 'MEDIUM', 'HIGH'])
  priority?: string;

  @IsISO8601()
  createdAt!: string;

  @IsISO8601()
  updatedAt!: string;

  @IsOptional()
  @IsISO8601()
  deletedAt?: string;
}
