import { IsOptional, IsString, IsUUID, MaxLength } from 'class-validator';

export class CoachNudgeDto {
  @IsOptional()
  @IsUUID('4')
  deviceId?: string;

  @IsString()
  @MaxLength(120)
  title!: string;

  @IsString()
  @MaxLength(500)
  body!: string;

  @IsOptional()
  @IsString()
  @MaxLength(200)
  deepLink?: string;
}
