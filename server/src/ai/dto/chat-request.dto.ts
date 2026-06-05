import { Type } from 'class-transformer';
import {
  IsNotEmpty,
  IsObject,
  IsOptional,
  IsString,
  IsUUID,
  MaxLength,
} from 'class-validator';

export class ChatRequestDto {
  @IsOptional()
  @IsUUID('4')
  threadId?: string;

  @IsUUID('4')
  clientMessageId!: string;

  @IsString()
  @IsNotEmpty()
  @MaxLength(8000)
  message!: string;

  @IsObject()
  @Type(() => Object)
  contextSnapshot!: Record<string, unknown>;
}
