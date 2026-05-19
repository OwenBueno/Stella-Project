import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { HydratedDocument } from 'mongoose';
import { stellaSchemaOptions } from '../schema-options';

export type HabitCheckInDocument = HydratedDocument<HabitCheckIn>;

@Schema({ ...stellaSchemaOptions, collection: 'HabitCheckIn' })
export class HabitCheckIn {
  @Prop({ type: String, required: true })
  _id!: string;

  @Prop({ required: true })
  habitId!: string;

  @Prop({ required: true })
  date!: string;

  @Prop({ required: true })
  status!: string;

  @Prop({ type: Date, required: true })
  updatedAt!: Date;
}

export const HabitCheckInSchema = SchemaFactory.createForClass(HabitCheckIn);
HabitCheckInSchema.index({ habitId: 1, date: 1 }, { unique: true });
HabitCheckInSchema.index({ updatedAt: 1 });
