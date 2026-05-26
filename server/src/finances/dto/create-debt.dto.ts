import {
  IsBoolean,
  IsIn,
  IsISO8601,
  IsNumber,
  IsOptional,
  IsString,
  IsUUID,
  Min,
} from 'class-validator';

export class CreateDebtDto {
  @IsUUID()
  id!: string;

  @IsString()
  contactName!: string;

  @IsIn(['owed_to_me', 'owed_by_me'])
  direction!: 'owed_to_me' | 'owed_by_me';

  @IsNumber()
  @Min(0)
  totalAmount!: number;

  @IsOptional()
  @IsISO8601()
  dueDate?: string;

  @IsOptional()
  @IsString()
  notes?: string;

  @IsISO8601()
  createdAt!: string;

  @IsISO8601()
  updatedAt!: string;
}

export class UpdateDebtDto {
  @IsOptional()
  @IsNumber()
  @Min(0)
  remainingAmount?: number;

  @IsOptional()
  @IsBoolean()
  isResolved?: boolean;

  @IsOptional()
  @IsString()
  notes?: string;

  @IsISO8601()
  updatedAt!: string;
}

export class CreatePenaltyDto {
  @IsUUID()
  taskId!: string;

  @IsNumber()
  @Min(0)
  amount!: number;

  @IsOptional()
  @IsString()
  description?: string;
}
