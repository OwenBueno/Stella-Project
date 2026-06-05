import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type AssistantMessageDocument = HydratedDocument<AssistantMessage>;

@Schema({ ...stellaSchemaOptions, collection: 'AssistantMessage' })
export class AssistantMessage {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true })
  threadId!: string;

  @Prop({ required: true, enum: ['user', 'assistant', 'system'] })
  role!: 'user' | 'assistant' | 'system';

  @Prop({ required: true })
  content!: string;

  @Prop({ type: String })
  clientMessageId?: string;

  @Prop({ type: Object })
  metadata?: {
    proposedActions?: Array<{
      id: string;
      type: string;
      summary: string;
      payload: Record<string, unknown>;
    }>;
    actionsStatus?: 'pending' | 'applied' | 'dismissed';
    source?: string;
    coachTitle?: string;
    deepLink?: string;
    signalTypes?: string[];
    priority?: string;
  };

  @Prop({ type: Date, required: true })
  createdAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;

  @Prop({ type: Date })
  deletedAt?: Date | null;
}

export const AssistantMessageSchema = SchemaFactory.createForClass(AssistantMessage);
AssistantMessageSchema.index({ threadId: 1, createdAt: 1 });
