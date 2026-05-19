import { IsArray, IsISO8601, IsString, IsUUID, ArrayMinSize, ArrayMaxSize } from 'class-validator';

export class CreateDailyIntentDto {
  @IsUUID() id!: string;
  @IsString() date!: string;
  @IsArray()
  @ArrayMinSize(3)
  @ArrayMaxSize(3)
  @IsUUID('4', { each: true })
  top3TaskIds!: string[];
  @IsISO8601() completedAt!: string;
  @IsString() nfcTagId!: string;
  @IsISO8601() updatedAt!: string;
}
