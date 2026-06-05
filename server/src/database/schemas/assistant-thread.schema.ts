import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type AssistantThreadDocument = HydratedDocument<AssistantThread>;

@Schema({ ...stellaSchemaOptions, collection: 'AssistantThread' })
export class AssistantThread {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ type: String })
  title?: string;

  /** Local calendar date YYYY-MM-DD — one session per day. */
  @Prop({ type: String })
  sessionDate?: string | null;

  @Prop({ type: Date, required: true })
  createdAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;

  @Prop({ type: Date })
  deletedAt?: Date | null;
}

export const AssistantThreadSchema = SchemaFactory.createForClass(AssistantThread);
