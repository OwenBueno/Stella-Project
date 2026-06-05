import { IsOptional, IsString, IsUUID, MaxLength } from 'class-validator';

export class TestPushDto {
  @IsOptional()
  @IsUUID('4')
  deviceId?: string;

  @IsOptional()
  @IsString()
  @MaxLength(128)
  title?: string;

  @IsOptional()
  @IsString()
  @MaxLength(512)
  body?: string;

  @IsOptional()
  @IsString()
  @MaxLength(256)
  deepLink?: string;
}
