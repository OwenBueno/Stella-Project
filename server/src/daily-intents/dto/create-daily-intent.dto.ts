import { IsArray, IsISO8601, IsString, IsUUID, ArrayMinSize } from 'class-validator';

export class CreateDailyIntentDto {
  @IsUUID() id!: string;
  @IsString() date!: string;
  @IsArray()
  @ArrayMinSize(3)
  @IsUUID('4', { each: true })
  plannedTaskIds!: string[];
  @IsISO8601() completedAt!: string;
  @IsString() nfcTagId!: string;
  @IsISO8601() updatedAt!: string;
}
