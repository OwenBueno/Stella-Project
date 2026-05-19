import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type CalendarEventDocument = HydratedDocument<CalendarEvent>;

@Schema({ ...stellaSchemaOptions, collection: 'CalendarEvent' })
export class CalendarEvent {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true })
  title!: string;

  @Prop({ type: Date, required: true })
  startAt!: Date;

  @Prop({ type: Date, required: true })
  endAt!: Date;

  @Prop({ type: String, default: null })
  linkedTaskId!: string | null;

  @Prop({ type: Date, required: true })
  createdAt!: Date;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;

  @Prop({ type: Date, default: null })
  deletedAt!: Date | null;
}

export const CalendarEventSchema = SchemaFactory.createForClass(CalendarEvent);
CalendarEventSchema.index({ updatedAt: 1 });
