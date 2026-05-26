import { IsIn, IsISO8601, IsNumber, IsOptional, IsString, IsUUID, Min } from 'class-validator';

export class CreateTransactionDto {
  @IsUUID()
  id!: string;

  @IsIn(['ingress', 'egress'])
  type!: 'ingress' | 'egress';

  @IsNumber()
  @Min(0)
  amount!: number;

  @IsString()
  category!: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsISO8601()
  date!: string;

  @IsOptional()
  @IsUUID()
  linkedTaskId?: string;

  @IsISO8601()
  createdAt!: string;

  @IsISO8601()
  updatedAt!: string;
}
