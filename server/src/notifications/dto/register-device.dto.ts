import { IsNotEmpty, IsString, IsUUID, MaxLength } from 'class-validator';

export class RegisterDeviceDto {
  @IsUUID('4')
  deviceId!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(4096)
  fcmToken!: string;
}
