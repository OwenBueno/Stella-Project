import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import {
  AssistantMessage,
  AssistantMessageSchema,
} from '../database/schemas/assistant-message.schema';
import {
  AssistantThread,
  AssistantThreadSchema,
} from '../database/schemas/assistant-thread.schema';
import { AiController } from './ai.controller';
import { AiService } from './ai.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: AssistantThread.name, schema: AssistantThreadSchema },
      { name: AssistantMessage.name, schema: AssistantMessageSchema },
    ]),
  ],
  controllers: [AiController],
  providers: [AiService],
  exports: [AiService],
})
export class AiModule {}
