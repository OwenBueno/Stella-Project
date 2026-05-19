import { IsISO8601, IsObject, IsOptional, IsString, IsUUID } from 'class-validator';

export class CreateEveningReviewDto {
  @IsUUID() id!: string;
  @IsString() date!: string;
  @IsOptional() @IsString() plannedVsActual?: string;
  @IsOptional() @IsString() reflectionText?: string;
  @IsObject() habitGridSnapshot!: Record<string, unknown>;
  @IsISO8601() completedAt!: string;
  @IsISO8601() updatedAt!: string;
}
