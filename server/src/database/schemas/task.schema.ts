import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type TaskDocument = HydratedDocument<Task>;

@Schema({ ...stellaSchemaOptions, collection: 'Task' })
export class Task {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true })
  title!: string;

  @Prop({ type: String, default: null })
  notes!: string | null;

  @Prop({ type: Date, default: null })
  scheduledAt!: Date | null;

  @Prop({ type: Number, default: null })
  durationMinutes!: number | null;

  @Prop({ required: true })
  status!: string;

  @Prop({ type: Number, default: 0 })
  sortOrder!: number;

  @Prop({ type: String, default: null })
  priority!: string | null;

  @Prop({ type: Date, required: true })
  createdAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;

  @Prop({ type: Date, default: null })
  deletedAt!: Date | null;
}

export const TaskSchema = SchemaFactory.createForClass(Task);
TaskSchema.index({ updatedAt: 1 });
